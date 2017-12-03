package MainPack.PricerClasses;

import MainPack.MapperClasses.Item;

import java.io.*;
import java.util.*;

public class Database {
    //  Name: Database
    //  Date created: 28.11.2017
    //  Last modified: 03.12.2017
    //  Description: Class used to store data, manage data and save data

    private static Map<String, ArrayList<String[]>> rawData = new TreeMap<>();
    private static Map<String, ArrayList<Double>> baseDatabase = new TreeMap<>();
    private static Map<String, StatsObject> statistics = new TreeMap<>();
    private static Map<String, ArrayList<Double>> hourly = new TreeMap<>();

    private static Integer loopCounter = 0;

    /////////////////////////////////////////////
    // Methods used to add values to databases //
    /////////////////////////////////////////////

    public void rawDataAddEntry(Item item) {
        //  Name: addEntry()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Method that adds entries to raw data database

        // Add value to database (in string format)
        if (rawData.containsKey(item.getKey())) {
            rawData.get(item.getKey()).add(new String[]{Double.toString(item.getPrice()), item.getPriceType()});
        } else {
            rawData.put(item.getKey(), new ArrayList<>() {{
                add(new String[]{Double.toString(item.getPrice()), item.getPriceType()});
            }});
        }
    }

    //////////////////////////////////////
    // Methods used to manage databases //
    //////////////////////////////////////

    public void mainLoop() {
        //  Name: mainLoop()
        //  Date created: 02.12.2017
        //  Last modified: 03.12.2017
        //  Description: Initiates methods used to read/write/manage data

        String hourlyJSON;

        // Legend:
        // [RAW DATA]   - rawData      - Contains values in the format of "key: [[5, dollars], [1, euro], [15, rubles]]"
        // [BASEDATA]   - baseDatabase - Contains values from [RAW DATA] but converted to a base currency (eg euro) in
        //                               in the format of "key: [4.4, 1, 0.23]"
        // [STATISTICS] - statistics   - Contains conversion rates for currencies and average prices of items in the
        //                               format of "key: StatsObject"
        // [HOURLY]     - hourly       - Contains the median values from [STATISTICS], added to it every cycle. Is used
        //                               to build a JSON-encoded statistical output file

        // 0. Increase loopCounter
        loopCounter++;
        // 1. Make sure base folder exists
        new File("./data").mkdir();
        // 2. [BASEDATA] and [STATISTICS] are loaded in from file
        readBaseFromFile();
        readStatsFromFile();
        // 3. Currency values from [RAW DATA] are converted to base currency with the conversion rates in [STATISTICS]
        //    and then are appended to [BASEDATA]
        buildDatabases();
        // 4. All values are removed from [RAW DATA]
        rawData.clear();
        // 5. Values in [BASEDATA] are compared to median values in [STATISTICS] and irregularities are removed
        purgeDatabases();
        // 6. [HOURLY] is loaded in from file
        readHourlyFromFile();
        // 7. Values from [BASEDATA] are used to rebuild [STATISTICS] from the ground up.
        //    Median values from [STATISTICS] are added to [HOURLY] in the same fashion as [BASEDATA]
        buildStatistics();
        // 8. Cleaned [BASEDATA] is written to file
        writeBaseToFile();
        // 9. All values are cleared from [BASEDATA]
        baseDatabase.clear();
        // 10. Updated [STATISTICS] is written to file
        writeStatsToFile();
        // 11. All values are cleared from [STATISTICS]
        statistics.clear();
        // 12. If 1 hour has passed, make JSON-encoded stats out of [HOURLY] and write it to file
        hourlyJSON = buildHourlyReport();
        writeHourlyReportToFile(hourlyJSON);
        // 13. Updated [HOURLY] is written to file
        writeHourlyToFile();
        // 14. All values are cleared from [HOURLY]
        hourly.clear();
    }

    // Creating databases

    private void buildDatabases() {
        //  Name: buildDatabases()
        //  Date created: 29.11.2017
        //  Last modified: 03.12.2017
        //  Description: Method that adds values from rawData to baseDatabase

        Double value;
        String index;
        Double chaosValue;

        // Loop through entries
        for (String key : rawData.keySet()) {
            for (String[] entry : rawData.get(key)) {
                value = Double.parseDouble(entry[0]);
                index = entry[1];

                // If we have the median price, use that
                if (!index.equals("1")) {
                    // Set initial value
                    chaosValue = 0.0;

                    // If there's a value in the statistics database, use that
                    if (statistics.containsKey(key))
                        chaosValue = statistics.get(key).getMedian();

                    if (chaosValue > 0.0)
                        value = value * chaosValue;
                    else
                        continue;
                }

                // Round it up
                value = Math.round(value * 1000) / 1000.0;

                baseDatabase.putIfAbsent(key, new ArrayList<>());
                baseDatabase.get(key).add(value);
            }
            if(baseDatabase.containsKey(key)) {
                if (baseDatabase.get(key).size() > 100)
                    baseDatabase.get(key).subList(0, baseDatabase.get(key).size() - 100).clear();
            }
        }
    }

    private void buildStatistics() {
        //  Name: buildStatistics()
        //  Date created: 29.11.2017
        //  Last modified: 03.12.2017
        //  Description: Method that adds entries to statistics

        Double mean;
        Double median;
        int count;

        // Loop through currency entries
        for (String key : baseDatabase.keySet()) {
            // Skip entries with a small number of elements as they're hard to base statistics on
            count = baseDatabase.get(key).size();
            if (count < 20)
                continue;

            // Make a copy so the original order persists
            ArrayList<Double> tempValueList = new ArrayList<>();
            tempValueList.addAll(baseDatabase.get(key));
            // Sort the entries in growing order
            Collections.sort(tempValueList);

            // Slice sorted copy for more precision
            if (count < 30) {
                tempValueList.subList(count - 11, count - 1).clear();
                tempValueList.subList(0, 2).clear();
            } else if (count < 60) {
                tempValueList.subList(count - 16, count - 1).clear();
                tempValueList.subList(0, 3).clear();
            } else if (count < 80) {
                tempValueList.subList(count - 21, count - 1).clear();
                tempValueList.subList(0, 4).clear();
            } else if (count < 100) {
                tempValueList.subList(count - 31, count - 1).clear();
                tempValueList.subList(0, 5).clear();
            }

            // Reassign count
            count = tempValueList.size();
            if (count < 20)
                continue;

            mean = 0.0;
            // Add up values to calculate mean
            for (Double i : tempValueList) { // TODO: replace with lambda
                mean += i;
            }

            // Calculate mean and median values and round them to 3 digits
            mean = Math.round(mean / count * 1000) / 1000.0;
            median = count / 2.0;
            median = Math.round(tempValueList.get(median.intValue()) * 1000) / 1000.0;

            // Turn that into a statistics object and put it in the database
            statistics.put(key, new StatsObject(count, mean, median));

            // Add values to hourly database
            if(!hourly.containsKey(key))
                hourly.put(key, new ArrayList<>());
            hourly.get(key).add(median);
        }
    }

    private void purgeDatabases() {
        //  Name: purgeDatabases()
        //  Date created: 29.11.2017
        //  Last modified: 03.12.2017
        //  Description: Method that removes entries from baseDatabase (based on statistics HashMap) depending
        //               whether there's a large difference between the two

        double median;

        for (String key : statistics.keySet()) {
            if (!baseDatabase.containsKey(key))
                continue;

            // Get the median
            median = statistics.get(key).getMedian();

            // Remove values that are larger/smaller than the median
            for (Double value : new ArrayList<>(baseDatabase.get(key))) {
                if (value > median * 2.0 || value < median / 4.0) {
                    baseDatabase.get(key).remove(value);
                }
            }
        }
    }

    // I/O

    private void readBaseFromFile() {
        //  Name: readBaseFromFile()
        //  Date created: 02.12.2017
        //  Last modified: 03.12.2017
        //  Description: Reads and parses baseDatabase data from file

        // TODO: quit when no files found

        String line;
        String[] splitLine;
        ArrayList<Double> values;

        try {
            File fFile = new File("./data/base.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fFile));

            while ((line = bufferedReader.readLine()) != null) {
                values = new ArrayList<>();
                splitLine = line.split("::");

                for (String value : splitLine[1].split(",")) {
                    values.add(Double.parseDouble(value));
                }

                baseDatabase.put(splitLine[0], values);
            }
        } catch (IOException e) { // TODO: file not found
            System.out.println("[ERROR] Could not read base.text");
        }

    }

    private void readStatsFromFile() {
        //  Name: readStatsFromFile()
        //  Date created: 03.12.2017
        //  Last modified: 03.12.2017
        //  Description: Reads and parses statistics data from file

        // TODO: quit when no files found

        String line;
        String[] splitLine;
        StatsObject stats;

        try {
            File fFile = new File("./data/stats.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fFile));

            while ((line = bufferedReader.readLine()) != null) {
                splitLine = line.split("::");
                stats = new StatsObject(0, 0.0, 0.0);
                stats.fromString(splitLine[1]);
                statistics.put(splitLine[0], stats);
            }

        } catch (IOException e) { // TODO: file not found
            System.out.println("[ERROR] Could not read stats.text");
        }

    }

    private void writeBaseToFile() {
        //  Name: writeBaseToFile()
        //  Date created: 02.12.2017
        //  Last modified: 03.12.2017
        //  Description: Stores (writes) baseDatabase to file

        StringBuilder line;

        try {
            File fFile = new File("./data/base.txt");
            OutputStream fOut = new FileOutputStream(fFile);

            for (String key : baseDatabase.keySet()) {
                line = new StringBuilder();

                line.append(key);
                line.append("::");

                for (Double d : baseDatabase.get(key)) {
                    line.append(d);
                    line.append(",");
                }

                line.deleteCharAt(line.lastIndexOf(","));
                line.append("\n");

                fOut.write(line.toString().getBytes());
            }

            fOut.flush();
            fOut.close();

        } catch (IOException e) { // TODO: finally close
            e.printStackTrace();
        }
    }

    private void writeStatsToFile() {
        //  Name: writeStatsToFile()
        //  Date created: 03.12.2017
        //  Last modified: 03.12.2017
        //  Description: Stores (writes) statistics to files

        StringBuilder line;

        // Writes values from statistics to file
        try {
            File fFile = new File("./data/stats.txt");
            OutputStream fOut = new FileOutputStream(fFile);

            for (String key : statistics.keySet()) {
                line = new StringBuilder();
                line.append(key);
                line.append("::");
                line.append(statistics.get(key).toString());
                line.append("\n");

                fOut.write(line.toString().getBytes());
            }

            fOut.flush();
            fOut.close();

        } catch (IOException e) { // TODO: finally close
            e.printStackTrace();
        }
    }

    // Hourly

    private void readHourlyFromFile(){
        //  Name: readHourlyFromFile()
        //  Date created: 03.12.2017
        //  Last modified: 03.12.2017
        //  Description: Reads and parses hourly data from file

        String line;
        String[] splitLine;
        ArrayList<Double> values;

        try {
            File fFile = new File("./data/hourly.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fFile));

            while ((line = bufferedReader.readLine()) != null) {
                values = new ArrayList<>();
                splitLine = line.split("::");

                for (String value : splitLine[1].split(",")) {
                    values.add(Double.parseDouble(value));
                }

                hourly.put(splitLine[0], values);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Could not read hourly.txt");
        }
    }

    private void writeHourlyToFile() {
        //  Name: writeHourlyToFile()
        //  Date created: 03.12.2017
        //  Last modified: 03.12.2017
        //  Description: Stores (writes) hourly to file

        StringBuilder line;

        try {
            File fFile = new File("./data/hourly.txt");
            OutputStream fOut = new FileOutputStream(fFile);

            for (String key : hourly.keySet()) {
                line = new StringBuilder();

                line.append(key);
                line.append("::");

                for (Double d : hourly.get(key)) {
                    line.append(d);
                    line.append(",");
                }

                line.deleteCharAt(line.lastIndexOf(","));
                line.append("\n");

                fOut.write(line.toString().getBytes());
            }

            fOut.flush();
            fOut.close();

        } catch (IOException e) { // TODO: finally close
            e.printStackTrace();
        }
    }

    private String buildHourlyReport(){
        //  Name: buildHourlyReport()
        //  Date created: 03.12.2017
        //  Last modified: 03.12.2017
        //  Description: Creates a JSON-encoded string of hourly medians

        // Run every x cycles
        if(loopCounter < 3) // TODO: inc/dec this value
            return "";

        Double mean;
        Double median;
        int count;
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{");

        for (String key : hourly.keySet()) {
            // Make a copy so the original order persists
            ArrayList<Double> tempValueList = new ArrayList<>();
            tempValueList.addAll(hourly.get(key));
            // Sort the entries in growing order
            Collections.sort(tempValueList);

            count = tempValueList.size();

            mean = 0.0;
            // Add up values to calculate mean
            for (Double i : tempValueList) {
                mean += i;
            }

            mean = Math.round(mean / count * 10000) / 10000.0;
            median = count / 2.0;
            median = Math.round(tempValueList.get(median.intValue()) * 10000) / 10000.0;

            // Reassign count
            count = baseDatabase.get(key).size();

            // Add JSON-encoded string
            stringBuilder.append("\"");
            stringBuilder.append(key);
            stringBuilder.append("\": ");
            stringBuilder.append("{\"median\": ");
            stringBuilder.append(median);
            stringBuilder.append(", \"mean\": ");
            stringBuilder.append(mean);
            stringBuilder.append(", \"count\": ");
            stringBuilder.append(count);
            stringBuilder.append("},");
        }

        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    private void writeHourlyReportToFile(String hourlyJSON){
        //  Name: writeHourlyReportToFile()
        //  Date created: 03.12.2017
        //  Last modified: 03.12.2017
        //  Description: Just writes a string to file, pretty much

        if(hourlyJSON.equals(""))
            return;

        try {
            File fFile = new File("./data/report.json");
            OutputStream fOut = new FileOutputStream(fFile);

            fOut.write(hourlyJSON.getBytes());
            fOut.flush();
            fOut.close();
        } catch (IOException e) { // TODO: finally close
            e.printStackTrace();
        }
    }

    //////////////////////////////////
    // Methods used for development //
    //////////////////////////////////

    public void devPrintData() {
        //  Name: devPrintData()
        //  Date created: 29.11.2017
        //  Last modified: 29.11.2017
        //  Description: Used for development. Prints out every single database entry

        System.out.println("\n[=================================================== RAW DATA ===================================================]");

        for (String key : rawData.keySet()) {
            System.out.print("[" + key + "]: ");

            for (String[] info : rawData.get(key)) {
                System.out.print(Arrays.toString(info) + ",");
            }

            System.out.println();
        }

        System.out.println("\n[=================================================== DATABASES ===================================================]");

        for (String key : baseDatabase.keySet()) {
            System.out.println("[" + key + "]: " + Arrays.toString(baseDatabase.get(key).toArray()));
        }

        System.out.println("\n[=================================================== STATISTICS ===================================================]");

        for (String key : statistics.keySet()) {
            System.out.println("[" + key + "] Median: " + statistics.get(key).getMedian());
            System.out.println("[" + key + "] Mean: " + statistics.get(key).getMean());
            System.out.println("[" + key + "] Count: " + statistics.get(key).getCount());
        }

    }

}
