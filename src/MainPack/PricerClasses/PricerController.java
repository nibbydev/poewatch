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
    //  Last modified: 12.12.2017
    //  Description: A threaded object that manages databases

    private static boolean flagLocalRun = true;
    private static boolean flagPause = false;
    private static final Map<String, DataEntry> entryMap = new TreeMap<>();
    private static final StringBuilder JSONBuilder = new StringBuilder();

    private static String lastLeague = "";
    private static String lastType = "";

    public void run() {
        //  Name: run()
        //  Date created: 28.11.2017
        //  Last modified: 12.12.2017
        //  Description: Main loop of the pricing service

        // Load data in on initial script launch
        readDataFromFile();

        int sleepLength = Integer.parseInt(PROPERTIES.getProperty("PricerControllerSleepCycle"));

        while (true) {
            sleepWhile(sleepLength);

            // Break if run flag has been lowered
            if (!flagLocalRun)
                break;

            flagPause = true;
            runCycle();
            flagPause = false;
        }
    }

    private void runCycle() {
        //  Name: runCycle()
        //  Date created: 11.12.2017
        //  Last modified: 11.12.2017
        //  Description: Calls methods that construct/parse/write database entryMap

        // Prepare for database building
        System.out.println(timeStamp() + " Generating databases");

        // Increase DataEntry's static cycle count
        DataEntry.incCycleCount();

        // Loop through database entries, calling their methods
        JSONBuilder.append("{");
        entryMap.forEach((String key, DataEntry entry) -> packageJSON(entry.databaseBuilder()));
        JSONBuilder.append("}");

        // Write generated data to file
        writeDataToFile();
        writeJSONToFile();

        // Zero DataEntry's static cycle count
        if(DataEntry.getCycleState()) {
            DataEntry.zeroCycleCount();
            System.out.println(timeStamp() + " Building JSON");
        }

        // Clean up after building
        JSONBuilder.setLength(0);
        lastType = "";
        lastLeague = "";
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

    public void parseItems(APIReply reply) {
        //  Name: parseItems()
        //  Date created: 28.11.2017
        //  Last modified: 10.12.2017
        //  Description: Method that's used to add entries to the databases

        // Loop through every single item, checking every single one of them
        for (Stash stash : reply.getStashes()) {
            for (Item item : stash.getItems()) {
                // Pause during I/O operations
                while (flagPause) {
                    // Sleep for 100ms
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Parse item data
                item.parseItem();
                if (item.isDiscard())
                    continue; // FML. this used to be return

                // Add item to database
                entryMap.putIfAbsent(item.getKey(), new DataEntry());
                entryMap.get(item.getKey()).addRaw(item);
            }
        }
    }

    //////////////////////////////////////
    // Methods used to manage databases //
    //////////////////////////////////////

    public void readDataFromFile() {
        //  Name: readDataFromFile()
        //  Date created: 06.12.2017
        //  Last modified: 11.12.2017
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
        //  Name: packageJSON()
        //  Date created: 06.12.2017
        //  Last modified: 12.12.2017
        //  Description: Packages JSON and writes to file

        String JSONPacket = entry.buildJSONPackage();
        if (JSONPacket.equals(""))
            return;

        // Reassign key (remove the league and type)
        String[] splitKey = entry.getKey().split("\\|");
        String key = removeLeagueAndTypeFromKey(splitKey);

        // Check if key (league) has been changed
        if (lastLeague.equals("")) {
            lastLeague = splitKey[0];
            JSONBuilder.append("\"");
            JSONBuilder.append(lastLeague);
            JSONBuilder.append("\": {");
        } else if (lastLeague.equals(splitKey[0])) {
            JSONBuilder.deleteCharAt(JSONBuilder.lastIndexOf("}"));
        } else {
            lastLeague = splitKey[0];
            JSONBuilder.append(",\"");
            JSONBuilder.append(lastLeague);
            JSONBuilder.append("\": {");
            lastType = "";
        }

        // Check if key (item type) has been changed
        if (lastType.equals("")) {
            lastType = splitKey[1];
            JSONBuilder.append("\"");
            JSONBuilder.append(lastType);
            JSONBuilder.append("\": {");
        } else if (lastType.equals(splitKey[1])) {
            JSONBuilder.deleteCharAt(JSONBuilder.lastIndexOf("}"));
            JSONBuilder.append(",");
        } else {
            lastType = splitKey[1];
            JSONBuilder.append(",\"");
            JSONBuilder.append(lastType);
            JSONBuilder.append("\": {");
        }

        // Generate and add statistical data to the JSON skeleton
        JSONBuilder.append("\"");
        JSONBuilder.append(key);
        JSONBuilder.append("\": ");
        JSONBuilder.append(JSONPacket);
        JSONBuilder.append("}}");
    }

    private void writeJSONToFile() {
        //  Name: writeJSONToFile()
        //  Date created: 06.12.2017
        //  Last modified: 11.12.2017
        //  Description: Basically writes JSON string to file

        if (JSONBuilder.length() < 5)
            return;

        // Zero DataEntry's static cycle count
        DataEntry.zeroCycleCount();

        System.out.println(timeStamp() + " Building JSON");

        OutputStream fOut = null;

        // Writes values from statistics to file
        try {
            File fFile = new File("./report.json");
            fOut = new FileOutputStream(fFile);
            fOut.write(JSONBuilder.toString().getBytes());

        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write report.json:");
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

    private String removeLeagueAndTypeFromKey(String[] splitKey){
        //  Name: removeLeagueAndTypeFromKey()
        //  Date created: 12.12.2017
        //  Last modified: 12.12.2017
        //  Description: Removes league and itemType fields from a database key
        //      {"Abyss", "Amulets", "Name", "Type", "3"} -> "Name|Type|3"

        // Convert array to ArrayList
        ArrayList<String> partialKey = new ArrayList<>(Arrays.asList(splitKey));
        // Remove the first 2 elements
        partialKey.subList(0, 2).clear();

        return String.join("|", partialKey);
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void stopController() {
        flagLocalRun = false;
    }

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
