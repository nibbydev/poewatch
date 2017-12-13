package MainPack.PricerClasses;

import MainPack.MapperClasses.APIReply;
import MainPack.MapperClasses.Item;
import MainPack.MapperClasses.Stash;

import java.io.*;
import java.util.*;

import static MainPack.Main.PROPERTIES;
import static MainPack.Main.timeStamp;

public class PricerController extends Thread {
    //  Name: PricerController
    //  Date created: 28.11.2017
    //  Last modified: 13.12.2017
    //  Description: A threaded object that manages databases

    private static boolean flagLocalRun = true;
    private static boolean flagPause = true;
    private static final Map<String, DataEntry> entryMap = new TreeMap<>();
    private static final StringBuilder JSONBuilder = new StringBuilder();

    private static String lastLeague = "";
    private static String lastType = "";

    public void run() {
        //  Name: run()
        //  Date created: 28.11.2017
        //  Last modified: 13.12.2017
        //  Description: Main loop of the pricing service

        int sleepLength = Integer.parseInt(PROPERTIES.getProperty("PricerControllerSleepCycle"));

        // Wait for user to initiate program before building databases
        while (flagPause) {
            sleep(100);
        }

        // Load data in on initial script launch
        readDataFromFile();

        while (flagLocalRun) {
            sleepWhile(sleepLength);

            flagPause = true;
            runCycle();
            flagPause = false;
        }
    }

    private void runCycle() {
        //  Name: runCycle2()
        //  Date created: 11.12.2017
        //  Last modified: 13.12.2017
        //  Description: Calls methods that construct/parse/write database entryMap

        // Make sure output folder exists
        new File("./output").mkdir();

        // Prepare for database building
        System.out.println(timeStamp() + " Generating databases");

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
    }

    private void sleepWhile(int howLongInSeconds) {
        //  Name: sleepWhile()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Sleeps for <howLongInSeconds> seconds
        //  Parent methods:
        //      run()

        for (int i = 0; i < howLongInSeconds; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Break if run flag has been lowered
            if (!flagLocalRun)
                break;
        }
    }

    private void sleep(int timeMS) {
        //  Name: sleep()
        //  Date created: 02.12.2017
        //  Last modified: 13.12.2017
        //  Description: Sleeps for <timeMS> ms

        try {
            Thread.sleep(timeMS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void parseItems(APIReply reply) {
        //  Name: parseItems()
        //  Date created: 28.11.2017
        //  Last modified: 13.12.2017
        //  Description: Method that's used to add entries to the databases

        // Loop through every single item, checking every single one of them
        for (Stash stash : reply.getStashes()) {
            for (Item item : stash.getItems()) {
                // Pause during I/O operations
                while (flagPause) {
                    sleep(100);
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

    public void stopController() {
        //  Name: stopController()
        //  Date created: 13.12.2017
        //  Last modified: 13.12.2017
        //  Description: Shuts down the controller safely

        flagPause = false;
        flagLocalRun = false;
    }


    //////////////////////////////////////
    // Methods used to manage databases //
    //////////////////////////////////////

    public void readDataFromFile() {
        //  Name: readDataFromFile()
        //  Date created: 06.12.2017
        //  Last modified: 13.12.2017
        //  Description: Reads and parses database data from file

        String line;
        BufferedReader bufferedReader = null;

        try {
            File fFile = new File("./data.txt");
            bufferedReader = new BufferedReader(new FileReader(fFile));

            while ((line = bufferedReader.readLine()) != null) {
                String[] splitLine = line.split("::");
                entryMap.put(splitLine[0], new DataEntry());
                entryMap.get(splitLine[0]).parseIOLine(splitLine);
            }

        } catch (IOException ex) {
            System.out.println("[INFO] File not found: ./data.txt");
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void writeDataToFile() {
        //  Name: writeDataToFile()
        //  Date created: 06.12.2017
        //  Last modified: 11.12.2017
        //  Description: Writes database data to file

        OutputStream fOut = null;

        // Writes values from statistics to file
        try {
            File fFile = new File("./data.txt");
            fOut = new FileOutputStream(fFile);

            for (String key : entryMap.keySet()) {
                if (!entryMap.get(key).isEmpty())
                    fOut.write(entryMap.get(key).makeIOLine().getBytes());
            }

        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write data:");
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
        //  Last modified: 13.12.2017
        //  Description: Basically writes JSON string to file

        if (JSONBuilder.length() < 5)
            return;

        OutputStream fOut = null;

        // Writes values from statistics to file
        try {
            File fFile = new File("./output/" + lastLeague + ".json");
            fOut = new FileOutputStream(fFile);
            fOut.write(JSONBuilder.toString().getBytes());

        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write ./output/" + lastLeague + ".json");
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

    public void setFlagPause(boolean newFlagPause) {
        flagPause = newFlagPause;
    }

    public boolean isFlagPause() {
        return flagPause;
    }

    public Map<String, DataEntry> getEntryMap() {
        return entryMap;
    }
}
