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
    private final Map<String, Entry> entryMap = new HashMap<>();
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
        // Load in data from CSV file
        loadDatabase();
    }

    private void loadDatabase2() {
        try (BufferedReader reader = defineReader(new File("./data/database.txt"))) {
            if (reader == null) return;

            loadStartParameters(reader.readLine());

            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.substring(0, line.indexOf("::"));
                entryMap.put(key, new Entry(line));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadDatabase() {
        try (BufferedReader reader = defineReader(new File("./data/database.txt"))) {
            if (reader == null) return;

            loadStartParameters(reader.readLine());

            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.substring(0, line.indexOf("::"));
                if (key.contains("|currency|")) entryMap.put(key, new Entry(line));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void saveDatabase() {
        File outputFile = new File("./data/database.txt");
        BufferedWriter writer = defineWriter(outputFile);
        if (writer == null) return;

        try {
            // Write startParameters to file
            writer.write(saveStartParameters());

            // Write new data to file (not found in data file)
            for (Map.Entry<String, Entry> entry : entryMap.entrySet()) {
                String line = entry.getValue().buildLine();
                if (line != null) writer.write(line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void cycle2() {
        // Write everything in currencyMap to file
        for (String key : entryMap.keySet()) {
            Entry entry = entryMap.get(key);
            entry.cycle();
            JSONParcel.add(entry);
        }
    }

    private void cycle() {
        File inputFile = new File("./data", "database.txt");
        File outputFile = new File("./data", "database.temp");

        BufferedReader reader = defineReader(inputFile);
        BufferedWriter writer = defineWriter(outputFile);

        if (reader == null) return;
        if (writer == null) return;

        // Make copy of entryMap's key set
        Set<String> parsedKeys = new HashSet<>(entryMap.keySet());

        try {
            reader.readLine();
            writer.write(saveStartParameters());

            // Add items that are present in the CSV file
            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.substring(0, line.indexOf("::"));
                Entry entry;

                if (entryMap.containsKey(key)) {
                    // Remove all keys that were present in the file
                    parsedKeys.remove(key);

                    if (key.contains("|currency|")) {
                        entry = entryMap.getOrDefault(key, new Entry(line));
                    } else {
                        entry = entryMap.remove(key);
                        entry.parseLine(line);
                    }
                } else {
                    entry = new Entry(line);
                }

                entry.cycle();
                JSONParcel.add(entry);

                writer.write(entry.buildLine());
                writer.flush();
            }

            // Add items that were present in entryMap but no the CSV file
            for (String key : parsedKeys) {
                Entry entry;
                if (key.contains("|currency|")) entry = entryMap.get(key);
                else entry = entryMap.remove(key);

                entry.cycle();
                JSONParcel.add(entry);
                writer.write(entry.buildLine());
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

        //inputFile.delete();
        //outputFile.renameTo(new File("./data", "database.txt"));
    }

    /**
     * Gathers some data and makes start parameters that will be saved in the database file
     *
     * @return Generated CSV-format start params
     */
    private String saveStartParameters() {
        // The general format should look something like this:
        // "writeTime: <long> | cycleCount: 10 | lastClearCycle: <long> | twentyFourCounter: <long>"

        String buffer = "writeTime: " + System.currentTimeMillis();
        buffer += " | twentyFourCounter: " + twentyFourCounter;
        buffer += " | sixtyCounter: " + sixtyCounter;
        buffer += " | tenCounter: " + tenCounter;
        buffer += "\n";

        return buffer;
    }

    private void loadStartParameters(String line) {
        // The general format of line should look something like this:
        // "writeTime: <long> | cycleCount: 10 | lastClearCycle: <long> | twentyFourCounter: <long>"

        // Parse start parameters (database's first line)
        String[] splitLine = line.split(" \\| ");

        // In splitLine we have:
        // ["writeTime: <long>",  "cycleCount: 10",  "lastClearCycle: <long>", "twentyFourCounter: <long>"]

        for (String entry : splitLine) {
            String[] splitEntry = entry.split(": ");

            switch (splitEntry[0]) {
                case "twentyFourCounter":
                    twentyFourCounter = Long.parseLong(splitEntry[1]);
                    break;
                case "sixtyCounter":
                    sixtyCounter = Long.parseLong(splitEntry[1]);
                    break;
                case "tenCounter":
                    tenCounter = Long.parseLong(splitEntry[1]);
                    break;
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
            tenCounter = System.currentTimeMillis();
            tenBool = true;
        }

        // Run once every 60min
        if ((System.currentTimeMillis() - sixtyCounter) > 3600000) {
            // Get a list of active leagues from pathofexile.com's api
            Main.RELATIONS.getLeagueList();

            sixtyCounter = System.currentTimeMillis();
            sixtyBool = true;
        }

        // Run once every 24h
        if ((System.currentTimeMillis() - twentyFourCounter) > 86400000) {
            twentyFourCounter = System.currentTimeMillis();
            twentyFourBool = true;
        }

        // The method that does it all
        long time_cycle = System.currentTimeMillis();
        cycle();
        time_cycle = System.currentTimeMillis() - time_cycle;

        // Save data to file
        long time_file = System.currentTimeMillis();
        saveDatabase();
        time_file = System.currentTimeMillis() - time_file;

        // Build JSON
        long time_json = System.currentTimeMillis();
        writeJSONToFile();
        time_json = System.currentTimeMillis() - time_json;

        // Prepare message
        String timeElapsedDisplay = "[Took:" + String.format("%4d", (System.currentTimeMillis() - lastRunTime) / 1000) + " sec]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - twentyFourCounter) / 60000) + " min]";
        String timeTookDisplay = " (Cycle:" + String.format("%5d", time_cycle) + " ms) (File:" + String.format("%5d", time_file) + " ms) (JSON:" + String.format("%5d", time_json) + " ms)";
        Main.ADMIN.log_(timeElapsedDisplay + resetTimeDisplay + twentyHourDisplay + timeTookDisplay, 1);

        // Set last run time
        lastRunTime = System.currentTimeMillis();

        Main.RELATIONS.saveData();

        // Switch off flags
        tenBool = sixtyBool = twentyFourBool = clearIndexes = false;
        flipPauseFlag();
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

                // Add item to database
                entryMap.putIfAbsent(item.key, new Entry());
                entryMap.get(item.key).add(item, stash.accountName);
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
        JSONParcel.sort();

        for (Map.Entry<String, Map<String, List<JSONParcel.JSONItem>>> league : JSONParcel.leagues.entrySet()) {
            for (Map.Entry<String, List<JSONParcel.JSONItem>> category : league.getValue().entrySet()) {
                try {
                    new File("./data/output/" + league.getKey()).mkdirs();
                    File file = new File("./data/output/" + league.getKey(), category.getKey() + ".json");

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

                    writer.write(gson.toJson(category.getValue()));
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        // Clear the parcel
        JSONParcel.clear();
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

    public Map<String, Entry> getEntryMap() {
        return entryMap;
    }
}
