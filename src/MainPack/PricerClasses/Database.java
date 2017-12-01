package MainPack.PricerClasses;

import MainPack.MapperClasses.Item;

import java.util.*;

public class Database {
    //  Name: Database
    //  Date created: 28.11.2017
    //  Last modified: 01.12.2017
    //  Description: Class used to store data, manage data and save data

    private static Map<String, ArrayList<String[]>> rawData = new TreeMap<>();
    private static Map<String, ArrayList<Double>> baseDatabase = new TreeMap<>();
    private static Map<String, StatsObject> statistics = new TreeMap<>();

    /* TODO: implement this
    1) values continuously added to [RAW DATA]
    2) after 5 minutes, [DATABASE] and [STATISTICS] are loaded from file into memory
    3) values from [RAW DATA] are converted to baseValues using [STATISTICS] and appended to [DATABASE]
    4) values in [DATABASE] are compared to [STATISTICS] and irregularities in [DATABASE] are removed
    5) [STATISTICS] is rebuilt using values from [DATABASE]
    6) [DATABASE] and [STATISTICS] are written to file and cleared from memory
    7) all values in [RAW DATA] are removed
     */

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

    public void clearRawData() {
        //  Name: clearRawData()
        //  Date created: 29.11.2017
        //  Last modified: 29.11.2017
        //  Description: Method used for clearing rawData HashMap

        rawData.clear();
    }

    public void buildDatabases() {
        //  Name: buildDatabases()
        //  Date created: 29.11.2017
        //  Last modified: 01.12.2017
        //  Description: Method that adds values from rawData HashMap to baseDatabase HashMap

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
        }
    }

    public void buildStatistics() {
        //  Name: buildStatistics()
        //  Date created: 29.11.2017
        //  Last modified: 01.12.2017
        //  Description: Method that adds entries to statistics HashMap

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
        }
    }

    public void purgeDatabases() {
        //  Name: purgeDatabases()
        //  Date created: 29.11.2017
        //  Last modified: 01.12.2017
        //  Description: Method that removes entries from baseDatabase HashMap (based on statistics HashMap) depending
        //               whether there's a 200% or larger difference between the two values

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
