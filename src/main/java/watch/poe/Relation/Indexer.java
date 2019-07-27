package poe.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Database.Database;
import poe.Item.Item;
import poe.Item.Key;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Indexer {
    private final Logger logger = LoggerFactory.getLogger(Indexer.class);
    private final Database database;

    private final Set<Key> inProgress = new HashSet<>();
    private final Map<Key, Integer> itemData = new HashMap<>();
    private final Map<Integer, Set<Integer>> leagueItems = new HashMap<>();
    private final Set<Integer> reindexSet = new HashSet<>();

    public Indexer(Database db) {
        this.database = db;
    }

    /**
     * Initializes the indexer
     *
     * @return
     */
    public boolean init() {
        boolean success;

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

    /**
     * Indexes an item's data
     *
     * @param item
     * @param id_l
     * @return
     */
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

    public Map<Key, Integer> getItemData() {
        return itemData;
    }
}
