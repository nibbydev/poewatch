package MainPack.PricerClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class DataEntry {
    //  Name: DataEntry
    //  Date created: 05.12.2017
    //  Last modified: 06.12.2017
    //  Description: An object that stores an item's price data

    private static int CYCLE_COUNT = 60;

    private int count = 0;
    private double mean = 0.0;
    private double median = 0.0;
    private String key;

    private ArrayList<String[]> rawData = new ArrayList<>();
    private ArrayList<Double> baseData = new ArrayList<>();
    private ArrayList<Double> hourlyData = new ArrayList<>();

    public void addRaw (String key, Double value, String type) {
        //  Name: addRaw
        //  Date created: 05.12.2017
        //  Last modified: 05.12.2017
        //  Description: Method that adds entries to raw data database

        this.key = key;
        rawData.add(new String[]{Double.toString(value), type});
    }

    public void buildBaseData(Map<String, DataEntry> statistics) {
        //  Name: buildBaseData
        //  Date created: 29.11.2017
        //  Last modified: 06.12.2017
        //  Description: Method that adds values from rawData to baseDatabase

        String index;
        Double value;

        // Loop through entries
        for (String[] entry : rawData) {
            value = Double.parseDouble(entry[0]);
            index = entry[1];

            // If we have the median price, use that
            if (!index.equals("1")) {
                // If there's a value in the statistics database, use that
                if (statistics.containsKey(key))
                    value = value * statistics.get(key).getMedian();
                else
                    continue;
            }

            // Round it up
            value = Math.round(value * 10000) / 10000.0;

            if (value > 0.01)
                baseData.add(value);
        }

        // Soft-cap list at 100 entries
        if (baseData.size() > 100)
            baseData.subList(0, baseData.size() - 100).clear();
    }

    public void purgeBaseData() {
        //  Name: purgeBaseData
        //  Date created: 29.11.2017
        //  Last modified: 06.12.2017
        //  Description: Method that removes entries from baseDatabase (based on statistics HashMap) depending
        //               whether there's a large difference between the two

        if (baseData.isEmpty())
            return;
        else if (median <= 0.0)
            return;

        // Make a copy of the original array (so it is not altered any further)
        for (Double value : new ArrayList<>(baseData)) {
            // Remove values that are larger/smaller than the median
            if (value > median * 2.0 || value < median / 2.0) {
                baseData.remove(value);
            }
        }
    }

    public void buildStatistics() {
        //  Name: buildBaseData
        //  Date created: 29.11.2017
        //  Last modified: 06.12.2017
        //  Description: Method that adds entries to statistics

        Double mean  = 0.0;
        int count = baseData.size();

        // Make a copy so the original order persists and sort new array
        ArrayList<Double> tempValueList = new ArrayList<>();
        tempValueList.addAll(baseData);
        Collections.sort(tempValueList);

        // Slice sorted copy for more precision. Skip entries with a small number of elements
        if (count < 15) {
            return;
        } else if (count < 30) {
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

        // Add up values to calculate mean
        for (Double i : tempValueList)
            mean += i;

        // Calculate mean and median values and round them to 3 digits
        this.mean = Math.round(mean / count * 10000) / 10000.0;
        this.median = Math.round(tempValueList.get((int) (count / 2.0)) * 10000) / 10000.0;

        // Add value to hourly
        hourlyData.add(this.median);
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
        //  Last modified: 06.12.2017
        //  Description: Creates a JSON-encoded string of hourly medians

        // Run every x cycles
        if (hourlyData.size() < CYCLE_COUNT)
            return "";

        // Sort the entries in growing order
        Collections.sort(hourlyData);
        int count = hourlyData.size();

        // Add up values to calculate mean
        Double mean = 0.0;
        for (Double i : hourlyData) {
            mean += i;
        }

        mean = Math.round(mean / count * 10000) / 10000.0;
        Double median = Math.round(hourlyData.get((int) (count / 2.0)) * 10000) / 10000.0;

        // Clear hourly statistics values
        hourlyData.clear();

        return "{\"median\": " + median + ", \"mean\": " + mean + ", \"count\": " + count + "},";
    }

    public void parseIOLine(String[] splitLine) {
        //  Name: parseIOLine()
        //  Date created: 06.12.2017
        //  Last modified: 06.12.2017
        //  Description: Turns a string array into values

        key = splitLine[0];

        // get basedata
        if(!splitLine[1].equals(" ")) {
            for (String value : splitLine[1].split(",")) {
                baseData.add(Double.parseDouble(value));
            }
        }

        // get hourly
        if(!splitLine[2].equals(" ")) {
            for (String value : splitLine[2].split(",")) {
                hourlyData.add(Double.parseDouble(value));
            }
        }

        // get stats
        if(!splitLine[3].equals(" ")) {
            count = Integer.parseInt(splitLine[3].split(",")[0]);
            mean = Double.parseDouble(splitLine[3].split(",")[1]);
            median = Double.parseDouble(splitLine[3].split(",")[2]);
        }
    }

    public String makeIOLine() {
        //  Name: makeIOLine()
        //  Date created: 06.12.2017
        //  Last modified: 06.12.2017
        //  Description: Turns this object's values into a special string

        StringBuilder stringBuilder = new StringBuilder();;

        // add base data
        stringBuilder.append(key);
        stringBuilder.append("::");
        if(baseData.isEmpty()) {
            stringBuilder.append(" ");
        } else {
            for (Double d : baseData) {
                stringBuilder.append(d);
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        }
        // add hourly data
        stringBuilder.append("::");
        if(hourlyData.isEmpty()) {
            stringBuilder.append(" ");
        } else {
            for (Double d : hourlyData) {
                stringBuilder.append(d);
                stringBuilder.append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        }

        // add stats
        stringBuilder.append("::");
        if(median + mean > 0) {
            stringBuilder.append(count);
            stringBuilder.append(",");
            stringBuilder.append(mean);
            stringBuilder.append(",");
            stringBuilder.append(median);
        } else {
            stringBuilder.append(" ");
        }

        // add newline
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
}
