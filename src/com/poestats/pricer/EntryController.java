package com.poestats.pricer;

import com.google.gson.Gson;
import com.poestats.*;
import com.poestats.pricer.maps.*;
import com.poestats.parcel.*;
import com.poestats.parcel.JSONMaps.*;

import java.io.*;
import java.util.*;

public class EntryController {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final LeagueMap leagueMap = new LeagueMap();
    private final JSONParcel JSONParcel = new JSONParcel();
    private final Object monitor = new Object();
    private final Gson gson = Main.getGson();

    private long lastRunTime = System.currentTimeMillis();
    private volatile boolean flagPause, tenBool, sixtyBool, twentyFourBool;
    private long twentyFourCounter, sixtyCounter, tenCounter;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Loads data in from file on object initialization
     */
    public EntryController() {
        loadStartParameters();
        loadDatabases();
    }

    /**
     * Saves current status data in text file
     */
    private void saveStartParameters() {
        try (BufferedWriter writer = Misc.defineWriter(Config.file_status)) {
            if (writer == null) return;

            String buffer = "writeTime: " + System.currentTimeMillis() + "\n";
            buffer += "twentyFourCounter: " + twentyFourCounter + "\n";
            buffer += "sixtyCounter: " + sixtyCounter + "\n";
            buffer += "tenCounter: " + tenCounter + "\n";

            writer.write(buffer);
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
        }
    }

    /**
     * Loads status data from file on program start
     */
    private void loadStartParameters() {
        try (BufferedReader reader = Misc.defineReader(Config.file_status)) {
            if (reader == null) return;

            String line;
            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split(": ");

                switch (splitLine[0]) {
                    case "twentyFourCounter":
                        twentyFourCounter = Long.parseLong(splitLine[1]);
                        break;
                    case "sixtyCounter":
                        sixtyCounter = Long.parseLong(splitLine[1]);
                        break;
                    case "tenCounter":
                        tenCounter = Long.parseLong(splitLine[1]);
                        break;
                }
            }
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
        }

        fixCounters();

        String tenMinDisplay = "[10m:" + String.format("%3d", 10 - (System.currentTimeMillis() - tenCounter) / 60000) + " min]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - twentyFourCounter) / 60000) + " min]";
        Main.ADMIN.log_("Loaded params: " + tenMinDisplay + resetTimeDisplay + twentyHourDisplay, -1);

        saveStartParameters();
    }

    //------------------------------------------------------------------------------------------------------------
    // Methods for multi-db file structure
    //------------------------------------------------------------------------------------------------------------

    /**
     * Load in currency data on app start and fill leagueMap with leagues
     */
    private void loadDatabases() {
        for (String league : Main.LEAGUE_MANAGER.getStringLeagues()) {
            File currencyFile = new File(Config.folder_database, league + "/currency.csv");

            if (!currencyFile.exists()) {
                Main.ADMIN.log_("Missing currency file for league: "+league, 2);
                continue;
            }

            try (BufferedReader reader = Misc.defineReader(currencyFile)) {
                if (reader == null) {
                    Main.ADMIN.log_("Could not create currency reader for: "+league, 2);
                    continue;
                }

                CategoryMap categoryMap = leagueMap.getOrDefault(league, new CategoryMap());
                IndexMap indexMap = categoryMap.getOrDefault("currency", new IndexMap());

                String line;
                while ((line = reader.readLine()) != null) {
                    String index = line.substring(0, line.indexOf("::"));
                    Entry entry = new Entry(line, league);
                    indexMap.put(index, entry);
                }

                categoryMap.putIfAbsent("currency", indexMap);
                leagueMap.putIfAbsent(league, categoryMap);
            } catch (IOException ex) {
                Main.ADMIN._log(ex, 4);
            }
        }
    }


    /**
     * Writes all collected data to file
     */
    private void cycle() {
        for (String league : Main.LEAGUE_MANAGER.getStringLeagues()) {
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
                if (twentyFourBool) {
                    Main.HISTORY_CONTROLLER.configure(league, category);
                    Main.HISTORY_CONTROLLER.readFile();
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
                            JSONParcel.add(entry);

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
                        JSONParcel.add(entry);

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
                if (twentyFourBool) {
                    Main.HISTORY_CONTROLLER.writeFile();
                }
            }

            // Commit a sin. This program is made for a box with 512-1024 MBs of memory. Java would run out of heap
            // space without a garbage collection suggestion. Also, it runs only every 24 hours.
            if (twentyFourBool) System.gc();
        }
    }

    /**
     * Writes JSONParcel object to JSON file
     */
    private void writeJSONToFile() {
        for (String league : JSONParcel.getJsonLeagueMap().keySet()) {
            JSONCategoryMap jsonCategoryMap = JSONParcel.getJsonLeagueMap().get(league);

            for (String category : jsonCategoryMap.keySet()) {
                JSONItemList jsonItems = jsonCategoryMap.get(category);

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
        if (current - lastRunTime < Config.entryController_sleepMS) return;
        lastRunTime = System.currentTimeMillis();

        // Don't run if there hasn't been a successful run in the past 30 seconds
        //if ((current - Main.ADMIN.changeIDElement.lastUpdate) / 1000 > 30) return;

        // Raise static flag that suspends other threads while the databases are being worked on
        flipPauseFlag();

        // Run once every 10min
        if (current - tenCounter > Config.entryController_tenMS) {
            tenCounter += (current - tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            tenBool = true;
            Main.ADMIN.log_("10 activated", 0);
        }

        // Run once every 60min
        if (current - sixtyCounter > Config.entryController_sixtyMS) {
            sixtyCounter += (current - sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            sixtyBool = true;
            Main.ADMIN.log_("60 activated", 0);

            // Get a list of active leagues from pathofexile.com's api
            Main.LEAGUE_MANAGER.download();
        }

        // Run once every 24h
        if (current - twentyFourCounter > Config.entryController_twentyFourMS) {
            if (twentyFourCounter == 0) twentyFourCounter -= Config.entryController_counterOffset;
            twentyFourCounter += (current - twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS ;
            twentyFourBool = true;
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
        JSONParcel.sort();
        time_sort = System.currentTimeMillis() - time_sort;

        // Build JSON
        long time_json = System.currentTimeMillis();
        writeJSONToFile();
        time_json = System.currentTimeMillis() - time_json;

        Main.RELATIONS.saveData();

        // Backup output folder
        if (twentyFourBool) {
            Main.ADMIN.log_("Starting backup (after)...", 0);
            long time_backup = System.currentTimeMillis();
            Main.ADMIN.backup(Config.folder_data, "daily_after");
            Main.ADMIN.log_("Backup (after) finished: " + (System.currentTimeMillis() - time_backup) + " ms", 0);
        }

        // Prepare message
        String timeElapsedDisplay = "[Took:" + String.format("%5d", System.currentTimeMillis() - lastRunTime) + " ms]";
        String tenMinDisplay = "[10m:" + String.format("%3d", 10 - (System.currentTimeMillis() - tenCounter) / 60000) + " min]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - twentyFourCounter) / 60000) + " min]";
        String timeTookDisplay = "(Cycle:" + String.format("%5d", time_cycle) + " ms)(JSON:" + String.format("%5d", time_json) +
                " ms)(sort:" + String.format("%5d", time_sort) + " ms)";
        Main.ADMIN.log_(timeElapsedDisplay + tenMinDisplay + resetTimeDisplay + twentyHourDisplay + timeTookDisplay, -1);

        // Clear the parcel
        JSONParcel.clear();

        // Switch off flags
        tenBool = sixtyBool = twentyFourBool = false;
        flipPauseFlag();

        saveStartParameters();
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

        if (current - tenCounter > Config.entryController_tenMS) {
            long gap = (current - tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            tenCounter += gap;
        }

        if (current - sixtyCounter > Config.entryController_sixtyMS) {
            long gap = (current - sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            sixtyCounter += gap;
        }

        if (current - twentyFourCounter > Config.entryController_twentyFourMS) {
            long gap = (current - twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS;
            if (twentyFourCounter == 0) twentyFourCounter -= Config.entryController_counterOffset;
            twentyFourCounter += gap;
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

    public boolean isTenBool() {
        return tenBool;
    }

    public boolean isSixtyBool() {
        return sixtyBool;
    }

    public boolean isTwentyFourBool() {
        return twentyFourBool;
    }

    public boolean isFlagPause() {
        return flagPause;
    }

    public long getTwentyFourCounter() {
        return twentyFourCounter;
    }

    public void setTwentyFourCounter(long twentyFourCounter) {
        this.twentyFourCounter = twentyFourCounter;
    }

    public long getTenCounter() {
        return tenCounter;
    }

    public void setTenCounter(long tenCounter) {
        this.tenCounter = tenCounter;
    }

    public long getSixtyCounter() {
        return sixtyCounter;
    }

    public void setSixtyCounter(long sixtyCounter) {
        this.sixtyCounter = sixtyCounter;
    }
}
