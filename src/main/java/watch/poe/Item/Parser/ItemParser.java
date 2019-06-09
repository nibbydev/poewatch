package poe.Item.Parser;

import com.typesafe.config.Config;
import poe.Db.Database;
import poe.Item.Deserializers.ApiItem;
import poe.Item.Deserializers.Reply;
import poe.Item.Deserializers.Stash;
import poe.Item.Branches.CraftingBaseBranch;
import poe.Item.Branches.DefaultBranch;
import poe.Item.Branches.EnchantBranch;
import poe.Item.Item;
import poe.Managers.LeagueManager;
import poe.Managers.RelationManager;
import poe.Managers.Stat.StatType;
import poe.Managers.StatisticsManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

public class ItemParser {
    private final LeagueManager lm;
    private final RelationManager rm;
    private final StatisticsManager sm;
    private final Database db;
    private final Config cf;

    private final Set<Long> dbStashes = new HashSet<>(100000);
    private final CRC32 crc = new CRC32();

    /**
     * Default constructor
     *
     * @param lm
     * @param rm
     * @param cf
     * @param sm
     * @param db
     */
    public ItemParser(LeagueManager lm, RelationManager rm, Config cf, StatisticsManager sm, Database db) {
        this.lm = lm;
        this.rm = rm;
        this.cf = cf;
        this.sm = sm;
        this.db = db;
    }

    /**
     * Instance initializer
     *
     * @return True on success
     */
    public boolean init() {
        // Get all stash ids
        return db.init.getStashIds(dbStashes);
    }

    /**
     * Processes items found though the public stash api
     */
    public void process(Reply reply) {
        // All users in the reply
        List<User> users = new ArrayList<>();
        // All stash IDs in the reply
        Set<Long> stashIdsToReset = new HashSet<>();
        // All items in the reply
        Set<DbItemEntry> dbItems = new HashSet<>();

        // Loop though all stashes in the reply
        int totalItemCount = processStashes(reply.stashes, users, stashIdsToReset, dbItems);

        // Collect some statistics
        sm.addValue(StatType.COUNT_TOTAL_STASHES, reply.stashes.size());
        sm.addValue(StatType.COUNT_TOTAL_ITEMS, totalItemCount);
        sm.addValue(StatType.COUNT_ACCEPTED_ITEMS, dbItems.size());

        // Shovel everything to db
        db.upload.uploadAccountNames(users);
        db.upload.uploadCharacterNames(users);
        db.flag.resetStashReferences(stashIdsToReset);
        db.upload.uploadEntries(dbItems);
    }

    /**
     * Goes through all stashes and all items and processes them
     *
     * @param users
     * @param stashIds
     * @param dbItems
     * @return Number of items in the reply
     */
    private int processStashes(List<Stash> stashes, List<User> users, Set<Long> stashIds, Set<DbItemEntry> dbItems) {
        // Number of items in the reply
        int totalItemCount = 0;

        // Loop though all stashes in the reply
        for (Stash stash : stashes) {
            // Add up the total items
            totalItemCount += stash.items.size();

            // Get league ID. If it's an unknown league (eg private or SSF), skip this stash
            Integer id_l = lm.getLeagueId(stash.league);
            if (id_l == null) continue;

            // Calculate CRC for the stash
            long stash_crc = calcCrc(stash.id);

            // If the stash is in use somewhere in the database
            synchronized (dbStashes) {
                if (dbStashes.contains(stash_crc)) {
                    stashIds.add(stash_crc);
                }
            }

            // Skip if missing data
            if (stash.accountName == null || !stash.isPublic) {
                continue;
            }

            // Create user (character name can be null here)
            User user = new User(id_l, stash.accountName, stash.lastCharacterName);

            // If the user already existed
            if (users.contains(user)) {
                user = users.get(users.indexOf(user));
            } else {
                users.add(user);
            }

            // If the stash contained any items that would be added to db
            boolean hasValidItems = false;

            // Loop through the items
            for (ApiItem apiItem : stash.items) {
                // Convert api items to poewatch items
                ArrayList<Item> pwItems = convertApiItem(apiItem);
                if (pwItems == null) continue;

                // Attempt to determine the price of the item
                Price price = new Price(apiItem.getNote(), stash.stashName);

                // If item didn't have a valid price
                if (!price.hasPrice() && !cf.getBoolean("entry.acceptNullPrice")) {
                    continue;
                }

                // Parse branched items and create objects for db upload
                for (Item item : pwItems) {
                    // Get item's ID (if missing, index it)
                    Integer id_d = rm.index(item, id_l);
                    if (id_d == null) continue;

                    // Calculate crc of item's ID
                    long itemCrc = calcCrc(apiItem.getId());

                    // Create DB entry object
                    DbItemEntry entry = new DbItemEntry(id_l, id_d, stash_crc, itemCrc, item.getStackSize(), price, user);
                    dbItems.add(entry);

                    // Set flag to indicate the stash contained at least 1 valid item
                    hasValidItems = true;
                }
            }

            // If stash contained at least 1 valid item, save the stash id
            if (hasValidItems) {
                dbStashes.add(stash_crc);
            }
        }

        return totalItemCount;
    }


    private ArrayList<Item> convertApiItem(ApiItem apiItem) {
        // Do a few checks on the league, note and etc
        if (checkIfDiscardApiItem(apiItem)) return null;

        // Branch item
        ArrayList<Item> branches = createBranches(apiItem);

        // Process the branches
        //branches.forEach(Item::process);
        branches.removeIf(Item::isDiscard);

        return branches;
    }

    /**
     * Check if the item should be discarded immediately.
     */
    private boolean checkIfDiscardApiItem(ApiItem apiItem) {
        // Filter out items posted on the SSF leagues
        if (apiItem.getLeague().contains("SSF")) {
            return true;
        }

        // Filter out a specific bug in the API
        if (apiItem.getLeague().equals("false")) {
            return true;
        }

        // Race rewards usually cost tens of times more than the average for their sweet, succulent altArt
        return apiItem.isRaceReward() != null && apiItem.isRaceReward();

    }

    /**
     * Check if item should be branched (i.e there could be more than one database entry from that item)
     */
    private ArrayList<Item> createBranches(ApiItem apiItem) {
        ArrayList<Item> branches = new ArrayList<>();

        // Default item
        branches.add(new DefaultBranch(apiItem));

        // If item is enchanted
        if (apiItem.getEnchantMods() != null) {
            branches.add(new EnchantBranch(apiItem));
        }

        // If item is a crafting base
        if (apiItem.getFrameType() < 3 && apiItem.getIlvl() >= 68 && !apiItem.getTypeLine().endsWith(" Incubator")) {
            branches.add(new CraftingBaseBranch(apiItem));
        }

        return branches;
    }


    private long calcCrc(String str) {
        if (str == null) {
            return 0;
        } else {
            crc.reset();
            crc.update(str.getBytes());
            return crc.getValue();
        }
    }

}
