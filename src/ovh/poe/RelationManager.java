package ovh.poe;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private Gson gson = Main.getGson();

    public Map<String, String> indexToName = new HashMap<>();
    public Map<String, String> nameToIndex = new HashMap<>();
    public Map<String, String> aliasToIndex = new HashMap<>();
    public Map<String, String> aliasToName = new HashMap<>();

    public Map<String, Integer> nameToIconIndex = new HashMap<>();
    public Map<Integer, Mappers.IconRelation> iconIndexToIcon = new TreeMap<>();

    public Map<String, List<String>> categories = new HashMap<>();
    public List<String> leagues = new ArrayList<>();

    /**
     * Reads currency and icon relation data from file on object init
     */
    public RelationManager() {
        readCurrencyRelationsFromFile();
        readIconsFromFile();
    }

    /**
     * Downloads a list of active leagues from pathofexile.com and appends them to list
     */
    public void getLeagueList() {
        List<Mappers.LeagueListElement> leagueList = null;
        InputStream stream = null;

        try {
            // Define the request
            URL request = new URL("http://api.pathofexile.com/leagues?type=main&compact=1");
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(Main.CONFIG.readTimeOut);
            connection.setConnectTimeout(Main.CONFIG.connectTimeOut);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Define some elements
            StringBuilder stringBuilderBuffer = new StringBuilder();
            byte[] byteBuffer = new byte[128];
            int byteCount;

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, Main.CONFIG.downloadChunkSize)) != -1) {
                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != Main.CONFIG.downloadChunkSize) {
                    byte[] trimmedByteBuffer = new byte[byteCount];
                    System.arraycopy(byteBuffer, 0, trimmedByteBuffer, 0, byteCount);

                    // Trim byteBuffer, convert it into string and add to string buffer
                    stringBuilderBuffer.append(new String(trimmedByteBuffer));
                } else {
                    stringBuilderBuffer.append(new String(byteBuffer));
                }
            }

            // Attempt to parse league list
            Type listType = new TypeToken<List<Mappers.LeagueListElement>>(){}.getType();
            leagueList = gson.fromJson(stringBuilderBuffer.toString(), listType);
        } catch (Exception ex) {
            System.out.println("[Error] Failed to download league list");
            ex.printStackTrace();
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // If download was unsuccessful, return
        if (leagueList == null || leagueList.size() < 3) return;

        // Clear and fill list
        leagues.clear();
        for (Mappers.LeagueListElement element : leagueList) {
            if (!element.id.contains("SSF")) leagues.add(element.id);
        }
    }

    /**
     * Reads currency relation data from file
     */
    private void readCurrencyRelationsFromFile() {
        File file = new File("./currencyRelations.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<ArrayList<Mappers.CurrencyRelation>>(){}.getType();
            List<Mappers.CurrencyRelation> relations = gson.fromJson(reader, listType);

            for (Mappers.CurrencyRelation relation : relations) {
                indexToName.put(relation.index, relation.name);
                nameToIndex.put(relation.name, relation.index);

                for (String alias : relation.aliases) {
                    aliasToIndex.put(alias, relation.index);
                    aliasToName.put(alias, relation.name);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Reads icon relation data from file
     */
    private void readIconsFromFile() {
        File file = new File("./iconRelations.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<HashMap<Integer, Mappers.IconRelation>>(){}.getType();
            HashMap<Integer, Mappers.IconRelation> relations = gson.fromJson(reader, listType);

            // Lambda loop
            relations.forEach((index, item) -> {
                nameToIconIndex.put(item.name, index);
                iconIndexToIcon.put(index, item);
            });

        } catch (IOException ex) {
            // Doesn't matter if the file exists or not. It will be written to later.
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        // Save icon relations to file
        File iconFile = new File("./iconRelations.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(iconFile), "UTF-8"))) {
            gson.toJson(iconIndexToIcon, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to icoRelations.json");
            ex.printStackTrace();
        }

        // Save item categories to file
        File categoryFile = new File("./categories.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(categoryFile), "UTF-8"))) {
            gson.toJson(categories, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to categories.json");
            ex.printStackTrace();
        }

        // Get a list of leagues from pathofexile.com
        getLeagueList();

        // Save leagues to file
        File leagueFile = new File("./leagues.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(leagueFile), "UTF-8"))) {
            gson.toJson(leagues, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to leagues.json");
            ex.printStackTrace();
        }
    }

    /**
     * Provides an interface for saving and retrieving icons and indexes
     *
     * @param key Item's key to add
     * @param icon Item's image url to add
     * @return Generated index of added url
     */
    public int addIcon(String key, String icon) {
        // If icon is already present, return icon index
        if (nameToIconIndex.containsKey(key)) return nameToIconIndex.get(key);

        // Get "?"'s index in url
        int tempIndex = (icon.contains("?") ? icon.indexOf("?") : icon.length());
        // Get everything before "?"
        icon = icon.substring(0, tempIndex);

        // Otherwise add to map and return icon index
        int index = nameToIconIndex.size();
        nameToIconIndex.put(key, index);
        iconIndexToIcon.put(index, new Mappers.IconRelation(key, icon));
        return index;
    }

    /**
     * Manages item category list
     *
     * @param parentCategory Parent category of item (e.g. "armour" or "flasks")
     * @param childCategory Child category of item (e.g. "gloves" or null)
     */
    public void addCategory(String parentCategory, String childCategory) {
        List<String> childCategories = categories.getOrDefault(parentCategory, new ArrayList<>());

        if (childCategory != null && !childCategories.contains(childCategory)) childCategories.add(childCategory);

        categories.putIfAbsent(parentCategory, childCategories);
    }

    /**
     * Manages league list
     *
     * @param league Item's league
     */
    public void addLeague(String league) {
        if (!leagues.contains(league)) leagues.add(league);
    }
}
