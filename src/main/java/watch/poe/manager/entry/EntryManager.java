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

public class EntryManager extends Thread {
    private static Logger logger = LoggerFactory.getLogger(EntryManager.class);
    private Config config;

    private Set<AccountEntry> accountSet = new HashSet<>();
    private Set<RawEntry> entrySet = new HashSet<>();
    private Map<Integer, Map<String, Double>> currencyLeagueMap = new HashMap<>();
    private StatusElement status;
    private Timer timer;

    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private final Object monitor = new Object();

    private Database database;
    private WorkerManager workerManager;
    private LeagueManager leagueManager;
    private RelationManager relationManager;
    private AccountManager accountManager;



    public EntryManager(Database database, LeagueManager leagueManager, AccountManager accountManager, RelationManager relationManager, Config config) {
        this.database = database;
        this.config = config;
        this.timer = new Timer(database);

        status = new StatusElement(config);

        this.leagueManager = leagueManager;
        this.accountManager = accountManager;
        this.relationManager = relationManager;
    }

    //------------------------------------------------------------------------------------------------------------
    // Thread control
    //------------------------------------------------------------------------------------------------------------

    /**
     * Method override for starting this object as a Thread
     */
    public void run() {
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
                status.lastRunTime = System.currentTimeMillis();
                cycle();
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

        uploadRawEntries();
        uploadAccounts();

        logger.info("EntryManager stopped");
    }

    //------------------------------------------------------------------------------------------------------------
    // Database access
    //------------------------------------------------------------------------------------------------------------

    private void uploadRawEntries() {
        Set<RawEntry> entrySet = this.entrySet;
        this.entrySet = new HashSet<>();

        database.upload.uploadRaw(entrySet);
    }

    private void uploadAccounts() {
        Set<AccountEntry> accountSet = this.accountSet;
        this.accountSet = new HashSet<>();

        database.account.uploadAccountNames(accountSet);
    }

    private void getCurrency() {
        Map<Integer, Map<String, Double>> currencyLeagueMap = database.init.getCurrencyMap();

        if (currencyLeagueMap == null) {
            return;
        }

        this.currencyLeagueMap = currencyLeagueMap;
    }

    //------------------------------------------------------------------------------------------------------------
    // Cycle
    //------------------------------------------------------------------------------------------------------------

    /**
     * Control method for starting up the minutely cycle
     */
    private void cycle() {
        status.checkFlagStates();
        timer.computeCycleDelays(status);

        // Upload gathered prices
        timer.start("upload");
        uploadRawEntries();
        timer.clk("upload");

        // Upload account names
        timer.start("account");
        uploadAccounts();
        timer.clk("account");

        // Recalculate prices in database
        timer.start("cycle", Timer.TimerType.NONE);
        cycleDatabase();
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
        logger.info(String.format("Cycle finished: %5d ms | %2d / %3d / %4d | c:%6d / p:%2d / u:%4d / a:%4d",
                System.currentTimeMillis() - status.lastRunTime,
                status.getTenRemainMin(),
                status.getSixtyRemainMin(),
                status.getTwentyFourRemainMin(),
                timer.getLatest("cycle"),
                timer.getLatest("prices"),
                timer.getLatest("upload"),
                timer.getLatest("account")));

        logger.info(String.format("[10%5d][11%5d][12%5d][13%5d][14%5d]",
                timer.getLatest("a10"),
                timer.getLatest("a11"),
                timer.getLatest("a12"),
                timer.getLatest("a13"),
                timer.getLatest("a14")));

        if (status.isSixtyBool()) logger.info(String.format("[20%5d][21%5d][22%5d][23%5d][24%5d][25%5d]",
                timer.getLatest("a20"),
                timer.getLatest("a21"),
                timer.getLatest("a22"),
                timer.getLatest("a23"),
                timer.getLatest("a24"),
                timer.getLatest("a25")));

        if (status.isTwentyFourBool()) logger.info(String.format("[30%5d][31%5d]",
                timer.getLatest("a30"),
                timer.getLatest("a31")));

        // Reset flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);

        // Add new delays to database
        timer.uploadDelays(status);
    }

    /**
     * Recalculates database data
     */
    private void cycleDatabase() {
        if (status.isSixtyBool()) {
            timer.start("a20", Timer.TimerType.SIXTY);
            database.flag.updateVolatile();
            timer.clk("a20");

            timer.start("a21", Timer.TimerType.SIXTY);
            database.calc.calculateVolatileMedian();
            timer.clk("a21");
        }

        timer.start("a10");
        database.flag.updateApproved();
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

        timer.start("a14");
        database.history.removeOldItemEntries();
        timer.clk("a14");

        if (status.isSixtyBool()) {
            timer.start("a22", Timer.TimerType.SIXTY);
            database.history.addHourly();
            timer.clk("a22");

            timer.start("a23", Timer.TimerType.SIXTY);
            database.flag.updateMultipliers();
            timer.clk("a23");

            timer.start("a24", Timer.TimerType.SIXTY);
            database.calc.calcQuantity();
            timer.clk("a24");
        }

        if (status.isTwentyFourBool()) {
            timer.start("a30", Timer.TimerType.TWENTY);
            database.history.addDaily();
            timer.clk("a30");

            timer.start("a31", Timer.TimerType.TWENTY);
            database.calc.calcSpark();
            timer.clk("a31");
        }

        if (status.isSixtyBool()) {
            timer.start("a25", Timer.TimerType.SIXTY);
            database.flag.resetCounters();
            timer.clk("a25");
        }
    }

    /**
     * Adds entries to the databases
     *
     * @param reply APIReply object that a worker has downloaded and deserialized
     */
    public void parseItems(Mappers.APIReply reply) {
        for (Mappers.Stash stash : reply.stashes) {
            Integer leagueId = null;

            for (Mappers.BaseItem baseItem : stash.items) {
                if (!workerManager.isFlag_Run()) {
                    return;
                }

                if (leagueId == null) {
                    String league = baseItem.getLeague();
                    leagueId = leagueManager.getLeagueId(league);

                    if (leagueId == null) {
                        break;
                    }
                }

                // Create ItemParser instance for the item
                ItemParser itemParser = new ItemParser(relationManager, baseItem, currencyLeagueMap.get(leagueId), config);

                // All  branched items should be discarded
                if (itemParser.isDiscard()) {
                    continue;
                }

                // Parse branched items
                ArrayList<Item> items = itemParser.parseItems(baseItem);

                for (Item item : items) {
                    // This specific branched item should be discarded
                    if (item.isDiscard()) {
                        continue;
                    }

                    // Get item ID (if missing, index it)
                    Integer itemId = relationManager.indexItem(item, leagueId);
                    if (itemId == null) continue;

                    // Create a RawEntry
                    RawEntry rawEntry = new RawEntry();
                    rawEntry.setItemId(itemId);
                    rawEntry.setLeagueId(leagueId);
                    rawEntry.setAccountName(stash.accountName);
                    rawEntry.setPrice(itemParser.getPrice());
                    rawEntry.setId(item.getId());

                    // Add it to the db queue
                    entrySet.add(rawEntry);
                }
            }

            if (stash.accountName != null && stash.lastCharacterName != null && leagueId != null) {
                accountSet.add(new AccountEntry(stash.accountName, stash.lastCharacterName, leagueId));
            }
        }
    }

    public void setWorkerManager(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }
}
