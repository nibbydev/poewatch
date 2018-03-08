package ovh.poe.Pricer;

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
    private final ArrayList<String> JSONParcel = new ArrayList<>();

    private long lastRunTime = System.currentTimeMillis();
    private volatile boolean flagPause = false;
    private final Object monitor = new Object();
    private final ArrayList<String> keyBlackList = new ArrayList<>();

    /**
     * Loads data in from file on object initialization
     */
    public PricerController() {
        ReadBlackListFromFile();
        readCurrencyFromFile();
    }

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
        System.out.println(Main.timeStamp() + " Generating databases [" + (DataEntry.getCycleCount() + 1) + "/" +
                Main.CONFIG.dataEntryCycleLimit + "] (" + (System.currentTimeMillis() - lastRunTime) / 1000 + " sec)");

        // Set last run time
        lastRunTime = System.currentTimeMillis();

        // Increase DataEntry's static cycle count
        DataEntry.incCycleCount();

        readFileParseFileWriteFile();
        writeJSONToFile();

        // Zero DataEntry's static cycle count
        if (DataEntry.getCycleState()) {
            DataEntry.zeroCycleCount();
            System.out.println(Main.timeStamp() + " Building JSON");
        }

        // Lower the pause flag, so that other Worker threads may continue using the databases
        flipPauseFlag();
    }

    /**
     * Adds entries to the databases
     *
     * @param reply APIReply object that a Worker has downloaded and deserialized
     */
    public void parseItems(Mappers.APIReply reply) {
        // Loop through every single item, checking every single one of them
        for (Mappers.Stash stash : reply.getStashes()) {
            for (Item item : stash.getItems()) {
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
                item.parseItem();
                if (item.isDiscard())
                    continue;

                // Add item to database, separating currency
                if (item.getKey().contains("currency:orbs")) {
                    currencyMap.putIfAbsent(item.getKey(), new DataEntry());
                    currencyMap.get(item.getKey()).add(item, stash.getAccountName());
                } else {
                    entryMap.putIfAbsent(item.getKey(), new DataEntry());
                    entryMap.get(item.getKey()).add(item, stash.getAccountName());
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

    /////////////////////////////////////////
    // Methods used to interface databases //
    /////////////////////////////////////////

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

                if (key.contains("currency:orbs")) currencyMap.put(key, new DataEntry(line));
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Calls DataEntry to form a JSON-encoded string and appends that string to the JSON StringBuilder
     *
     * @param entry DataEntry object which should create a JSON package
     */
    private void packageJSON(DataEntry entry) {
        // Attempt to get JSON-encoded package from database entry
        String JSONPackage = entry.JSONController();
        if (JSONPackage == null) return;

        // Add new package to parcel
        JSONParcel.add(entry.getKey() + "::" + JSONPackage);
    }

    /**
     * Takes the CSV-format JSONParcel, converts it to a valid JSON string and writes the result to different files
     */
    private void writeJSONToFile() {
        if (JSONParcel.isEmpty()) return;

        // Sort the list of JSON-encoded packages so they can be written to file
        Collections.sort(JSONParcel);

        // Define historical variables
        String lastLeague = null;
        String lastType = null;
        BufferedWriter writer = null;

        try {
            for (String line : JSONParcel) {
                String league = line.substring(0, line.indexOf("|"));
                String type = line.substring(line.indexOf("|") + 1, line.indexOf("|", line.indexOf("|") + 1));
                String pack = line.substring(line.indexOf("::") + 2);

                // Prepare league changes (since each league will be written in a different file)
                if (lastLeague == null || !lastLeague.equals(league)) {
                    // If another writer was active, close it and start a new one
                    if (writer != null) {
                        // Write JSON closing brackets
                        writer.write("}}", 0, 2);
                        writer.flush();
                        writer.close();
                    }

                    // League changed or this is the first time writing, need to create a new writer
                    writer = defineWriter(new File("./http/data/" + league + ".json"));
                    if (writer == null) throw new NullPointerException();

                    // Write JSON opening bracket
                    writer.write("{", 0, 1);

                    // Clear type (since it obviously will change)
                    lastType = null;
                }

                // Prepare type changes
                if (lastType == null) {
                    writer.write("\"" + type + "\":{", 0, 4 + type.length());
                } else if (lastType.equals(type)) {
                    writer.write(",", 0, 1);
                } else {
                    writer.write("},\"" + type + "\":{", 0, 6 + type.length());
                }

                // Write pack
                writer.write(pack, 0, pack.length());

                // Flush output
                writer.flush();

                // Set new history variables
                lastLeague = league;
                lastType = type;
            }

            // Finalize writing
            if (writer != null) {
                // Add closing bracket to JSON
                writer.write("}", 0, 1);

                // idk man
                if (lastLeague.equals("Standard"))
                    writer.write("}", 0, 1);

                // Flush output
                writer.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            // Clear the parcel
            JSONParcel.clear();
        }
    }

    //////////////////
    // New methods ///
    //////////////////

    private boolean checkKey(String key) {
        String[] splitKey = key.split("\\|");

        // Abyss|gems:vaal|Vaal Lightning Warp|4|1|20|1
        if (splitKey[3].equals("4")) {
            int lvl = Integer.parseInt(splitKey[4]);
            int quality = Integer.parseInt(splitKey[5]);
            String cor = splitKey[6];

            if (key.contains("Empower") || key.contains("Enlighten") || key.contains("Enhance")) {
                return false;
            } else if (key.contains("Vaal ")) {
                // Allow only if its "vaal" and is corrupted
                return cor.equals("0");
            } else if (cor.equals("1")) {
                // If gem is not vaal gem and is corrupted, remove if
                if (lvl < 20 && quality > 20) return true;
                else if (lvl == 21 && quality == 10) return true;
                else if (lvl < 20 && quality < 20) return true;
            }

            return (lvl > 21 || quality > 23);
        }

        return false;
    }

    private void readFileParseFileWriteFile() {
        //  Name: readFileParseFileWriteFile()
        //  Date created: 06.12.2017
        //  Last modified: 26.01.2018
        //  Description: reads data from file (line by line), parses it and writes it back

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
                packageJSON(entry);
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
                    packageJSON(entry);
                    entryMap.remove(key);

                    // Write line to temp output file
                    writer.write(entry.buildLine());
                }
            }

            // Write new data to file (not found in data file)
            for (String key : entryMap.keySet()) {
                DataEntry entry = entryMap.get(key);
                entry.cycle();
                packageJSON(entry);
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

    private BufferedReader defineReader(File inputFile) {
        //  Name: defineReader()
        //  Date created: 25.12.2017
        //  Last modified: 25.12.2017
        //  Description: Assigns reader buffer

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

    private BufferedWriter defineWriter(File outputFile) {
        //  Name: defineWriter()
        //  Date created: 25.12.2017
        //  Last modified: 25.12.2017
        //  Description: Assigns writer buffer

        // Open up the writer (if this throws an exception holy fuck something went massively wrong)
        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void loadStartParameters(String line) {
        //  Name: loadStartParameters()
        //  Date created: 26.12.2017
        //  Last modified: 27.01.2018
        //  Description: Parses whatever data was saved in the database file's first line

        String[] splitLine = line.split("::");

        // First parameter is the version of the config, I suppose
        switch (splitLine[0]) {
            // Version 00000
            case "00001":
                // 0 - version nr
                // 1 - last build/write time
                // 2 - cycle counter

                System.out.println("[INFO] Found start parameters:\n    Cycle counter: " + splitLine[2] +
                        "\n    Last write time: " + (System.currentTimeMillis() - Long.parseLong(splitLine[1])) /
                        1000 + " sec ago");

                // Set the cycle counter to whatever is in the file
                DataEntry.setCycleCount(Integer.parseInt(splitLine[2]));
                break;
        }
    }

    private String saveStartParameters() {
        //  Name: saveStartParameters()
        //  Date created: 26.12.2017
        //  Last modified: 22.01.2018
        //  Description: Gathers some data and makes start parameters that will be saved in the database file

        String builder;

        builder = "00001"
                + "::"
                + System.currentTimeMillis()
                + "::"
                + DataEntry.getCycleCount()
                + "\n";

        return builder;
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public Map<String, DataEntry> getCurrencyMap() {
        return currencyMap;
    }
}
