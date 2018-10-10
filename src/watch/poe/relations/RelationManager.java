package watch.poe.relations;

import watch.poe.admin.Flair;
import watch.poe.item.Item;
import watch.poe.Main;
import watch.poe.item.Key;

import java.util.*;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private Map<Key, Integer> keyToId = new HashMap<>();
    private Map<String, String> currencyAliasToName = new HashMap<>();
    private Map<String, CategoryEntry> categoryRelations = new HashMap<>();
    private List<Key> currentlyIndexingChildKeys = new ArrayList<>();
    // List of ids currently used in a league. Used for determining whether to create a new item entry in DB
    private Map<Integer, List<Integer>> leagueIds = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------
    // Initialization
    //------------------------------------------------------------------------------------------------------------

    /**
     * Reads currency and item data from file on object prep
     */
    public boolean init() {
        boolean success;

        success = Main.DATABASE.getCurrencyAliases(currencyAliasToName);
        if (!success) {
            Main.ADMIN.log("Failed to query currency aliases from database. Shutting down...", Flair.FATAL);
            return false;
        } else if (currencyAliasToName.isEmpty()) {
            Main.ADMIN.log("Database did not contain any currency aliases. Shutting down...", Flair.FATAL);
            return false;
        }

        success = Main.DATABASE.getCategories(categoryRelations);
        if (!success) {
            Main.ADMIN.log("Failed to query categories from database. Shutting down...", Flair.FATAL);
            return false;
        } else if (categoryRelations.isEmpty()) {
            Main.ADMIN.log("Database did not contain any category information", Flair.WARN);
        }

        success = Main.DATABASE.getItemData(keyToId);
        if (!success) {
            Main.ADMIN.log("Failed to query item IDs from database. Shutting down...", Flair.FATAL);
            return false;
        } else if (keyToId.isEmpty()) {
            Main.ADMIN.log("Database did not contain any item id information", Flair.WARN);
        }

        success = Main.DATABASE.getLeagueItemIds(leagueIds);
        if (!success) {
            Main.ADMIN.log("Failed to query league item IDs from database. Shutting down...", Flair.FATAL);
            return false;
        } else if (leagueIds.isEmpty()) {
            Main.ADMIN.log("Database did not contain any league item id information", Flair.WARN);
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------------------
    // Indexing methods
    //------------------------------------------------------------------------------------------------------------

    public Integer indexItem(Item item, Integer leagueId) {
        Key itemKey = item.getKey();
        Integer itemId = keyToId.get(itemKey);

        // If the item is indexed and the league contains that item, return item's ID
        if (itemId != null) {
            List<Integer> idList = leagueIds.get(leagueId);
            if (idList != null && idList.contains(itemId)) {
                return itemId;
            }
        }

        // If the same item is currently being indexed in another thread
        if (currentlyIndexingChildKeys.contains(itemKey)) {
            return null;
        } else currentlyIndexingChildKeys.add(itemKey);

        indexCategory(item);

        // Add itemdata to database
        if (itemId == null) {
            // Get the category ids related to the item
            CategoryEntry categoryEntry = categoryRelations.get(item.getParentCategory());
            Integer parentCategoryId = categoryEntry.getId();
            Integer childCategoryId = categoryEntry.getChildCategoryId(item.getChildCategory());

            // Add item data to the database and get its id
            itemId = Main.DATABASE.indexItemData(item, parentCategoryId, childCategoryId);
            if (itemId != null) keyToId.put(itemKey, itemId);
        }

        // Check if the item's id is present under the league
        List<Integer> idList = leagueIds.getOrDefault(leagueId, new ArrayList<>());
        if (!idList.contains(itemId)) {
            idList.add(itemId);
            leagueIds.putIfAbsent(leagueId, idList);

            Main.DATABASE.createLeagueItem(leagueId, itemId);
        }

        // Remove unique key from the list
        currentlyIndexingChildKeys.remove(itemKey);

        return itemId;
    }

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
}
