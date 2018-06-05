package com.poestats.relations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.Misc;

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

    private final Map<String, String> currencyAliasToName = new HashMap<>();
    private final Map<String, List<String>> categories = new HashMap<>();
    private final IndexRelations indexRelations = new IndexRelations();

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

        success = Main.DATABASE.getItemData(indexRelations);
        if (!success) {
            Main.ADMIN.log_("Failed to query item indexes from database. Shutting down...", 5);
            return false;
        } else if (indexRelations.getCompleteIndexList().isEmpty()) {
            Main.ADMIN.log_("Database did not contain any item data information", 2);
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

    //------------------------------------------------------------------------------------------------------------
    // Indexing methods
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

        String sup = indexRelations.getGenericKeyToSupIndex().get(item.getGenericKey());
        String index = indexRelations.getUniqueKeyToFullIndex().get(item.getUniqueKey());
        String sub;

        if (index != null)  {
            return index;
        } else if (item.isDoNotIndex()) {
            // If there wasn't an already existing index, return null without indexing
            return null;
        } else if (sup != null) {
            sub = findNextSubIndex(sup);
            index = sup + sub;

            indexRelations.getCompleteIndexList().add(index);
            indexRelations.getSupIndexToSubs().get(sup).add(sub);
            indexRelations.getUniqueKeyToFullIndex().put(item.getUniqueKey(), index);

            Main.DATABASE.addSubItemData(sup, sub, item);
        } else {
            sup = findNextSupIndex();
            sub = Config.index_subBase;
            index = sup + sub;

            indexRelations.getCompleteIndexList().add(index);
            indexRelations.getSupIndexToSubs().get(sup).add(sub);
            indexRelations.getUniqueKeyToFullIndex().put(item.getUniqueKey(), index);
            indexRelations.getGenericKeyToSupIndex().put(item.getGenericKey(), sup);

            Main.DATABASE.addSupItemData(sup, item);
            Main.DATABASE.addSubItemData(sup, sub, item);
        }

        return index;
    }

    private String findNextSupIndex() {
        String sup = Integer.toHexString(indexRelations.getSupIndexToSubs().size());
        return (Config.index_superBase + sup).substring(sup.length());
    }

    private String findNextSubIndex(String sup) {
        String sub = Integer.toHexString(indexRelations.getSupIndexToSubs().get(sup).size());
        return (Config.index_subBase + sub).substring(sub.length());
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
