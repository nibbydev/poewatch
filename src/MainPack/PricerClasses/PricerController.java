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
    //  Last modified: 11.12.2017
    //  Description: A threaded object that manages databases

    private static boolean flagLocalRun = true;
    private static boolean flagPause = false;
    private static Map<String, DataEntry> data = new TreeMap<>();
    private static StringBuilder JSONBuilder = new StringBuilder();

    private static String lastLeague = "";
    private static String lastType = "";

    // TODO: move to separate functions
    public void run() {
        //  Name: run()
        //  Date created: 28.11.2017
        //  Last modified: 11.12.2017
        //  Description: Contains the main loop of the pricing service

        // Load data in on initial script launch
        readDataFromFile();

        while (true) {
            sleepWhile(Integer.parseInt(PROPERTIES.getProperty("PricerControllerSleepCycle")));
            System.out.println(timeStamp() + " Generating databases");

            // Break if run flag has been lowered
            if (!flagLocalRun)
                break;

            // Prepare for database building
            flagPause = true;
            JSONBuilder.append("{");

            // Increase DataEntry's static cycle count
            DataEntry.incCycleCount();

            // manage+write data
            data.forEach((String key, DataEntry entry) -> {
                entry.buildBaseData(data);
                entry.purgeBaseData();
                entry.buildStatistics();
                entry.clearRawData();
                packageJSON(entry);
            });

            JSONBuilder.append("}");

            writeDataToFile();
            writeJSONToFile();

            // Clean up after database building
            JSONBuilder.setLength(0);
            lastType = "";
            lastLeague = "";
            flagPause = false;
        }
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
                data.putIfAbsent(item.getKey(), new DataEntry());
                data.get(item.getKey()).addRaw(item);
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
                data.put(splitLine[0], new DataEntry());
                data.get(splitLine[0]).parseIOLine(splitLine);
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

            for (String key : data.keySet()) {
                if (!data.get(key).isEmpty())
                    fOut.write(data.get(key).makeIOLine().getBytes());
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
        //  Last modified: 08.12.2017
        //  Description: Packages JSON and writes to file

        String JSONPacket = entry.buildJSONPackage();
        if (JSONPacket.equals(""))
            return;

        // Reassign key (remove the league and type)
        String key = "";
        String[] splitKey = entry.getKey().split("\\|");
        for (int i = 0; i < splitKey.length; i++) {
            if (i > 1)
                key += splitKey[i] + "|";
        }

        // Reformat key, removing league and item type
        ArrayList<String> partialKey = new ArrayList<>(Arrays.asList(splitKey));
        partialKey.subList(0, 2).clear();
        key = String.join("|", partialKey);

        // Check if key (league) has been changed
        if (lastLeague.equals("")) {
            lastLeague = splitKey[0];
            JSONBuilder.append("\"");
            JSONBuilder.append(lastLeague);
            JSONBuilder.append("\": {");
        } else {
            if (lastLeague.equals(splitKey[0])) {
                JSONBuilder.deleteCharAt(JSONBuilder.lastIndexOf("}"));
            } else {
                lastLeague = splitKey[0];
                JSONBuilder.append(",\"");
                JSONBuilder.append(lastLeague);
                JSONBuilder.append("\": {");
                lastType = "";
            }
        }

        // Check if key (item type) has been changed
        if (lastType.equals("")) {
            lastType = splitKey[1];
            JSONBuilder.append("\"");
            JSONBuilder.append(lastType);
            JSONBuilder.append("\": {");
        } else {
            if (lastType.equals(splitKey[1])) {
                JSONBuilder.deleteCharAt(JSONBuilder.lastIndexOf("}"));
                JSONBuilder.append(",");
            } else {
                lastType = splitKey[1];
                JSONBuilder.append(",\"");
                JSONBuilder.append(lastType);
                JSONBuilder.append("\": {");
            }
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

}
