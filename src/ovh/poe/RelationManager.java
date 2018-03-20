package ovh.poe;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ovh.poe.Pricer.DataEntry;

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

    public Map<String, String> currencyIndexToName = new HashMap<>();
    public Map<String, String> currencyNameToIndex = new HashMap<>();
    public Map<String, String> currencyAliasToIndex = new HashMap<>();
    public Map<String, String> currencyAliasToName = new HashMap<>();

    public Map<String, Integer> itemKeyToIndex = new HashMap<>();
    public Map<Integer, Mappers.IndexedItem> itemIndexToData = new TreeMap<>();

    public Map<String, List<String>> categories = new HashMap<>();
    public List<String> leagues = new ArrayList<>();

    /**
     * Reads currency and item data from file on object init
     */
    public RelationManager() {
        readCurrencyRelationsFromFile();
        readItemDataFromFile();
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

        // Sort the list for aesthetic purposes
        String[] tempList = new String[leagues.size()];
        int counter = 0;
        for (String league : leagues) {
            if (league.equals("Hardcore")) tempList[leagues.size() - 1] = league;
            else if (league.equals("Standard")) tempList[leagues.size() - 2] = league;
            else if (league.contains("Hardcore ")) tempList[leagues.size() - 3] = league;
            else {
                tempList[counter] = league;
                counter++;
            }
        }

        // Write the new list to the global var
        leagues.clear();
        leagues.addAll(Arrays.asList(tempList));
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
                currencyIndexToName.put(relation.index, relation.name);
                currencyNameToIndex.put(relation.name, relation.index);

                for (String alias : relation.aliases) {
                    currencyAliasToIndex.put(alias, relation.index);
                    currencyAliasToName.put(alias, relation.name);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Reads item relation data from file
     */
    private void readItemDataFromFile() {
        File file = new File("./itemData.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<HashMap<Integer, Mappers.IndexedItem>>(){}.getType();
            HashMap<Integer, Mappers.IndexedItem> relations = gson.fromJson(reader, listType);

            // Lambda loop
            relations.forEach((index, item) -> {
                itemKeyToIndex.put(item.name, index);
                itemIndexToData.put(index, item);
            });

        } catch (IOException ex) {
            // Doesn't matter if the file exists or not. It will be written to later.
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        // Save item relations to file
        File itemFile = new File("./itemData.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(itemFile), "UTF-8"))) {
            gson.toJson(itemIndexToData, writer);
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
     * Provides an interface for saving and retrieving item data and indexes
     *
     * @param item Item object to index
     * @return Generated index of added url
     */
    public int indexItem(Item item) {
        if (item.icon == null) return -1;

        // Generalize key
        String key = resolveSpecificKey(item.key);
        // If icon is already present, return icon index
        if (itemKeyToIndex.containsKey(key)) return itemKeyToIndex.get(key);

        // Create an instance of IndexedItem
        Mappers.IndexedItem indexedItem = new Mappers.IndexedItem();
        indexedItem.add(item);

        // Add IndexedItem instance to maps and return it's index for storage
        int index = itemKeyToIndex.size();
        itemKeyToIndex.put(key, index);
        itemIndexToData.put(index, indexedItem);
        return index;
    }

    /**
     * Attempts to find the item's index
     *
     * @param key DataEntry's database key
     * @return Index or -1, if missing
     */
    public int getIndex(String key) {
        String generalizedKey = resolveSpecificKey(key);
        return itemKeyToIndex.getOrDefault(generalizedKey, -1);
    }

    /**
     * Generalizes a specific key. E.g: "Standard|gems:activegem|Flame Dash|4|l:10|q:20|c:0"
     * is turned into: "gems:activegem|Flame Dash|4"
     *
     * @return Generalized item key
     */
    public String resolveSpecificKey(String key) {
        // "Hardcore Bestiary|armour:chest|Shroud of the Lightless:Carnal Armour|3|var:1 socket"

        StringBuilder genericKey = new StringBuilder();
        String[] splitKey = key.split("\\|");

        // Add item category
        genericKey.append(splitKey[1]);
        genericKey.append("|");

        // Add item name
        genericKey.append(splitKey[2]);
        genericKey.append("|");

        // Add item frameType
        genericKey.append(splitKey[3]);

        // Add var info, if present (eg Impresence has different icons based on variation)
        for (int i = 4; i < splitKey.length; i++) {
            if (splitKey[i].contains("var:")) {
                genericKey.append("|");
                genericKey.append(splitKey[i]);
                break;
            }
        }

        return genericKey.toString();
    }

    /**
     * Manages item category list
     *
     * @param item Item object to add
     */
    public void addCategory(Item item) {
        List<String> childCategories = categories.getOrDefault(item.parentCategory, new ArrayList<>());

        if (item.childCategory != null && !childCategories.contains(item.childCategory))
            childCategories.add(item.childCategory);

        categories.putIfAbsent(item.parentCategory, childCategories);
    }

    /**
     * Manages league list
     *
     * @param item Item object to add
     */
    public void addLeague(Item item) {
        if (!leagues.contains(item.league))
            leagues.add(item.league);
    }
}
