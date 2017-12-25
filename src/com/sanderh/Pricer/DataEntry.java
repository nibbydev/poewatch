package com.sanderh.Pricer;

import com.sanderh.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import static com.sanderh.Main.CONFIG;
import static com.sanderh.Main.PRICER_CONTROLLER;

public class DataEntry {
    //  Name: DataEntry
    //  Date created: 05.12.2017
    //  Last modified: 25.12.2017
    //  Description: An object that stores an item's price data

    private static int cycleCount = 0;
    private int totalCount = 0;
    private int newItemsFoundInCycle = 0;
    private int oldItemsDiscardedInCycle = 0;
    private double mean = 0.0;
    private double median = 0.0;
    private String key;

    private String league = "";
    private String type = "";
    private String JSONkey = "";

    // Lists that hold price data
    private ArrayList<String> rawData = new ArrayList<>();
    private ArrayList<Double> baseData = new ArrayList<>(CONFIG.baseDataSize);
    private ArrayList<Double> hourlyMean = new ArrayList<>(CONFIG.hourlyDataSize);
    private ArrayList<Double> hourlyMedian = new ArrayList<>(CONFIG.hourlyDataSize);
    private ArrayList<String> duplicates = new ArrayList<>(CONFIG.duplicatesSize);

    //////////////////
    // Main methods //
    //////////////////

    public DataEntry(String line) {
        //  Name: DataEntry
        //  Date created: 05.12.2017
        //  Last modified: 25.12.2017
        //  Description: Used to load data in on object initialization

        parseLine(line);
    }

    public DataEntry() {
    }

    public void add(Item item) {
        //  Name: addItem
        //  Date created: 05.12.2017
        //  Last modified: 24.12.2017
        //  Description: Adds entries to the rawData and duplicates lists

        // Skip duplicate items
        if (duplicates.contains(item.getId()))
            return;

        // Increment total added item counter
        newItemsFoundInCycle++;

        // Assign key if missing
        if (key == null)
            key = item.getKey();

        // Add new value to raw data array
        rawData.add(item.getPrice() + "," + item.getPriceType());

        // Add item ID to duplicate list
        duplicates.add(item.getId());

        // Slice off excess entries in duplicates list
        if (duplicates.size() > CONFIG.duplicatesSize)
            duplicates.subList(0, duplicates.size() - CONFIG.duplicatesSize).clear();
    }

    public String cycle(String line) {
        //  Name: cycle()
        //  Date created: 06.12.2017
        //  Last modified: 25.12.2017
        //  Description: Methods that constructs the object's. Should be called at a timed interval

        // Load data into lists
        parseLine(line);

        // Build statistics and databases
        parse();
        purge();
        build();

        return buildLine();
    }

    /////////////////////
    // Private methods //
    /////////////////////

    private void parse() {
        //  Name: parse
        //  Date created: 29.11.2017
        //  Last modified: 24.12.2017
        //  Description: Method that adds values from rawData to baseDatabase

        Double value;
        String[] splitEntry;
        String currencyKey;
        DataEntry currencyEntry;

        // Loop through entries
        for (String entry : rawData) {
            splitEntry = entry.split(",");
            value = Double.parseDouble(splitEntry[0]);

            // If we have the median price, use that
            if (!splitEntry[1].equals("Chaos Orb")) {
                // TODO: splitEntry[1] needs to be looked up from a table
                currencyKey = key.substring(0, key.indexOf("|")) + "|currency:orbs|" + splitEntry[1] + "|5";

                // If there's a value in the statistics database, use that
                if (PRICER_CONTROLLER.getCurrencyMap().containsKey(currencyKey)) {
                    currencyEntry = PRICER_CONTROLLER.getCurrencyMap().get(currencyKey);
                    if (currencyEntry.getCount() >= 10) {
                        value = value * currencyEntry.getMedian();
                    } else {
                        oldItemsDiscardedInCycle++;
                        continue;
                    }
                } else {
                    oldItemsDiscardedInCycle++;
                    continue;
                }
            }

            // If value is more than a very small amount, round it up and add to list
            if (value > 0.001)
                baseData.add(Math.round(value * 10000) / 10000.0);
        }

        // Clear raw data after extracting and converting values
        rawData.clear();

        // Soft-cap price list at <x> entries
        if (baseData.size() > CONFIG.baseDataSize)
            baseData.subList(0, baseData.size() - CONFIG.baseDataSize).clear();
    }

    private void purge() {
        //  Name: purge
        //  Date created: 29.11.2017
        //  Last modified: 24.12.2017
        //  Description: Method that removes entries from baseDatabase (based on statistics HashMap) depending
        //               whether there's a large difference between the two

        if (baseData.isEmpty())
            return;
        else if (median <= 0.0)
            return;
        else if (totalCount < 10)
            return;

        int oldSize = baseData.size();
        BaseDataClearingPredicate<Double> filter = new BaseDataClearingPredicate<>();
        filter.median = median;
        baseData.removeIf(filter);

        // Increment discard counter by how many were discarded
        oldItemsDiscardedInCycle += oldSize - baseData.size();
    }

    private void build() {
        //  Name: parse
        //  Date created: 29.11.2017
        //  Last modified: 24.12.2017
        //  Description: Method that adds entries to statistics

        // Make a copy so the original order persists and sort new array
        ArrayList<Double> tempValueList = new ArrayList<>();
        tempValueList.addAll(baseData);
        Collections.sort(tempValueList);

        // Slice sorted copy for more precision. Skip entries with a small number of elements
        int count = baseData.size();
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
        hourlyMean.add(this.mean);
        hourlyMedian.add(this.median);

        // Limit hourly to <x> values
        if (hourlyMean.size() > CONFIG.dataEntryCycleLimit)
            hourlyMean.subList(0, hourlyMean.size() - CONFIG.dataEntryCycleLimit).clear();
        if (hourlyMedian.size() > CONFIG.dataEntryCycleLimit)
            hourlyMedian.subList(0, hourlyMedian.size() - CONFIG.dataEntryCycleLimit).clear();
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
        //  Last modified: 25.12.2017
        //  Description: Creates a JSON-encoded string of hourly medians

        // Run every x cycles AND if there's enough data
        if (cycleCount < CONFIG.dataEntryCycleLimit || hourlyMedian.isEmpty() || hourlyMean.isEmpty())
            return null;

        // Format the key to fit for JSON
        if (league.equals("") || type.equals("")) {
            String[] splitKey = key.split("\\|");

            // Convert array to ArrayList
            ArrayList<String> partialKey = new ArrayList<>(Arrays.asList(splitKey));
            // Remove the first 2 elements
            partialKey.subList(0, 2).clear();

            JSONkey = String.join("|", partialKey);
            league = splitKey[0];
            type = splitKey[1];
        }

        // Warn the user if there are irregularities with the discard counter
        if (newItemsFoundInCycle < oldItemsDiscardedInCycle && oldItemsDiscardedInCycle - newItemsFoundInCycle > 20)
            System.out.println("[INFO][" + key + "] Odd discard ratio: " + newItemsFoundInCycle + "/" +
                    oldItemsDiscardedInCycle + " (add/discard)");

        // Make a copy so the original order persists and sort the entries in growing order
        ArrayList<Double> tempMedianList = new ArrayList<>();
        tempMedianList.addAll(hourlyMedian);
        Collections.sort(tempMedianList);

        // Add new items to total counter
        totalCount += newItemsFoundInCycle - oldItemsDiscardedInCycle;

        // Soft-cap totalCount to 1000
        if (totalCount > 999)
            totalCount = 999;
        else if (totalCount < 0)
            totalCount = 0;

        // Form the return JSON string
        String returnString = "\"" + JSONkey + "\":{\"mean\":" + findMean(hourlyMean) + ",\"median\":" +
                findMedian(tempMedianList) + ",\"count\":" + totalCount + ",\"new\":" + newItemsFoundInCycle + "}";

        // Clear counters
        hourlyMean.clear();
        hourlyMedian.clear();

        // Clear the counters
        this.newItemsFoundInCycle = 0;
        this.oldItemsDiscardedInCycle = 0;

        return returnString;
    }

    public String buildLine() {
        //  Name: buildLine()
        //  Date created: 06.12.2017
        //  Last modified: 25.12.2017
        //  Description: Converts this object's values into a string that's used for text-file-based storage

        StringBuilder stringBuilder = new StringBuilder();

        // Add key
        stringBuilder.append(key);

        // Add delimiter
        stringBuilder.append("::");

        // Add statistics
        if (median + mean > 0 || newItemsFoundInCycle + oldItemsDiscardedInCycle > 0) {
            stringBuilder.append(totalCount);
            stringBuilder.append(",");
            stringBuilder.append(mean);
            stringBuilder.append(",");
            stringBuilder.append(median);
            stringBuilder.append(",");
            stringBuilder.append(newItemsFoundInCycle);
            stringBuilder.append(",");
            stringBuilder.append(oldItemsDiscardedInCycle);
        } else {
            stringBuilder.append("-");
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add base data
        if (baseData.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (Double d : baseData) {
                stringBuilder.append(d);
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add duplicates
        if (duplicates.isEmpty()) {
            stringBuilder.append("-");
        } else {
            stringBuilder.append(String.join(",", duplicates));
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly mean
        if (hourlyMean.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (Double d : hourlyMean) {
                stringBuilder.append(d);
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly median
        if (hourlyMedian.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (Double d : hourlyMedian) {
                stringBuilder.append(d);
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        }

        // Add newline and return string
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private void parseLine(String line) {
        //  Name: parseLine()
        //  Date created: 06.12.2017
        //  Last modified: 25.12.2017
        //  Description: Reads values from a string and adds them to the lists

        /*
        Storage positions:

        0 - key
        1 - stats
            0 - total count (0-999)
            1 - mean
            2 - median
            3 - added items
            4 - discarded items
        2 - base
        3 - duplicates
        4 - hourly mean
        5 - hourly median

        0 key :: 1 stats :: 2 base :: 3 duplicates :: 4 h_mean :: 5 h_median
         */

        String[] splitLine = line.split("::");

        // Add key if missing
        if(key == null)
            key = splitLine[0];

        // Import statistical values
        if (!splitLine[1].equals("-") && mean + median <= 0) {
            String[] values = splitLine[1].split(",");
            totalCount = Integer.parseInt(values[0]);
            mean = Double.parseDouble(values[1]);
            median = Double.parseDouble(values[2]);

            // TODO: remove this clause but not the contents
            if (values.length > 3) {
                newItemsFoundInCycle += Integer.parseInt(values[3]);
            }

            // TODO: remove this clause but not the contents
            if (values.length > 4) {
                oldItemsDiscardedInCycle += Integer.parseInt(values[4]);
            }
        }

        // Import baseData values
        if (!splitLine[2].equals("-")) {
            if (baseData.isEmpty()) {
                for (String value : splitLine[2].split(",")) {
                    baseData.add(Double.parseDouble(value));
                }
            } else {
                // Make a copy of the list and clear it, as we need these elements to be on the top of the stack
                ArrayList<Double> tempDoubleStorageList = new ArrayList<>(baseData);
                baseData.clear();

                // Add values found in files to the bottom of the stack
                for (String value : splitLine[2].split(",")) {
                    baseData.add(Double.parseDouble(value));
                }

                // Add new values back to the list, but on top of the stack
                baseData.addAll(tempDoubleStorageList);

                // Clear excess elements
                if (baseData.size() > CONFIG.baseDataSize)
                    baseData.subList(0, baseData.size() - CONFIG.baseDataSize).clear();
            }
        }

        // Import duplicates
        if (!splitLine[3].equals("-")) {
            if (duplicates.isEmpty()) {
                Collections.addAll(duplicates, splitLine[3].split(","));
            } else {
                // Make a copy of the list and clear it, as we need these elements to be on the top of the stack
                ArrayList<String> tempStringStorageList = new ArrayList<>(duplicates);
                duplicates.clear();

                // Add values found in files to the bottom of the stack
                Collections.addAll(duplicates, splitLine[3].split(","));

                // Add new values back to the list, but on top of the stack
                duplicates.addAll(tempStringStorageList);

                // Clear excess elements
                if (duplicates.size() > CONFIG.duplicatesSize)
                    duplicates.subList(0, duplicates.size() - CONFIG.duplicatesSize).clear();
            }
        }

        // Safety measure for converting from one database type to another
        // TODO: remove this clause but not the contents
        if (splitLine.length > 5) {
            // Import hourly mean
            if (!splitLine[4].equals("-")) {
                if (hourlyMean.isEmpty()) {
                    for (String value : splitLine[4].split(",")) {
                        hourlyMean.add(Double.parseDouble(value));
                    }
                } else {
                    // Make a copy of the list and clear it, as we need these elements to be on the top of the stack
                    ArrayList<Double> tempDoubleStorageList = new ArrayList<>(hourlyMean);
                    hourlyMean.clear();

                    // Add values found in files to the bottom of the stack
                    for (String value : splitLine[4].split(",")) {
                        hourlyMean.add(Double.parseDouble(value));
                    }

                    // Add new values back to the list, but on top of the stack
                    hourlyMean.addAll(tempDoubleStorageList);

                    // Clear excess elements
                    if (hourlyMean.size() > CONFIG.dataEntryCycleLimit)
                        hourlyMean.subList(0, hourlyMean.size() - CONFIG.dataEntryCycleLimit).clear();
                }
            }
        }

        // Safety measure for converting from one database type to another
        // TODO: remove this clause but not the contents
        if (splitLine.length > 6) {
            // Import hourly median
            if (!splitLine[5].equals("-")) {
                if (hourlyMedian.isEmpty()) {
                    for (String value : splitLine[5].split(",")) {
                        hourlyMedian.add(Double.parseDouble(value));
                    }
                } else {
                    // Make a copy of the list and clear it, as we need these elements to be on the top of the stack
                    ArrayList<Double> tempDoubleStorageList = new ArrayList<>(hourlyMedian);
                    hourlyMedian.clear();

                    // Add values found in files to the bottom of the stack
                    for (String value : splitLine[5].split(",")) {
                        hourlyMedian.add(Double.parseDouble(value));
                    }

                    // Add new values back to the list, but on top of the stack
                    hourlyMedian.addAll(tempDoubleStorageList);

                    // Clear excess elements
                    if (hourlyMedian.size() > CONFIG.dataEntryCycleLimit)
                        hourlyMedian.subList(0, hourlyMedian.size() - CONFIG.dataEntryCycleLimit).clear();
                }
            }
        }
    }

    ///////////////////////////////////////
    // Predicate used to filter baseData //
    ///////////////////////////////////////

    private static class BaseDataClearingPredicate<T> implements Predicate<T> {
        //  Name: BaseDataClearingPredicate()
        //  Date created: 24.12.2017
        //  Last modified: 24.12.2017
        //  Description: Predicate used to filter baseData's values. If a value strays too far from median, it is removed

        Double median;

        public boolean test(T value) {
            return (Double) value > median * 2.0 || (Double) value < median / 2.0;
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public double getMedian() {
        return median;
    }

    public String getKey() {
        return key;
    }

    public static void incCycleCount() {
        cycleCount++;
    }

    public static void zeroCycleCount() {
        cycleCount = 0;
    }

    public int getCount() {
        return totalCount;
    }

    public static boolean getCycleState() {
        return cycleCount >= CONFIG.dataEntryCycleLimit;
    }

    public static int getCycleCount() {
        return cycleCount;
    }
}
