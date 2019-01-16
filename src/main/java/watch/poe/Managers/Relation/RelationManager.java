package poe.Managers.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.DB.Database;
import poe.Initializers.BaseItems;
import poe.Initializers.CurrencyAliases;
import poe.Managers.Entry.item.Item;
import poe.Managers.Entry.item.Key;

import java.util.*;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private final Logger logger;
    private final Database database;

    /*private Map<Key, Integer> keyToId = new HashMap<>();
    private static Map<String, String> currencyAliasToName = CurrencyAliases.GetAliasMap();
    private Map<String, CategoryEntry> categoryRelations = new HashMap<>();
    private Set<Key> indexingKeys = Collections.synchronizedSet(new HashSet<>());
    private Map<Integer, List<Integer>> leagueIds = new HashMap<>();
    private static Map<String, Set<String>> baseMap = BaseItems.GenBaseMap();*/

    private final Set<Key> inProgress = new HashSet<>();
    private final Map<Key, Integer> itemData = new HashMap<>();
    private final Map<Integer, Set<Integer>> leagueItems = new HashMap<>();
    private final Map<String, CategoryEntry> categories = new HashMap<>();

    private final Map<String, Set<String>> baseItems;
    private final Map<String, String> currencyAliasToName;

    public RelationManager(Database db) {
        this.logger = LoggerFactory.getLogger(RelationManager.class);
        this.database = db;

        currencyAliasToName = CurrencyAliases.GetAliasMap();
        baseItems = BaseItems.GenBaseMap();
    }

    /**
     * Reads currency and item data from file on object prep
     */
    public boolean init() {
        boolean success;

        success = database.init.getCategories(categories);
        if (!success) {
            logger.error("Failed to query categories from database. Shutting down...");
            return false;
        } else if (categories.isEmpty()) {
            logger.warn("Database did not contain any category information");
        }

        success = database.init.getItemData(itemData);
        if (!success) {
            logger.error("Failed to query item IDs from database. Shutting down...");
            return false;
        } else if (itemData.isEmpty()) {
            logger.warn("Database did not contain any item id information");
        }

        success = database.init.getLeagueItemIds(leagueItems);
        if (!success) {
            logger.error("Failed to query league item IDs from database. Shutting down...");
            return false;
        } else if (leagueItems.isEmpty()) {
            logger.warn("Database did not contain any league item id information");
        }

        return true;
    }

    public Integer index(Item item, int id_l) {
        Integer id_d, id_cat, id_grp;

        // Check if item already has been indexed
        synchronized (itemData) {
            id_d = itemData.get(item.getKey());
        }

        // DB contains that item data entry
        if (id_d != null) {
            // Check if DB has item entry in that specific league
            synchronized (leagueItems) {
                Set<Integer> itemSet = leagueItems.get(id_l);

                if (itemSet != null && itemSet.contains(id_d)) {
                    return id_d;
                }
            }
        }

        // If the another thread is currently processing the same item
        synchronized (inProgress) {
            if (inProgress.contains(item.getKey())) {
                return null;
            }

            inProgress.add(item.getKey());
        }

        // Do not allow empty category/group definitions if they should appear
        if (item.getCategory() == null || item.getGroup() == null) {
            logger.error(String.format("Null category/group found for: %s (%s - %s)",
                    item.getKey(), item.getCategory(), item.getGroup()));
            inProgress.remove(item.getKey());
            return null;
        }

        // Check if the database contains the item's category and group
        synchronized (categories) {
            // Get category entry or null
            CategoryEntry categoryEntry = categories.get(item.getCategory());

            // DB didn't contain that category
            if (categoryEntry == null) {
                // Attempt to add it
                id_cat = database.index.createCategory(item.getCategory());

                if (id_cat == null) {
                    logger.error(String.format("Could not create category for: %s (%s - %s)",
                            item.getKey(), item.getCategory(), item.getGroup()));
                    inProgress.remove(item.getKey());
                    return null;
                }

                // Create new entry and store it in locally
                categoryEntry = new CategoryEntry(id_cat);
                categories.put(item.getCategory(), categoryEntry);
            } else {
                id_cat = categoryEntry.getCategoryId();
            }

            // Check if the category already has that group
            if (!categoryEntry.hasGroup(item.getGroup())) {
                // Attempt to add it
                id_grp = database.index.addGroup(id_cat, item.getGroup());

                if (id_grp == null) {
                    logger.error(String.format("Could not create group for: %s (%s - %s)",
                            item.getKey(), item.getCategory(), item.getGroup()));
                    inProgress.remove(item.getKey());
                    return null;
                }

                // Store the group withing the category
                categoryEntry.addGroup(item.getGroup(), id_grp);
            } else {
                id_grp = categoryEntry.getGroupId(item.getGroup());
            }
        }

        // Right, now that we have verified that the item's category and group exist in the database, we can go ahead
        // and add the item itself to the database

        // Add item data to the database and get its id
        if (id_d == null) {
            id_d = database.index.indexItemData(item, id_cat, id_grp);

            if (id_d == null) {
                logger.error(String.format("Could not create item data for: %s (%s - %s)",
                        item.getKey(), item.getCategory(), item.getGroup()));
                inProgress.remove(item.getKey());
                return null;
            }
        }

        // Check if the item's id is present in the league item table
        synchronized (leagueItems) {
            // Get league-specific set
            Set<Integer> idSet = leagueItems.getOrDefault(id_l, new HashSet<>());

            if (!idSet.contains(id_d)) {
                // Create database entry
                database.index.createLeagueItem(id_l, id_d);

                idSet.add(id_d);
                leagueItems.putIfAbsent(id_l, idSet);
            }
        }

        // Add entry to local lookup table
        itemData.put(item.getKey(), id_d);

        // We've verified the integrity of entries everywhere, remove the item key from the process list and return
        // its id
        inProgress.remove(item.getKey());
        return id_d;
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

        Set<String> baseSet = baseItems.get(group);

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
