package ovh.poe.Pricer;

import ovh.poe.Item;
import ovh.poe.Main;
import ovh.poe.RelationManager;

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
        private double mean, median, mode;
        private int quantity;
        private String raw;

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

        //---------------------------------------------------
        // Getters and setters
        //---------------------------------------------------

        public double getMean() {
            return mean;
        }

        public double getMedian() {
            return median;
        }

        public double getMode() {
            return mode;
        }

        public int getQuantity() {
            return quantity;
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

    private String league, index;
    private int total_counter, inc_counter, dec_counter, quantity;
    private double mean, median, mode, threshold_multiplier;

    private List<RawEntry> db_raw = new ArrayList<>();
    private List<RawEntry> db_temp = new ArrayList<>(Main.CONFIG.dbTempSize);
    private List<ItemEntry> db_items = new ArrayList<>(Main.CONFIG.baseDataSize);
    private List<DailyEntry> db_daily = new ArrayList<>(7);
    private List<HourlyEntry> db_hourly = new ArrayList<>(24);
    private List<TenMinuteEntry> db_minutely = new ArrayList<>(6);

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Used to load data in on object initialization
     *
     * @param line Database entry from the CSV-format file
     */
    Entry (String line, String league) {
        if (this.league == null) this.league = league;
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
    public void add(Item item, String accountName, String index) {
        if (this.index == null) this.index = index;
        if (league == null) league = item.league;

        // Add new value to raw data array
        RawEntry rawDataItem = new RawEntry();
        rawDataItem.add(item, accountName);
        db_raw.add(rawDataItem);
    }

    /**
     * Caller method. Calls other methods
     */
    public void cycle() {
        // Build statistics and databases
        parse();
        build();

        // Runs every 10 minutes
        if (Main.ENTRY_CONTROLLER.isTenBool()) {
            TenMinuteEntry tenMinuteEntry = new TenMinuteEntry();
            tenMinuteEntry.add(mean, median, mode);
            db_minutely.add(tenMinuteEntry);

            // Since the build() method overwrote mean, median and mode so an entry could be added to db_minutely, these
            // variables should be overwritten once again
            mean = findMeanHourly();
            median = findMedianHourly();
            mode = findModeHourly();
        }

        // Runs every 60 minutes
        if (Main.ENTRY_CONTROLLER.isSixtyBool()) {
            HourlyEntry hourlyEntry = new HourlyEntry();
            hourlyEntry.add(mean, median, mode);
            db_hourly.add(hourlyEntry);

            total_counter += inc_counter;
            quantity += inc_counter;
            inc_counter = dec_counter = 0;
        }

        // Runs every 24 hours
        if (Main.ENTRY_CONTROLLER.isTwentyFourBool()) {
            DailyEntry dailyEntry = new DailyEntry();
            dailyEntry.add(mean, median, mode, quantity);
            db_daily.add(dailyEntry);

            // Add this entry to league history
            Main.HISTORY_CONTROLLER.add(index, this);

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
        EntryController.IndexMap currencyMap = Main.ENTRY_CONTROLLER.getCurrencyMap(league);

        // Loop through entries
        for (RawEntry raw : db_raw) {
            if (checkRaw(raw)) continue;

            // If the item was not listed for chaos orbs, then find the value in chaos
            if (!raw.priceType.equals("Chaos Orb")) {
                if (currencyMap == null) continue;

                String fullIndex = Main.RELATIONS.getCurrencyNameToFullIndex().getOrDefault(raw.priceType, null);
                if (fullIndex == null) continue;

                Entry currencyEntry = currencyMap.getOrDefault(fullIndex, null);
                if (currencyEntry == null) continue;

                if (currencyEntry.getCount() < 20) continue;

                // Convert the item's price into Chaos Orbs
                raw.price = raw.price * currencyEntry.getMedian();
            }

            // Hard-cap item prices
            if (raw.price > 80000.0 || raw.price < 0.001) continue;

            // Round em up
            raw.price = Math.round(raw.price * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;

            // Add entry to the database
            ItemEntry itemEntry = new ItemEntry();
            itemEntry.add(raw.price, raw.accountName, raw.id);

            inc_counter++;

            if ( checkEntry(itemEntry) ) {
                db_items.add(itemEntry);
            } else {
                dec_counter++;
            }
        }

        // Clear raw data after extracting and converting values
        db_raw.clear();
    }

    /**
     * If a user already has listed the same item before, ignore it
     *
     * @param raw RawEntry to check and then store
     * @return True if should be discarded
     */
    private boolean checkRaw(RawEntry raw) {
        try {
            for (RawEntry tempEntry : db_temp) {
                if (tempEntry.accountName.equals(raw.accountName) || tempEntry.id.equals(raw.id)) {
                    return true;
                }
            }

            for (ItemEntry itemEntry : db_items) {
                if (itemEntry.accountName.equals(raw.accountName) || itemEntry.id.equals(raw.id)) {
                    return true;
                }
            }
        } finally {
            db_temp.add(raw);

            if (db_temp.size() > Main.CONFIG.dbTempSize) {
                db_temp.subList(0, db_temp.size() - Main.CONFIG.dbTempSize).clear();
            }
        }

        return false;
    }

    /**
     * Checks if entries should be added to the database
     *
     * @param entry Item entry to be evaluated
     * @return True if should be added, false if not
     */
    private boolean checkEntry(ItemEntry entry) {
        // No items have been listed
        if (db_items.isEmpty()) return true;
        // No average price found
        if (mean <= 0 || median <= 0) return true;

        // If price is more than double or less than half the median, remove it. Since we removed elements with
        // index i we need to adjust for the rest of them that fell back one place
        if (entry.price > median * (1.15 + threshold_multiplier) || entry.price < median / (1.15 + threshold_multiplier)) {
            return false;
        }

        // If the item  has been available for the past 2 days, checkEntry if price is much higher or lower than the
        // average price was 10 minutes ago
        if (db_daily.size() > 2 && !db_minutely.isEmpty()) {
            double tmpPastMean = db_minutely.get(db_minutely.size() - 1).mean;
            double tmpPercent = entry.price / tmpPastMean * 100;

            RelationManager.IndexedItem indexedItem = Main.RELATIONS.genericIndexToData(index);
            if (indexedItem == null) {
                System.out.println("null: "+index);
                return false;
            }

            switch (indexedItem.parent) {
                case "enchantments":
                    return tmpPercent > 5 && tmpPercent < 140;
                case "currency":
                    return tmpPercent > 50 && tmpPercent < 110;
                case "essence":
                    return tmpPercent > 50 && tmpPercent < 120;
                default:
                    return tmpPercent > 40 && tmpPercent < 200;
            }
        }

        return true;
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
        if (inc_counter > 0) {
            if (dec_counter > 0 && (dec_counter / (double)inc_counter) * 100.0 > 90) {
                threshold_multiplier += 0.1;
            } else {
                threshold_multiplier -= 0.01;
            }
        }

        // Don't let it grow infinitely
        if (threshold_multiplier > 4) threshold_multiplier = 4;
        if (threshold_multiplier < 0) threshold_multiplier = 0;
    }

    /**
     * Soft-caps database lists
     */
    private void cap() {
        // If an array has more elements than specified, remove everything from the possible last index up until
        // however many excess elements it has

        if (db_items.size() > Main.CONFIG.baseDataSize) {
            db_items.subList(0, db_items.size() - Main.CONFIG.baseDataSize).clear();
        }

        if (Main.ENTRY_CONTROLLER.isTenBool() && db_minutely.size() > 6) {
            db_minutely.subList(0, db_minutely.size() - 6).clear();
        }

        if (Main.ENTRY_CONTROLLER.isSixtyBool() && db_hourly.size() > 24) {
            db_hourly.subList(0, db_hourly.size() - 24).clear();
        }

        if (Main.ENTRY_CONTROLLER.isTwentyFourBool() && db_daily.size() > 7) {
            db_daily.subList(0, db_daily.size() - 7).clear();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Mean/median/mode calculation TODO: look for a better solution
    //------------------------------------------------------------------------------------------------------------

    private int findMeanQuantity() {
        if (db_daily.isEmpty()) return 0;

        int mean = 0;
        for (DailyEntry entry : db_daily) {
            mean += entry.quantity;
        }

        return mean / db_daily.size();
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
        if (db_minutely.isEmpty()) return 0;

        double mean = 0.0;
        for (TenMinuteEntry entry : db_minutely) {
            mean += entry.mean;
        }
        mean = Math.round(mean / db_minutely.size() * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;

        return mean;
    }

    private double findMedianHourly() {
        if (db_minutely.isEmpty()) return 0;

        ArrayList<Double> tempList = new ArrayList<>();
        for (TenMinuteEntry entry : db_minutely) {
            tempList.add(entry.median);
        }

        Collections.sort(tempList);

        return Math.round(tempList.get(tempList.size() / Main.CONFIG.medianLeftShift) * Main.CONFIG.pricePrecision)
                / Main.CONFIG.pricePrecision;
    }

    private double findModeHourly() {
        double maxValue = 0, maxCount = 0;

        for (TenMinuteEntry entry_1 : db_minutely) {
            int count = 0;

            for (TenMinuteEntry entry_2 : db_minutely) {
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
            0 - index
            1 - stats (Spliterator: "," and ":")
                count
                inc
                dec
                multiplier
                quantity
            2 - db_items entries (Spliterator: "|" and ",")
                0 - price
                1 - account name
                2 - item id
            3 - weekly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                2 - mode
                3 - quantity
            4 - daily (Spliterator: "|" and ",")
                0 - mean
                1 - median
                2 - mode
            5 - hourly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                2 - mode
         */

        if (index == null || index.equals("null")) {
            Main.ADMIN.log_("MISSING INDEX", 4);
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();

        // Add key
        stringBuilder.append(index);
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
        if (db_daily.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (DailyEntry entry : db_daily) {
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
            for (HourlyEntry entry : db_hourly) {
                stringBuilder.append(entry.toString());
                stringBuilder.append("|");
            }

            // Remove the overflow "|"
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        // Add delimiter
        stringBuilder.append("::");

        // Add hourly entries
        if (db_minutely.isEmpty()) {
            stringBuilder.append("-");
        } else {
            for (TenMinuteEntry entry : db_minutely) {
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
    public void parseLine(String line) {
        /* (Spliterator: "::")
            0 - index
            1 - stats (Spliterator: "," and ":")
                count
                inc
                dec
                multiplier
                quantity
            2 - db_items entries (Spliterator: "|" and ",")
                0 - price
                1 - account name
                2 - item id
            3 - weekly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                2 - mode
                3 - quantity
            4 - daily (Spliterator: "|" and ",")
                0 - mean
                1 - median
                2 - mode
            5 - hourly (Spliterator: "|" and ",")
                0 - mean
                1 - median
                2 - mode
         */

        String[] splitLine = line.split("::");

        // Add index if missing
        if (index == null && !RelationManager.isIndex(splitLine[0])) index = splitLine[0];

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
                    case "quantity":
                        quantity = Integer.parseInt(splitDataItem[1]);
                        break;
                    default:
                        Main.ADMIN.log_("Unknown field: " + splitDataItem[0], 3);
                        break;
                }
            }
        }

        // Import db_items' values
        if (!splitLine[2].equals("-")) {
            List<ItemEntry> temp = new ArrayList<>();

            for (String entry : splitLine[2].split("\\|")) {
                ItemEntry itemEntry = new ItemEntry();
                itemEntry.add(entry);
                temp.add(itemEntry);
            }

            db_items.addAll(0, temp);
        }

        // Import weekly values
        if (!splitLine[3].equals("-")) {
            List<DailyEntry> temp = new ArrayList<>();

            for (String entry : splitLine[3].split("\\|")) {
                DailyEntry dailyEntry = new DailyEntry();
                dailyEntry.add(entry);
                temp.add(dailyEntry);
            }

            db_daily.addAll(0, temp);
        }

        // Import daily values
        if (!splitLine[4].equals("-")) {
            List<HourlyEntry> temp = new ArrayList<>();

            for (String entry : splitLine[4].split("\\|")) {
                HourlyEntry hourlyEntry = new HourlyEntry();
                hourlyEntry.add(entry);
                temp.add(hourlyEntry);
            }

            db_hourly.addAll(0, temp);
        }


        // Import hourly values
        if (!splitLine[5].equals("-")) {
            List<TenMinuteEntry> temp = new ArrayList<>();

            for (String entry : splitLine[5].split("\\|")) {
                TenMinuteEntry tenMinuteEntry = new TenMinuteEntry();
                tenMinuteEntry.add(entry);
                temp.add(tenMinuteEntry);
            }

            db_minutely.addAll(0, temp);

            // Using the imported values, calculate the prices
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

    public int getCount() {
        return total_counter;
    }

    public int getInc_counter() {
        return inc_counter;
    }

    public String getIndex() {
        return index;
    }

    public int getQuantity() {
        return findMeanQuantity();
    }

    public List<DailyEntry> getDb_daily() {
        return db_daily;
    }

    public void setLeague(String league) {
        if (this.league == null) this.league = league;
    }

    public String getLeague() {
        return league;
    }
}
