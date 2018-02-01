package com.sanderh.Pricer;

import com.sanderh.Item;

import java.util.*;

import static com.sanderh.Main.CONFIG;
import static com.sanderh.Main.PRICER_CONTROLLER;
import static com.sanderh.Main.RELATIONS;

public class DataEntry {
    //  Name: DataEntry
    //  Date created: 05.12.2017
    //  Last modified: 30.01.2018
    //  Description: An object that stores an item's price data

    // TODO: instead of oldItem_counter have a percentage?

    private static int cycleCount = 0;
    private long total_counter = 0;
    private int newItem_counter = 0;
    private int oldItem_counter = 0;
    private double mean = 0.0;
    private double median;
    private double threshold_multiplier = 0.0;
    private String key;

    // Lists that hold price data
    private ArrayList<String> rawData = new ArrayList<>();
    private ArrayList<Double> database_prices = new ArrayList<>(CONFIG.baseDataSize);
    private ArrayList<String> database_accounts = new ArrayList<>(CONFIG.baseDataSize);
    private ArrayList<String> database_itemIDs = new ArrayList<>(CONFIG.baseDataSize);
    private ArrayList<Double> database_hourlyMean = new ArrayList<>(CONFIG.hourlyDataSize);
    private ArrayList<Double> database_hourlyMedian = new ArrayList<>(CONFIG.hourlyDataSize);

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
        // This is needed tbh
    }

    public void add(Item item, String accountName) {
        //  Name: add
        //  Date created: 05.12.2017
        //  Last modified: 26.01.2018
        //  Description: Adds entries to the rawData and database_itemIDs lists

        // Assign key if missing TODO: is this needed?
        if (key == null) key = item.getKey();

        // Add new value to raw data array
        rawData.add(item.getPrice() + "," + item.getPriceType() + "," + item.getId() + "," + accountName);
    }

    public void cycle(String line) {
        //  Name: cycle()
        //  Date created: 06.12.2017
        //  Last modified: 26.01.2018
        //  Description: Methods that constructs the object's. Should be called at a timed interval

        // Load data into lists
        parseLine(line);

        // Build statistics and databases
        parse();
        purge();
        build();

        // Limit list sizes
        cap();
    }

    public void cycle() {
        //  Name: cycle()
        //  Date created: 06.12.2017
        //  Last modified: 26.01.2018
        //  Description: Methods that constructs the object's. Should be called at a timed interval

        // Build statistics and databases
        parse();
        purge();
        build();

        // Limit list sizes
        cap();
    }

    /////////////////////
    // Private methods //
    /////////////////////

    private void parse() {
        //  Name: parse()
        //  Date created: 29.11.2017
        //  Last modified: 26.01.2018
        //  Description: Method that adds values from rawData array to database_prices array

        // Loop through entries
        for (String entry : rawData) {
            String[] splitEntry = entry.split(",");

            Double price = Double.parseDouble(splitEntry[0]);
            String priceType = splitEntry[1];
            String id = splitEntry[2];
            String account = splitEntry[3];

            // If a user already has listed a similar item, ignore it
            if (database_itemIDs.contains(id)) continue;
            // If a user already has listed the same item before, ignore it
            if (database_accounts.contains(account)) continue;

            // If the item was not listed for chaos orbs ("1" == Chaos Orb), then find the value in chaos
            if (!priceType.equals("1")) {
                // Get the database key of the currency the item was listed for
                String currencyKey = key.substring(0, key.indexOf("|")) + "|currency:orbs|" + RELATIONS.indexToName.get(priceType) + "|5";

                // If there does not exist a relation between listed currency to Chaos Orbs, ignore the item
                if (!PRICER_CONTROLLER.getCurrencyMap().containsKey(currencyKey)) continue;

                // Get the currency item entry the item was listed in
                DataEntry currencyEntry = PRICER_CONTROLLER.getCurrencyMap().get(currencyKey);

                // If the currency the item was listed in has very few listings then ignore this item
                if (currencyEntry.getCount() < 20) continue;

                // Convert the item's price into Chaos Orbs
                price = price * currencyEntry.getMedian();
            }

            // Hard-cap item prices
            if (price > 500000.0 || price < 0.001) continue;

            // Add values to the front of the lists
            database_prices.add(0, Math.round(price * CONFIG.pricePrecision) / CONFIG.pricePrecision);
            database_itemIDs.add(0, id);
            database_accounts.add(0, account);

            // Increment total added item counter
            newItem_counter++;
        }

        // Clear raw data after extracting and converting values
        rawData.clear();
    }

    private void purge() {
        //  Name: purge
        //  Date created: 29.11.2017
        //  Last modified: 27.01.2018
        //  Description: Method that removes entries from baseDatabase (based on statistics HashMap) depending
        //               whether there's a large difference between the two

        // Precautions
        if (database_prices.isEmpty()) return;
        // If too few items have been found then it probably doesn't have a median price
        if (total_counter < 20) return;
        // No median price found
        if (median <= 0) return;

        // Loop through database_prices, if the price is lower than the boundaries, remove the first instance of the
        // price and its related account name and ID
        int weight = 0;
        int oldSize = database_prices.size();
        for (int i = 0; i < oldSize; i++) {
            if (database_prices.get(i - weight) < median * (3.0 + threshold_multiplier))
                if (database_prices.get(i - weight) > median / (3.0 + threshold_multiplier)) continue;

            database_prices.remove(i - weight);
            database_itemIDs.remove(i - weight);
            database_accounts.remove(i - weight);

            // Since we removed elements with index i we need to adjust for the rest of them that fell back one place
            weight++;
        }

        // Increment discard counter by how many were discarded
        if (weight > 0) oldItem_counter += weight;
    }

    private void build() {
        // Precaution
        if (database_prices.isEmpty()) return;

        // Calculate mean and median values
        mean = findMean(database_prices);
        median = findMedian(database_prices);

        // add to hourly
        database_hourlyMean.add(0, mean);
        database_hourlyMedian.add(0, median);
    }

    private void cap() {
        //  Name: cap
        //  Date created: 26.01.2018
        //  Last modified: 30.01.2018
        //  Description: Makes sure lists don't exceed X amount of elements

        // If an array has more elements than specified, remove everything from the possible last index up until
        // however many excess elements it has
        if (database_prices.size() > CONFIG.baseDataSize) {
            database_prices.subList(CONFIG.baseDataSize, database_prices.size() - 1).clear();
            database_itemIDs.subList(CONFIG.baseDataSize, database_prices.size() - 1).clear();
            database_accounts.subList(CONFIG.baseDataSize, database_prices.size() - 1).clear();
        }

        // If an array has more elements than specified, remove everything from the possible last index up until
        // however many excess elements it has
        if (database_hourlyMean.size() > CONFIG.hourlyDataSize) {
            database_hourlyMean.subList(CONFIG.hourlyDataSize, database_hourlyMean.size() - 1).clear();
            database_hourlyMedian.subList(CONFIG.hourlyDataSize, database_hourlyMean.size() - 1).clear();
        }
    }

    private double findMean(ArrayList<Double> list) {
        //  Name: findMean
        //  Date created: 12.12.2017
        //  Last modified: 28.01.2018
        //  Description: Finds the mean value of an array

        double mean = 0.0;

        // Add up values to calculate mean
        for (Double i : list) mean += i;

        return Math.round(mean / list.size() * CONFIG.pricePrecision) / CONFIG.pricePrecision;
    }

    /**
     * Finds the median of the given array
     * @param list Unsorted array of doubles
     * @return Median value of the list, shifted by however much specified in the config
     */
    private double findMedian(ArrayList<Double> list) {
        //  Name: findMedian
        //  Date created: 12.12.2017
        //  Last modified: 28.12.2017
        //  Description: Finds the median value of an array. Has a shift to left

        // Precaution
        if (list.isEmpty()) return 0;

        // Make a copy so the original order persists and sort new array
        ArrayList<Double> tempList = new ArrayList<>(list);
        Collections.sort(tempList);

        return Math.round(tempList.get(tempList.size() / CONFIG.medianLeftShift) * CONFIG.pricePrecision) / CONFIG.pricePrecision;
    }

    /**
     * Finds the mode of the given array
     * @param list Unsorted array of doubles
     * @return Most frequently occurring value in the list
     */
    private double findMode(ArrayList<Double> list) {
        double maxValue = 0, maxCount = 0;

        for (double value_1 : list) {
            int count = 0;

            for (double value_2 : list) if (value_2 == value_1) ++count;

            if (count > maxCount) {
                maxCount = count;
                maxValue = value_1;
            }
        }

        return maxValue;
    }

    /////////////////
    // I/O helpers //
    /////////////////

    public String buildLine() {
        //  Name: buildLine()
        //  Date created: 06.12.2017
        //  Last modified: 30.01.2018
        //  Description: Converts this object's values into a string that's used for text-file-based storage

        StringBuilder stringBuilder = new StringBuilder();

        // Add key
        stringBuilder.append(key);

        // Add delimiter
        stringBuilder.append("::");

        // Add statistics
        stringBuilder.append(total_counter);
        stringBuilder.append(",");
        stringBuilder.append(mean);
        stringBuilder.append(",");
        stringBuilder.append(median);
        stringBuilder.append(",");
        stringBuilder.append(newItem_counter);
        stringBuilder.append(",");
        stringBuilder.append(oldItem_counter);
        stringBuilder.append(",");
        stringBuilder.append(Math.round(threshold_multiplier * 100.0) / 100.0);

        // Add delimiter
        stringBuilder.append("::");

        // Add database entries
        if (database_prices.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (int i = 0; i < database_prices.size(); i++) {
                stringBuilder.append(database_prices.get(i));
                stringBuilder.append(",");
                stringBuilder.append(database_accounts.get(i));
                stringBuilder.append(",");
                stringBuilder.append(database_itemIDs.get(i));
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (database_hourlyMean.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (int i = 0; i < database_hourlyMean.size(); i++) {
                stringBuilder.append(database_hourlyMean.get(i));
                stringBuilder.append(",");
                stringBuilder.append(database_hourlyMedian.get(i));
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add newline and return string
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private void parseLine(String line) {
        //  Name: parseLine()
        //  Date created: 06.12.2017
        //  Last modified: 30.01.2018
        //  Description: Reads values from a string and adds them to the lists

        /* (Spliterator: "::")
            0 - key
            1 - stats (Spliterator: ",")
                0 - total count (0-999-xxx)
                1 - mean
                2 - median
                3 - added items
                4 - discarded items
                5 - nr of problematic threshold_multiplier in the last x cycles
            2 - database entries (Spliterator: ";" and ",")
                0 - price
                1 - account name
                2 - item id
            3 - hourly (Spliterator: ",")
                0 - mean
                1 - median
         */

        String[] splitLine = line.split("::");

        // Add key if missing
        if (key == null) key = splitLine[0];

        // Import statistical values
        if (!splitLine[1].equals("-")) {
            String[] values = splitLine[1].split(",");
            total_counter = Long.parseLong(values[0]);
            mean = Double.parseDouble(values[1]);
            median = Double.parseDouble(values[2]);
            newItem_counter += Integer.parseInt(values[3]);
            oldItem_counter += Integer.parseInt(values[4]);
            threshold_multiplier = Double.parseDouble(values[5]);
        }

        // Import database_prices, account names and item IDs
        if (!splitLine[2].equals("-")) {
            for (String entry : splitLine[2].split("\\|")) {
                String[] entryList = entry.split(",");

                database_prices.add(Double.parseDouble(entryList[0]));
                database_accounts.add(entryList[1]);
                database_itemIDs.add(entryList[2]);
            }
        }

        // Import hourly mean and median values
        if (!splitLine[3].equals("-")) {
            for (String entry : splitLine[3].split("\\|")) {
                String[] entryList = entry.split(",");

                database_hourlyMean.add(Double.parseDouble(entryList[0]));
                database_hourlyMedian.add(Double.parseDouble(entryList[1]));
            }
        }
    }

    public String JSONController() {
        //  Name: JSONController()
        //  Date created: 31.12.2017
        //  Last modified: 30.01.2018
        //  Description: Decides whether to make a JSON package or not

        // Run every Xth cycle
        if (cycleCount < CONFIG.dataEntryCycleLimit) return null;
        // Run if there's any data
        if (database_hourlyMedian.isEmpty()) return null;

        // If more items were removed than added and at least 6 were removed, update counter by 0.1
        if (newItem_counter > 0 && oldItem_counter / newItem_counter * 100.0 > 70 && oldItem_counter > 5)
            threshold_multiplier += 0.1;
        else if (newItem_counter > 0 && threshold_multiplier > -1)
            threshold_multiplier -= 0.1;

        // Don't let it grow infinitely
        if (threshold_multiplier > 7) threshold_multiplier -= 0.2;

        // Display a warning in the console
        //if (newItem_counter > 0 && oldItem_counter / newItem_counter * 100.0 > 70 && oldItem_counter > 5)
        //    System.out.println("[WARN][" + key + "] " + newItem_counter + "/" + oldItem_counter
        //            + " (new/old); multi: " + threshold_multiplier);

        // Add new items to total counter
        total_counter += newItem_counter - oldItem_counter;
        if (total_counter < 0) total_counter = 0;

        // Form the return JSON string
        String JSONKey = key.substring(key.indexOf("|", key.indexOf("|") + 1) + 1);
        String returnString = "\"" + JSONKey + "\":{" +
                "\"mean\":" + findMean(database_prices) + "," +
                "\"median\":" + findMedian(database_prices) + "," +
                "\"mode\":" + findMode(database_prices) + "," +
                "\"count\":" + total_counter + "," +
                "\"inc\":" + newItem_counter + "," +
                "\"dec\":" + oldItem_counter + "}";

        // Clear the counters
        newItem_counter = 0;
        oldItem_counter = 0;

        return returnString;
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

    public long getCount() {
        return total_counter;
    }

    public static boolean getCycleState() {
        return cycleCount >= CONFIG.dataEntryCycleLimit;
    }

    public static int getCycleCount() {
        return cycleCount;
    }

    public static void setCycleCount(int cycleCount) {
        DataEntry.cycleCount = cycleCount;
    }
}
