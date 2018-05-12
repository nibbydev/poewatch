package com.poestats;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private static class CurrencyRelation {
        String name;
        String[] aliases;
    }

    public static class IndexedItem {
        public Map<String, SubIndexedItem> subIndexes = new TreeMap<>();
        public String name, type, parent, child, tier, genericKey, icon;
        public int frame;

        public IndexedItem(Item item) {
            if (item.frameType != -1) name = item.name;
            parent = item.getParentCategory();
            frame = item.frameType;

            genericKey = resolveSpecificKey(item.getKey());

            if (item.icon != null) icon = Misc.formatIconURL(item.icon);
            if (item.typeLine != null) type = item.typeLine;
            if (item.getChildCategory() != null) child = item.getChildCategory();
            if (item.getTier() != null) tier = item.getTier();
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
            specificKey = item.getKey();

            if (item.getVariation() != null) {
                var = item.getVariation();

                if (item.frameType == -1) {
                    name = resolveSpecificKey(item.getKey());

                    // Replace all instances of "#" with the associated value
                    for (String value : var.split("-")) {
                        name = name.replaceFirst("#", value);
                    }
                }
            } else {
                if (item.frameType == -1) {
                    name = resolveSpecificKey(item.getKey());
                }
            }

            if (item.getLinks() > 4) links = Integer.toString(item.getLinks());


            if (item.frameType == 4) {
                // Gson wants to serialize uninitialized integers and booleans
                quality = Integer.toString(item.getQuality());
                lvl = Integer.toString(item.getLevel());
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
            Main.ADMIN._log(ex, 4);
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
            if (writer == null) throw new IOException();
            gson.toJson(itemSubIndexToData, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to '"+itemFile.getName()+"'", 3);
            Main.ADMIN._log(ex, 3);
        }

        // Save item categories to file
        File categoryFile = new File("./data/categories.json");
        try (Writer writer = Misc.defineWriter(categoryFile)) {
            if (writer == null) throw new IOException();
            gson.toJson(categories, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to '"+categoryFile.getName()+"'", 3);
            Main.ADMIN._log(ex, 3);
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
        List<String> childCategories = categories.getOrDefault(item.getParentCategory(), new ArrayList<>());
        if (item.getChildCategory() != null && !childCategories.contains(item.getChildCategory())) childCategories.add(item.getChildCategory());
        categories.putIfAbsent(item.getParentCategory(), childCategories);

        String index;
        String genericKey = resolveSpecificKey(item.getKey());
        if (itemSpecificKeyToFullIndex.containsKey(item.getKey())) {
            // Return index if item is already indexed
            return itemSpecificKeyToFullIndex.get(item.getKey());
        } else if (item.isDoNotIndex()) {
            // If there wasn't an already existing index, return null without indexing
            return null;
        } else if (itemGenericKeyToSuperIndex.containsKey(genericKey)) {
            String superIndex = itemGenericKeyToSuperIndex.get(genericKey);
            IndexedItem indexedGenericItem = itemSubIndexToData.get(superIndex);

            String subIndex = indexedGenericItem.subIndex(item);
            index = itemGenericKeyToSuperIndex.get(genericKey) + "-" + subIndex;

            itemSpecificKeyToFullIndex.put(item.getKey(), index);
        } else {
            String superIndex = Integer.toHexString(itemGenericKeyToSuperIndex.size());
            superIndex = ("0000" + superIndex).substring(superIndex.length());

            IndexedItem indexedItem = new IndexedItem(item);
            String subIndex = indexedItem.subIndex(item);
            index = superIndex + "-" + subIndex;

            itemGenericKeyToSuperIndex.put(genericKey, superIndex);
            itemSubIndexToData.put(superIndex, indexedItem);
            itemSpecificKeyToFullIndex.put(item.getKey(), index);
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

        // Add item id
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
    public IndexedItem indexToGenericData(String index) {
        if (isIndex(index)) return null;

        String primaryIndex = index.substring(0, 4);

        return itemSubIndexToData.getOrDefault(primaryIndex, null);
    }

    public SubIndexedItem indexToSpecificData(String index) {
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

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }
}
