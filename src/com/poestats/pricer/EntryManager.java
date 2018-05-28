package com.poestats.pricer;

import com.poestats.*;
import com.poestats.database.DatabaseEntryHolder;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.entries.RawEntry;
import com.poestats.pricer.maps.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final Map<String, Map<String, Integer>> leagueIndexes = new HashMap<>();

    private final CurrencyLeagueMap currencyLeagueMap = new CurrencyLeagueMap();
    private final Object monitor = new Object();

    private volatile boolean flagPause;
    private final StatusElement status = new StatusElement();

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Loads data in from file on object initialization
     */
    public EntryManager() {
        loadStartParameters();
        loadCurrency();
    }

    /**
     * Loads status data from file on program start
     */
    private void loadStartParameters() {
        boolean querySuccessful = Main.DATABASE.getStatus(status);
        if (!querySuccessful) {
            Main.ADMIN.log_("Could not query status from database", 5);
            System.exit(-1);
        }

        fixCounters();

        String tenMinDisplay = "[10m:" + String.format("%3d", 10 - (System.currentTimeMillis() - status.tenCounter) / 60000) + " min]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000) + " min]";
        Main.ADMIN.log_("Loaded params: " + tenMinDisplay + resetTimeDisplay + twentyHourDisplay, -1);

        Main.DATABASE.updateStatus(status);
    }

    //------------------------------------------------------------------------------------------------------------
    // Methods for multi-db file structure
    //------------------------------------------------------------------------------------------------------------

    /**
     * Loads in currency rates on program start
     */
    private void loadCurrency() {
        currencyLeagueMap.clear();

        for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
            String league = leagueEntry.getId();

            CurrencyMap currencyMap = currencyLeagueMap.getOrDefault(league, new CurrencyMap());
            Main.DATABASE.getCurrency(league, currencyMap);
            currencyLeagueMap.putIfAbsent(league, currencyMap);
        }
    }

    /**
     * Writes all collected data to file
     */
    private void cycle() {
        Map<String, Map<String, Integer>> leagueIndexes = new HashMap<>(this.leagueIndexes);
        this.leagueIndexes.clear();

        for (String league : leagueIndexes.keySet()) {
            long time1 = System.currentTimeMillis();

            for (String index : leagueIndexes.get(league).keySet()) {
                int count = leagueIndexes.get(league).get(index);

                DatabaseEntryHolder entryHolder = new DatabaseEntryHolder();
                Main.DATABASE.getItem(league, index, entryHolder);
                Main.DATABASE.getEntries(league, index, entryHolder);

                entryHolder.calculate();
                entryHolder.incCounters(count);

                Main.DATABASE.updateFullItem(league, index, entryHolder);
                Main.DATABASE.removeOldItemEntries(league, index);
            }

            time1 = System.currentTimeMillis() - time1;
            System.out.println(String.format("    %-30s (%4d ms)(%4d items)", league, time1,  leagueIndexes.get(league).size()));

            Main.DATABASE.addMinutely(league);
            Main.DATABASE.removeOldMinutelyEntries(league);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Often called controller methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Main loop of the pricing service. Can be called whenever, only runs after specific amount of time has passed
     */
    public void run() {
        long current = System.currentTimeMillis();

        // Run every minute (-ish)
        if (current - status.lastRunTime < Config.entryController_sleepMS) return;
        status.lastRunTime = System.currentTimeMillis();

        // Raise static flag that suspends other threads while the databases are being worked on
        flipPauseFlag();

        // Run once every 10min
        if (current - status.tenCounter > Config.entryController_tenMS) {
            status.tenCounter += (current - status.tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            status.setTenBool(true);
            Main.ADMIN.log_("10 activated", 0);
        }

        // Run once every 60min
        if (current - status.sixtyCounter > Config.entryController_sixtyMS) {
            status.sixtyCounter += (current - status.sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            status.setSixtyBool(true);
            Main.ADMIN.log_("60 activated", 0);

            // Get a list of active leagues from pathofexile.com's api
            Main.LEAGUE_MANAGER.download();
        }

        // Run once every 24h
        if (current - status.twentyFourCounter > Config.entryController_twentyFourMS) {
            if (status.twentyFourCounter == 0) status.twentyFourCounter -= Config.entryController_counterOffset;
            status.twentyFourCounter += (current - status.twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS ;
            status.setTwentyFourBool(true);
            Main.ADMIN.log_("24 activated", 0);

            // Make a backup before 24h mark passes
            Main.ADMIN.log_("Starting backup (before)...", 0);
            long time_backup = System.currentTimeMillis();
            Main.ADMIN.backup(Config.folder_data, "daily_before");
            Main.ADMIN.log_("Backup (before) finished: " + (System.currentTimeMillis() - time_backup) + " ms", 0);
        }

        // Sort JSON
        long time_cycle = System.currentTimeMillis();
        cycle();
        time_cycle = System.currentTimeMillis() - time_cycle;

        // Get latest currency data
        long time_load_currency = System.currentTimeMillis();
        loadCurrency();
        time_load_currency = System.currentTimeMillis() - time_load_currency;

        // Build JSON
        long time_json = System.currentTimeMillis();
        time_json = System.currentTimeMillis() - time_json;

        // Backup output folder
        if (status.isTwentyFourBool()) {
            Main.ADMIN.log_("Starting backup (after)...", 0);
            long time_backup = System.currentTimeMillis();
            Main.ADMIN.backup(Config.folder_data, "daily_after");
            Main.ADMIN.log_("Backup (after) finished: " + (System.currentTimeMillis() - time_backup) + " ms", 0);
        }

        // Prepare message
        String timeElapsedDisplay = "[Took:" + String.format("%5d", System.currentTimeMillis() - status.lastRunTime) + " ms]";
        String tenMinDisplay = "[10m:" + String.format("%3d", 10 - (System.currentTimeMillis() - status.tenCounter) / 60000) + " min]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000) + " min]";
        String timeTookDisplay = "(Cycle:" + String.format("%5d", time_cycle) + " ms)(JSON:" + String.format("%5d", time_json) +
                " ms)(currency:" + String.format("%5d", time_load_currency) + " ms)";
        Main.ADMIN.log_(timeElapsedDisplay + tenMinDisplay + resetTimeDisplay + twentyHourDisplay + timeTookDisplay, -1);

        // Switch off flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);
        flipPauseFlag();

        Main.DATABASE.updateStatus(status);
        //System.gc();
    }

    /**
     * Adds entries to the databases
     *
     * @param reply APIReply object that a worker has downloaded and deserialized
     */
    public void parseItems(Mappers.APIReply reply) {
        LeagueMap leagueMap = new LeagueMap();

        for (Mappers.Stash stash : reply.stashes) {
            stash.fix();
            for (Item item : stash.items) {
                // Snooze. The lock will be lifted in about 0.1 seconds. This loop is NOT time-sensitive
                while (flagPause) {
                    synchronized (monitor) {
                        try {
                            monitor.wait(Config.monitorTimeoutMS);
                        } catch (InterruptedException ex) {
                        }
                    }
                }

                item.fix();
                item.parseItem();
                if (item.isDiscard()) continue;

                String index = Main.RELATIONS.indexItem(item);
                if (index == null) continue; // Some currency items have invalid icons

                IndexMap indexMap = leagueMap.getOrDefault(item.league, new IndexMap());
                RawList rawList = indexMap.getOrDefault(index, new RawList());

                RawEntry rawEntry = new RawEntry();
                rawEntry.add(item, stash.accountName);

                boolean discard = rawEntry.convertPrice(currencyLeagueMap.get(item.league));
                if (discard) continue; // Couldn't convert the listed currency to chaos

                rawList.add(rawEntry);

                Map<String, Integer> leagueIndexes = this.leagueIndexes.getOrDefault(item.league, new HashMap<>());
                int count = leagueIndexes.getOrDefault(index, 0);
                leagueIndexes.put(index, ++count);
                this.leagueIndexes.putIfAbsent(item.league, leagueIndexes);

                indexMap.putIfAbsent(index, rawList);
                leagueMap.putIfAbsent(item.league, indexMap);
            }
        }

        for (String league : leagueMap.keySet()) {
            Main.DATABASE.uploadRaw(league, leagueMap.get(league));
            Main.DATABASE.removeItemOutliers(league, leagueMap.get(league));
        }
    }

    /**
     * Switches pause boolean from state to state and wakes monitor
     */
    private void flipPauseFlag() {
        synchronized (monitor) {
            flagPause = !flagPause;
            monitor.notifyAll();
        }
    }

    /**
     * Makes sure counters don't fall behind
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
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public CurrencyMap getCurrencyMap (String league) {
        return currencyLeagueMap.getOrDefault(league, null);
    }

    public boolean isFlagPause() {
        return flagPause;
    }

    public StatusElement getStatus() {
        return status;
    }
}
