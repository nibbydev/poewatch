package poe.manager.entry;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.account.AccountManager;
import poe.manager.entry.item.Item;
import poe.manager.entry.item.ItemParser;
import poe.manager.entry.item.Mappers;
import poe.manager.entry.timer.Timer;
import poe.manager.league.LeagueManager;
import poe.manager.relation.RelationManager;
import poe.manager.worker.WorkerManager;

import java.util.*;
import java.util.zip.CRC32;

public class EntryManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(EntryManager.class);
    private static final CRC32 crc = new CRC32();
    private Config config;

    private Map<Integer, Map<String, Double>> currencyLeagueMap = new HashMap<>();
    private StatusElement status;
    private Timer timer;

    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private final Object monitor = new Object();
    private WorkerManager workerManager;

    private Database database;
    private LeagueManager leagueManager;
    private RelationManager relationManager;
    private AccountManager accountManager;

    private Set<Long> DbStashes = new HashSet<>(100000);

    public EntryManager(Database db, LeagueManager lm, AccountManager am, RelationManager rm, Config cnf) {
        this.database = db;
        this.config = cnf;
        this.timer = new Timer(db);

        status = new StatusElement(cnf);

        this.leagueManager = lm;
        this.accountManager = am;
        this.relationManager = rm;

        if (!cnf.getBoolean("misc.enableTimers")) {
            timer.stop();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Thread control
    //------------------------------------------------------------------------------------------------------------

    /**
     * Method override for starting this object as a Thread
     */
    public void run() {
        // Just a status msg on startup
        System.out.printf("Got %d distinct stashes\n", DbStashes.size());

        // Loads in currency rates on program start
        getCurrency();

        // Round counters
        status.fixCounters();

        // Get delays
        timer.getDelays();

        // Display counters
        logger.info(String.format("Loaded params: [10m:%3d min][1h:%3d min][24h:%5d min]",
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000));

        // Main thread loop
        while (flagLocalRun) {
            // Wait on monitor
            synchronized (monitor) {
                try {
                    monitor.wait(100);
                } catch (InterruptedException e) {
                }
            }

            // If monitor was woken, check if correct interval has passed
            if (System.currentTimeMillis() - status.lastRunTime > config.getInt("entry.sleepTime")) {
                workerManager.setWorkerSleepState(true);

                // Wait until all workers are paused
                while (!workerManager.getWorkerSleepState()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                status.lastRunTime = System.currentTimeMillis();
                cycle();

                workerManager.setWorkerSleepState(false);
            }
        }

        // If main loop was interrupted, raise flag indicating program is ready to safely exit
        readyToExit = true;
    }

    /**
     * Stops the threaded controller and saves all ephemeral data
     */
    public void stopController() {
        logger.info("Stopping EntryManager");

        flagLocalRun = false;

        logger.info("Removing timers");
        timer.stop();
        logger.info("Timers removed");

        while (!readyToExit) try {
            synchronized (monitor) {
                monitor.notify();
            }

            Thread.sleep(50);
        } catch (InterruptedException ex) {
        }

        logger.info("EntryManager stopped");
    }

    //------------------------------------------------------------------------------------------------------------
    // Cycle
    //------------------------------------------------------------------------------------------------------------

    private void getCurrency() {
        Map<Integer, Map<String, Double>> currencyLeagueMap = database.init.getCurrencyMap();

        if (currencyLeagueMap == null) {
            return;
        }

        this.currencyLeagueMap = currencyLeagueMap;
    }

    /**
     * Control method for starting up the minutely cycle
     */
    private void cycle() {
        status.checkFlagStates();
        timer.computeCycleDelays(status);

        // Start cycle timer
        timer.start("cycle", Timer.TimerType.NONE);

        if (status.isSixtyBool()) {
            timer.start("a21", Timer.TimerType.SIXTY);
            database.flag.deleteItemEntries();
            timer.clk("a21");
        }

        timer.start("a10");
        database.flag.updateOutliers();
        timer.clk("a10");

        timer.start("a11");
        database.flag.updateCounters();
        timer.clk("a11");

        timer.start("a12");
        database.calc.calculatePrices();
        timer.clk("a12");

        timer.start("a13");
        database.calc.calculateExalted();
        timer.clk("a13");

        if (status.isSixtyBool()) {
            timer.start("a22", Timer.TimerType.SIXTY);
            database.history.addHourly();
            timer.clk("a22");

            timer.start("a24", Timer.TimerType.SIXTY);
            database.calc.calcDaily();
            timer.clk("a24");
        }

        if (status.isTwentyFourBool()) {
            timer.start("a30", Timer.TimerType.TWENTY);
            database.history.addDaily();
            timer.clk("a30");

            timer.start("a31", Timer.TimerType.TWENTY);
            database.calc.calcSpark();
            timer.clk("a31");

            timer.start("a32", Timer.TimerType.TWENTY);
            database.history.removeOldItemEntries();
            timer.clk("a32");
        }

        if (status.isSixtyBool()) {
            timer.start("a25", Timer.TimerType.SIXTY);
            database.flag.resetCounters();
            timer.clk("a25");
        }

        // End cycle timer
        timer.clk("cycle");

        // Get latest currency rates
        timer.start("prices");
        getCurrency();
        timer.clk("prices");

        // Check league API
        if (status.isTenBool()) {
            timer.start("leagues", Timer.TimerType.TEN);
            leagueManager.cycle();
            timer.clk("leagues");
        }

        // Check if there are matching account name changes
        if (status.isTwentyFourBool()) {
            timer.start("accountChanges", Timer.TimerType.TWENTY);
            accountManager.checkAccountNameChanges();
            timer.clk("accountChanges");
        }

        // Prepare cycle message
        logger.info(String.format("Cycle finished: %5d ms | %2d / %3d / %4d | c:%6d / p:%2d",
                System.currentTimeMillis() - status.lastRunTime,
                status.getTenRemainMin(),
                status.getSixtyRemainMin(),
                status.getTwentyFourRemainMin(),
                timer.getLatest("cycle"),
                timer.getLatest("prices")));

        logger.info(String.format("[10%5d][11%5d][12%5d][13%5d]",
                timer.getLatest("a10"),
                timer.getLatest("a11"),
                timer.getLatest("a12"),
                timer.getLatest("a13")));

        if (status.isSixtyBool()) logger.info(String.format("[21%5d][22%5d][23%5d][24%5d][25%5d]",
                timer.getLatest("a21"),
                timer.getLatest("a22"),
                timer.getLatest("a23"),
                timer.getLatest("a24"),
                timer.getLatest("a25")));

        if (status.isTwentyFourBool()) logger.info(String.format("[30%5d][31%5d][32%5d]",
                timer.getLatest("a30"),
                timer.getLatest("a31"),
                timer.getLatest("a32")));

        // Reset flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);

        // Add new delays to database
        timer.uploadDelays(status);
    }

    /**
     * Adds entries to the databases
     *
     * @param reply APIReply object that a worker has downloaded and deserialized
     */
    public void parseItems(Mappers.APIReply reply) {
        // Set of account names and items extracted from the API call
        Set<Long> accounts = new HashSet<>();
        Set<Long> nullStashes = new HashSet<>();
        Set<RawItemEntry> items = new HashSet<>();
        // Separate set for collecting account and character names
        Set<RawUsernameEntry> usernames = new HashSet<>();

        for (Mappers.Stash stash : reply.stashes) {
            // Get league ID. If it's an unknown ID, skip this stash
            Integer id_l = leagueManager.getLeagueId(stash.league);
            if (id_l == null) {
                continue;
            }

            // Calculate CRCs
            long account_crc = calcCrc(stash.accountName);
            long stash_crc = calcCrc(stash.id);

            // If the stash is in use somewhere in the database
            if (DbStashes.contains(stash_crc)) {
                nullStashes.add(stash_crc);
            }

            if (stash.accountName ==  null || !stash.isPublic) {
                continue;
            }

            boolean hasValidItems = false;

            for (Mappers.BaseItem baseItem : stash.items) {
                long item_crc = calcCrc(baseItem.getId());

                // Create an ItemParser instance for every item in the stash, as one item
                // may branch into multiple db entries. For examples, a Devoto's Devotion with
                // a Tornado Shot enchantment creates 2 entries.
                ItemParser itemParser = new ItemParser(baseItem, currencyLeagueMap.get(id_l));

                // There was something off with the base item, discard it and don'tt create branched items
                if (itemParser.isDiscard()) {
                    continue;
                }

                // Parse branched items and create objects for db upload
                for (Item item : itemParser.getBranchedItems()) {
                    // Check if this specific branched item should be discarded
                    if (item.isDiscard()) {
                        continue;
                    }

                    // Get item's ID (if missing, index it)
                    Integer id_d = relationManager.indexItem(item, id_l);
                    if (id_d == null) {
                        continue;
                    }

                    RawItemEntry rawItem = new RawItemEntry(id_l, id_d, account_crc, stash_crc, item_crc, itemParser.getPrice());
                    items.remove(rawItem);
                    items.add(rawItem);

                    // Set flag to indicate stash contained at least 1 valid item
                    if (!hasValidItems) {
                        hasValidItems = true;
                    }
                }
            }

            // If stash contained at least 1 valid item, save the account
            if (hasValidItems) {
                DbStashes.add(stash_crc);
                accounts.add(account_crc);
            }

            // As this is a completely separate service, collect all character and account names separately
            if (stash.lastCharacterName != null) {
                usernames.add(new RawUsernameEntry(stash.accountName, stash.lastCharacterName, id_l));
            }
        }

        // Shovel everything to db
        long start = System.currentTimeMillis();
        database.upload.uploadAccounts(accounts);
        database.flag.resetStashReferences(nullStashes);
        database.upload.uploadItems(items);
        database.upload.uploadUsernames(usernames);
        System.out.printf("%d ms\n", System.currentTimeMillis() - start);
    }

    private static long calcCrc(String str) {
        if (str == null) {
            return 0;
        } else {
            crc.reset();
            crc.update(str.getBytes());
            return crc.getValue();
        }
    }

    public void setWorkerManager(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    public Set<Long> getDbStashes() {
        return DbStashes;
    }
}
