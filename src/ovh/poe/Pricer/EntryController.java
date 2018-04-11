package ovh.poe.Pricer;

import com.google.gson.Gson;
import ovh.poe.AdminSuite;
import ovh.poe.Mappers;
import ovh.poe.Item;
import ovh.poe.Main;

import java.io.*;
import java.util.*;

/**
 * Manages database
 */
public class EntryController {
    private final Map<String, Map<String, Entry>> entryMap = new HashMap<>();
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
        // Load in data from CSV file
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

    // Load in currency data on app start
    private void loadDatabases() {
        File dbFolder = new File("./data/database");
        List<File> dbFiles = new ArrayList<>();
        AdminSuite.getAllFiles(dbFolder, dbFiles);

        for (File dbFile : dbFiles) {
            try (BufferedReader reader = defineReader(dbFile)) {
                if (reader == null) continue;

                String league = dbFile.getName().substring(0, dbFile.getName().indexOf("."));
                Map<String, Entry> leagueMap = entryMap.getOrDefault(league, new HashMap<>());

                String line;
                while ((line = reader.readLine()) != null) {
                    String key = line.substring(0, line.indexOf("::"));
                    if (key.contains("currency|")) leagueMap.put(key, new Entry(line));
                }

                entryMap.putIfAbsent(league, leagueMap);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void cycle() {
        File dbFolder = new File("./data/database");
        List<File> dbFiles = new ArrayList<>();
        AdminSuite.getAllFiles(dbFolder, dbFiles);
        List<String> parsedLeagues = new ArrayList<>();

        // Loop through database files
        for (File dbFile : dbFiles) {
            String league = dbFile.getName().substring(0, dbFile.getName().indexOf("."));
            Map<String, Entry> leagueMap = entryMap.getOrDefault(league, new HashMap<>());

            // Add current working league to list
            parsedLeagues.add(league);

            File dbFileTmp = null;
            try {
                dbFileTmp = new File(dbFile.getCanonicalPath() + ".tmp");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (dbFileTmp == null) continue;

            BufferedReader reader = defineReader(dbFile);
            if (reader == null) continue;
            BufferedWriter writer = defineWriter(dbFileTmp);
            if (writer == null) continue;

            try {
                // Make copy of entryMap's key set for the current league
                Set<String> unparsedKeys = new HashSet<>(leagueMap.keySet());

                // Add items that are present in the CSV file
                String line;
                while ((line = reader.readLine()) != null) {
                    String key = line.substring(0, line.indexOf("::"));
                    Entry entry;

                    if (leagueMap.containsKey(key)) {
                        // Remove all keys that were present in the file
                        unparsedKeys.remove(key);

                        if (key.contains("currency|")) {
                            entry = leagueMap.getOrDefault(key, new Entry(line));
                        } else {
                            entry = leagueMap.remove(key);
                            entry.parseLine(line);
                        }
                    } else {
                        entry = new Entry(line);
                    }

                    entry.setLeague(league);
                    entry.cycle();
                    JSONParcel.add(entry);

                    String writeLine = entry.buildLine();
                    if (writeLine == null) Main.ADMIN.log_("Deleted entry: " + entry.getKey(), 0);
                    else writer.write(writeLine);
                }

                // Add items that were present in entryMap but not the CSV file
                for (String key : unparsedKeys) {
                    Entry entry;
                    if (key.contains("currency|")) entry = leagueMap.get(key);
                    else entry = leagueMap.remove(key);

                    entry.setLeague(league);
                    entry.cycle();
                    JSONParcel.add(entry);

                    String writeLine = entry.buildLine();
                    if (writeLine == null) Main.ADMIN.log_("Deleted entry: " + entry.getKey(), 0);
                    else writer.write(writeLine);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    reader.close();
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    Main.ADMIN.log_(ex.getMessage(), 3);
                }
            }

            if (!dbFile.delete()) Main.ADMIN.log_("Unable to remove database.tmp", 4);
            if (!dbFileTmp.renameTo(dbFile)) Main.ADMIN.log_("Unable to rename database.tmp", 4);
        }

        for (Map.Entry<String, Map<String, Entry>> tmpMap : entryMap.entrySet()) {
            String league = tmpMap.getKey();
            Map<String, Entry> leagueMap = tmpMap.getValue();

            // Skip the leagues that were already present in the CSV files
            if (parsedLeagues.contains(league)) continue;

            File dbFile = new File(dbFolder, league + ".csv");
            BufferedWriter writer = defineWriter(dbFile);
            if (writer == null) continue;

            try {
                // Add items that were present in entryMap but not the CSV file
                for (String key : new HashSet<>(leagueMap.keySet())) {
                    Entry entry;
                    if (key.contains("currency|")) entry = leagueMap.get(key);
                    else entry = leagueMap.remove(key);

                    entry.setLeague(league);
                    entry.cycle();
                    JSONParcel.add(entry);

                    String writeLine = entry.buildLine();
                    if (writeLine == null) Main.ADMIN.log_("Deleted entry: " + entry.getKey(), 0);
                    else writer.write(writeLine);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    Main.ADMIN.log_(ex.getMessage(), 3);
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

                // Parse item data
                item.fix();
                item.parseItem();
                if (item.discard) continue;

                Map<String, Entry> leagueMap = entryMap.getOrDefault(item.league, new HashMap<>());
                leagueMap.putIfAbsent(item.key, new Entry());
                leagueMap.get(item.key).add(item, stash.accountName);
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

    public Map<String, Map<String, Entry>> getEntryMap() {
        return entryMap;
    }
}
