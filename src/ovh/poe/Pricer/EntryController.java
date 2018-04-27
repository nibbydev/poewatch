package ovh.poe.Pricer;

import com.google.gson.Gson;
import ovh.poe.Mappers;
import ovh.poe.Item;
import ovh.poe.Main;

import java.io.*;
import java.util.*;

/**
 * Manages database
 */
public class EntryController {
    // Generic map. Has mappings of: [league name - league map]
    static class EntryMap extends HashMap<String, LeagueMap> { }
    // League map. Has mappings of: [category name - index map]
    static class LeagueMap extends HashMap<String, CategoryMap> { }
    // Category map. Has mappings of: [index - Entry]
    static class CategoryMap extends HashMap<String, Entry> { }

    private final EntryMap entryMap = new EntryMap();

    private final JSONParcel JSONParcel = new JSONParcel();
    private final Object monitor = new Object();
    private final Gson gson = Main.getGson();

    private long lastRunTime = System.currentTimeMillis();
    public volatile boolean flagPause, clearIndexes, tenBool, sixtyBool, twentyFourBool;
    private long twentyFourCounter, sixtyCounter, tenCounter;

    //------------------------------------------------------------------------------------------------------------
    // Upon starting/stopping the program
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
        File paramFile = new File("./data/status.csv");

        try (BufferedWriter writer = defineWriter(paramFile)) {
            if (writer == null) return;

            String buffer = "writeTime: " + System.currentTimeMillis() + "\n";
            buffer += "twentyFourCounter: " + twentyFourCounter + "\n";
            buffer += "sixtyCounter: " + sixtyCounter + "\n";
            buffer += "tenCounter: " + tenCounter + "\n";

            writer.write(buffer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Loads status data from file on program start
     */
    private void loadStartParameters() {
        File paramFile = new File("./data/status.csv");

        try (BufferedReader reader = defineReader(paramFile)) {
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
            ex.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Methods for multi-db file structure
    //------------------------------------------------------------------------------------------------------------

    /**
     * Initializes entryMap with current leagues and categories
     */
    private void buildEntryMapStructure() {
        for (String league : Main.RELATIONS.leagues) {
            LeagueMap leagueMap = entryMap.getOrDefault(league, new LeagueMap());

            for (String category : Main.RELATIONS.categories.keySet()) {
                CategoryMap categoryMap = leagueMap.getOrDefault(category, new CategoryMap());
                leagueMap.putIfAbsent(category, categoryMap);
            }

            entryMap.putIfAbsent(league, leagueMap);
        }
    }

    /**
     * Load in currency data on app start and fill entryMap with leagues
     */
    private void loadDatabases() {
        for (String league : Main.RELATIONS.leagues) {
            File currencyFile = new File("./data/database/" + league + "/currency.csv");

            if (!currencyFile.exists()) {
                System.out.println("Missing currency file for league: " + league);
                continue;
            }

            try (BufferedReader reader = defineReader(currencyFile)) {
                if (reader == null) continue;

                LeagueMap leagueMap = entryMap.getOrDefault(league, new LeagueMap());
                CategoryMap categoryMap = leagueMap.getOrDefault("currency", new CategoryMap());

                String line;
                while ((line = reader.readLine()) != null) {
                    String index = line.substring(0, line.indexOf("::"));
                    Entry entry = new Entry(line, league);
                    categoryMap.put(index, entry);
                }

                leagueMap.putIfAbsent(league, categoryMap);
                entryMap.putIfAbsent(league, leagueMap);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * Writes all collected data to file
     */
    private void cycle() {
        for (String league : Main.RELATIONS.leagues) {
            LeagueMap leagueMap = entryMap.getOrDefault(league, new LeagueMap());
            File leagueFolder = new File("./data/database/"+league+"/");

            if (!leagueFolder.exists()) {
                System.out.println("[asdf] Missing folder for league '"+league+"'");
                leagueFolder.mkdirs();
            }

            for (String category : Main.RELATIONS.categories.keySet()) {
                CategoryMap categoryMap = leagueMap.getOrDefault(category, new CategoryMap());
                Set<String> tmp_unparsedIndexes = new HashSet<>(categoryMap.keySet());
                File leagueFile = new File("./data/database/"+league+"/"+category+".csv");

                // The file data will be stored in
                File tmpLeagueFile = new File("./data/database/"+league+"/"+category+".tmp");

                // Define IO objects
                BufferedReader reader = defineReader(leagueFile);
                BufferedWriter writer = defineWriter(tmpLeagueFile);
                if (writer == null) continue;

                if (reader != null) {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String index = line.substring(0, line.indexOf("::"));
                            Entry entry;

                            if (categoryMap.containsKey(index)) {
                                // Remove processed indexes from the set so that only entries that were found in the map
                                // but not the file will remain
                                tmp_unparsedIndexes.remove(index);

                                if (category.equals("currency")) {
                                    entry = categoryMap.get(index);
                                } else {
                                    entry = categoryMap.remove(index);
                                    entry.parseLine(line);
                                }
                            } else {
                                entry = new Entry(line, league);
                            }

                            entry.setLeague(league);
                            entry.cycle();
                            JSONParcel.add(entry);

                            String writeLine = entry.buildLine();
                            if (writeLine == null) Main.ADMIN.log_("Deleted entry: " + entry.getIndex(), 0);
                            else writer.write(writeLine);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("Missing database '"+category+"' for '"+league+"'");
                }

                try {
                    for (String index : tmp_unparsedIndexes) {
                        Entry entry;

                        if (category.equals("currency")) entry = categoryMap.get(index);
                        else entry = categoryMap.remove(index);

                        entry.setLeague(league);
                        entry.cycle();
                        JSONParcel.add(entry);

                        String writeLine = entry.buildLine();
                        if (writeLine == null) Main.ADMIN.log_("Deleted entry: "+entry.getIndex(), 0);
                        else writer.write(writeLine);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // Close file
                try {
                    if (reader != null) reader.close();
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
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
        // Run every minute (-ish)
        if (System.currentTimeMillis() - lastRunTime < Main.CONFIG.pricerControllerSleepCycle * 1000) return;
        // Don't run if there hasn't been a successful run in the past 30 seconds
        if ((System.currentTimeMillis() - Main.ADMIN.changeIDElement.lastUpdate) / 1000 > 30) return;

        // Raise static flag that suspends other threads while the databases are being worked on
        flipPauseFlag();

        // Run once every 10min
        if ((System.currentTimeMillis() - tenCounter) > 3600000) {
            if (tenCounter - System.currentTimeMillis() < 1) tenCounter = System.currentTimeMillis();
            else tenCounter += 10 * 60 * 1000;

            tenBool = true;
        }

        // Run once every 60min
        if ((System.currentTimeMillis() - sixtyCounter) > 3600000) {
            if (sixtyCounter - System.currentTimeMillis() < 1) sixtyCounter = System.currentTimeMillis();
            else sixtyCounter += 60 * 60 * 1000;

            sixtyBool = true;

            // Get a list of active leagues from pathofexile.com's api
            Main.RELATIONS.getLeagueList();
        }

        // Run once every 24h
        if ((System.currentTimeMillis() - twentyFourCounter) > 86400000) {
            if (twentyFourCounter - System.currentTimeMillis() < 1) twentyFourCounter = System.currentTimeMillis();
            else twentyFourCounter += 24 * 60 * 60 * 1000;

            twentyFourBool = true;
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

        // Prepare message
        String timeElapsedDisplay = "[Took:" + String.format("%4d", (System.currentTimeMillis() - lastRunTime) / 1000) + " sec]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - twentyFourCounter) / 60000) + " min]";
        String timeTookDisplay = " (Cycle:" + String.format("%5d", time_cycle) + " ms) (JSON:" + String.format("%5d", time_json) + " ms) (sort:" + String.format("%5d", time_sort) + " ms)";
        Main.ADMIN.log_(timeElapsedDisplay + resetTimeDisplay + twentyHourDisplay + timeTookDisplay, -1);

        // Set last run time
        lastRunTime = System.currentTimeMillis();

        Main.RELATIONS.saveData();

        // Backup output folder
        if (twentyFourBool) {
            long time_backup = System.currentTimeMillis();
            Main.ADMIN.backup(new File("./data/output"), "daily");
            time_backup = System.currentTimeMillis() - time_backup;
            Main.ADMIN.log_("Backup took: " + time_backup + " ms", 0);
        }

        // Clear the parcel
        JSONParcel.clear();

        // Switch off flags
        tenBool = sixtyBool = twentyFourBool = clearIndexes = false;
        flipPauseFlag();

        saveStartParameters();
    }

    /**
     * Adds entries to the databases
     *
     * @param reply APIReply object that a Worker has downloaded and deserialized
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
                            monitor.wait(500);
                        } catch (InterruptedException ex) {
                        }
                    }
                }

                item.fix();
                item.parseItem();
                if (item.discard) continue;

                LeagueMap leagueMap = entryMap.getOrDefault(item.league, new LeagueMap());
                CategoryMap categoryMap = leagueMap.getOrDefault(item.parentCategory, new CategoryMap());

                String index = Main.RELATIONS.indexItem(item);
                if (index == null) continue; // Some currency items have their icons deleted by parseItem()

                Entry entry = categoryMap.getOrDefault(index, new Entry());
                entry.add(item, stash.accountName, index);

                categoryMap.putIfAbsent(index, entry);
                leagueMap.putIfAbsent(item.parentCategory, categoryMap);
                entryMap.putIfAbsent(item.league, leagueMap);
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
     * Writes JSONParcel object to JSON file
     */
    private void writeJSONToFile() {
        for (Map.Entry<String, Map<String, List<JSONParcel.JSONItem>>> league : JSONParcel.leagues.entrySet()) {
            for (Map.Entry<String, List<JSONParcel.JSONItem>> category : league.getValue().entrySet()) {
                try {
                    new File("./data/output/" + league.getKey()).mkdirs();
                    File file = new File("./data/output/" + league.getKey(), category.getKey() + ".json");

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                    gson.toJson(category.getValue(), writer);

                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Internal utility methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Create a BufferedReader instance
     *
     * @param inputFile File to read
     * @return Created BufferedReader instance
     */
    private BufferedReader defineReader(File inputFile) {
        if (!inputFile.exists())
            return null;

        // Open up the reader (it's fine if the file is missing)
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a BufferedWriter instance
     *
     * @param outputFile File to write
     * @return Created BufferedWriter instance
     */
    private BufferedWriter defineWriter(File outputFile) {
        // Open up the writer (if this throws an exception holy fuck something went massively wrong)
        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public CategoryMap getCurrencyMap (String league) {
        LeagueMap leagueMap = entryMap.getOrDefault(league, null);
        if (leagueMap == null) return null;

        CategoryMap categoryMap = leagueMap.getOrDefault("currency", null);
        if (categoryMap == null) return null;

        return categoryMap;
    }
}
