package ovh.poe.Pricer;

import com.google.gson.Gson;
import ovh.poe.Mappers;
import ovh.poe.Item;
import ovh.poe.Main;

import java.io.*;
import java.util.*;

/**
 * Manages CSV database
 */
public class PricerController {
    private final Map<String, DataEntry> entryMap = new HashMap<>();
    private final Map<String, DataEntry> currencyMap = new HashMap<>();

    private long lastRunTime = System.currentTimeMillis();
    private volatile boolean flagPause = false;
    private final Object monitor = new Object();
    private final ArrayList<String> keyBlackList = new ArrayList<>();

    private Mappers.JSONParcel JSONParcel = new Mappers.JSONParcel();
    private Gson gson = Main.getGson();
    private long lastClearCycle;
    public volatile boolean clearStats = false;
    public volatile boolean clearIndexes = false;
    private volatile boolean writeJSON = false;
    private int cycleCount = 0;

    ////////////////////////////////////////
    // Upon starting/stopping the program //
    ////////////////////////////////////////

    /**
     * Loads data in from file on object initialization
     */
    public PricerController() {
        ReadBlackListFromFile();
        readCurrencyFromFile();
    }

    /**
     * Loads in list of keys that should be removed from the database during program start
     */
    private void ReadBlackListFromFile() {
        try (BufferedReader reader = defineReader(new File("./blacklist.txt"))) {
            if (reader == null) return;

            String line;

            while ((line = reader.readLine()) != null) keyBlackList.add(line);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Reads currency data from file and adds to list. Should only be called on initial object creation
     */
    private void readCurrencyFromFile() {
        try (BufferedReader reader = defineReader(new File("./database.txt"))) {
            if (reader == null) return;

            String line, key;

            // Set the startParameters, the first line has important data
            loadStartParameters(reader.readLine());

            while ((line = reader.readLine()) != null) {
                key = line.substring(0, line.indexOf("::"));

                if (keyBlackList.contains(key)) continue;

                if (key.contains("currency")) currencyMap.put(key, new DataEntry(line));
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Parses whatever data was saved in the database file's first line
     *
     * @param line CSV format starting line
     */
    private void loadStartParameters(String line) {
        String[] splitLine = line.split("::");

        // First parameter is the version of the config, I suppose
        switch (splitLine[0]) {
            case "00002":
                // 0 - version nr
                // 1 - last build/write time
                // 2 - cycle counter
                // 3 - last clear time

                System.out.println("[INFO] Found start parameters:");
                System.out.println("    Cycle counter: " + splitLine[2]);

                long lastWriteTime = (System.currentTimeMillis() - Long.parseLong(splitLine[1])) / 1000;
                System.out.println("    Last write time: " + lastWriteTime + " sec ago");


                // Set the cycle counter to whatever is in the file
                cycleCount = Integer.parseInt(splitLine[2]);

                lastClearCycle = Long.parseLong(splitLine[3]);
                break;
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

        // Prepare for database building
        System.out.println(Main.timeStamp() + " Generating databases [" + (cycleCount + 1) + "/" +
                Main.CONFIG.dataEntryCycleLimit + "] (" + (System.currentTimeMillis() - lastRunTime) / 1000 + " sec)" +
                "(reset " + (60 - (System.currentTimeMillis() - lastClearCycle) / 60000) + " min)"
        );

        // Set last run time
        lastRunTime = System.currentTimeMillis();

        // Increase DataEntry's static cycle count
        cycleCount++;

        if ((System.currentTimeMillis() - lastClearCycle) > 3600000) {
            lastClearCycle = System.currentTimeMillis();
            clearStats = true;
        }

        // Zero DataEntry's static cycle count
        if (cycleCount >= Main.CONFIG.dataEntryCycleLimit) {
            cycleCount = 0;
            writeJSON = true;
            System.out.println(Main.timeStamp() + " Building JSON");
        }

        // The method that does it all
        readFileParseFileWriteFile();

        // If this cycle is the JSON writing cycle
        if (writeJSON) {
            // Write JSON to file
            writeJSONToFile();
            // Save generated icon data
            Main.RELATIONS.saveData();
        }

        // Switch off flags
        clearStats = clearIndexes = writeJSON = false;
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
                if (item.discard)
                    continue;

                // Add item to database, separating currency
                if (item.key.contains("currency")) {
                    currencyMap.putIfAbsent(item.key, new DataEntry());
                    currencyMap.get(item.key).add(item, stash.accountName);
                } else {
                    entryMap.putIfAbsent(item.key, new DataEntry());
                    entryMap.get(item.key).add(item, stash.accountName);
                }
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

        for (Map.Entry<String, Map<String, List<Mappers.JSONParcel.Item>>> entry : JSONParcel.leagues.entrySet()) {
            try {
                BufferedWriter writer = defineWriter(new File("./http/api/data/" + entry.getKey() + ".json"));
                if (writer == null) continue;

                writer.write(gson.toJson(entry.getValue()));
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // Clear the parcel
        JSONParcel.clear();
    }

    /**
     * Reads data from file (line by line), parses it and writes it back
     */
    private void readFileParseFileWriteFile() {
        File inputFile = new File("./database.txt");
        File outputFile = new File("./database.temp");

        BufferedReader reader = defineReader(inputFile);
        BufferedWriter writer = defineWriter(outputFile);

        // If there was a problem opening the writer, something seriously went wrong. Close the reader if necessary and
        // return from the method.
        if (writer == null) {
            if (reader != null) try {
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }

        try {
            // Write startParameters to file
            writer.write(saveStartParameters());

            // Write everything in currencyMap to file
            for (String key : currencyMap.keySet()) {
                DataEntry entry = currencyMap.get(key);
                entry.cycle();
                if (writeJSON) JSONParcel.add(entry);
                writer.write(entry.buildLine());
            }

            // Re-write everything in file
            if (reader != null) {
                // Read in the first line which holds version info
                reader.readLine();

                String line;
                DataEntry entry;
                while ((line = reader.readLine()) != null) {
                    String key = line.substring(0, line.indexOf("::"));

                    /*
                    if (checkKey(key)) {
                        System.out.println("removed: " + key);
                        continue;
                    }
                     */

                    // Ignore some items
                    // if (keyBlackList.contains(key)) continue;
                    // Ignore currency that's stored in a separate list
                    if (currencyMap.containsKey(key)) continue;

                    // Create an instance of DataEntry related to the item
                    if (entryMap.containsKey(key)) entry = entryMap.get(key);
                    else entry = new DataEntry();

                    entry.cycle(line);
                    if (writeJSON) JSONParcel.add(entry);
                    entryMap.remove(key);

                    // Write line to temp output file
                    writer.write(entry.buildLine());
                }
            }

            // Write new data to file (not found in data file)
            for (String key : entryMap.keySet()) {
                DataEntry entry = entryMap.get(key);
                entry.cycle();
                if (writeJSON) JSONParcel.add(entry);
                writer.write(entry.buildLine());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // Clear entryMap
        entryMap.clear();

        if (inputFile.exists() && !inputFile.delete())
            System.out.println("[ERROR] Could not delete: " + inputFile.getName());
        if (!outputFile.renameTo(inputFile))
            System.out.println("[ERROR] Could not rename: " + outputFile.getName() + " to " + inputFile.getName());
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

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public Map<String, DataEntry> getCurrencyMap() {
        return currencyMap;
    }
}
