package ovh.poe.Pricer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ovh.poe.Mappers;
import ovh.poe.Item;
import ovh.poe.Main;

import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Manages database
 */
public class PricerController {
    private final Map<String, DataEntry> entryMap = new HashMap<>();

    private long lastRunTime = System.currentTimeMillis();
    private volatile boolean flagPause = false;
    private final Object monitor = new Object();

    private JSONParcel JSONParcel = new JSONParcel();
    private Gson gson = Main.getGson();
    private long lastClearCycle;
    public volatile boolean clearStats = false;
    public volatile boolean clearIndexes = false;
    public volatile boolean twentyFourBool = false;
    private int cycleCount = 0;
    private long twentyFourCounter;

    ////////////////////////////////////////
    // Upon starting/stopping the program //
    ////////////////////////////////////////

    /**
     * Loads data in from file on object initialization
     */
    public PricerController() {
        // Load in data from CSV file
        loadDatabase();
    }

    private void loadDatabase() {
        try (BufferedReader reader = defineReader(new File("./data/database.txt"))) {
            if (reader == null) return;

            // Parse whatever data was saved in the database file's first line
            String[] splitLine = reader.readLine().split("::");

            System.out.println("[INFO] Found start parameters:");
            System.out.println("    Cycle counter: " + splitLine[2]);
            long lastWriteTime = (System.currentTimeMillis() - Long.parseLong(splitLine[1])) / 1000;
            System.out.println("    Last write time: " + lastWriteTime + " sec ago");

            // Set the cycle counter to whatever is in the file
            cycleCount = Integer.parseInt(splitLine[2]);
            lastClearCycle = Long.parseLong(splitLine[3]);
            twentyFourCounter = Long.parseLong(splitLine[4]);

            String line;
            while ((line = reader.readLine()) != null) {
                String key = line.substring(0, line.indexOf("::"));
                entryMap.put(key, new DataEntry(line));
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
            for (Map.Entry<String, DataEntry> entry : entryMap.entrySet()) {
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

    private void cycle() {
        // Write everything in currencyMap to file
        for (String key : entryMap.keySet()) {
            DataEntry entry = entryMap.get(key);
            entry.cycle();
            JSONParcel.add(entry);
        }
    }

    /**
     * Gathers some data and makes start parameters that will be saved in the database file
     *
     * @return Generated CSV-format start params
     */
    private String saveStartParameters() {
        String builder;

        builder = "00002"
                + "::"
                + System.currentTimeMillis()
                + "::"
                + cycleCount
                + "::"
                + lastClearCycle
                + "::"
                + twentyFourCounter
                + "\n";

        return builder;
    }


    /////////////////////////////////////
    // Often called controller methods //
    /////////////////////////////////////

    /**
     * Main loop of the pricing service. Can be called whenever, only runs after specific amount of time has passed
     */
    public void run() {
        // Run every minute (-ish)
        if ((System.currentTimeMillis() - lastRunTime) / 1000 < Main.CONFIG.pricerControllerSleepCycle) return;
        // Don't run if there hasn't been a successful run in the past 30 seconds
        if ((System.currentTimeMillis() - Main.STATISTICS.getLastSuccessfulPullTime()) / 1000 > 30) return;

        // Raise static flag that suspends other threads while the databases are being worked on
        flipPauseFlag();

        // Get a list of leagues from pathofexile.com. Will only run every 30 minutes
        Main.RELATIONS.getLeagueList();

        // Count cycles
        if (cycleCount >= Main.CONFIG.dataEntryCycleLimit) cycleCount = 0;
        else cycleCount++;

        // Clear stats ever x-minutes
        if ((System.currentTimeMillis() - lastClearCycle) > 3600000) {
            lastClearCycle = System.currentTimeMillis();
            clearStats = true;
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
        String cycleCounterDisplay = "[" + String.format("%2d", cycleCount) + "/" + String.format("%2d", Main.CONFIG.dataEntryCycleLimit) + "]";
        String timeElapsedDisplay = "[Took:" + String.format("%4d", (System.currentTimeMillis() - lastRunTime) / 1000) + " sec]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - lastClearCycle) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - twentyFourCounter) / 60000) + " min]";
        String timeTookDisplay = " (Cycle:" + String.format("%5d", time_cycle) + " ms) (File:" + String.format("%5d", time_file) + " ms) (JSON:" + String.format("%5d", time_json) + " ms)";

        System.out.println(Main.timeStamp() + cycleCounterDisplay + timeElapsedDisplay + resetTimeDisplay + twentyHourDisplay + timeTookDisplay);

        // Set last run time
        lastRunTime = System.currentTimeMillis();

        Main.RELATIONS.saveData();

        // Switch off flags
        clearStats = clearIndexes = false;
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
                entryMap.putIfAbsent(item.key, new DataEntry());
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

    //////////////////////////////
    // Internal utility methods //
    //////////////////////////////

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

    public Map<String, DataEntry> getEntryMap() {
        return entryMap;
    }
}
