package ovh.poe.Pricer;

import ovh.poe.Item;
import ovh.poe.Main;

import java.util.*;

/**
 * Price database entry object
 */
public class Entry {
    private static class RawEntry {
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
        String raw;

        void add (double mean, double median, double mode) {
            this.mean = mean;
            this.median = median;
            this.mode = mode;
        }

        void add(String raw) {
            // Eg "8.241,5.0,5.0", mean median mode respectively
            this.raw = raw;

            String[] splitRaw = raw.split(",");
            mean = Double.parseDouble(splitRaw[0]);
            median = Double.parseDouble(splitRaw[1]);
            mode = Double.parseDouble(splitRaw[2]);
        }

        @Override
        public String toString() {
            if (raw == null) return mean + "," + median + "," + mode;
            else return raw;
        }
    }

    private static class TenMinuteEntry {
        double mean, median, mode;
        String raw;

        void add (double mean, double median, double mode) {
            this.mean = mean;
            this.median = median;
            this.mode = mode;
        }

        void add(String raw) {
            // Eg "8.241,5.0,5.0", mean median mode respectively
            this.raw = raw;

            String[] splitRaw = raw.split(",");
            mean = Double.parseDouble(splitRaw[0]);
            median = Double.parseDouble(splitRaw[1]);
            mode = Double.parseDouble(splitRaw[2]);
        }

        @Override
        public String toString() {
            if (raw == null) return mean + "," + median + "," + mode;
            else return raw;
        }
    }

    public static class DailyEntry {
        double mean, median, mode;
        int quantity;
        String raw;

        void add(String raw) {
            // Eg "8.241,5.0,5.0,3283", mean median mode quantity respectively
            this.raw = raw;

            String[] splitRaw = raw.split(",");
            mean = Double.parseDouble(splitRaw[0]);
            median = Double.parseDouble(splitRaw[1]);
            mode = Double.parseDouble(splitRaw[2]);
            quantity = Integer.parseInt(splitRaw[3]);
        }

        void add(double mean, double median, double mode, int quantity) {
            this.mean = mean;
            this.median = median;
            this.mode = mode;
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            if (raw == null) return mean + "," + median + "," + mode + "," + quantity;
            else return raw;
        }
    }

    private static class ItemEntry {
        double price;
        String accountName, id, raw;

        void add (String raw) {
            this.raw = raw;
            String[] splitRaw = raw.split(",");

            this.price = Double.parseDouble(splitRaw[0]);
            this.accountName = splitRaw[1];
            this.id = splitRaw[2];
        }

        void add (double price, String accountName, String id) {
            this.price = price;
            this.accountName = accountName;
            this.id = id;
        }

        @Override
        public String toString() {
            if (raw == null) return price + "," + accountName + "," + id;
            else return raw;
        }
    }

    private String key, index = "-";
    private int total_counter, inc_counter, dec_counter, quantity;
    private double mean, median, mode, threshold_multiplier;

    // Lists that hold price data
    private List<RawEntry> db_raw = new ArrayList<>();
    private List<ItemEntry> db_items = new ArrayList<>(Main.CONFIG.baseDataSize);

    private List<DailyEntry> db_weekly = new ArrayList<>(7);
    private List<HourlyEntry> db_daily = new ArrayList<>(24);
    private List<TenMinuteEntry> db_hourly = new ArrayList<>(6);

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Used to load data in on object initialization
     *
     * @param line Database entry from the CSV-format file
     */
    Entry(String line) {
        parseLine(line);
    }

    /**
     * Needed to create an instance without initial parameters
     */
    Entry() {
    }

    /**
     * Adds entries to the db_raw and database_itemIDs lists
     *
     * @param item Item object
     * @param accountName Account name of the seller
     */
    public void add(Item item, String accountName) {
        if (key == null) key = item.key;

        // If missing, get item's index
        if (index.equals("-")) index = Main.RELATIONS.indexItem(item);

        // Add new value to raw data array
        RawEntry rawDataItem = new RawEntry();
        rawDataItem.add(item, accountName);
        db_raw.add(rawDataItem);
    }

    /**
     * Caller method. Calls other methods
     */
    public void cycle() {
        // Clear icon index if user requested icon database wipe
        if (Main.ENTRY_CONTROLLER.clearIndexes) {
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

        // This runs every 10 minutes
        if (Main.ENTRY_CONTROLLER.tenBool) {
            TenMinuteEntry tenMinuteEntry = new TenMinuteEntry();
            tenMinuteEntry.add(mean, median, mode);
            db_hourly.add(0, tenMinuteEntry);

            // Since the build() method overwrote mean, median and mode so an entry could be added to db_hourly, these
            // variables should be overwritten once again
            mean = findMeanHourly();
            median = findMedianHourly();
            mode = findModeHourly();
        }

        // This runs every 60 minutes
        if (Main.ENTRY_CONTROLLER.sixtyBool) {
            HourlyEntry hourlyEntry = new HourlyEntry();
            hourlyEntry.add(mean, median, mode);
            db_daily.add(0, hourlyEntry);

            total_counter += inc_counter - dec_counter;
            quantity += inc_counter - dec_counter;
            inc_counter = dec_counter = 0;
        }

        // This runs every 24 hours
        if (Main.ENTRY_CONTROLLER.twentyFourBool) {
            DailyEntry dailyEntry = new DailyEntry();
            dailyEntry.add(mean, median, mode, quantity);
            db_weekly.add(0, dailyEntry);
            quantity = 0;
        }

        // Limit list sizes
        cap();
    }

    //------------------------------------------------------------------------------------------------------------
    // Cycle methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Adds values from db_raw array to prices database array
     */
    private void parse() {
        // Loop through entries
        for (RawEntry raw : db_raw) {
            // If a user already has listed the same item before, ignore it
            boolean discard = false;
            for (ItemEntry itemEntry : db_items) {
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
                if (!Main.ENTRY_CONTROLLER.getEntryMap().containsKey(currencyKey)) continue;

                // Get the currency item entry the item was listed in
                Entry currencyEntry = Main.ENTRY_CONTROLLER.getEntryMap().get(currencyKey);

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
            ItemEntry itemEntry = new ItemEntry();
            itemEntry.add(raw.price, raw.accountName, raw.id);
            db_items.add(0,itemEntry);

            // Increment total added item counter
            inc_counter++;
        }

        // Clear raw data after extracting and converting values
        db_raw.clear();
    }

    /**
     * Removes improper entries from databases
     */
    private void purge() {
        // Precautions
        if (db_items.isEmpty()) return;
        // If too few items have been found then it probably doesn't have a median price
        if (total_counter + inc_counter < 10) return;
        // No median price found
        if (median <= 0) return;
        // 90% of added items are discarded
        if (inc_counter > 0 && dec_counter / inc_counter * 100 > 90) return;

        // Loop through database_prices, if the price is lower than the boundaries, remove the first instance of the
        // price and its related account name and ID
        int offset = 0;
        int oldSize = db_items.size();
        for (int i = 0; i < oldSize; i++) {
            double price = db_items.get(i - offset).price;

            // If price is more than double or less than half the median, remove it
            if (price > median * (2 + threshold_multiplier) || price < median / (2 + threshold_multiplier)) {
                // Remove the item
                db_items.remove(i - offset);

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
        if (db_items.isEmpty()) return;

        // Calculate mean and median values
        mean = findMeanItems();
        median = findMedianItems();
        mode = findModeItems();

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

        if (db_items.size() > Main.CONFIG.baseDataSize) {
            db_items.subList(Main.CONFIG.baseDataSize, db_items.size() - 1).clear();
        }

        if (Main.ENTRY_CONTROLLER.tenBool && db_hourly.size() > 6) {
            db_hourly.subList(6, db_hourly.size() - 1).clear();
        }

        if (Main.ENTRY_CONTROLLER.sixtyBool && db_daily.size() > 24) {
            db_daily.subList(24, db_daily.size() - 1).clear();
        }

        if (Main.ENTRY_CONTROLLER.twentyFourBool && db_weekly.size() > 7) {
            db_weekly.subList(7, db_weekly.size() - 1).clear();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Mean/median/mode calculation TODO: look for a better solution
    //------------------------------------------------------------------------------------------------------------

    private int findMeanQuantity() {
        if (db_weekly.isEmpty()) return 0;

        int mean = 0;
        for (DailyEntry entry : db_weekly) {
            mean += entry.quantity;
        }

        return mean / db_weekly.size();
    }

    private double findMeanItems() {
        if (db_items.isEmpty()) return 0;

        double mean = 0.0;
        for (ItemEntry entry : db_items) {
            mean += entry.price;
        }
        mean = Math.round(mean / db_items.size() * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;

        return mean;
    }

    private double findMedianItems() {
        if (db_items.isEmpty()) return 0;

        ArrayList<Double> tempList = new ArrayList<>();
        for (ItemEntry entry : db_items) {
            tempList.add(entry.price);
        }

        Collections.sort(tempList);

        return Math.round(tempList.get(tempList.size() / Main.CONFIG.medianLeftShift) * Main.CONFIG.pricePrecision)
                / Main.CONFIG.pricePrecision;
    }

    private double findModeItems() {
        double maxValue = 0, maxCount = 0;

        for (ItemEntry entry_1 : db_items) {
            int count = 0;

            for (ItemEntry entry_2 : db_items) {
                if (entry_2.price == entry_1.price) ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = entry_1.price;
            }
        }

        return maxValue;
    }

    private double findMeanHourly() {
        if (db_hourly.isEmpty()) return 0;

        double mean = 0.0;
        for (TenMinuteEntry entry : db_hourly) {
            mean += entry.mean;
        }
        mean = Math.round(mean / db_hourly.size() * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;

        return mean;
    }

    private double findMedianHourly() {
        if (db_hourly.isEmpty()) return 0;

        ArrayList<Double> tempList = new ArrayList<>();
        for (TenMinuteEntry entry : db_hourly) {
            tempList.add(entry.median);
        }

        Collections.sort(tempList);

        return Math.round(tempList.get(tempList.size() / Main.CONFIG.medianLeftShift) * Main.CONFIG.pricePrecision)
                / Main.CONFIG.pricePrecision;
    }

    private double findModeHourly() {
        double maxValue = 0, maxCount = 0;

        for (TenMinuteEntry entry_1 : db_hourly) {
            int count = 0;

            for (TenMinuteEntry entry_2 : db_hourly) {
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

        if (db_items.isEmpty()) return null;

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
        stringBuilder.append(",multiplier:");
        stringBuilder.append(Math.round(threshold_multiplier * 100.0) / 100.0);
        stringBuilder.append(",index:");
        stringBuilder.append(index);
        stringBuilder.append(",quantity:");
        stringBuilder.append(quantity);

        // Add delimiter
        stringBuilder.append("::");

        // Add database entries
        if (db_items.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (ItemEntry entry : db_items) {
                stringBuilder.append(entry.toString());
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (db_weekly.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (DailyEntry entry : db_weekly) {
                stringBuilder.append(entry.toString());
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (db_daily.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (HourlyEntry entry : db_daily) {
                stringBuilder.append(entry.toString());
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (db_hourly.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (TenMinuteEntry entry : db_hourly) {
                stringBuilder.append(entry.toString());
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
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
                    case "multiplier":
                        threshold_multiplier = Double.parseDouble(splitDataItem[1]);
                        break;
                    case "index":
                        index = splitDataItem[1];
                        break;
                    case "quantity":
                        quantity = Integer.parseInt(splitDataItem[1]);
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
                ItemEntry itemEntry = new ItemEntry();
                itemEntry.add(entry);
                db_items.add(itemEntry);
            }
        }

        // Import daily values
        if (!splitLine[3].equals("-")) {
            // Loop through all entries in the CSV
            for (String entry : splitLine[3].split("\\|")) {

                DailyEntry dailyEntry = new DailyEntry();
                dailyEntry.add(entry); // "8.241,5.0,5.0,3283"
                db_weekly.add(dailyEntry);
            }
        }

        // Import daily values
        if (!splitLine[4].equals("-")) {
            // Loop through all entries in the CSV
            for (String entry : splitLine[4].split("\\|")) {
                HourlyEntry hourlyEntry = new HourlyEntry();
                hourlyEntry.add(entry);
                db_daily.add(hourlyEntry);
            }
        }


        // Import hourly values
        if (!splitLine[5].equals("-")) {
            // Loop through all entries in the CSV
            for (String entry : splitLine[5].split("\\|")) {

                TenMinuteEntry tenMinuteEntry = new TenMinuteEntry();
                tenMinuteEntry.add(entry);
                db_hourly.add(tenMinuteEntry);
            }

            // Using the imported values, find the prices
            mean = findMeanHourly();
            median = findMedianHourly();
            mode = findModeHourly();
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
        return findMeanQuantity();
    }

    public List<DailyEntry> getDb_weekly() {
        return db_weekly;
    }
}
