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
        String modtString = String.format("Loaded params: [10m:%3d min][1h:%3d min][24h:%5d min]",
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000);
        Main.ADMIN.log(modtString, Flair.INFO);

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
        long time_upload = System.currentTimeMillis();
        uploadRawEntries();
        time_upload = System.currentTimeMillis() - time_upload;

        // Upload account names
        long time_account = System.currentTimeMillis();
        uploadAccounts();
        time_account = System.currentTimeMillis() - time_account;

        // Recalculate prices in database
        long time_cycle = System.currentTimeMillis();
        cycleDatabase();
        time_cycle = System.currentTimeMillis() - time_cycle;

        // Get latest currency rates
        long time_prices = System.currentTimeMillis();
        currencyLeagueMap = Main.DATABASE.getCurrencyMap();
        time_prices = System.currentTimeMillis() - time_prices;

        // Prepare cycle message
        String cycleMsg = String.format("Cycle finished: %5d ms | %2d / %3d / %4d | c:%6d / p:%2d / u:%4d / a:%4d",
                System.currentTimeMillis() - status.lastRunTime,
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000,
                time_cycle, time_prices,
                time_upload, time_account);
        Main.ADMIN.log(cycleMsg, Flair.STATUS);

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
        long a;
        long a10 = 0, a11 = 0, a12 = 0, a13 = 0, a14 = 0;
        long a20 = 0, a21 = 0, a22 = 0, a23 = 0, a24 = 0, a25 = 0;
        long a30 = 0, a31 = 0;

        if (status.isSixtyBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.updateVolatile();
            a20 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.calculateVolatileMedian();
            a21 += System.currentTimeMillis() - a;
        }

        a = System.currentTimeMillis();
        Main.DATABASE.updateApproved();
        a10 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.updateCounters();
        a11 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.calculatePrices();
        a12 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.calculateExalted();
        a13 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.removeOldItemEntries();
        a14 += System.currentTimeMillis() - a;

        System.out.printf("{1X series} > [10%5d][11%5d][12%5d][13%5d][14%5d]\n", a10, a11, a12, a13, a14);

        if (status.isSixtyBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.addHourly();
            a22 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.updateMultipliers();
            a23 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.calcQuantity();
            a24 += System.currentTimeMillis() - a;
        }

        if (status.isTwentyFourBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.addDaily();
            a30 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.calcSpark();
            a31 += System.currentTimeMillis() - a;

            System.out.printf("{3X series} > [30%5d][31%5d]\n", a30, a31);
        }

        if (status.isSixtyBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.resetCounters();
            a25 += System.currentTimeMillis() - a;

            System.out.printf("{2X series} > [20%5d][21%5d][22%5d][23%5d][24%5d][25%5d]\n", a20, a21, a22, a23, a24, a25);
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

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public StatusElement getStatus() {
        return status;
    }
}
