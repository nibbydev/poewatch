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
    private class LeagueListElement { String id;}

    private class CurrencyRelation {
        String name, index;
        String[] aliases;
    }

    public class IndexedItem {
        public Map<String, SubIndexedItem> subIndexes = new TreeMap<>();
        public String name, type, parent, child, tier, genericKey, icon;
        public int frame;

        public IndexedItem(Item item) {
            if (item.frameType != -1) name = item.name;
            parent = item.parentCategory;
            frame = item.frameType;

            genericKey = resolveSpecificKey(item.key);

            if (item.icon != null) icon = Misc.formatIconURL(item.icon);
            if (item.typeLine != null) type = item.typeLine;
            if (item.childCategory != null) child = item.childCategory;
            if (item.tier != null) tier = item.tier;
        }

        private String subIndex(Item item) {
            String subIndex = Integer.toHexString(subIndexes.size());
            subIndex = ("00" + subIndex).substring(subIndex.length());

            SubIndexedItem subIndexedItem = new SubIndexedItem(item);
            subIndexes.put(subIndex, subIndexedItem);

            return subIndex;
        }
    }

    public static class SubIndexedItem {
        public String var, specificKey, lvl, quality, links, corrupted, name;

        public SubIndexedItem (Item item) {
            //specificKey = item.key.substring(item.key.indexOf('|') + 1);
            specificKey = item.key;

            if (item.variation != null) {
                var = item.variation;

                if (item.frameType == -1) {
                    name = resolveSpecificKey(item.key);

                    // Replace all instances of "#" with the associated value
                    for (String value : var.split("-")) {
                        name = name.replaceFirst("#", value);
                    }
                }
            } else {
                if (item.frameType == -1) {
                    name = resolveSpecificKey(item.key);
                }
            }

            if (item.links > 4) links = Integer.toString(item.links);


            if (item.frameType == 4) {
                // Gson wants to serialize uninitialized integers and booleans
                quality = Integer.toString(item.quality);
                lvl = Integer.toString(item.level);
                corrupted = Boolean.toString(item.corrupted);
            }
        }
    }

    private Gson gson = Main.getGson();

    private Map<String, String> currencyAliasToName = new HashMap<>();
    private Map<String, String> currencyNameToFullIndex = new HashMap<>();

    private Map<String, String> itemSpecificKeyToFullIndex = new HashMap<>();
    private Map<String, String> itemGenericKeyToSuperIndex = new HashMap<>();
    private Map<String, IndexedItem> itemSubIndexToData = new TreeMap<>();

    private Map<String, List<String>> categories = new HashMap<>();
    private List<String> leagues = new ArrayList<>();

    /**
     * Reads currency and item data from file on object init
     */
    RelationManager() {
        readItemDataFromFile();
        readCurrencyRelationsFromFile();
        readCategoriesFromFile();
    }

    //------------------------------------------------------------------------------------------------------------
    // Common I/O
    //------------------------------------------------------------------------------------------------------------

    /**
     * Downloads a list of active leagues from pathofexile.com, sorts them, adds them to 'List<String> leagues' and
     * writes them to './data/leagues.json'
     */
    public void downloadLeagueList() {
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
            byte[] byteBuffer = new byte[64];
            int byteCount;

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0,64)) != -1) {
                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != 64) {
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
            Main.ADMIN.log_("Failed to download league list", 3);
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

        Main.ADMIN.log_("League list updated", 1);
    }

    /**
     * Reads currency relation data from file
     */
    private void readCurrencyRelationsFromFile() {
        File file = new File("./data/currencyRelations.json");

        // Open up the reader
        try (Reader reader = Misc.defineReader(file)) {
            if (reader == null) throw new IOException("File '" + file.getName() + "' not found");

            Type listType = new TypeToken<ArrayList<CurrencyRelation>>(){}.getType();
            List<CurrencyRelation> relations = gson.fromJson(reader, listType);

            for (CurrencyRelation relation : relations) {
                for (String alias : relation.aliases) {
                    currencyAliasToName.put(alias, relation.name);
                }
            }

        } catch (IOException ex) {
            Main.ADMIN.log_(ex.toString(), 3);
        }
    }

    /**
     * Reads item relation data from file
     */
    private void readItemDataFromFile() {
        File file = new File("./data/itemData.json");

        // Open up the reader
        try (Reader reader = Misc.defineReader(file)) {
            if (reader == null) throw new IOException("File '" + file.getName() + "' not found");

            Type listType = new TypeToken<Map<String, IndexedItem>>(){}.getType();
            Map<String, IndexedItem> relations = gson.fromJson(reader, listType);

            // Lambda loop
            relations.forEach((superIndex, superItem) -> {
                superItem.subIndexes.forEach((subIndex, subItem) -> {
                    String index = superIndex + "-" + subIndex;
                    itemSpecificKeyToFullIndex.put(subItem.specificKey, index);

                    // Add currency indexes to a special map
                    if (superItem.frame == 5) {
                        currencyNameToFullIndex.put(superItem.name, index);
                    }
                });

                itemGenericKeyToSuperIndex.put(superItem.genericKey, superIndex);
                itemSubIndexToData.put(superIndex, superItem);
            });

        } catch (IOException ex) {
            Main.ADMIN.log_("Couldn't load itemData.json", 3);
        }
    }

    /**
     * Reads item categories from file
     */
    private void readCategoriesFromFile() {
        File file = new File("./data/categories.json");

        // Open up the reader
        try (Reader reader = Misc.defineReader(file)) {
            if (reader == null) throw new IOException("File '" + file.getName() + "' not found");

            Type listType = new TypeToken<Map<String, List<String>>>(){}.getType();
            categories = gson.fromJson(reader, listType);
        } catch (IOException ex) {
            Main.ADMIN.log_("Couldn't load categories.json", 3);
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        // Save item relations to file
        File itemFile = new File("./data/itemData.json");
        try (Writer writer = Misc.defineWriter(itemFile)) {
            if (writer == null) throw new IOException("File '" + itemFile.getName() + "' error");
            gson.toJson(itemSubIndexToData, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to itemData.json", 3);
            ex.printStackTrace();
        }

        // Save item categories to file
        File categoryFile = new File("./data/categories.json");
        try (Writer writer = Misc.defineWriter(categoryFile)) {
            if (writer == null) throw new IOException("File '" + categoryFile.getName() + "' error");
            gson.toJson(categories, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to categories.json", 3);
            ex.printStackTrace();
        }

        // Save leagues to file
        File leagueFile = new File("./data/leagues.json");
        try (Writer writer = Misc.defineWriter(leagueFile)) {
            if (writer == null) throw new IOException("File '" + leagueFile.getName() + "' error");
            gson.toJson(leagues, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to leagues.json", 3);
            ex.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Indexing interface
    //------------------------------------------------------------------------------------------------------------

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

        // Manage item league list as a precaution. This list gets replaced by pathofexile's official league list
        // every 60 minutes
        if (!leagues.contains(item.league)) leagues.add(item.league);

        String index;
        String genericKey = resolveSpecificKey(item.key);
        if (itemSpecificKeyToFullIndex.containsKey(item.key)) {
            // Return index if item is already indexed
            return itemSpecificKeyToFullIndex.get(item.key);
        } else if (item.doNotIndex) {
            // If there wasn't an already existing index, return null without indexing
            return null;
        } else if (itemGenericKeyToSuperIndex.containsKey(genericKey)) {
            String superIndex = itemGenericKeyToSuperIndex.get(genericKey);
            IndexedItem indexedGenericItem = itemSubIndexToData.get(superIndex);

            String subIndex = indexedGenericItem.subIndex(item);
            index = itemGenericKeyToSuperIndex.get(genericKey) + "-" + subIndex;

            itemSpecificKeyToFullIndex.put(item.key, index);
        } else {
            String superIndex = Integer.toHexString(itemGenericKeyToSuperIndex.size());
            superIndex = ("0000" + superIndex).substring(superIndex.length());

            IndexedItem indexedItem = new IndexedItem(item);
            String subIndex = indexedItem.subIndex(item);
            index = superIndex + "-" + subIndex;

            itemGenericKeyToSuperIndex.put(genericKey, superIndex);
            itemSubIndexToData.put(superIndex, indexedItem);
            itemSpecificKeyToFullIndex.put(item.key, index);
        }

        return index;
    }

    /**
     * Searches key-to-index database for a match based on input. Requires a unique key
     *
     * @param key Item key
     * @return Index and subIndex if successful, null if unsuccessful
     */
    public String getIndexFromKey(String key) {
        return itemSpecificKeyToFullIndex.getOrDefault(key, null);
    }

    /**
     * Generalizes a specific key. E.g: "Flame Dash|4|l:10|q:20|c:0"
     * is turned into: "Flame Dash|4"
     *
     * @param key Specific item key with league and category and additional info
     * @return Generalized item key
     */
    public static String resolveSpecificKey(String key) {
        // "Shroud of the Lightless:Carnal Armour|3|var:1 socket"

        StringBuilder genericKey = new StringBuilder();
        String[] splitKey = key.split("\\|");

        // Add item name
        genericKey.append(splitKey[0]);

        // If it's an enchant, don't add frametype nor var info
        if (splitKey[1].equals("-1")) return genericKey.toString();

        // Add item frameType
        genericKey.append("|");
        genericKey.append(splitKey[1]);

        // Add var info, if present (eg Impresence has different icons based on variation)
        for (int i = 2; i < splitKey.length; i++) {
            if (splitKey[i].contains("var:")) {
                genericKey.append("|");
                genericKey.append(splitKey[i]);
                break;
            }
        }

        return genericKey.toString();
    }

    /**
     * Allows returning the SubIndexedItem entry from a complete index
     *
     * @param index Index of item. Must have length of 7
     * @return Requested indexed item entry or null on failure
     */
    public IndexedItem genericIndexToData(String index) {
        if (isIndex(index)) return null;

        String primaryIndex = index.substring(0, 4);

        return itemSubIndexToData.getOrDefault(primaryIndex, null);
    }

    public SubIndexedItem specificIndexToData(String index) {
        if (isIndex(index)) return null;

        String primaryIndex = index.substring(0, 4);
        String secondaryIndex = index.substring(5);

        IndexedItem indexedItem = itemSubIndexToData.getOrDefault(primaryIndex, null);
        if (indexedItem == null) return null;

        SubIndexedItem subIndexedItem = indexedItem.subIndexes.getOrDefault(secondaryIndex, null);
        if (subIndexedItem == null) return null;

        return subIndexedItem;
    }

    /**
     * Very primitive method to check if provided string is an index.
     *
     * @param index String to check
     * @return True if not an index
     */
    public static boolean isIndex(String index) {
        return index.length() != 7 || index.charAt(4) != '-';
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public Map<String, String> getCurrencyNameToFullIndex() {
        return currencyNameToFullIndex;
    }

    public Map<String, IndexedItem> getItemSubIndexToData() {
        return itemSubIndexToData;
    }

    public List<String> getLeagues() {
        return leagues;
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }
}
