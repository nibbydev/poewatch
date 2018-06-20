package com.poestats.relations;

import com.poestats.Item;
import com.poestats.Main;

import java.util.*;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Map<String, String> currencyAliasToName = new HashMap<>();
    private IndexRelations indexRelations = new IndexRelations();
    private Map<String, CategoryEntry> categoryRelations = new HashMap<>();

    private List<String> currentlyIndexingChildKeys = new ArrayList<>();

    //------------------------------------------------------------------------------------------------------------
    // Initialization
    //------------------------------------------------------------------------------------------------------------

    /**
     * Reads currency and item data from file on object init
     */
    public boolean init() {
        boolean success;

        success = Main.DATABASE.getCurrencyAliases(currencyAliasToName);
        if (!success) {
            Main.ADMIN.log_("Failed to query currency aliases from database. Shutting down...", 5);
            return false;
        } else if (currencyAliasToName.isEmpty()) {
            Main.ADMIN.log_("Database did not contain any currency aliases. Shutting down...", 5);
            return false;
        }

        success = Main.DATABASE.getCategories(categoryRelations);
        if (!success) {
            Main.ADMIN.log_("Failed to query categories from database. Shutting down...", 5);
            return false;
        } else if (categoryRelations.isEmpty()) {
            Main.ADMIN.log_("Database did not contain any category information", 2);
        }

        success = Main.DATABASE.getItemIds(indexRelations, Main.LEAGUE_MANAGER.getLeagues());
        if (!success) {
            Main.ADMIN.log_("Failed to query item ids from database. Shutting down...", 5);
            return false;
        } else if (indexRelations.isEmpty_leagueToKeyToId()) {
            Main.ADMIN.log_("Database did not contain any item id information", 2);
        }

        success = Main.DATABASE.getItemDataParentIds(indexRelations);
        if (!success) {
            Main.ADMIN.log_("Failed to query parent item ids from database. Shutting down...", 5);
            return false;
        } else if (indexRelations.isEmpty_parentKeyToParentId()) {
            Main.ADMIN.log_("Database did not contain any parent item id information", 2);
        }

        success = Main.DATABASE.getItemDataChildIds(indexRelations);
        if (!success) {
            Main.ADMIN.log_("Failed to query child item ids from database. Shutting down...", 5);
            return false;
        } else if (indexRelations.isEmpty_childKeyToChildId()) {
            Main.ADMIN.log_("Database did not contain any child item id information", 2);
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------------------
    // Indexing methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Provides an interface for saving and retrieving item data (data, leagues, categories) and indexes
     *
     * @param item Item object to index
     * @param league
     * @return
     */
    public Integer indexItem(Item item, String league) {
        String uniqueKey = item.getUniqueKey();
        String genericKey = item.getGenericKey();

        Integer id = indexRelations.getItemId(league, uniqueKey);

        if (id != null) return id;
        // If there wasn't an already existing parentChildId, return null without indexing
        if (item.isDoNotIndex()) return null;

        // If the current item is currently being processed/indexed by another worker thread
        if (currentlyIndexingChildKeys.contains(uniqueKey)) return null;
        else currentlyIndexingChildKeys.add(uniqueKey);

        indexCategory(item);

        Integer parentItemDataId = indexRelations.getParentItemDataId(genericKey);
        if (parentItemDataId == null) {
            CategoryEntry categoryEntry = categoryRelations.get(item.getParentCategory());

            Integer parentCategoryId = categoryEntry.getId();
            Integer childCategoryId = categoryEntry.getChildCategoryId(item.getChildCategory());

            parentItemDataId = Main.DATABASE.indexParentItemData(item, parentCategoryId, childCategoryId);
            indexRelations.addParentKeyToParentItemDataId(genericKey, parentItemDataId);
        }

        Integer childItemDataId = indexRelations.getChildItemDataId(uniqueKey);
        if (childItemDataId == null) {
            childItemDataId = Main.DATABASE.indexChildItemData(item, parentItemDataId);
            indexRelations.addChildKeyToChildItemDataId(uniqueKey, childItemDataId);
        }

        // Now that we have the item data's ids, we can index the item itself
        id = Main.DATABASE.indexItem(league, parentItemDataId, childItemDataId);

        if (id != null) {
            indexRelations.addLeagueToKeyToId(league, uniqueKey, id);
            indexRelations.addLeagueToIds(league, id);
        }

        // Remove the unique key from the list
        currentlyIndexingChildKeys.remove(uniqueKey);

        return id;
    }

    /**
     * Manage item category list
     *
     * @param item
     */
    private void indexCategory(Item item) {
        CategoryEntry categoryEntry = categoryRelations.get(item.getParentCategory());

        if (categoryEntry == null) {
            Integer parentId = Main.DATABASE.addParentCategory(item.getParentCategory());
            if (parentId == null) return;

            categoryEntry = new CategoryEntry();
            categoryEntry.setId(parentId);

            categoryRelations.put(item.getParentCategory(), categoryEntry);
        }

        if (item.getChildCategory() != null && !categoryEntry.hasChild(item.getChildCategory())) {
            int parentId = categoryRelations.get(item.getParentCategory()).getId();

            Integer childId = Main.DATABASE.addChildCategory(parentId, item.getChildCategory());
            if (childId == null) return;

            categoryEntry.addChild(item.getChildCategory(), childId);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }

    public Map<String, CategoryEntry> getCategoryRelations() {
        return categoryRelations;
    }
}
