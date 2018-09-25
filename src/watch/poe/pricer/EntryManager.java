package watch.poe.pricer;

import watch.poe.*;
import watch.poe.Config;
import watch.poe.admin.Flair;
import watch.poe.item.Item;
import watch.poe.item.ItemParser;
import watch.poe.item.Mappers;

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
        currencyLeagueMap = Main.DATABASE.getCurrencyMap();

        // Round counters
        fixCounters();

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
    // Upon initialization
    //------------------------------------------------------------------------------------------------------------

    /**
     * Rounds counters on program start
     */
    private void fixCounters() {
        long current = System.currentTimeMillis();

        if (current - status.tenCounter > Config.entryController_tenMS) {
            long gap = (current - status.tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            status.tenCounter += gap;
        }

        if (current - status.sixtyCounter > Config.entryController_sixtyMS) {
            long gap = (current - status.sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            status.sixtyCounter += gap;
        }

        if (current - status.twentyFourCounter > Config.entryController_twentyFourMS) {
            if (status.twentyFourCounter == 0) status.twentyFourCounter -= Config.entryController_counterOffset;
            long gap = (current - status.twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS;
            status.twentyFourCounter += gap;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Data saving
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

    //------------------------------------------------------------------------------------------------------------
    // Cycle
    //------------------------------------------------------------------------------------------------------------

    /**
     * Control method for starting up the minutely cycle
     */
    private void cycle() {
        Main.ADMIN.log("Cycle starting", Flair.STATUS);
        checkIntervalFlagStates();

        // Check league API every 10 minutes
        if (status.isTenBool()) {
            Main.LEAGUE_MANAGER.cycle();
        }

        // Check if there are matching account name changes
        if (status.isTwentyFourBool()) {
            Main.ACCOUNT_MANAGER.checkAccountNameChanges();
        }

        // Upload gathered prices
        timer.start("upload");
        uploadRawEntries();
        timer.clk("upload");

        // Upload account names
        timer.start("account");
        uploadAccounts();
        timer.clk("account");

        // Recalculate prices in database
        timer.start("cycle");
        cycleDatabase();
        timer.clk("cycle");

        // Get latest currency rates
        timer.start("prices");
        currencyLeagueMap = Main.DATABASE.getCurrencyMap();
        timer.clk("prices");

        // Prepare cycle message
        Main.ADMIN.log(String.format("Cycle finished: %5d ms | %2d / %3d / %4d | c:%6d / p:%2d / u:%4d / a:%4d",
                System.currentTimeMillis() - status.lastRunTime,
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000,
                timer.getLatestMS("cycle"),
                timer.getLatestMS("prices"),
                timer.getLatestMS("upload"),
                timer.getLatestMS("account")), Flair.STATUS);

        // Switch off flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);
    }

    /**
     * Raises certain flags after certain intervals
     */
    private void checkIntervalFlagStates() {
        long current = System.currentTimeMillis();

        // Run once every 10min
        if (current - status.tenCounter > Config.entryController_tenMS) {
            status.tenCounter += (current - status.tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            status.setTenBool(true);
            Main.ADMIN.log("10 activated", Flair.STATUS);
        }

        // Run once every 60min
        if (current - status.sixtyCounter > Config.entryController_sixtyMS) {
            status.sixtyCounter += (current - status.sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            status.setSixtyBool(true);
            Main.ADMIN.log("60 activated", Flair.STATUS);
        }

        // Run once every 24h
        if (current - status.twentyFourCounter > Config.entryController_twentyFourMS) {
            if (status.twentyFourCounter == 0) {
                status.twentyFourCounter -= Config.entryController_counterOffset;
            }

            status.twentyFourCounter += (current - status.twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS ;
            status.setTwentyFourBool(true);
            Main.ADMIN.log("24 activated", Flair.STATUS);
        }
    }

    /**
     * Recalculates database data
     */
    private void cycleDatabase() {
        if (status.isSixtyBool()) {
            timer.start("a20");
            Main.DATABASE.updateVolatile();
            timer.clk("a20");

            timer.start("a21");
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

        Main.ADMIN.log(String.format("[10%5d][11%5d][12%5d][13%5d][14%5d]",
                timer.getLatestMS("a10"),
                timer.getLatestMS("a11"),
                timer.getLatestMS("a12"),
                timer.getLatestMS("a13"),
                timer.getLatestMS("a14")), Flair.STATUS);

        if (status.isSixtyBool()) {
            timer.start("a22");
            Main.DATABASE.addHourly();
            timer.clk("a22");

            timer.start("a23");
            Main.DATABASE.updateMultipliers();
            timer.clk("a23");

            timer.start("a24");
            Main.DATABASE.calcQuantity();
            timer.clk("a24");
        }

        if (status.isTwentyFourBool()) {
            timer.start("a30");
            Main.DATABASE.addDaily();
            timer.clk("a30");

            timer.start("a31");
            Main.DATABASE.calcSpark();
            timer.clk("a31");

            Main.ADMIN.log(String.format("[30%5d][31%5d]",
                    timer.getLatestMS("a30"),
                    timer.getLatestMS("a31")), Flair.STATUS);
        }

        if (status.isSixtyBool()) {
            timer.start("a25");
            Main.DATABASE.resetCounters();
            timer.clk("a25");

            Main.ADMIN.log(String.format("[20%5d][21%5d][22%5d][23%5d][24%5d][25%5d]",
                    timer.getLatestMS("a20"),
                    timer.getLatestMS("a21"),
                    timer.getLatestMS("a22"),
                    timer.getLatestMS("a23"),
                    timer.getLatestMS("a24"),
                    timer.getLatestMS("a25")), Flair.STATUS);
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
                    Integer itemId = Main.RELATIONS.indexItem(item, leagueId, itemParser.isDoNotIndex());
                    if (itemId == null) continue;

                    // Create a RawEntry
                    RawEntry rawEntry = new RawEntry();
                    rawEntry.setItemId(itemId);
                    rawEntry.setLeagueId(leagueId);
                    rawEntry.setAccountName(stash.accountName);
                    rawEntry.setPrice(itemParser.getPrice());

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
