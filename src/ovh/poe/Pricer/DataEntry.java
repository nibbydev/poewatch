package ovh.poe.Pricer;

import ovh.poe.Item;
import ovh.poe.Main;

import java.util.*;

/**
 * Price database entry object
 */
public class DataEntry {
    private static class RawDataItem {
        String accountName, priceType, id;
        double price;

        void add (Item item, String accountName) {
            this.price = item.price;
            this.id = item.id;
            this.accountName = accountName;
            this.priceType = item.priceType;
        }
    }

    private static class HourlyEntry {
        double mean, median, mode;

        HourlyEntry (double mean, double median, double mode) {
            this.mean = mean;
            this.median = median;
            this.mode = mode;
        }
    }

    private static class ItemEntry {
        double price;
        String accountName, id;

        ItemEntry (double price, String accountName, String id) {
            this.price = price;
            this.accountName = accountName;
            this.id = id;
        }
    }

    private String key;
    private String index = "-";
    private int total_counter = 0;
    private int inc_counter = 0;
    private int dec_counter = 0;
    private double mean = 0.0;
    private double median, mode;
    private double threshold_multiplier = 0.0;
    private int tempQuantity = 0;
    private int quantity = 0;

    // Lists that hold price data
    private ArrayList<RawDataItem> rawData = new ArrayList<>();
    private ArrayList<ItemEntry> database_items = new ArrayList<>(Main.CONFIG.baseDataSize);
    private ArrayList<HourlyEntry> database_hourly = new ArrayList<>(Main.CONFIG.hourlyDataSize);
    private ArrayList<Integer> database_quantity = new ArrayList<>(7);

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Used to load data in on object initialization
     *
     * @param line Database entry from the CSV-format file
     */
    public DataEntry(String line) {
        parseLine(line);
    }

    /**
     * This is needed to create an instance without initial parameters
     */
    public DataEntry() {
    }

    /**
     * Adds entries to the rawData and database_itemIDs lists
     *
     * @param item Item object
     * @param accountName Account name of the seller
     */
    public void add(Item item, String accountName) {
        if (key == null) key = item.key;

        // If missing, get item's index
        if (index.equals("-")) index = Main.RELATIONS.indexItem(item);

        // Add new value to raw data array
        RawDataItem rawDataItem = new RawDataItem();
        rawDataItem.add(item, accountName);
        rawData.add(rawDataItem);
    }

    /**
     * Caller method. Calls other methods
     */
    public void cycle() {
        // Clear icon index if user requested icon database wipe
        if (Main.PRICER_CONTROLLER.clearIndexes) {
            // Clear indexes flag was up, clear the indexed item database
            index = "-";
        } else if (index.equals("-")) {
            // Attempt to find the item's index
            index = Main.RELATIONS.getIndexFromKey(key);
        }

        // Build statistics and databases
        parse();
        purge();
        build();

        // This runs every ~10 cycles
        if (Main.PRICER_CONTROLLER.clearStats) {
            total_counter += inc_counter;
            tempQuantity += inc_counter;
            inc_counter = dec_counter = 0;
        }

        // This runs every 24 hours
        if (Main.PRICER_CONTROLLER.twentyFourBool) {
            database_quantity.add(0 , tempQuantity);

            // Cap the list
            if (database_quantity.size() > 7) {
                database_quantity.subList(7, database_hourly.size() - 1).clear();
            }

            tempQuantity = 0;
            quantity = findMeanQuantity();
        }

        // Limit list sizes
        cap();
    }

    //------------------------------------------------------------------------------------------------------------
    // Cycle methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Adds values from rawData array to prices database array
     */
    private void parse() {
        // Loop through entries
        for (RawDataItem raw : rawData) {
            // If a user already has listed the same item before, ignore it
            boolean discard = false;
            for (ItemEntry itemEntry : database_items) {
                if (itemEntry.accountName.equals(raw.accountName) || itemEntry.id.equals(raw.id)) {
                    discard = true;
                    break;
                }
            }
            if (discard) continue;

            // If the item was not listed for chaos orbs ("0" == Chaos Orb), then find the value in chaos
            if (!raw.priceType.equals("0")) {
                // Get the database key of the currency the item was listed for
                String currencyKey = key.substring(0, key.indexOf("|")) + "|currency|" +
                        Main.RELATIONS.currencyIndexToName.get(raw.priceType) + "|5";

                // If there does not exist a relation between listed currency to Chaos Orbs, ignore the item
                if (!Main.PRICER_CONTROLLER.getEntryMap().containsKey(currencyKey)) continue;

                // Get the currency item entry the item was listed in
                DataEntry currencyEntry = Main.PRICER_CONTROLLER.getEntryMap().get(currencyKey);

                // If the currency the item was listed in has very few listings then ignore this item
                if (currencyEntry.getCount() < 20) continue;

                // Convert the item's price into Chaos Orbs
                raw.price = raw.price * currencyEntry.getMedian();
            }

            // Hard-cap item prices
            if (raw.price > 50000.0 || raw.price < 0.001) continue;

            // Round em up
            raw.price = Math.round(raw.price * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;

            // Add entry to the database
            database_items.add(0, new ItemEntry(raw.price, raw.accountName, raw.id));

            // Increment total added item counter
            inc_counter++;
        }

        // Clear raw data after extracting and converting values
        rawData.clear();
    }

    /**
     * Removes improper entries from databases
     */
    private void purge() {
        // Precautions
        if (database_items.isEmpty()) return;
        // If too few items have been found then it probably doesn't have a median price
        if (total_counter + inc_counter < 10) return;
        // No median price found
        if (median <= 0) return;
        // 90% of added items are discarded
        if (inc_counter > 0 && dec_counter / inc_counter * 100 > 90) return;

        // Loop through database_prices, if the price is lower than the boundaries, remove the first instance of the
        // price and its related account name and ID
        int offset = 0;
        int oldSize = database_items.size();
        for (int i = 0; i < oldSize; i++) {
            double price = database_items.get(i - offset).price;

            // If price is more than double or less than half the median, remove it
            if (price > median * (2 + threshold_multiplier) || price < median / (2 + threshold_multiplier)) {
                // Remove the item
                database_items.remove(i - offset);

                // Since we removed elements with index i we need to adjust for the rest of them that fell back one place
                offset++;
            }
        }

        // Increment discard counter by how many were discarded
        if (offset > 0) dec_counter += offset;
    }

    /**
     * Calculates mean/median
     */
    private void build() {
        // Precautions
        if (database_items.isEmpty()) return;

        // Calculate mean and median values
        double tempMean = findMeanItems();
        double tempMedian = findMedianItems();
        double tempMode = findModeItems();

        // Add to hourly
        if (tempMean + tempMedian + tempMode > 0)
            database_hourly.add(0, new HourlyEntry(tempMean, tempMedian, tempMode));

        // Calculate mean and median values
        mean = findMeanHourly();
        median = findMedianHourly();
        mode = findModeHourly();

        // If more items were removed than added and at least 6 were removed, update counter by 0.1
        if (inc_counter > 0 && dec_counter > 0 && (dec_counter / (double)inc_counter) * 100.0 > 80)
            threshold_multiplier += 0.1;
        else if (inc_counter > 0)
            threshold_multiplier -= 0.01;

        // Don't let it grow infinitely
        if (threshold_multiplier > 7) threshold_multiplier = 7;
        if (threshold_multiplier < 0) threshold_multiplier = 0;
    }

    /**
     * Soft-caps database lists
     */
    private void cap() {
        // If an array has more elements than specified, remove everything from the possible last index up until
        // however many excess elements it has

        if (database_items.size() > Main.CONFIG.baseDataSize) {
            database_items.subList(Main.CONFIG.baseDataSize, database_items.size() - 1).clear();
        }

        if (database_hourly.size() > Main.CONFIG.hourlyDataSize) {
            database_hourly.subList(Main.CONFIG.hourlyDataSize, database_hourly.size() - 1).clear();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Mean/median/mode calculation TODO: look for a better solution
    //------------------------------------------------------------------------------------------------------------

    private double findMeanItems() {
        if (database_items.isEmpty()) return 0;

        double mean = 0.0;
        for (ItemEntry entry : database_items) {
            mean += entry.price;
        }
        mean = Math.round(mean / database_items.size() * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;

        return mean;
    }

    private double findMeanHourly() {
        if (database_hourly.isEmpty()) return 0;

        double mean = 0.0;
        for (HourlyEntry entry : database_hourly) {
            mean += entry.mean;
        }
        mean = Math.round(mean / database_hourly.size() * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;

        return mean;
    }

    private int findMeanQuantity() {
        if (database_quantity.isEmpty()) return 0;

        int mean = 0;
        for (Integer entry : database_quantity) {
            mean += entry;
        }

        return mean / database_quantity.size();
    }

    private double findMedianItems() {
        if (database_items.isEmpty()) return 0;

        ArrayList<Double> tempList = new ArrayList<>();
        for (ItemEntry entry : database_items) {
            tempList.add(entry.price);
        }

        Collections.sort(tempList);

        return Math.round(tempList.get(tempList.size() / Main.CONFIG.medianLeftShift) * Main.CONFIG.pricePrecision)
                / Main.CONFIG.pricePrecision;
    }

    private double findMedianHourly() {
        if (database_hourly.isEmpty()) return 0;

        ArrayList<Double> tempList = new ArrayList<>();
        for (HourlyEntry entry : database_hourly) {
            tempList.add(entry.median);
        }

        Collections.sort(tempList);

        return Math.round(tempList.get(tempList.size() / Main.CONFIG.medianLeftShift) * Main.CONFIG.pricePrecision)
                / Main.CONFIG.pricePrecision;
    }

    private double findModeItems() {
        double maxValue = 0, maxCount = 0;

        for (ItemEntry entry_1 : database_items) {
            int count = 0;

            for (ItemEntry entry_2 : database_items) {
                if (entry_2.price == entry_1.price) ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = entry_1.price;
            }
        }

        return maxValue;
    }

    private double findModeHourly() {
        double maxValue = 0, maxCount = 0;

        for (HourlyEntry entry_1 : database_hourly) {
            int count = 0;

            for (HourlyEntry entry_2 : database_hourly) {
                if (entry_2.mode == entry_1.mode) ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = entry_1.mode;
            }
        }

        return maxValue;
    }

    //------------------------------------------------------------------------------------------------------------
    // Generic I/O
    //------------------------------------------------------------------------------------------------------------

    /**
     * Converts this instance's values into CSV format
     *
     * @return CSV line
     */
    public String buildLine() {
        /* (Spliterator: "::")
            0 - key
            1 - stats (Spliterator: "," and ":")
                cnt: - total count during league
                inc: - added items per 24h
                dec: - discarded items 24h
                mea: - mean
                med: - median
                mod: - mode
                mtp: - threshold_multiplier
                ico: - icon index
                quantity: - quantity counter
            2 - database entries (Spliterator: "|" and ",")
                0 - price
                1 - account name
                2 - item id
            3 - hourly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                1 - mode
         */

        if (database_items.isEmpty()) return null;

        StringBuilder stringBuilder = new StringBuilder();

        // Add key
        stringBuilder.append(key);
        stringBuilder.append("::");

        // Add statistics
        stringBuilder.append("count:");
        stringBuilder.append(total_counter);
        stringBuilder.append(",inc:");
        stringBuilder.append(inc_counter);
        stringBuilder.append(",dec:");
        stringBuilder.append(dec_counter);
        stringBuilder.append(",mean:");
        stringBuilder.append(mean);
        stringBuilder.append(",median:");
        stringBuilder.append(median);
        stringBuilder.append(",mode:");
        stringBuilder.append(mode);
        stringBuilder.append(",multiplier:");
        stringBuilder.append(Math.round(threshold_multiplier * 100.0) / 100.0);
        stringBuilder.append(",index:");
        stringBuilder.append(index);
        stringBuilder.append(",tempQuantity:");
        stringBuilder.append(tempQuantity);
        stringBuilder.append(",quantity:");
        stringBuilder.append(quantity);

        // Add delimiter
        stringBuilder.append("::");

        // Add database entries
        if (database_items.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (ItemEntry entry : database_items) {
                stringBuilder.append(entry.price);
                stringBuilder.append(",");
                stringBuilder.append(entry.accountName);
                stringBuilder.append(",");
                stringBuilder.append(entry.id);
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (database_hourly.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (HourlyEntry entry : database_hourly) {
                stringBuilder.append(entry.mean);
                stringBuilder.append(",");
                stringBuilder.append(entry.median);
                stringBuilder.append(",");
                stringBuilder.append(entry.mode);
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (database_quantity.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (Integer entry : database_quantity) {
                stringBuilder.append(entry);
                stringBuilder.append(",");
            }

            // Remove the overflow ","
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add newline and return string
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    /**
     * Reads values from CSV line and adds them to lists
     *
     * @param line CSV line
     */
    private void parseLine(String line) {
        /* (Spliterator: "::")
            0 - key
            1 - stats (Spliterator: "," and ":")
                cnt: - total count during league
                inc: - added items per 24h
                dec: - discarded items 24h
                mea: - mean
                med: - median
                mod: - mode
                mtp: - threshold_multiplier
                ico: - icon index
                quantity: - quantity counter
            2 - database entries (Spliterator: "|" and ",")
                0 - price
                1 - account name
                2 - item id
            3 - hourly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                1 - mode
         */

        String[] splitLine = line.split("::");

        // Add key if missing
        if (key == null) key = splitLine[0];

        // Import statistical values
        if (!splitLine[1].equals("-")) {
            String[] values = splitLine[1].split(",");

            for (String dataItem : values) {
                String[] splitDataItem = dataItem.split(":");

                switch (splitDataItem[0]) {
                    case "count":
                        total_counter = Integer.parseInt(splitDataItem[1]);
                        break;
                    case "inc":
                        inc_counter += Integer.parseInt(splitDataItem[1]);
                        break;
                    case "dec":
                        dec_counter += Integer.parseInt(splitDataItem[1]);
                        break;
                    case "mean":
                        mean = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "median":
                        median = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "mode":
                        mode = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "multiplier":
                        threshold_multiplier = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "index":
                        index = splitDataItem[1];
                        break;
                    case "quantity":
                        quantity = Integer.parseInt(splitDataItem[1]);
                        break;
                    case "tempQuantity":
                        tempQuantity = Integer.parseInt(splitDataItem[1]);
                        break;
                    default:
                        Main.ADMIN.log_("Unknown field: " + splitDataItem[0], 3);
                        break;
                }
            }
        }

        // Import database_prices, account names and item IDs
        if (!splitLine[2].equals("-")) {
            for (String entry : splitLine[2].split("\\|")) {
                String[] entryList = entry.split(",");

                database_items.add(new ItemEntry(Double.parseDouble(entryList[0]), entryList[1], entryList[2]));
            }
        }

        // Import hourly mean and median values
        if (!splitLine[3].equals("-")) {
            for (String entry : splitLine[3].split("\\|")) {
                String[] entryList = entry.split(",");

                database_hourly.add(new HourlyEntry(Double.parseDouble(entryList[0]), Double.parseDouble(entryList[1]), Double.parseDouble(entryList[2])));
            }
        }

        // Import hourly mean and median values
        if (!splitLine[4].equals("-")) {
            for (String entry : splitLine[4].split(",")) {
                database_quantity.add(Integer.parseInt(entry));
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public double getMode() {
        return mode;
    }

    public String getKey() {
        return key;
    }

    public int getCount() {
        return total_counter;
    }

    public int getInc_counter() {
        return inc_counter;
    }

    public String getItemIndex() {
        return index;
    }

    public int getQuantity() {
        return quantity;
    }
}
