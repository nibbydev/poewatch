package com.poestats.relations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.relations.entries.CurrencyRelation;
import com.poestats.relations.entries.IndexedItem;
import com.poestats.relations.entries.SubIndexedItem;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Gson gson = Main.getGson();

    private Map<String, String> currencyAliasToName = new HashMap<>();
    private Map<String, String> currencyNameToFullIndex = new HashMap<>();

    private Map<String, String> itemSpecificKeyToFullIndex = new HashMap<>();
    private Map<String, String> itemGenericKeyToSuperIndex = new HashMap<>();
    private Map<String, IndexedItem> itemSubIndexToData;

    private Map<String, List<String>> categories;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    /**
     * Reads currency and item data from file on object init
     */
    public RelationManager() {
        categories = Main.DATABASE.getCategories();
        if (categories == null) {
            Main.ADMIN.log_("Failed to query categories from database. Shutting down...", 5);
            System.exit(-1);
        } else if (categories.isEmpty()) {
            Main.ADMIN.log_("Database did not contain any category information", 2);
        }

        itemSubIndexToData = Main.DATABASE.getItemData();
        if (itemSubIndexToData == null) {
            Main.ADMIN.log_("Failed to query item data from database. Shutting down...", 5);
            System.exit(-1);
        } else if (itemSubIndexToData.isEmpty()) {
            Main.ADMIN.log_("Database did not contain any item data information", 2);
        }

        readCurrencyRelationsFromFile();
    }

    //------------------------------------------------------------------------------------------------------------
    // Common I/O
    //------------------------------------------------------------------------------------------------------------

    /**
     * Reads currency relation data from file
     */
    private void readCurrencyRelationsFromFile() {
        // Open up the reader
        try (Reader reader = Misc.defineReader(Config.file_relations)) {
            if (reader == null) {
                Main.ADMIN.log_("File not found: '" + Config.file_relations.getPath() + "'", 4);
                return;
            }

            Type listType = new TypeToken<ArrayList<CurrencyRelation>>(){}.getType();
            List<CurrencyRelation> relations = gson.fromJson(reader, listType);

            for (CurrencyRelation relation : relations) {
                for (String alias : relation.getAliases()) {
                    currencyAliasToName.put(alias, relation.getName());
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
        try (Reader reader = Misc.defineReader(Config.file_itemData)) {
            if (reader == null) {
                Main.ADMIN.log_("File not found: '" + Config.file_itemData.getPath() + "'", 2);
                return;
            }

            Type listType = new TypeToken<Map<String, IndexedItem>>(){}.getType();
            Map<String, IndexedItem> relations = gson.fromJson(reader, listType);

            // Lambda loop
            relations.forEach((superIndex, superItem) -> {
                superItem.getSubIndexes().forEach((subIndex, subItem) -> {
                    String index = superIndex + "-" + subIndex;
                    itemSpecificKeyToFullIndex.put(subItem.getKey(), index);

                    // Add currency indexes to a special map
                    if (superItem.getFrame() == 5) {
                        currencyNameToFullIndex.put(superItem.getName(), index);
                    }
                });

                //itemGenericKeyToSuperIndex.put(superItem.getGenericKey(), superIndex);
                itemSubIndexToData.put(superIndex, superItem);
            });

        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
        }
    }

    /**
     * Reads item categories from file
     */
    private void readCategoriesFromFile() {
        try (Reader reader = Misc.defineReader(Config.file_categories)) {
            if (reader == null) {
                Main.ADMIN.log_("File not found: '" + Config.file_categories.getPath() + "'", 4);
                return;
            }
            Type listType = new TypeToken<Map<String, List<String>>>(){}.getType();
            categories = gson.fromJson(reader, listType);
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        // Save item data to file
        try (Writer writer = Misc.defineWriter(Config.file_itemData)) {
            if (writer == null) throw new IOException();
            gson.toJson(itemSubIndexToData, writer);
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
        }

        // Save item categories to file
        try (Writer writer = Misc.defineWriter(Config.file_categories)) {
            if (writer == null) throw new IOException();
            gson.toJson(categories, writer);
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
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
            index = itemGenericKeyToSuperIndex.get(genericKey) + Config.index_separator + subIndex;

            itemSpecificKeyToFullIndex.put(item.getKey(), index);
        } else {
            String superIndex = Integer.toHexString(itemGenericKeyToSuperIndex.size());
            superIndex = (Config.index_superBase + superIndex).substring(superIndex.length());

            IndexedItem indexedItem = new IndexedItem(item);
            String subIndex = indexedItem.subIndex(item);
            index = superIndex + Config.index_separator + subIndex;

            itemGenericKeyToSuperIndex.put(genericKey, superIndex);
            itemSubIndexToData.put(superIndex, indexedItem);
            itemSpecificKeyToFullIndex.put(item.getKey(), index);
        }

        return index;
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
     * Allows returning the IndexedItem entries from a complete index
     *
     * @param index Index of item
     * @return Requested indexed item entries or null on failure
     */
    public IndexedItem indexToGenericData(String index) {
        if (isIndex(index)) return null;

        String primaryIndex = index.substring(0, Config.index_superSize);

        return itemSubIndexToData.getOrDefault(primaryIndex, null);
    }

    /**
     * Allows returning the SubIndexedItem entries from a complete index
     *
     * @param index Index of item
     * @return Requested indexed item entries or null on failure
     */
    public SubIndexedItem indexToSpecificData(String index) {
        if (isIndex(index)) return null;

        String primaryIndex = index.substring(0, Config.index_superSize);
        String secondaryIndex = index.substring(Config.index_superSize + 1);

        IndexedItem indexedItem = itemSubIndexToData.getOrDefault(primaryIndex, null);
        if (indexedItem == null) return null;

        SubIndexedItem subIndexedItem = indexedItem.getSubIndexes().getOrDefault(secondaryIndex, null);
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
        String separator = index.substring(Config.index_superSize, Config.index_size - Config.index_subSize);
        return index.length() != Config.index_size || !separator.equals(Config.index_separator);
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
