package com.sanderh.Pricer;

import com.sanderh.Mappers;
import com.sanderh.Item;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import javax.xml.crypto.Data;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.sanderh.Main.*;

public class PricerController {
    //  Name: PricerController
    //  Date created: 28.11.2017
    //  Last modified: 26.12.2017
    //  Description: An object that manages databases

    private final Map<String, DataEntry> entryMap = new HashMap<>();
    private final Map<String, DataEntry> currencyMap = new HashMap<>();
    private final ArrayList<String> JSONParcel = new ArrayList<>();

    private long lastRunTime = System.currentTimeMillis();
    private volatile boolean flagPause = false;
    private final Object monitor = new Object();

    public PricerController() {
        //  Name: PricerController
        //  Date created: 25.12.2017
        //  Last modified: 25.12.2017
        //  Description: Used to load data in on object initialization

        readCurrencyFromFile();
    }

    public void run() {
        //  Name: run()
        //  Date created: 11.12.2017
        //  Last modified: 25.12.2017
        //  Description: Main loop of the pricing service. Is called whenever a worker is assigned a new job

        // Run every minute (-ish)
        if ((System.currentTimeMillis() - lastRunTime) / 1000 < CONFIG.pricerControllerSleepCycle)
            return;
        // Don't run if there hasn't been a successful run in the past 30 seconds
        if ((System.currentTimeMillis() - STATISTICS.getLastSuccessfulPullTime()) / 1000 > 30)
            return;

        // Raise static flag that suspends other threads while the databases are being worked on
        flipPauseFlag();

        // Prepare for database building
        System.out.println(timeStamp() + " Generating databases [" + (DataEntry.getCycleCount() + 1) + "/" +
                CONFIG.dataEntryCycleLimit + "] (" + (System.currentTimeMillis() - lastRunTime) / 1000 + " sec)");

        // Set last run time
        lastRunTime = System.currentTimeMillis();

        // Increase DataEntry's static cycle count
        DataEntry.incCycleCount();

        readFileParseFileWriteFile();
        writeJSONToFile();

        // Zero DataEntry's static cycle count
        if (DataEntry.getCycleState()) {
            DataEntry.zeroCycleCount();
            System.out.println(timeStamp() + " Building JSON");
        }

        // Lower the pause flag, so that other Worker threads may continue using the databases
        flipPauseFlag();
    }

    public void parseItems(Mappers.APIReply reply) {
        //  Name: parseItems()
        //  Date created: 28.11.2017
        //  Last modified: 25.12.2017
        //  Description: Method that's used to add entries to the databases

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
                    currencyMap.get(item.getKey()).add(item);
                } else {
                    entryMap.putIfAbsent(item.getKey(), new DataEntry());
                    entryMap.get(item.getKey()).add(item);
                }
            }
        }
    }

    private void flipPauseFlag() {
        //  Name: flipPauseFlag()
        //  Date created: 21.12.2017
        //  Last modified: 24.12.2017
        //  Description: Switches pause boolean from state to state and wakes monitor

        flagPause = !flagPause;

        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    /////////////////////////////////////////
    // Methods used to interface databases //
    /////////////////////////////////////////

    public void readCurrencyFromFile() {
        //  Name: readCurrencyFromFile()
        //  Date created: 06.12.2017
        //  Last modified: 26.12.2017
        //  Description: Reads data from file and adds to list, reads only lines that have "currency:orbs" in them
        //               Should only be called on initial object creation

        try (BufferedReader reader = defineReader(new File("./database.txt"))) {
            if (reader == null) return;

            String line, key;

            // Set the startParameters, the first line has important data
            loadStartParameters(reader.readLine());

            while ((line = reader.readLine()) != null) {
                key = line.substring(0, line.indexOf("::"));

                if (key.contains("currency:orbs"))
                    currencyMap.put(key, new DataEntry(line));
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void packageJSON(DataEntry entry) {
        //  Name: packageJSON2()
        //  Date created: 13.12.2017
        //  Last modified: 25.12.2017
        //  Description: Builds a JSON-string out of JSON packets

        // Attempt to get JSON-encoded package from database entry
        String JSONPackage = entry.buildJSONPackage();
        if (JSONPackage == null) return;

        // Add new package to parcel
        JSONParcel.add(entry.getKey() + "::" + JSONPackage);
    }

    private void writeJSONToFile() {
        //  Name: writeJSONToFile2()
        //  Date created: 06.12.2017
        //  Last modified: 25.12.2017
        //  Description: Basically writes JSON string to file

        if (JSONParcel.isEmpty())
            return;

        // Sort the list of JSON-encoded packages so they can be written to file
        Collections.sort(JSONParcel);

        // Define historical variables
        String lastLeague = null;
        String lastType = null;
        BufferedWriter writer = null;

        String league, type, pack;
        try {
            for (String line : JSONParcel) {
                league = line.substring(0, line.indexOf("|"));
                type = line.substring(line.indexOf("|") + 1, line.indexOf("|", line.indexOf("|") + 1));
                pack = line.substring(line.indexOf("::") + 2);

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

                // Set new history variables
                lastLeague = league;
                lastType = type;
            }

            // Finalize writing
            if (writer != null) {
                // Add closing bracket to JSON
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

    public void readFileParseFileWriteFile() {
        //  Name: readFileParseFileWriteFile()
        //  Date created: 06.12.2017
        //  Last modified: 26.12.2017
        //  Description: reads data from file (line by line), parses it and writes it back

        ArrayList<String> currencyKeysWrittenToFile = new ArrayList<>();
        String line, key;
        DataEntry entry;

        File inputFile = new File("./database.txt");
        File outputFile = new File("./database.temp");

        BufferedReader reader = defineReader(inputFile);
        BufferedWriter writer = defineWriter(outputFile);

        // If there was a problem opening the writer, something seriously went wrong. Close the reader if necessary and
        // return from the method.
        if (writer == null) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
            return;
        }

        try {
            // Write startParameters to file
            writer.write(saveStartParameters());

            // Write currency data to file
            for (String key2 : currencyMap.keySet()) {
                currencyKeysWrittenToFile.add(key2);
                entry = currencyMap.get(key2);
                entry.cycle();
                packageJSON(entry);
                writer.write(entry.buildLine());
            }

            // Read, parse and write file
            if (reader != null) {
                while ((line = reader.readLine()) != null) {
                    key = line.substring(0, line.indexOf("::"));

                    // If new values have been found, append them to the line and write it to the file, else just write
                    // line to file
                    if (currencyMap.containsKey(key))
                        continue;
                    else if (entryMap.containsKey(key))
                        entry = entryMap.get(key);
                    else if (key.length() == 5)
                        continue;
                    else
                        entry = new DataEntry();

                    entry.cycle(line);
                    packageJSON(entry);
                    entryMap.remove(key);

                    // Skip currency entries that have already been written to file
                    if (currencyKeysWrittenToFile.contains(key))
                        continue;

                    // Write line to temp output file
                    writer.write(entry.buildLine());
                }
            }

            // Write leftover item data to file
            for (String key2 : entryMap.keySet()) {
                entry = entryMap.get(key2);
                entry.cycle();
                packageJSON(entry);
                writer.write(entry.buildLine());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
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
        //  Last modified: 26.12.2017
        //  Description: Parses whatever data was saved in the database file's first line

        String[] splitLine = line.split("::");

        // First parameter is the version of the config, I suppose
        switch (splitLine[0]) {
            // Version 00000
            case "00000":
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
        //  Last modified: 26.12.2017
        //  Description: Gathers some data and makes start parameters that will be saved in the database file

        String builder;

        builder = "00000"
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
