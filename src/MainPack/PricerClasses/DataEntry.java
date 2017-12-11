package MainPack.PricerClasses;

import MainPack.MapperClasses.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static MainPack.Main.PROPERTIES;

public class DataEntry {
    //  Name: DataEntry
    //  Date created: 05.12.2017
    //  Last modified: 11.12.2017
    //  Description: An object that stores an item's price data

    private static int CYCLE_COUNT = 0;
    private static final int CYCLE_LIMIT = Integer.parseInt(PROPERTIES.getProperty("DataEntryCycleLimit"));

    private int count = 0;
    private double mean = 0.0;
    private double median = 0.0;
    private String key;
    private int discardedCounter = 0;
    private int addedCounter = 0;

    private ArrayList<String[]> rawData = new ArrayList<>();
    private ArrayList<Double> baseData = new ArrayList<>();
    private ArrayList<Double> hourlyData = new ArrayList<>();
    private ArrayList<String> duplicates = new ArrayList<>();

    public void addRaw(Item item) {
        //  Name: addRaw
        //  Date created: 05.12.2017
        //  Last modified: 11.12.2017
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
        if (duplicates.size() > 60)
            duplicates.subList(0, duplicates.size() - 60).clear();
    }

    public void buildBaseData(Map<String, DataEntry> database) {
        //  Name: buildBaseData
        //  Date created: 29.11.2017
        //  Last modified: 11.12.2017
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
                if (database.containsKey(currencyKey)) {
                    if (database.get(currencyKey).getCount() >= 10) {
                        value = value * database.get(currencyKey).getMedian();
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

        // Soft-cap list at 100 entries
        if (baseData.size() > 100)
            baseData.subList(0, baseData.size() - 100).clear();
    }

    public void purgeBaseData() {
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

    public void buildStatistics() {
        //  Name: buildBaseData
        //  Date created: 29.11.2017
        //  Last modified: 10.12.2017
        //  Description: Method that adds entries to statistics

        Double mean = 0.0;
        int count = baseData.size();
        this.count = count;

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

        // Reassign count
        count = tempValueList.size();

        // Add up values to calculate mean
        for (Double i : tempValueList)
            mean += i;

        // Calculate mean and median values and round them to 3 digits
        this.mean = Math.round(mean / count * 10000) / 10000.0;
        this.median = Math.round(tempValueList.get((int) (count / 2.0)) * 10000) / 10000.0;

        // Add value to hourly
        hourlyData.add(this.median);

        // Limit hourly to 60 values
        if (hourlyData.size() > 60)
            hourlyData.subList(0, hourlyData.size() - 60).clear();
    }

    public void clearRawData() {
        //  Name: clearRawData
        //  Date created: 06.12.2017
        //  Last modified: 06.12.2017
        //  Description: Method that removes all entries from rawData from outside this object

        rawData.clear();
    }

    /////////////////
    // I/O helpers //
    /////////////////

    public String buildJSONPackage() {
        //  Name: buildJSONPackage()
        //  Date created: 06.12.2017
        //  Last modified: 11.12.2017
        //  Description: Creates a JSON-encoded string of hourly medians

        // Run every x cycles AND if there's enough data
        if (CYCLE_COUNT < CYCLE_LIMIT || hourlyData.isEmpty())
            return "";

        // Warn the user if there are irregularities with the discard counter
        if (addedCounter < discardedCounter && discardedCounter > 20)
            System.out.println("Odd discard ratio: " + key + " - " + addedCounter + " / " + discardedCounter);

        // Clear the counters
        this.discardedCounter = 0;
        this.addedCounter = 0;

        // Make a copy so the original order persists and sort the entries in growing order
        ArrayList<Double> tempValueList = new ArrayList<>();
        tempValueList.addAll(hourlyData);
        Collections.sort(tempValueList);

        // Add up values to calculate mean
        Double mean = 0.0;
        for (Double i : tempValueList) {
            mean += i;
        }

        int count = tempValueList.size();
        mean = Math.round(mean / count * 10000) / 10000.0;
        Double median = Math.round(tempValueList.get((int) (count / 2.0)) * 10000) / 10000.0;

        return "{\"median\": " + median + ", \"mean\": " + mean + ", \"count\": " + this.count + "}";
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

    public String getKey() {
        return key;
    }

    public static void incCycleCount() {
        CYCLE_COUNT++;
    }

    public static void zeroCycleCount() {
        CYCLE_COUNT = 0;
    }

    public int getCount() {
        return count;
    }

}
