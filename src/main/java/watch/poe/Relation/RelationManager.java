package poe.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Database.Database;
import poe.Item.Category.GroupEnum;
import poe.Item.Item;
import poe.Item.Key;

import java.util.*;

/**
 * maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    private final Logger logger = LoggerFactory.getLogger(RelationManager.class);
    private final Database database;

    private final Set<Integer> reindexSet = new HashSet<>();
    private final Set<Key> inProgress = new HashSet<>();
    private final Map<Key, Integer> itemData = new HashMap<>();
    private final Map<Integer, Set<Integer>> leagueItems = new HashMap<>();
    public final RelationResources relationResources = new RelationResources();

    public RelationManager(Database db) {
        this.database = db;
    }


    /**
     * Reads currency and item data from file on object prep
     */
    public boolean init() {
        boolean success;

        logger.info("Initializing relations");

        success = loadRelations();
        if (!success) {
            logger.error("Failed to initialize relations");
            return false;
        }

        logger.info("Relations initialized successfully");
        logger.info("Loading resource files");

        success = relationResources.load(itemData);
        if (!success) {
            logger.error("Failed to load resource files");
            return false;
        }

        logger.info("Resource files loaded successfully");

        return true;
    }

    private boolean loadRelations() {
        boolean success;

        success = database.setup.verifyCategories();
        if (!success) {
            logger.error("Failed to verify database category integrity. Shutting down...");
            return false;
        }

        success = database.setup.verifyGroups();
        if (!success) {
            logger.error("Failed to verify database group integrity. Shutting down...");
            return false;
        }

        success = database.init.getItemData(itemData, reindexSet);
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
        Integer id_d;

        // Check if item already has been indexed
        synchronized (itemData) {
            id_d = itemData.get(item.getKey());
        }

        // DB contains that item data entry and item is not scheduled for reindexing
        synchronized (reindexSet) {
            if (id_d != null && !reindexSet.contains(id_d)) {
                // Check if DB has item entry in that specific league
                synchronized (leagueItems) {
                    Set<Integer> itemSet = leagueItems.get(id_l);

                    if (itemSet != null && itemSet.contains(id_d)) {
                        return id_d;
                    }
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

        // Add item data to the database and get its id
        if (id_d == null) {
            id_d = database.index.indexItemData(item);

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

        // If item should be indexed again
        synchronized (reindexSet) {
            if (reindexSet.contains(id_d)) {
                logger.info(String.format("Reindexing item %d", id_d));

                boolean success = database.index.reindexItemData(id_d, item);
                if (success) {
                    reindexSet.remove(id_d);
                }

                synchronized (itemData) {
                    Integer finalId_d = id_d;
                    // Remove key from itemData map
                    itemData.entrySet().removeIf(i -> i.getValue().equals(finalId_d));
                }
            }
        }

        // Add entry to local lookup table
        synchronized (itemData) {
            itemData.put(item.getKey(), id_d);
        }

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
     * @param name  Item name
     * @return Extracted name or null on failure
     */
    public String extractItemBaseName(GroupEnum group, String name) {
        if (name == null) {
            return null;
        }

        Set<String> baseSet = relationResources.getBaseItems().get(group);

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

    public String extractMapBaseName(String name) {
        if (name == null) {
            return null;
        }

        return relationResources.getDefaultMaps().stream()
                .filter(name::contains)
                .findFirst()
                .orElse(null);
    }

    public boolean isInCurrencyBlacklist(String name) {
        if (name == null) {
            return false;
        }

        return relationResources.getCurrencyBlackList().stream().anyMatch(name::equalsIgnoreCase);
    }

    public String findUnidentifiedUniqueMapName(String type, int frame) {
        UniqueMap uniqueMap = relationResources.getUniqueMaps().stream()
                .filter(i -> i.getFrame() == frame && i.getType().equals(type))
                .findFirst()
                .orElse(null);

        if (uniqueMap == null) {
            return null;
        }

        return uniqueMap.getName();
    }
}
