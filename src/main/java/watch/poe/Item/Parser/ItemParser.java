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
    private final Set<Long> dbStashes;
    private final LeagueManager leagueManager;
    private final RelationManager relationManager;
    private final StatisticsManager statisticsManager;
    private final Database database;
    private final Config config;
    private final CRC32 crc;

    public ItemParser(LeagueManager lm, RelationManager rm, Config cnf, StatisticsManager sm, Database db) {
        this.leagueManager = lm;
        this.relationManager = rm;
        this.config = cnf;
        this.statisticsManager = sm;
        this.database = db;

        dbStashes = new HashSet<>(100000);
        crc = new CRC32();
    }

    /**
     * Class instance initializer
     *
     * @return True on success
     */
    public boolean init() {
        // Get *all* stash ids
        boolean success = database.init.getStashIds(dbStashes);
        if (!success) {
            return false;
        }

        return true;
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

    /**
     * Parses the raw items found on the stash api
     */
    public void processApiReply(Reply reply) {
        List<User> users = new ArrayList<>();

        Set<Long> nullStashes = new HashSet<>();
        Set<DbItemEntry> dbItems = new HashSet<>();
        int totalItemCount = 0;

        for (Stash stash : reply.stashes) {
            totalItemCount += stash.items.size();

            // Get league ID. If it's an unknown ID, skip this stash
            Integer id_l = leagueManager.getLeagueId(stash.league);
            if (id_l == null) {
                continue;
            }

            // Calculate CRCs
            long stash_crc = calcCrc(stash.id);

            // If the stash is in use somewhere in the database
            synchronized (dbStashes) {
                if (dbStashes.contains(stash_crc)) {
                    nullStashes.add(stash_crc);
                }
            }

            if (stash.accountName == null || !stash.isPublic) {
                continue;
            }

            // Create user (character name can be null here)
            User user = new User(id_l, stash.accountName, stash.lastCharacterName);
            if (users.contains(user)) {
                user = users.get(users.indexOf(user));
            } else {
                users.add(user);
            }

            boolean hasValidItems = false;

            for (ApiItem apiItem : stash.items) {
                // Convert api items to poewatch items
                ArrayList<Item> pwItems = convertApiItem(apiItem);
                if (pwItems == null) continue;

                // Attempt to determine the price of the item
                Price price = new Price(apiItem.getNote(), stash.stashName);

                // If item didn't have a valid price
                if (!price.hasPrice() && !config.getBoolean("entry.acceptNullPrice")) {
                    continue;
                }

                // Parse branched items and create objects for db upload
                for (Item item : pwItems) {
                    // Get item's ID (if missing, index it)
                    Integer id_d = relationManager.index(item, id_l);
                    if (id_d == null) continue;

                    // Find crc of item's ID
                    long itemCrc = calcCrc(apiItem.getId());

                    // Create DB entry object
                    DbItemEntry entry = new DbItemEntry(id_l, id_d, stash_crc, itemCrc, item.getStackSize(), price, user);
                    dbItems.add(entry);

                    // Set flag to indicate the stash contained at least 1 valid item
                    hasValidItems = true;
                }
            }

            // If stash contained at least 1 valid item, save the account
            if (hasValidItems) {
                dbStashes.add(stash_crc);
            }
        }

        // Collect some statistics
        statisticsManager.addValue(StatType.COUNT_TOTAL_STASHES, reply.stashes.size());
        statisticsManager.addValue(StatType.COUNT_TOTAL_ITEMS, totalItemCount);
        statisticsManager.addValue(StatType.COUNT_ACCEPTED_ITEMS, dbItems.size());

        // Shovel everything to db
        database.upload.uploadAccountNames(users);
        database.upload.uploadCharacterNames(users);
        database.flag.resetStashReferences(nullStashes);
        database.upload.uploadEntries(dbItems);
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
}
