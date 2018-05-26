package com.poestats.pricer;

import com.google.gson.Gson;
import com.poestats.*;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.maps.*;
import com.poestats.parcel.*;
import com.poestats.parcel.ParcelMaps.*;

import java.io.*;
import java.util.*;

public class EntryManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final LeagueMap leagueMap = new LeagueMap();
    private final Parcel Parcel = new Parcel();
    private final Object monitor = new Object();
    private final Gson gson = Main.getGson();

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
     * Load in currency data on app start and fill leagueMap with leagues
     */
    private void loadCurrency() {
        for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
            String league = leagueEntry.getId();

            CategoryMap categoryMap = leagueMap.getOrDefault(league, new CategoryMap());
            IndexMap indexMap = categoryMap.getOrDefault("currency", new IndexMap());

            Main.DATABASE.getCurrency(league, indexMap);

            categoryMap.putIfAbsent("currency", indexMap);
            leagueMap.putIfAbsent(league, categoryMap);
        }
    }

    /**
     * Writes all collected data to file
     */
    private void cycle() {
        for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
            String league = leagueEntry.getId();

            CategoryMap categoryMap = leagueMap.getOrDefault(league, new CategoryMap());
            File leagueFolder = new File(Config.folder_database, league);

            if (leagueFolder.mkdir()) {
                Main.ADMIN.log_("Created database folder for league: " + league, 2);
            }

            for (String category : Main.RELATIONS.getCategories().keySet()) {
                IndexMap indexMap = categoryMap.getOrDefault(category, new IndexMap());
                Set<String> tmp_unparsedIndexes = new HashSet<>(indexMap.keySet());
                File leagueFile = new File(Config.folder_database, league+"/"+category+".csv");

                // The file data will be stored in
                File tmpLeagueFile = new File(Config.folder_database, league+"/"+category+".tmp");

                // If it's time, prep history controller
                if (status.isTwentyFourBool()) {
                    Main.HISTORY_MANAGER.configure(league, category);
                    Main.HISTORY_MANAGER.readFile();
                }

                // Define IO objects
                BufferedReader reader = Misc.defineReader(leagueFile);
                BufferedWriter writer = Misc.defineWriter(tmpLeagueFile);
                if (writer == null) continue;

                if (reader != null) {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String index = line.substring(0, line.indexOf("::"));
                            Entry entry;

                            if (indexMap.containsKey(index)) {
                                // Remove processed indexes from the set so that only entries that were found in the map
                                // but not the file will remain
                                tmp_unparsedIndexes.remove(index);

                                if (category.equals("currency")) {
                                    entry = indexMap.getOrDefault(index, new Entry(line, league));
                                } else {
                                    entry = indexMap.remove(index);
                                    entry.parseLine(line);
                                }
                            } else {
                                entry = new Entry(line, league);
                            }

                            entry.setLeague(league);
                            entry.cycle();
                            Parcel.add(entry);

                            String writeLine = entry.buildLine();
                            if (writeLine == null) Main.ADMIN.log_("Deleted entries: " + entry.getIndex(), 0);
                            else writer.write(writeLine);
                        }
                    } catch (IOException ex) {
                        Main.ADMIN._log(ex, 4);
                    }
                } else {
                    Main.ADMIN.log_("Missing database '"+category+"' for '"+league+"'", 2);
                }

                try {
                    for (String index : tmp_unparsedIndexes) {
                        Entry entry;

                        if (category.equals("currency")) entry = indexMap.get(index);
                        else entry = indexMap.remove(index);

                        entry.setLeague(league);
                        entry.cycle();
                        Parcel.add(entry);

                        String writeLine = entry.buildLine();
                        if (writeLine == null) Main.ADMIN.log_("Deleted entries: "+entry.getIndex(), 0);
                        else writer.write(writeLine);
                    }
                } catch (IOException ex) {
                    Main.ADMIN._log(ex, 4);
                }

                // Close file
                try {
                    if (reader != null) reader.close();
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    Main.ADMIN._log(ex, 4);
                }

                // Remove original file
                if (leagueFile.exists() && !leagueFile.delete()) {
                    String errorMsg = "Unable to remove '"+league+"/"+category+"/"+leagueFile.getName()+"'";
                    Main.ADMIN.log_(errorMsg, 4);
                }
                // Rename temp file to original file
                if (tmpLeagueFile.exists() && !tmpLeagueFile.renameTo(leagueFile)) {
                    String errorMsg = "Unable to rename '"+league+"/"+category+"/"+tmpLeagueFile.getName()+"'";
                    Main.ADMIN.log_(errorMsg, 4);
                }

                // If it's time, close history controller
                if (status.isTwentyFourBool()) {
                    Main.HISTORY_MANAGER.writeFile();
                }
            }

            // Commit a sin. This program is made for a box with 512-1024 MBs of memory. Java would run out of heap
            // space without a garbage collection suggestion. Also, it runs only every 24 hours.
            if (status.isTwentyFourBool()) System.gc();
        }
    }

    /**
     * Writes Parcel object to JSON file
     */
    private void writeJSONToFile() {
        for (String league : Parcel.getParcelLeagueMap().keySet()) {
            ParcelCategoryMap parcelCategoryMap = Parcel.getParcelLeagueMap().get(league);

            for (String category : parcelCategoryMap.keySet()) {
                ParcelItemList jsonItems = parcelCategoryMap.get(category);

                try {
                    if (new File(Config.folder_output, league).mkdir()) {
                        Main.ADMIN.log_("Created output folder for league: " + league, 2);
                    }
                    File file = new File(Config.folder_output, league+"/"+category+".json");

                    BufferedWriter writer = Misc.defineWriter(file);
                    if (writer == null) throw new IOException("File '"+league+"' error");

                    gson.toJson(jsonItems, writer);

                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    Main.ADMIN._log(ex, 3);
                }
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

        // Don't run if there hasn't been a successful run in the past 30 seconds
        //if ((current - Main.ADMIN.changeIDElement.lastUpdate) / 1000 > 30) return;

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

        // The method that does it all
        long time_cycle = System.currentTimeMillis();
        cycle();
        time_cycle = System.currentTimeMillis() - time_cycle;

        // Sort JSON
        long time_sort = System.currentTimeMillis();
        Parcel.sort();
        time_sort = System.currentTimeMillis() - time_sort;

        // Build JSON
        long time_json = System.currentTimeMillis();
        writeJSONToFile();
        time_json = System.currentTimeMillis() - time_json;

        Main.RELATIONS.saveData();

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
                " ms)(sort:" + String.format("%5d", time_sort) + " ms)";
        Main.ADMIN.log_(timeElapsedDisplay + tenMinDisplay + resetTimeDisplay + twentyHourDisplay + timeTookDisplay, -1);

        // Clear the parcel
        Parcel.clear();

        // Switch off flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);
        flipPauseFlag();

        Main.DATABASE.updateStatus(status);
    }

    /**
     * Adds entries to the databases
     *
     * @param reply APIReply object that a worker has downloaded and deserialized
     */
    public void parseItems(Mappers.APIReply reply) {
        // Loop through every single item, checking every single one of them
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

                CategoryMap categoryMap = leagueMap.getOrDefault(item.league, new CategoryMap());
                IndexMap indexMap = categoryMap.getOrDefault(item.getParentCategory(), new IndexMap());

                String index = Main.RELATIONS.indexItem(item);
                if (index == null) continue; // Some currency items have invalid icons

                Entry entry = indexMap.getOrDefault(index, new Entry());
                entry.add(item, stash.accountName, index);

                indexMap.putIfAbsent(index, entry);
                categoryMap.putIfAbsent(item.getParentCategory(), indexMap);
                leagueMap.putIfAbsent(item.league, categoryMap);
            }
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
            long gap = (current - status.twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS;
            if (status.twentyFourCounter == 0) status.twentyFourCounter -= Config.entryController_counterOffset;
            status.twentyFourCounter += gap;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public IndexMap getCurrencyMap (String league) {
        CategoryMap categoryMap = leagueMap.getOrDefault(league, null);
        if (categoryMap == null) return null;

        IndexMap indexMap = categoryMap.getOrDefault("currency", null);
        if (indexMap == null) return null;

        return indexMap;
    }

    public boolean isFlagPause() {
        return flagPause;
    }

    public StatusElement getStatus() {
        return status;
    }
}
