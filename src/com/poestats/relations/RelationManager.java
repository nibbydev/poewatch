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

    private Map<String, String> currencyAliasToName = new HashMap<>();
    private Map<String, String> genericKeyToSuperIndex = new HashMap<>();
    private Map<String, String> specificKeyToFullIndex = new HashMap<>();
    private Map<String, SupIndexedItem> supIndexToData = new HashMap<>();
    private Map<String, List<String>> categories = new HashMap<>();

    private Gson gson;

    //------------------------------------------------------------------------------------------------------------
    // Initialization
    //------------------------------------------------------------------------------------------------------------

    /**
     * Reads currency and item data from file on object init
     */
    public boolean init() {
        boolean success;

        success = Main.DATABASE.getCategories(categories);
        if (!success) {
            Main.ADMIN.log_("Failed to query categories from database. Shutting down...", 5);
            return false;
        } else if (categories.isEmpty()) {
            Main.ADMIN.log_("Database did not contain any category information", 2);
        }

        success = Main.DATABASE.getItemData(supIndexToData);
        if (!success) {
            Main.ADMIN.log_("Failed to query item data from database. Shutting down...", 5);
            return false;
        } else if (supIndexToData.isEmpty()) {
            Main.ADMIN.log_("Database did not contain any item data information", 2);
        } else {
            for (String sup : supIndexToData.keySet()) {
                SupIndexedItem supIndexedItem = supIndexToData.get(sup);

                genericKeyToSuperIndex.put(supIndexedItem.getKey(), sup);

                for (String sub : supIndexedItem.getSubIndexes().keySet()) {
                    SubIndexedItem subIndexedItem = supIndexedItem.getSubIndexes().get(sub);
                    specificKeyToFullIndex.put(subIndexedItem.getKey(), sup + sub);
                }
            }
        }

        List<CurrencyRelation> currencyRelations = readCurrencyRelationsFromFile();
        if (currencyRelations == null) {
            Main.ADMIN.log_("Failed loadItem currency relations. Shutting down...", 5);
            return false;
        } else {
            for (CurrencyRelation relation : currencyRelations) {
                for (String alias : relation.getAliases()) {
                    currencyAliasToName.put(alias, relation.getName());
                }
            }
        }

        return true;
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
     * Checks if the specified index exists in the maps (and by extension in the database)
     *
     * @param index Index to check
     * @return True if exists
     */
    public boolean checkIfIndexed(String index) {
        return specificKeyToFullIndex.values().contains(index);
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

        String subKey = item.getSubKey();
        String supKey = item.getSupKey();

        String sup = genericKeyToSuperIndex.get(supKey);
        String sub;
        String full = specificKeyToFullIndex.get(subKey);

        if (sup != null && full != null)  {
            return full;
        } else if (item.isDoNotIndex()) {
            // If there wasn't an already existing index, return null without indexing
            return null;
        } else if (sup != null) {
            SupIndexedItem supIndexedItem = supIndexToData.get(sup);

            sub = supIndexedItem.subIndex(item, sup);
            full = sup + sub;

            specificKeyToFullIndex.put(subKey, full);

            Main.DATABASE.addSubItemData(supIndexedItem, sup, sub);
        } else {
            sup = Integer.toHexString(supIndexToData.size());
            sup = (Config.index_superBase + sup).substring(sup.length());

            SupIndexedItem supIndexedItem = new SupIndexedItem(item);
            sub = supIndexedItem.subIndex(item, sup);
            full = sup + sub;

            genericKeyToSuperIndex.put(supKey, sup);
            specificKeyToFullIndex.put(subKey, full);
            supIndexToData.put(sup, supIndexedItem);

            Main.DATABASE.addFullItemData(supIndexedItem, sup, sub);
        }

        return full;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }
}
