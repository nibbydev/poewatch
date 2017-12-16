package com.sanderh.Pricer;

import com.sanderh.Mappers.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.sanderh.Main.PRICER_CONTROLLER;
import static com.sanderh.Main.PROPERTIES;

public class DataEntry {
    //  Name: DataEntry
    //  Date created: 05.12.2017
    //  Last modified: 13.12.2017
    //  Description: An object that stores an item's price data

    // Cycle counters
    private static int cycleCount = 0;
    private static final int CYCLE_LIMIT = Integer.parseInt(PROPERTIES.getProperty("DataEntryCycleLimit"));

    // Statistical values
    private int count = 0;
    private int discardedCounter = 0;
    private int addedCounter = 0;
    private double mean = 0.0;
    private double median = 0.0;
    private String key;

    private String league = "";
    private String type = "";
    private String JSONkey = "";

    // Lists that hold price data
    private ArrayList<String[]> rawData = new ArrayList<>();
    private ArrayList<Double> baseData = new ArrayList<>();
    private ArrayList<Double> hourlyData = new ArrayList<>();
    private ArrayList<String> duplicates = new ArrayList<>();

    // Constants that limit list sizes
    private static final int BASEDATA_SIZE = Integer.parseInt(PROPERTIES.getProperty("baseDataSize"));
    private static final int HOURLY_DATA_SIZE = Integer.parseInt(PROPERTIES.getProperty("hourlyDataSize"));
    private static final int DUPLICATES_SIZE = Integer.parseInt(PROPERTIES.getProperty("duplicatesSize"));

    //////////////////
    // Main methods //
    //////////////////

    public DataEntry databaseBuilder() {
        //  Name: databaseBuilder
        //  Date created: 11.12.2017
        //  Last modified: 12.12.2017
        //  Description: Methods that constructs the database. Should be called at a timed interval

        buildBaseData();
        purgeBaseData();
        buildStatistics();

        return this;
    }

    public void addRaw(Item item) {
        //  Name: addRaw
        //  Date created: 05.12.2017
        //  Last modified: 12.12.2017
        //  Description: Method that adds entries to raw data database

        // Skip duplicate items
        if (duplicates.contains(item.getId()))
            return;

        // Increase the added item count
        addedCounter++;

        // Assign key and add value to raw data array
        this.key = item.getKey();
        rawData.add(new String[]{Double.toString(item.getPrice()), item.getPriceType()});

        // Add item ID to duplicate list
        duplicates.add(item.getId());
        if (duplicates.size() > DUPLICATES_SIZE)
            duplicates.subList(0, duplicates.size() - DUPLICATES_SIZE).clear();
    }

    private void removeLeagueAndTypeFromKey() {
        //  Name: removeLeagueAndTypeFromKey()
        //  Date created: 12.12.2017
        //  Last modified: 13.12.2017
        //  Description: Removes league and itemType fields from a database key
        //      {"Abyss", "Amulets", "Name", "Type", "3"} -> "Name|Type|3"

        String[] splitKey = key.split("\\|");

        // Convert array to ArrayList
        ArrayList<String> partialKey = new ArrayList<>(Arrays.asList(splitKey));
        // Remove the first 2 elements
        partialKey.subList(0, 2).clear();

        JSONkey = String.join("|", partialKey);
        league = splitKey[0];
        type = splitKey[1];
    }

    private void buildBaseData() {
        //  Name: buildBaseData
        //  Date created: 29.11.2017
        //  Last modified: 12.12.2017
        //  Description: Method that adds values from rawData to baseDatabase

        String index;
        Double value;

        // Loop through entries
        for (String[] entry : rawData) {
            value = Double.parseDouble(entry[0]);
            index = entry[1];

            // If we have the median price, use that
            if (!index.equals("Chaos Orb")) {
                String currencyKey = key.split("\\|")[0] + "|Currency|" + index + "|5";
                // If there's a value in the statistics database, use that
                if (PRICER_CONTROLLER.getEntryMap().containsKey(currencyKey)) {
                    if (PRICER_CONTROLLER.getEntryMap().get(currencyKey).getCount() >= 10) {
                        value = value * PRICER_CONTROLLER.getEntryMap().get(currencyKey).getMedian();
                    } else {
                        discardedCounter++;
                        continue;
                    }
                } else {
                    discardedCounter++;
                    continue;
                }
            }

            // Round it up
            value = Math.round(value * 10000) / 10000.0;

            if (value > 0.001)
                baseData.add(value);
        }

        // Clear raw data
        rawData.clear();

        // Soft-cap list at 100 entries
        if (baseData.size() > BASEDATA_SIZE)
            baseData.subList(0, baseData.size() - BASEDATA_SIZE).clear();
    }

    private void purgeBaseData() {
        //  Name: purgeBaseData
        //  Date created: 29.11.2017
        //  Last modified: 11.12.2017
        //  Description: Method that removes entries from baseDatabase (based on statistics HashMap) depending
        //               whether there's a large difference between the two

        if (baseData.isEmpty())
            return;
        else if (median <= 0.0)
            return;
        else if (count < 10)
            return;

        // Make a copy of the original array (so it is not altered any further)
        for (Double value : new ArrayList<>(baseData)) {
            // Remove values that are larger/smaller than the median
            if (value > median * 2.0 || value < median / 2.0) {
                baseData.remove(value);
                discardedCounter++;
            }
        }
    }

    private void buildStatistics() {
        //  Name: buildBaseData
        //  Date created: 29.11.2017
        //  Last modified: 12.12.2017
        //  Description: Method that adds entries to statistics

        // Assign count to local "global" variable
        this.count = baseData.size();

        // Make a copy so the original order persists and sort new array
        ArrayList<Double> tempValueList = new ArrayList<>();
        tempValueList.addAll(baseData);
        Collections.sort(tempValueList);

        // Slice sorted copy for more precision. Skip entries with a small number of elements
        if (count <= 2) {
            return;
        } else if (count < 5) {
            tempValueList.subList(count - 2, count - 1).clear();
        } else if (count < 10) {
            tempValueList.subList(count - 3, count - 1).clear();
            tempValueList.subList(0, 1).clear();
        } else if (count < 15) {
            tempValueList.subList(count - 4, count - 1).clear();
            tempValueList.subList(0, 2).clear();
        } else if (count < 30) {
            tempValueList.subList(count - 11, count - 1).clear();
            tempValueList.subList(0, 2).clear();
        } else if (count < 60) {
            tempValueList.subList(count - 16, count - 1).clear();
            tempValueList.subList(0, 3).clear();
        } else if (count < 80) {
            tempValueList.subList(count - 21, count - 1).clear();
            tempValueList.subList(0, 4).clear();
        } else if (count < 110) {
            tempValueList.subList(count - 31, count - 1).clear();
            tempValueList.subList(0, 5).clear();
        }

        // Calculate mean and median values
        this.mean = findMean(tempValueList);
        this.median = findMedian(tempValueList);

        // Add value to hourly
        hourlyData.add(this.median);

        // Limit hourly to 60 values
        if (hourlyData.size() > HOURLY_DATA_SIZE)
            hourlyData.subList(0, hourlyData.size() - HOURLY_DATA_SIZE).clear();
    }

    private double findMean(ArrayList<Double> valueList) {
        //  Name: findMean
        //  Date created: 12.12.2017
        //  Last modified: 12.12.2017
        //  Description: Finds the mean value of an array

        double mean = 0.0;
        int count = valueList.size();

        // Add up values to calculate mean
        for (Double i : valueList)
            mean += i;

        return Math.round(mean / count * 10000) / 10000.0;
    }

    private double findMedian(ArrayList<Double> valueList) {
        //  Name: findMedian
        //  Date created: 12.12.2017
        //  Last modified: 12.12.2017
        //  Description: Finds the median value of an array. Has 1/4 shift to left

        return Math.round(valueList.get((int) (valueList.size() / 4.0)) * 10000) / 10000.0;
    }

    /////////////////
    // I/O helpers //
    /////////////////

    public String buildJSONPackage() {
        //  Name: buildJSONPackage()
        //  Date created: 06.12.2017
        //  Last modified: 16.12.2017
        //  Description: Creates a JSON-encoded string of hourly medians

        // Run every x cycles AND if there's enough data
        if (cycleCount < CYCLE_LIMIT || hourlyData.isEmpty())
            return "";

        // Format the key to fit for JSON
        if (league.equals("") || type.equals(""))
            removeLeagueAndTypeFromKey();

        // Warn the user if there are irregularities with the discard counter
        if (addedCounter < discardedCounter && discardedCounter > 20)
            System.out.println("[INFO][" + key + "] Odd discard ratio: " + addedCounter + "/" + discardedCounter + " (add/discard)");

        // Clear the counters
        this.discardedCounter = 0;
        this.addedCounter = 0;

        // Make a copy so the original order persists and sort the entries in growing order
        ArrayList<Double> tempValueList = new ArrayList<>();
        tempValueList.addAll(hourlyData);
        Collections.sort(tempValueList);
        hourlyData.clear();

        return "\"" + JSONkey + "\":{\"median\":" + findMedian(tempValueList) + ",\"mean\":" + findMean(tempValueList) + ",\"count\":" + this.count + "}";
    }

    public void parseIOLine(String[] splitLine) {
        //  Name: parseIOLine()
        //  Date created: 06.12.2017
        //  Last modified: 08.12.2017
        //  Description: Reads values from a string and adds them to the lists

        key = splitLine[0];

        // get stats
        if (!splitLine[1].equals("-")) {
            count = Integer.parseInt(splitLine[1].split(",")[0]);
            mean = Double.parseDouble(splitLine[1].split(",")[1]);
            median = Double.parseDouble(splitLine[1].split(",")[2]);
        }

        // get basedata
        if (!splitLine[2].equals("-")) {
            for (String value : splitLine[2].split(",")) {
                baseData.add(Double.parseDouble(value));
            }
        }

        // get duplicates
        if (!splitLine[3].equals("-")) {
            Collections.addAll(duplicates, splitLine[3].split(","));
        }
    }

    public String makeIOLine() {
        //  Name: makeIOLine()
        //  Date created: 06.12.2017
        //  Last modified: 08.12.2017
        //  Description: Converts this object's values into a string

        StringBuilder stringBuilder = new StringBuilder();

        // add stats
        stringBuilder.append(key);
        stringBuilder.append("::");
        if (median + mean > 0) {
            stringBuilder.append(count);
            stringBuilder.append(",");
            stringBuilder.append(mean);
            stringBuilder.append(",");
            stringBuilder.append(median);
        } else {
            stringBuilder.append("-");
        }

        // add base data
        stringBuilder.append("::");
        if (baseData.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (Double d : baseData) {
                stringBuilder.append(d);
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        }

        // add duplicate IDs
        stringBuilder.append("::");
        if (duplicates.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (String s : duplicates) {
                stringBuilder.append(s);
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        }

        // add newline and return
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public double getMedian() {
        return median;
    }

    public boolean isEmpty() {
        return baseData.isEmpty();
    }

    public static void incCycleCount() {
        cycleCount++;
    }

    public static void zeroCycleCount() {
        cycleCount = 0;
    }

    public int getCount() {
        return count;
    }

    public static boolean getCycleState() {
        return cycleCount >= CYCLE_LIMIT;
    }

    public String getType() {
        return type;
    }

    public String getLeague() {
        return league;
    }
}
