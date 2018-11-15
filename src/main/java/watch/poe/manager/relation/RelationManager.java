package poe.manager.relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.initializers.BaseItemInitializer;
import poe.initializers.CurrencyAliasInitializer;
import poe.manager.entry.item.Item;
import poe.manager.entry.item.Key;

import java.util.*;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private static Logger logger = LoggerFactory.getLogger(RelationManager.class);
    private Database database;

    private Map<Key, Integer> keyToId = new HashMap<>();
    private static Map<String, String> currencyAliasToName = CurrencyAliasInitializer.GetAliasMap();
    private Map<String, CategoryEntry> categoryRelations = new HashMap<>();
    private List<Key> keysInUse = new ArrayList<>();
    // List of ids currently used in a league. Used for determining whether to create a new item entry in DB
    private Map<Integer, List<Integer>> leagueIds = new HashMap<>();
    private static Map<String, Set<String>> baseMap = BaseItemInitializer.GenBaseMap();

    public RelationManager(Database database) {
        this.database = database;
    }

    /**
     * Reads currency and item data from file on object prep
     */
    public boolean init() {
        boolean success;

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
        if (item.getGroup() == null) {
            return null;
        }

        // If the same item is currently being indexed in another thread
        if (keysInUse.contains(itemKey)) {
            return null;
        } else keysInUse.add(itemKey);

        indexCategory(item);

        // Add itemdata to database
        if (itemId == null) {
            // Get the category ids related to the item
            CategoryEntry categoryEntry = categoryRelations.get(item.getCategory());
            Integer categoryId = categoryEntry.getId();
            Integer groupId = categoryEntry.getGroupId(item.getGroup());

            // Add item data to the database and get its id
            itemId = database.index.indexItemData(item, categoryId, groupId);
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
        keysInUse.remove(itemKey);

        return itemId;
    }

    private void indexCategory(Item item) {
        CategoryEntry categoryEntry = categoryRelations.get(item.getCategory());

        if (categoryEntry == null) {
            Integer categoryId = database.index.addCategory(item.getCategory());
            if (categoryId == null) return;

            categoryEntry = new CategoryEntry();
            categoryEntry.setId(categoryId);

            categoryRelations.put(item.getCategory(), categoryEntry);
        }

        if (item.getGroup() != null && !categoryEntry.hasGroup(item.getGroup())) {
            int categoryId = categoryRelations.get(item.getCategory()).getId();

            Integer groupId = database.index.addGroup(categoryId, item.getGroup());
            if (groupId == null) return;

            categoryEntry.addGroup(item.getGroup(), groupId);
        }
    }

    /**
     * Extracts item's base class from its name
     * Eg 'Blasting Corsair Sword of Needling' -> 'Corsair Sword'
     *
     * @param group Group the item belongs to
     * @param name Item name
     * @return Extracted name or null on failure
     */
    public String extractItemBaseName(String group, String name) {
        if (name == null) {
            return null;
        }

        Set<String> baseSet = baseMap.get(group);

        if (baseSet == null) {
            return null;
        }

        for (String base : baseSet) {
            if (name.contains(base)) {
                return base;
            }
        }

        return null;
    }

    public Map<String, String> getCurrencyAliasToName() {
        return currencyAliasToName;
    }
}
