package watch.poe.manager.entry;

import watch.poe.*;
import watch.poe.Config;
import watch.poe.manager.admin.Flair;
import watch.poe.manager.entry.item.Item;
import watch.poe.manager.entry.item.ItemParser;
import watch.poe.manager.entry.item.Mappers;
import watch.poe.manager.entry.timer.Timer;

import java.util.*;

public class EntryManager extends Thread {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Set<AccountEntry> accountSet = new HashSet<>();
    private Set<RawEntry> entrySet = new HashSet<>();
    private Map<Integer, Map<String, Double>> currencyLeagueMap = new HashMap<>();
    private StatusElement status = new StatusElement();
    private Timer timer = new Timer();

    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private final Object monitor = new Object();

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
        Main.ADMIN.log(String.format("Loaded params: [10m:%3d min][1h:%3d min][24h:%5d min]",
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000), Flair.INFO);

        // Main thread loop
        while (flagLocalRun) {
            // Wait on monitor
            synchronized (monitor) {
                try {
                    monitor.wait(100);
                } catch (InterruptedException e) { }
            }

            // If monitor was woken, check if correct interval has passed
            if (System.currentTimeMillis() - status.lastRunTime > Config.entryController_sleepMS) {
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
        Main.ADMIN.log("Stopping EntryManager", Flair.INFO);

        flagLocalRun = false;

        Main.ADMIN.log("Removing timers", Flair.INFO);
        timer.stop();
        Main.ADMIN.log("Timers removed", Flair.INFO);

        while (!readyToExit) try {
            synchronized (monitor) {
                monitor.notify();
            }

            Thread.sleep(50);
        } catch (InterruptedException ex) { }

        uploadRawEntries();
        uploadAccounts();

        Main.ADMIN.log("EntryManager stopped", Flair.INFO);
    }

    //------------------------------------------------------------------------------------------------------------
    // Database access
    //------------------------------------------------------------------------------------------------------------

    private void uploadRawEntries() {
        Set<RawEntry> entrySet = this.entrySet;
        this.entrySet = new HashSet<>();

        Main.DATABASE.uploadRaw(entrySet);
    }

    private void uploadAccounts() {
        Set<AccountEntry> accountSet = this.accountSet;
        this.accountSet = new HashSet<>();

        Main.DATABASE.uploadAccountNames(accountSet);
    }

    private void getCurrency() {
        Map<Integer, Map<String, Double>> currencyLeagueMap = Main.DATABASE.getCurrencyMap();

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
            Main.LEAGUE_MANAGER.cycle();
            timer.clk("leagues");
        }

        // Check if there are matching account name changes
        if (status.isTwentyFourBool()) {
            timer.start("accountChanges", Timer.TimerType.TWENTY);
            Main.ACCOUNT_MANAGER.checkAccountNameChanges();
            timer.clk("accountChanges");
        }

        // Prepare cycle message
        Main.ADMIN.log(String.format("Cycle finished: %5d ms | %2d / %3d / %4d | c:%6d / p:%2d / u:%4d / a:%4d",
                System.currentTimeMillis() - status.lastRunTime,
                status.getTenRemainMin(),
                status.getSixtyRemainMin(),
                status.getTwentyFourRemainMin(),
                timer.getLatest("cycle"),
                timer.getLatest("prices"),
                timer.getLatest("upload"),
                timer.getLatest("account")), Flair.STATUS);

        Main.ADMIN.log(String.format("[10%5d][11%5d][12%5d][13%5d][14%5d]",
                timer.getLatest("a10"),
                timer.getLatest("a11"),
                timer.getLatest("a12"),
                timer.getLatest("a13"),
                timer.getLatest("a14")), Flair.STATUS);

        if (status.isSixtyBool()) Main.ADMIN.log(String.format("[20%5d][21%5d][22%5d][23%5d][24%5d][25%5d]",
                timer.getLatest("a20"),
                timer.getLatest("a21"),
                timer.getLatest("a22"),
                timer.getLatest("a23"),
                timer.getLatest("a24"),
                timer.getLatest("a25")), Flair.STATUS);

        if (status.isTwentyFourBool()) Main.ADMIN.log(String.format("[30%5d][31%5d]",
                timer.getLatest("a30"),
                timer.getLatest("a31")), Flair.STATUS);

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
            Main.DATABASE.updateVolatile();
            timer.clk("a20");

            timer.start("a21", Timer.TimerType.SIXTY);
            Main.DATABASE.calculateVolatileMedian();
            timer.clk("a21");
        }

        timer.start("a10");
        Main.DATABASE.updateApproved();
        timer.clk("a10");

        timer.start("a11");
        Main.DATABASE.updateCounters();
        timer.clk("a11");

        timer.start("a12");
        Main.DATABASE.calculatePrices();
        timer.clk("a12");

        timer.start("a13");
        Main.DATABASE.calculateExalted();
        timer.clk("a13");

        timer.start("a14");
        Main.DATABASE.removeOldItemEntries();
        timer.clk("a14");

        if (status.isSixtyBool()) {
            timer.start("a22", Timer.TimerType.SIXTY);
            Main.DATABASE.addHourly();
            timer.clk("a22");

            timer.start("a23", Timer.TimerType.SIXTY);
            Main.DATABASE.updateMultipliers();
            timer.clk("a23");

            timer.start("a24", Timer.TimerType.SIXTY);
            Main.DATABASE.calcQuantity();
            timer.clk("a24");
        }

        if (status.isTwentyFourBool()) {
            timer.start("a30", Timer.TimerType.TWENTY);
            Main.DATABASE.addDaily();
            timer.clk("a30");

            timer.start("a31", Timer.TimerType.TWENTY);
            Main.DATABASE.calcSpark();
            timer.clk("a31");
        }

        if (status.isSixtyBool()) {
            timer.start("a25", Timer.TimerType.SIXTY);
            Main.DATABASE.resetCounters();
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
                if (!Main.WORKER_MANAGER.isFlag_Run()) {
                    return;
                }

                if (leagueId == null) {
                    String league = baseItem.getLeague();
                    leagueId = Main.LEAGUE_MANAGER.getLeagueId(league);

                    if (leagueId == null) {
                        break;
                    }
                }

                // Create ItemParser instance for the item
                ItemParser itemParser = new ItemParser(baseItem, currencyLeagueMap.get(leagueId));

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
                    Integer itemId = Main.RELATIONS.indexItem(item, leagueId);
                    if (itemId == null) continue;

                    // Create a RawEntry
                    RawEntry rawEntry = new RawEntry();
                    rawEntry.setItemId(itemId);
                    rawEntry.setLeagueId(leagueId);
                    rawEntry.setAccountName(stash.accountName);
                    rawEntry.setPrice(itemParser.getPrice());
                    rawEntry.setIdentifier(item.getId());

                    // Add it to the db queue
                    entrySet.add(rawEntry);
                }
            }

            if (stash.accountName != null && stash.lastCharacterName != null && leagueId != null) {
                accountSet.add(new AccountEntry(stash.accountName, stash.lastCharacterName, leagueId));
            }
        }
    }
}
