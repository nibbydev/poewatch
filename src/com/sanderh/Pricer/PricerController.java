package com.sanderh.Pricer;

import com.sanderh.Mappers;
import com.sanderh.Item;

import java.io.*;
import java.util.*;

import static com.sanderh.Main.*;

public class PricerController {
    //  Name: PricerController
    //  Date created: 28.11.2017
    //  Last modified: 21.12.2017
    //  Description: An object that manages databases

    private final Map<String, DataEntry> entryMap = new TreeMap<>();
    private final StringBuilder JSONBuilder = new StringBuilder();

    private String lastLeague = "";
    private String lastType = "";

    private long lastRunTime = System.currentTimeMillis();
    private volatile boolean flagPause = false;
    private final Object monitor = new Object();

    public PricerController() {
        // Load data in on initial script launch
        readDataFromFile();
    }

    public void run() {
        //  Name: run()
        //  Date created: 11.12.2017
        //  Last modified: 21.12.2017
        //  Description: Main loop of the pricing service

        // Run every minute (-ish)
        if ((System.currentTimeMillis() - lastRunTime) / 1000 < CONFIG.pricerControllerSleepCycle)
            return;
        // Don't run if there hasn't been a successful run in the past 30 seconds
        if ((System.currentTimeMillis() - STATISTICS.getLastSuccessfulPullTime()) / 1000 > 30)
            return;

        // Raise static flag that suspends other threads while the databases are being worked on
        flipPauseFlag();

        // Prepare for database building
        System.out.println(timeStamp() + " Generating databases (" + (System.currentTimeMillis() - lastRunTime) / 1000 + " sec)");

        // Set last run time
        lastRunTime = System.currentTimeMillis();

        // Increase DataEntry's static cycle count
        DataEntry.incCycleCount();

        // Loop through database entries, calling their methods
        entryMap.forEach((String key, DataEntry entry) -> packageJSON(entry.databaseBuilder()));
        JSONBuilder.append("}");
        writeJSONToFile();

        // Write generated data to file
        writeDataToFile();

        // Zero DataEntry's static cycle count
        if (DataEntry.getCycleState()) {
            DataEntry.zeroCycleCount();
            System.out.println(timeStamp() + " Building JSON");
        }

        // Clean up after building
        JSONBuilder.setLength(0);
        lastLeague = "";
        lastType = "";

        // Lower the pause flag, so that other Worker threads may continue using the databases
        flipPauseFlag();
    }

    public void parseItems(Mappers.APIReply reply) {
        //  Name: parseItems()
        //  Date created: 28.11.2017
        //  Last modified: 21.12.2017
        //  Description: Method that's used to add entries to the databases

        // Loop through every single item, checking every single one of them
        for (Mappers.Stash stash : reply.getStashes()) {
            for (Item item : stash.getItems()) {
                // Snooze. The lock will be lifted in about 0.1 seconds. This loop is NOT time-sensitive
                while (flagPause) {
                    synchronized (monitor) {
                        try {
                            monitor.wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }

                // Parse item data
                item.parseItem();
                if (item.isDiscard())
                    continue;

                // Add item to database
                entryMap.putIfAbsent(item.getKey(), new DataEntry());
                entryMap.get(item.getKey()).addRaw(item);
            }
        }
    }

    private void flipPauseFlag() {
        //  Name: flipPauseFlag()
        //  Date created: 21.12.2017
        //  Last modified: 21.12.2017
        //  Description: Switches boolean from state to state and wakes monitor

        flagPause = !flagPause;

        synchronized (monitor) {
            monitor.notifyAll();
        }

    }

    //////////////////////////////////////
    // Methods used to manage databases //
    //////////////////////////////////////

    public void readDataFromFile() {
        //  Name: readDataFromFile()
        //  Date created: 06.12.2017
        //  Last modified: 20.12.2017
        //  Description: Reads and parses database data from file

        String line;
        File file = new File("./database.txt");

        if (!file.exists())
            return;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitLine = line.split("::");

                entryMap.put(splitLine[0], new DataEntry());
                entryMap.get(splitLine[0]).parseIOLine(splitLine);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void writeDataToFile() {
        //  Name: writeDataToFile()
        //  Date created: 06.12.2017
        //  Last modified: 18.12.2017
        //  Description: Writes database data to file

        OutputStream fOut = null;

        // Writes values from statistics to file
        try {
            File fFile = new File("./database.txt");
            fOut = new FileOutputStream(fFile);

            for (String key : entryMap.keySet()) {
                if (!entryMap.get(key).isEmpty())
                    fOut.write(entryMap.get(key).makeIOLine().getBytes());
            }

        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write ./database.txt:");
            ex.printStackTrace();
        } finally {
            try {
                if (fOut != null) {
                    fOut.flush();
                    fOut.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void packageJSON(DataEntry entry) {
        //  Name: packageJSON2()
        //  Date created: 13.12.2017
        //  Last modified: 13.12.2017
        //  Description: Builds a JSON-string out of JSON packets

        String JSONPackage = entry.buildJSONPackage();
        if (JSONPackage.equals(""))
            return;

        if (lastLeague.equals("")) {
            JSONBuilder.append("{");
        } else if (!lastLeague.equals(entry.getLeague())) {
            JSONBuilder.append("}}");
            lastType = "";

            writeJSONToFile();
            JSONBuilder.setLength(0);
            JSONBuilder.append("{");
        }

        if (lastType.equals("")) {
            JSONBuilder.append("\"");
            JSONBuilder.append(entry.getType());
            JSONBuilder.append("\":{");
        } else if (lastType.equals(entry.getType())) {
            JSONBuilder.append(",");
        } else {
            JSONBuilder.append("},\"");
            JSONBuilder.append(entry.getType());
            JSONBuilder.append("\":{");
        }

        lastLeague = entry.getLeague();
        lastType = entry.getType();
        JSONBuilder.append(JSONPackage);
    }

    private void writeJSONToFile() {
        //  Name: writeJSONToFile2()
        //  Date created: 06.12.2017
        //  Last modified: 18.12.2017
        //  Description: Basically writes JSON string to file

        if (JSONBuilder.length() < 5)
            return;

        OutputStream fOut = null;

        // Writes values from statistics to file
        try {
            File fFile = new File("./http/data/" + lastLeague + ".json");
            fOut = new FileOutputStream(fFile);
            fOut.write(JSONBuilder.toString().getBytes());

        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write ./http/data" + lastLeague + ".json");
            ex.printStackTrace();
        } finally {
            try {
                if (fOut != null) {
                    fOut.flush();
                    fOut.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public Map<String, DataEntry> getEntryMap() {
        return entryMap;
    }
}
