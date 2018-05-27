package com.poestats.relations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.relations.entries.CurrencyRelation;
import com.poestats.relations.entries.SupIndexedItem;
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

    private Map<String, String> genericKeyToSuperIndex = new HashMap<>();
    private Map<String, String> specificKeyToFullIndex = new HashMap<>();

    private Map<String, SupIndexedItem> supIndexToData;
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
                SupIndexedItem supIndexedItem = supIndexToData.get(sup);

                genericKeyToSuperIndex.putIfAbsent(supIndexedItem.getKey(), sup);

                if (supIndexedItem.getFrame() == 5) {
                    currencyNameToFullIndex.put(supIndexedItem.getName(), sup + Config.index_subBase);
                }

                for (String sub : supIndexedItem.getSubIndexes().keySet()) {
                    SubIndexedItem subIndexedItem = supIndexedItem.getSubIndexes().get(sub);
                    specificKeyToFullIndex.put(subIndexedItem.getKey(), sup + sub);
                }
            }
        }

        List<CurrencyRelation> currencyRelations = readCurrencyRelationsFromFile();
        if (currencyRelations == null) {
            Main.ADMIN.log_("Failed loadItem currency relations. Shutting down...", 5);
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

        String specificKey = item.getKey();
        String genericKey = resolveSpecificKey(item.getKey());

        String sup = genericKeyToSuperIndex.get(genericKey);
        String sub;
        String full = specificKeyToFullIndex.get(specificKey);

        if (sup != null && full != null)  {
            return full;
        } else if (item.isDoNotIndex()) {
            // If there wasn't an already existing index, return null without indexing
            return null;
        } else if (sup != null) {
            SupIndexedItem supIndexedItem = supIndexToData.get(sup);

            sub = supIndexedItem.subIndex(item, sup);
            full = sup + sub;

            specificKeyToFullIndex.put(item.getKey(), full);

            Main.DATABASE.addSubItemData(supIndexedItem, sup, sub);
        } else {
            sup = Integer.toHexString(genericKeyToSuperIndex.size());
            sup = (Config.index_superBase + sup).substring(sup.length());

            SupIndexedItem supIndexedItem = new SupIndexedItem(item);
            sub = supIndexedItem.subIndex(item, sup);
            full = sup + sub;

            genericKeyToSuperIndex.put(genericKey, sup);
            specificKeyToFullIndex.put(item.getKey(), full);
            supIndexToData.put(sup, supIndexedItem);

            Main.DATABASE.addFullItemData(supIndexedItem, sup, sub);
        }

        return full;
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

        // Add item frameType
        genericKey.append("|");
        genericKey.append(splitKey[1]);

        return genericKey.toString();
    }

    /**
     * Allows returning the SupIndexedItem entries from a complete index
     *
     * @param index Index of item
     * @return Requested indexed item entries or null on failure
     */
    public SupIndexedItem indexToGenericData(String index) {
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

        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize + 1);

        SupIndexedItem supIndexedItem = supIndexToData.getOrDefault(sup, null);
        if (supIndexedItem == null) return null;

        SubIndexedItem subIndexedItem = supIndexedItem.getSubIndexes().getOrDefault(sub, null);
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
        return index.length() != Config.index_size;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public Map<String, String> getCurrencyNameToFullIndex() {
        return currencyNameToFullIndex;
    }

    public Map<String, SupIndexedItem> getSupIndexToData() {
        return supIndexToData;
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }
}
