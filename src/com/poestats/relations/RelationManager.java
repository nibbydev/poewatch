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
    private Map<String, String> currencyNameToIndex = new HashMap<>();

    private Map<String, String> supKeyToSup = new HashMap<>();
    private Map<String, String> subKeyToSub = new HashMap<>();

    private Map<String, IndexedItem> newSup = new HashMap<>();
    private Map<String, SubIndexedItem> newSub = new HashMap<>();

    private Map<String, IndexedItem> supIndexToData;
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

        supIndexToData = Main.DATABASE.getItemData();
        if (supIndexToData == null) {
            Main.ADMIN.log_("Failed to query item data from database. Shutting down...", 5);
            System.exit(-1);
        } else if (supIndexToData.isEmpty()) {
            Main.ADMIN.log_("Database did not contain any item data information", 2);
        } else {
            for (String sup : supIndexToData.keySet()) {
                IndexedItem indexedItem = supIndexToData.get(sup);

                supKeyToSup.putIfAbsent(indexedItem.getKey(), sup);

                if (indexedItem.getFrame() == 5) {
                    currencyNameToIndex.put(indexedItem.getName(), sup + Config.index_subBase);
                }

                for (String sub : indexedItem.getSubIndexes().keySet()) {
                    SubIndexedItem subIndexedItem = indexedItem.getSubIndexes().get(sub);
                    subKeyToSub.put(subIndexedItem.getKey(), sub);
                }
            }
        }

        List<CurrencyRelation> currencyRelations = readCurrencyRelationsFromFile();
        if (currencyRelations == null) {
            Main.ADMIN.log_("Failed load currency relations. Shutting down...", 5);
            System.exit(-1);
        } else {
            for (CurrencyRelation relation : currencyRelations) {
                for (String alias : relation.getAliases()) {
                    currencyAliasToName.put(alias, relation.getName());
                }
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Common I/O
    //------------------------------------------------------------------------------------------------------------

    /**
     * Reads currency relation data from file
     *
     * @return List of CurrencyRelation objects or null on error
     */
    private List<CurrencyRelation> readCurrencyRelationsFromFile() {
        List<CurrencyRelation> currencyRelations;

        // Open up the reader
        try (Reader reader = Misc.defineReader(Config.file_relations)) {
            if (reader == null) {
                Main.ADMIN.log_("File not found: '" + Config.file_relations.getPath() + "'", 4);
                return null;
            }

            Type listType = new TypeToken<ArrayList<CurrencyRelation>>(){}.getType();
            currencyRelations = gson.fromJson(reader, listType);

            return currencyRelations;
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
            return null;
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveData() {
        boolean updateSuccessful = Main.DATABASE.updateItemData(newSup, newSub);

        if (updateSuccessful) {
            newSup.clear();
            newSub.clear();
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

        String sup = supKeyToSup.get(genericKey);
        String sub = subKeyToSub.get(item.getKey());

        if (sup != null && sub != null)  {
            return sup + sub;
        } else if (item.isDoNotIndex()) {
            // If there wasn't an already existing index, return null without indexing
            return null;
        } else if (sup != null) {
            IndexedItem indexedGenericItem = supIndexToData.get(sup);

            sub = indexedGenericItem.subIndex(item);
            index = sup + sub;

            newSub.put(index, supIndexToData.get(sup).getSubIndexes().get(sub));

            subKeyToSub.put(item.getKey(), sub);
        } else {
            sup = Integer.toHexString(supKeyToSup.size());
            sup = (Config.index_superBase + sup).substring(sup.length());

            IndexedItem indexedItem = new IndexedItem(item);
            sub = indexedItem.subIndex(item);
            index = sup + sub;

            newSup.put(sup, indexedItem);
            newSub.put(index, supIndexToData.get(sup).getSubIndexes().get(sub));

            supKeyToSup.put(genericKey, sup);
            supIndexToData.put(sup, indexedItem);
            subKeyToSub.put(item.getKey(), sub);
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

        return supIndexToData.getOrDefault(primaryIndex, null);
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

        IndexedItem indexedItem = supIndexToData.getOrDefault(primaryIndex, null);
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

    public Map<String, String> getCurrencyNameToIndex() {
        return currencyNameToIndex;
    }

    public Map<String, IndexedItem> getSupIndexToData() {
        return supIndexToData;
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }
}
