package watch.poe.relations;

import watch.poe.admin.Flair;
import watch.poe.item.Item;
import watch.poe.Main;
import watch.poe.item.Key;
import watch.poe.league.LeagueEntry;

import java.util.*;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Map<Integer, List<Integer>> leagueToIds = new HashMap<>();
    private Map<Key, Integer> keyToId = new HashMap<>();


    private Map<String, String> currencyAliasToName = new HashMap<>();
    private Map<String, CategoryEntry> categoryRelations = new HashMap<>();
    private List<Key> currentlyIndexingChildKeys = new ArrayList<>();
    private volatile boolean newIndexedItem = false;

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

        success = Main.DATABASE.getItemIds(leagueToIds, keyToId);
        if (!success) {
            Main.ADMIN.log("Failed to query item ids from database. Shutting down...", Flair.FATAL);
            return false;
        } else if (keyToId.isEmpty()) {
            Main.ADMIN.log("Database did not contain any item id information", Flair.WARN);
        }

        if (leagueToIds.isEmpty()) {
            for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
                leagueToIds.putIfAbsent(leagueEntry.getId(), new ArrayList<>());
            }
        } else {
            for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
                if (!leagueToIds.containsKey(leagueEntry.getId())) {
                    leagueToIds.put(leagueEntry.getId(), new ArrayList<>());
                }
            }
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------------------
    // Indexing methods
    //------------------------------------------------------------------------------------------------------------

    public Integer indexItem(Item item, Integer leagueId, boolean doNotIndex) {
        Key itemKey = item.getKey();
        Integer itemId = keyToId.get(itemKey);

        // If the item is indexed and the league contains that item, return item's id
        if (itemId != null) {
            List<Integer> idList = leagueToIds.get(leagueId);
            if (idList != null && idList.contains(itemId)) return itemId;
        }

        // If the item was marked not to be indexed
        if (item.isDoNotIndex() || doNotIndex) {
            return null;
        }

        // If the same item is currently being processed in the same method in another thread
        if (currentlyIndexingChildKeys.contains(itemKey)) {
            return null;
        } else currentlyIndexingChildKeys.add(itemKey);

        indexCategory(item);

        // Add itemdata to database
        if (itemId == null) {
            // Flip flag that will regenerate the itemdata files
            newIndexedItem = true;

            // Get the category ids related to the item
            CategoryEntry categoryEntry = categoryRelations.get(item.getParentCategory());
            Integer parentCategoryId = categoryEntry.getId();
            Integer childCategoryId = categoryEntry.getChildCategoryId(item.getChildCategory());

            // Add item data to the database and get its id
            itemId = Main.DATABASE.indexItemData(item, parentCategoryId, childCategoryId);
            if (itemId != null) keyToId.put(itemKey, itemId);
        }

        // Check if the item's id is present under the league
        List<Integer> idList = leagueToIds.get(leagueId);
        if (idList != null && !idList.contains(itemId)) {
            idList.add(itemId);
            leagueToIds.putIfAbsent(leagueId, idList);

            Main.DATABASE.createLeagueItem(leagueId, itemId);
        }

        // Remove unique key from the list
        currentlyIndexingChildKeys.remove(itemKey);

        return itemId;
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

    public boolean isNewIndexedItem() {
        if (newIndexedItem) {
            newIndexedItem = false;
            return true;
        } else return false;
    }

    public String getCategoryName(Integer id) {
        for (Map.Entry<String, CategoryEntry> entry : categoryRelations.entrySet()) {
            if (entry.getValue().getId() == id) {
                return entry.getKey();
            }
        }

        return  null;
    }
}
