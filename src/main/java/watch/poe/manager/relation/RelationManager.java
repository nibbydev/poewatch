package poe.manager.relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.entry.item.Item;
import poe.manager.entry.item.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private static Logger logger = LoggerFactory.getLogger(RelationManager.class);

    private Map<Key, Integer> keyToId = new HashMap<>();
    private Map<String, String> currencyAliasToName = new HashMap<>();
    private Map<String, CategoryEntry> categoryRelations = new HashMap<>();
    private List<Key> currentlyIndexingChildKeys = new ArrayList<>();
    // List of ids currently used in a league. Used for determining whether to create a new item entry in DB
    private Map<Integer, List<Integer>> leagueIds = new HashMap<>();
    private Database database;

    public RelationManager(Database database) {
        this.database = database;
    }

    /**
     * Reads currency and item data from file on object prep
     */
    public boolean init() {
        boolean success;

        success = database.init.getCurrencyAliases(currencyAliasToName);
        if (!success) {
            logger.error("Failed to query currency aliases from database. Shutting down...");
            return false;
        } else if (currencyAliasToName.isEmpty()) {
            logger.error("Database did not contain any currency aliases. Shutting down...");
            return false;
        }

        success = database.init.getCategories(categoryRelations);
        if (!success) {
            logger.error("Failed to query categories from database. Shutting down...");
            return false;
        } else if (categoryRelations.isEmpty()) {
            logger.warn("Database did not contain any category information");
        }

        success = database.init.getItemData(keyToId);
        if (!success) {
            logger.error("Failed to query item IDs from database. Shutting down...");
            return false;
        } else if (keyToId.isEmpty()) {
            logger.warn("Database did not contain any item id information");
        }

        success = database.init.getLeagueItemIds(leagueIds);
        if (!success) {
            logger.error("Failed to query league item IDs from database. Shutting down...");
            return false;
        } else if (leagueIds.isEmpty()) {
            logger.warn("Database did not contain any league item id information");
        }

        return true;
    }

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

        // Do not allow empty category definitions
        if (item.getChildCategory() == null) {
            return null;
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
            itemId = database.index.indexItemData(item, parentCategoryId, childCategoryId);
            if (itemId != null) keyToId.put(itemKey, itemId);
        }

        // Check if the item's id is present under the league
        List<Integer> idList = leagueIds.getOrDefault(leagueId, new ArrayList<>());
        if (!idList.contains(itemId)) {
            idList.add(itemId);
            leagueIds.putIfAbsent(leagueId, idList);

            database.index.createLeagueItem(leagueId, itemId);
        }

        // Remove unique key from the list
        currentlyIndexingChildKeys.remove(itemKey);

        return itemId;
    }

    private void indexCategory(Item item) {
        CategoryEntry categoryEntry = categoryRelations.get(item.getParentCategory());

        if (categoryEntry == null) {
            Integer parentId = database.index.addParentCategory(item.getParentCategory());
            if (parentId == null) return;

            categoryEntry = new CategoryEntry();
            categoryEntry.setId(parentId);

            categoryRelations.put(item.getParentCategory(), categoryEntry);
        }

        if (item.getChildCategory() != null && !categoryEntry.hasChild(item.getChildCategory())) {
            int parentId = categoryRelations.get(item.getParentCategory()).getId();

            Integer childId = database.index.addChildCategory(parentId, item.getChildCategory());
            if (childId == null) return;

            categoryEntry.addChild(item.getChildCategory(), childId);
        }
    }

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }
}
