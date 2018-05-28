package com.poestats.pricer;

import com.poestats.*;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.entries.RawEntry;
import com.poestats.pricer.maps.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final Map<String, List<String>> leagueIndexes = new HashMap<>();

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
        for (String league : leagueIndexes.keySet()) {
            long time0 = System.currentTimeMillis();
            long time1;
            long time2;
            long time3;
            long time4;
            long tmp;

            List<String> tmpList = new ArrayList<>(leagueIndexes.get(league));
            leagueIndexes.get(league).clear();

            tmp = System.currentTimeMillis();
            for (String index : tmpList) {
                Main.DATABASE.calculateMean(league, index);
                Main.DATABASE.calculateMedian(league, index);
                Main.DATABASE.removeOldItemEntries(league, index);
            }
            time1 = System.currentTimeMillis() - tmp;

            tmp = System.currentTimeMillis();
            Main.DATABASE.calculateExalted(league);
            time2 = System.currentTimeMillis() - tmp;

            tmp = System.currentTimeMillis();
            Main.DATABASE.addMinutely(league);
            time3 = System.currentTimeMillis() - tmp;

            tmp = System.currentTimeMillis();
            Main.DATABASE.removeOldHistoryEntries(league, "minutely", "1 HOUR");
            time4 = System.currentTimeMillis() - tmp;

            System.out.println(String.format("[cycle] %-30s (total:%4d)(items:%4d) - (pri:%4d)(exa:%4d)(minu:%4d)(old:%4d)",
                    league, System.currentTimeMillis() - time0, tmpList.size(),
                    time1, time2, time3, time4));
        }

        if (status.isSixtyBool()) {
            for (String league : leagueIndexes.keySet()) {
                long time1 = 0;
                long time2 = 0;
                long time3 = 0;
                long tmp;

                // 1. remove all old hourlyEntries
                tmp = System.currentTimeMillis();
                Main.DATABASE.removeOldHistoryEntries(league, "hourly", "1 DAY");
                time1 += System.currentTimeMillis() - tmp;

                // 2. sum up minutely values and create hourlyEntry
                tmp = System.currentTimeMillis();
                Main.DATABASE.addHourly(league);
                time2 += System.currentTimeMillis() - tmp;

                // 3. sum up hourlyEntry's incs and update item's quantity (also zero inc and dec)
                tmp = System.currentTimeMillis();
                Main.DATABASE.calcQuantity(league);
                time3 += System.currentTimeMillis() - tmp;

                System.out.println(String.format("[HOURLY] %-30s (%4d ms)(add: %4d ms)(quant: %4d)", league, time1, time2, time3));
            }
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

        // Get latest currency rates
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
                Main.DATABASE.createItem(item.league, index);

                List<String> leagueIndexes = this.leagueIndexes.getOrDefault(item.league, new ArrayList<>());
                if (!leagueIndexes.contains(index)) leagueIndexes.add(index);
                this.leagueIndexes.putIfAbsent(item.league, leagueIndexes);

                indexMap.putIfAbsent(index, rawList);
                leagueMap.putIfAbsent(item.league, indexMap);
            }
        }


        for (String league : leagueMap.keySet()) {
            long time1 = System.currentTimeMillis();

            IndexMap indexMap = leagueMap.get(league);

            long time2 = System.currentTimeMillis();
            Main.DATABASE.uploadRaw(league, indexMap);
            time2 = System.currentTimeMillis() - time2;

            long time4 = System.currentTimeMillis();
            Main.DATABASE.updateCounters(league, indexMap);
            time4 = System.currentTimeMillis() - time4;

            long time3 = System.currentTimeMillis();
            Main.DATABASE.removeItemOutliers(league, indexMap);
            time3 = System.currentTimeMillis() - time3;

            //System.out.println(String.format("    %-30s (%4d ms)(%4d dif items) - (raw: %4d ms)(out: %4d ms)(cnt: %4d ms)",
            //        league, System.currentTimeMillis() - time1,  indexMap.size(), time2, time3, time4));
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
