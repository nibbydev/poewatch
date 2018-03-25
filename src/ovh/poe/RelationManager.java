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
    public static class LeagueListElement {
        String id, startAt, endAt;
    }

    public static class CurrencyRelation {
        String name, index;
        String[] aliases;
    }

    public static class IndexedItem {
        public String name, type, parent, child, icon, var, tier, key;
        public int frame;

        void add(Item item, String key) {
            name = item.name;
            parent = item.parentCategory;
            frame = item.frameType;
            this.key = key;


            if (item.icon != null) icon = formatIconURL(item.icon);
            if (item.typeLine != null) type = item.typeLine;
            if (item.childCategory != null) child = item.childCategory;
            if (item.variation != null) var = item.variation;
            if (item.tier != null) tier = item.tier;
        }

        /**
         * Removes any unnecessary fields from the item's icon
         *
         * @param icon An item's bloated URL
         * @return Formatted icon URL
         */
        String formatIconURL(String icon) {
            String[] splitURL = icon.split("\\?");
            String fullIcon = splitURL[0];

            if (splitURL.length > 1) {
                StringBuilder paramBuilder = new StringBuilder();

                for (String param : splitURL[1].split("&")) {
                    String[] splitParam = param.split("=");

                    switch (splitParam[0]) {
                        case "scale":
                        case "w":
                        case "h":
                        case "mr": // shaped
                        case "mn": // background
                        case "mt": // tier
                        case "relic":
                            paramBuilder.append("&");
                            paramBuilder.append(splitParam[0]);
                            paramBuilder.append("=");
                            paramBuilder.append(splitParam[1]);
                            break;
                        default:
                            break;
                    }
                }

                // If there are parameters that should be kept, add them to fullIcon
                if (paramBuilder.length() > 0) {
                    // Replace the first "&" symbol with "?"
                    paramBuilder.setCharAt(0, '?');
                    fullIcon += paramBuilder.toString();
                }
            }

            return fullIcon;
        }
    }

    private Gson gson = Main.getGson();

    public Map<String, String> currencyIndexToName = new HashMap<>();
    public Map<String, String> currencyNameToIndex = new HashMap<>();
    public Map<String, String> currencyAliasToIndex = new HashMap<>();
    public Map<String, String> currencyAliasToName = new HashMap<>();

    private Map<String, String> itemKeyToIndex = new HashMap<>(); // Private as item keys need to be generalized first
    public Map<String, IndexedItem> itemIndexToData = new TreeMap<>();

    public Map<String, List<String>> categories = new HashMap<>();
    public List<String> leagues = new ArrayList<>();
    private long lastLeagueListUpdate;

    /**
     * Reads currency and item data from file on object init
     */
    RelationManager() {
        readCurrencyRelationsFromFile();
        readItemDataFromFile();
        readCategoriesFromFile();
    }

    /**
     * Downloads a list of active leagues from pathofexile.com, sorts them, adds them to 'List<String> leagues' and
     * writes them to './data/leagues.json'. Can be called whenever but will only run every 30 minutes.
     */
    public void getLeagueList() {
        // Run every 30min
        if ((System.currentTimeMillis() - lastLeagueListUpdate) < 1800000) return;
        lastLeagueListUpdate = System.currentTimeMillis();

        List<LeagueListElement> leagueList = null;
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
            Type listType = new TypeToken<List<LeagueListElement>>(){}.getType();
            leagueList = gson.fromJson(stringBuilderBuffer.toString(), listType);
        } catch (Exception ex) {
            System.out.println("[Error] Failed to download league list");
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
        for (LeagueListElement element : leagueList) {
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

        // Save leagues to file
        new File("./data/").mkdirs();
        File leagueFile = new File("./data", "leagues.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(leagueFile), "UTF-8"))) {
            gson.toJson(leagues, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to leagues.json");
            ex.printStackTrace();
        }

        System.out.println(Main.timeStamp() + "[INFO] League list updated");
    }

    /**
     * Reads currency relation data from file
     */
    private void readCurrencyRelationsFromFile() {
        File file = new File("./data/currencyRelations.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<ArrayList<CurrencyRelation>>(){}.getType();
            List<CurrencyRelation> relations = gson.fromJson(reader, listType);

            for (CurrencyRelation relation : relations) {
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
        File file = new File("./data/itemData.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<Map<String, IndexedItem>>(){}.getType();
            Map<String, IndexedItem> relations = gson.fromJson(reader, listType);

            // Lambda loop
            relations.forEach((index, item) -> {
                itemKeyToIndex.put(item.key, index);
                itemIndexToData.put(index, item);
            });

        } catch (IOException ex) {
            // Doesn't matter if the file exists or not. It will be written to later.
        }
    }

    /**
     * Reads item relation data from file
     */
    private void readCategoriesFromFile() {
        File file = new File("./data/categories.json");

        // Open up the reader
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Type listType = new TypeToken<Map<String, List<String>>>(){}.getType();
            categories = gson.fromJson(reader, listType);
        } catch (IOException ex) {
            // Doesn't matter if the file exists or not. It will be written to later.
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        // Save item relations to file
        File itemFile = new File("./data/itemData.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(itemFile), "UTF-8"))) {
            gson.toJson(itemIndexToData, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to icoRelations.json");
            ex.printStackTrace();
        }

        // Save item categories to file
        File categoryFile = new File("./data/categories.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(categoryFile), "UTF-8"))) {
            gson.toJson(categories, writer);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write to categories.json");
            ex.printStackTrace();
        }
    }

    /**
     * Provides an interface for saving and retrieving item data (data, leagues, categories) and indexes
     *
     * @param item Item object to index
     * @return Generated index of added url
     */
    public String indexItem(Item item) {
        // Manage item category list
        List<String> childCategories = categories.getOrDefault(item.parentCategory, new ArrayList<>());
        if (item.childCategory != null && !childCategories.contains(item.childCategory)) childCategories.add(item.childCategory);
        categories.putIfAbsent(item.parentCategory, childCategories);

        // Manage item league list
        if (!leagues.contains(item.league)) leagues.add(item.league);

        // If item has no icon, don't index it
        if (item.icon == null) return "-";

        // Generalize key String key = resolveSpecificKey(item.key);

        // If icon is already present, return icon index. Otherwise create an instance of IndexedItem and add
        // IndexedItem instance to maps and return its index
        String key = resolveSpecificKey(item.key);
        String index;
        if (itemKeyToIndex.containsKey(key)) {
            index = itemKeyToIndex.get(key);
        } else {
            IndexedItem indexedItem = new IndexedItem();
            indexedItem.add(item, key);

            index = String.format("%07d", itemKeyToIndex.size());
            itemKeyToIndex.put(key, index);
            itemIndexToData.put(index, indexedItem);
        }

        return index;
    }

    /**
     * Searches key-to-index database for a match based on input. Requires an unique key
     *
     * @param key Item key (must contain league info)
     * @return Index if successful, "-" if unsuccessful
     */
    public String getIndexFromKey(String key) {
        String resolvedKey = resolveSpecificKey(key);
        return itemKeyToIndex.getOrDefault(resolvedKey, "-");
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
}
