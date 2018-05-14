package com.poestats.Pricer;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.Pricer.Entries.*;
import com.poestats.Pricer.Maps.*;
import com.poestats.RelationManager;

import java.util.*;

/**
 * Price database entry object
 */
public class Entry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String league, index;
    private int total_counter, inc, dec, quantity;
    private double mean, median, mode;

    private List<RawEntry> db_raw = new ArrayList<>();
    private List<RawEntry> db_temp = new ArrayList<>(Config.entry_tempSize);
    private List<ItemEntry> db_items = new ArrayList<>(Config.entry_itemsSize);
    private List<DailyEntry> db_daily = new ArrayList<>(Config.entry_dailySize);
    private List<HourlyEntry> db_hourly = new ArrayList<>(Config.entry_hourlySize);
    private List<TenMinuteEntry> db_minutely = new ArrayList<>(Config.entry_minutelySize);

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
     * @param accountName Account id of the seller
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

            // These don't align up due to manual intervention
            if (!Main.ENTRY_CONTROLLER.isTenBool()) {
                mean = findMeanHourly();
                median = findMedianHourly();
                mode = findModeHourly();
            }

            HourlyEntry hourlyEntry = new HourlyEntry();
            hourlyEntry.add(mean, median, mode, inc, dec);
            db_hourly.add(hourlyEntry);

            total_counter += inc;
            inc = dec = 0;
            quantity = calcQuantity();
        }

        // Runs every 24 hours
        if (Main.ENTRY_CONTROLLER.isTwentyFourBool()) {

            // These don't align up due to manual intervention
            if (!Main.ENTRY_CONTROLLER.isTenBool()) {
                mean = findMeanHourly();
                median = findMedianHourly();
                mode = findModeHourly();
            }

            DailyEntry dailyEntry = new DailyEntry();
            dailyEntry.add(mean, median, mode, calcQuantity() + inc);
            db_daily.add(dailyEntry);

            // Add this entry to league history
            Main.HISTORY_CONTROLLER.add(index, this);
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
        IndexMap currencyMap = Main.ENTRY_CONTROLLER.getCurrencyMap(league);
        RelationManager.IndexedItem indexedItem = Main.RELATIONS.indexToGenericData(index);

        // Loop through entries
        for (RawEntry raw : db_raw) {
            if (checkRaw(raw, indexedItem.frame)) continue;

            // If the item was not listed for chaos orbs, then find the value in chaos
            if (!raw.getPriceType().equals("Chaos Orb")) {
                if (currencyMap == null) continue;

                String fullIndex = Main.RELATIONS.getCurrencyNameToFullIndex().getOrDefault(raw.getPriceType(), null);
                if (fullIndex == null) continue;

                Entry currencyEntry = currencyMap.getOrDefault(fullIndex, null);
                if (currencyEntry == null) continue;

                if (currencyEntry.getCount() < 20) continue;

                // Convert the item's price into Chaos Orbs
                raw.setPrice(raw.getPrice() * currencyEntry.getMedian());
            }

            // Hard-cap item prices
            if (raw.getPrice() > 120000.0 || raw.getPrice() < 0.001) continue;

            // Round em up
            raw.setPrice(Math.round(raw.getPrice() * Config.item_pricePrecision) / Config.item_pricePrecision);

            // Add entry to the database
            ItemEntry itemEntry = new ItemEntry();
            itemEntry.add(raw.getPrice(), raw.getAccountName(), raw.getId());

            inc++;

            if ( checkEntry(itemEntry) ) {
                db_items.add(itemEntry);
            } else {
                dec++;
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
    private boolean checkRaw(RawEntry raw, int frame) {
        try {
            // Don't check for duplicates if less than 10 are listed each day
            if (frame == 5 && quantity < 20) return false;

            for (RawEntry tempEntry : db_temp) {
                if (tempEntry.getId().equals(raw.getId())) return true;
                if (tempEntry.getAccountName().equals(raw.getAccountName())) return true;
            }

            for (ItemEntry itemEntry : db_items) {
                if (itemEntry.getId().equals(raw.getId())) return true;
                if (itemEntry.getAccountName().equals(raw.getAccountName())) return true;
            }
        } finally {
            db_temp.add(raw);

            if (db_temp.size() > Config.entry_tempSize) {
                db_temp.subList(0, db_temp.size() - Config.entry_tempSize).clear();
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
        // Very few items have been listed
        if (total_counter < 15) return true;

        RelationManager.IndexedItem indexedItem = Main.RELATIONS.indexToGenericData(index);
        if (indexedItem == null) {
            System.out.println("null: "+index);
            return false;
        }

        // If the item  has been available for the past 2 days, checkEntry if price is much higher or lower than the
        // average price was 10 minutes ago
        if (!db_minutely.isEmpty()) {
            double tmpPastMedian = db_minutely.get(db_minutely.size() - 1).getMedian();
            if (tmpPastMedian == 0) return true;
            double tmpPercent = entry.getPrice() / tmpPastMedian * 100;

            if (db_daily.size() > 1) {
                switch (indexedItem.parent) {
                    case "enchantments":
                        return tmpPercent < 140;
                    case "currency":
                        if (tmpPastMedian > 300) {
                            return tmpPercent > 80 && tmpPercent < 140;
                        } else {
                            //return tmpPercent > 90 && tmpPercent < 110;
                            return tmpPercent < 200;
                        }
                    case "essence":
                        return tmpPercent > 50 && tmpPercent < 120;
                    default:
                        return tmpPercent > 40 && tmpPercent < 200;
                }
            } else {
                return tmpPercent < 200;
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
        List<Double> sortedItemPrices = sortItemPrices();

        mean = findMeanItems(sortedItemPrices);
        median = findMedianItems(sortedItemPrices);
        mode = findModeItems(sortedItemPrices);
    }

    /**
     * Soft-caps database lists
     */
    private void cap() {
        // If an array has more elements than specified, remove everything from the possible last index up until
        // however many excess elements it has

        if (db_items.size() > Config.entry_itemsSize) {
            db_items.subList(0, db_items.size() - Config.entry_itemsSize).clear();
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

    private int calcQuantity() {
        int tmp = 0;
        for (HourlyEntry entry : db_hourly) {
            tmp += entry.getInc();
        }

        return tmp;
    }

    private List<Double> sortItemPrices() {
        List<Double> tempList = new ArrayList<>();
        for (ItemEntry entry : db_items) tempList.add(entry.getPrice());
        Collections.sort(tempList);

        if (db_items.size() > 1) {
            if (db_daily.size() < 2) {
                int startIndex = db_items.size() * Config.entry_shiftPercent / 100;
                return tempList.subList(0, startIndex);
            } else {
                int endIndex = db_items.size() * Config.entry_shiftPercentNew / 100;
                int startIndex = (tempList.size() - endIndex) / 2;
                return tempList.subList(startIndex, endIndex);
            }
        } else {
            return tempList;
        }
    }

    private double findMeanItems(List<Double> sortedItemPrices) {
        if (sortedItemPrices.isEmpty()) return 0;

        double mean = 0.0;
        for (Double entry : sortedItemPrices) mean += entry;

        return Math.round(mean / sortedItemPrices.size() * Config.item_pricePrecision) / Config.item_pricePrecision;
    }

    private double findMedianItems(List<Double> sortedItemPrices) {
        if (sortedItemPrices.isEmpty()) return 0;

        int medianIndex = sortedItemPrices.size() / 2;
        return Math.round(sortedItemPrices.get(medianIndex) * Config.item_pricePrecision) / Config.item_pricePrecision;
    }

    private double findModeItems(List<Double> sortedItemPrices) {
        if (sortedItemPrices.isEmpty()) return 0;

        double maxValue = 0, maxCount = 0;

        for (Double entry1 : sortedItemPrices) {
            int count = 0;

            for (Double entry2 : sortedItemPrices) {
                if (entry1.equals(entry2)) ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = entry1;
            }
        }

        return maxValue;
    }

    private double findMeanHourly() {
        if (db_minutely.isEmpty()) return 0;

        double mean = 0.0;
        for (TenMinuteEntry entry : db_minutely) {
            mean += entry.getMean();
        }
        mean = Math.round(mean / db_minutely.size() * Config.item_pricePrecision) / Config.item_pricePrecision;

        return mean;
    }

    private double findMedianHourly() {
        if (db_minutely.isEmpty()) return 0;

        ArrayList<Double> tempList = new ArrayList<>();
        for (TenMinuteEntry entry : db_minutely) {
            tempList.add(entry.getMedian());
        }

        Collections.sort(tempList);

        int medianIndex = tempList.size() / 2;
        return Math.round(tempList.get(medianIndex) * Config.item_pricePrecision) / Config.item_pricePrecision;
    }

    private double findModeHourly() {
        double maxValue = 0, maxCount = 0;

        for (TenMinuteEntry entry1 : db_minutely) {
            int count = 0;

            for (TenMinuteEntry entry2 : db_minutely) {
                if (entry2.getMode() == entry1.getMode()) ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = entry1.getMode();
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
            2 - db_items entries (Spliterator: "|" and ",")
                0 - price
                1 - account id
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
        stringBuilder.append(inc);
        stringBuilder.append(",dec:");
        stringBuilder.append(dec);

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
            2 - db_items entries (Spliterator: "|" and ",")
                0 - price
                1 - account id
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
                        inc += Integer.parseInt(splitDataItem[1]);
                        break;
                    case "dec":
                        dec += Integer.parseInt(splitDataItem[1]);
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

            // Using the imported values, calculate daily quantity
            quantity = calcQuantity();
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

    public int getQuantity() {
        return quantity;
    }

    public int getCount() {
        return total_counter;
    }

    public int getInc() {
        return inc;
    }

    public String getIndex() {
        return index;
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
