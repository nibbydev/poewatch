package com.poestats.database;

import com.poestats.Config;
import com.poestats.Main;
import com.poestats.pricer.entries.RawEntry;
import com.poestats.pricer.maps.RawList;
import com.poestats.relations.entries.SupIndexedItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private SupIndexedItem supIndexedItem;
    private String index, sup, sub, time;
    private int count, quantity, inc, dec;
    private double mean, median, mode, exalted;

    private List<DatabaseEntry> databaseEntryList = new ArrayList<>();
    private List<DatabaseEntry> databaseEntryListToRemove = new ArrayList<>();
    private List<DatabaseEntry> databaseEntryListToAdd = new ArrayList<>();

    public DatabaseItem(String sup, String sub) {
        this.sup = sup;
        this.sub = sub;
        this.index = sup + sub;
    }

    //------------------------------------------------------------------------------------------------------------
    // I/O
    //------------------------------------------------------------------------------------------------------------

    public void loadItem(ResultSet result) throws SQLException {
        time = result.getString("time");
        mean = result.getDouble("mean");
        median = result.getDouble("median");
        mode = result.getDouble("mode");
        exalted = result.getDouble("exalted");
        count = result.getInt("count");
        quantity = result.getInt("quantity");
        inc = result.getInt("inc");
        dec = result.getInt("dec");
    }

    public void loadEntries(ResultSet result) throws SQLException {
        while (result.next()) {
            DatabaseEntry databaseEntry = new DatabaseEntry();
            databaseEntry.loadFromDatabase(result);
            databaseEntryList.add(databaseEntry);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void processRaw(RawList rawList) {
        supIndexedItem = Main.RELATIONS.indexToGenericData(index);
        if (supIndexedItem == null) {
            Main.ADMIN.log_("Data null for: " + index, 3);
            return;
        }


        for (RawEntry rawEntry : rawList) {
            if ( discardDuplicate(rawEntry) ) continue;

            inc++;

            if ( checkEntryPrice(rawEntry) ) {
                DatabaseEntry databaseEntry = new DatabaseEntry();
                databaseEntry.loadFromRaw(rawEntry);
                databaseEntryList.add(databaseEntry);
                databaseEntryListToAdd.add(databaseEntry);
            } else {
                dec++;
            }
        }
    }

    public void removeOutliers() {
        if (databaseEntryList.isEmpty()) return;
        if (mean == 0) return;

        for (DatabaseEntry databaseEntry : databaseEntryList) {
            double percent = databaseEntry.getPrice() / mean * 100;

            if (databaseEntry.getPrice() <= 0) {
                databaseEntryListToRemove.add(databaseEntry);
            } else if (mean < 2) {
                if (percent > Config.entry_pluckPercentLT2) databaseEntryListToRemove.add(databaseEntry);
            } else {
                if (percent > Config.entry_pluckPercentGT2) databaseEntryListToRemove.add(databaseEntry);
            }
        }

        for (DatabaseEntry databaseEntry : databaseEntryListToRemove) {
            databaseEntryList.remove(databaseEntry);
            dec++;
        }
    }

    public void calculate() {
        List<Double> entryPrices = sortItemPrices();

        mean = findMean(entryPrices);
        median = findMedian(entryPrices);
        mode = findMode(entryPrices);
    }

    //------------------------------------------------------------------------------------------------------------
    // Internal workings
    //------------------------------------------------------------------------------------------------------------

    private boolean checkEntryPrice(RawEntry entry) {
        if (databaseEntryList.isEmpty()) return true;
        if (mean == 0) return true;

        double percent = entry.getPrice() / mean * 100;

        switch (supIndexedItem.getParent()) {
            case "enchantments":
                return percent < 140;
            case "currency":
                return percent < 200;
            case "essence":
                return percent < 120;
            default:
                return percent < 200;
        }
    }

    private boolean discardDuplicate(RawEntry rawEntry) {
        for (DatabaseEntry databaseEntry : databaseEntryList) {
            if (databaseEntry.getId().equals(rawEntry.getId())) {
                return true;
            } else if (databaseEntry.getAccount().equals(rawEntry.getAccount())) {
                return true;
            }
        }

        return false;
    }

    private List<Double> sortItemPrices() {
        List<Double> tempList = new ArrayList<>();
        for (DatabaseEntry entry : databaseEntryList) tempList.add(entry.getPrice());
        Collections.sort(tempList);
        return tempList;

        /*if (databaseEntryList.size() > Config.entry_itemsSize * 3 / 4) {
            if (count < 20) {
                int startIndex = databaseEntryList.size() * Config.entry_shiftPercent / 100;
                return tempList.subList(0, startIndex);
            } else {
                int endIndex = databaseEntryList.size() * Config.entry_shiftPercentNew / 100;
                int startIndex = (tempList.size() - endIndex) / 2;
                return tempList.subList(startIndex, endIndex);
            }
        } else {
            return tempList;
        }*/
    }

    private double findMean(List<Double> sortedItemPrices) {
        if (sortedItemPrices.isEmpty()) return 0;

        double mean = 0.0;
        for (Double entry : sortedItemPrices) mean += entry;

        return Math.round(mean / sortedItemPrices.size() * Config.item_pricePrecision) / Config.item_pricePrecision;
    }

    private double findMedian(List<Double> sortedItemPrices) {
        if (sortedItemPrices.isEmpty()) return 0;

        int medianIndex = sortedItemPrices.size() / 2;
        return Math.round(sortedItemPrices.get(medianIndex) * Config.item_pricePrecision) / Config.item_pricePrecision;
    }

    private double findMode(List<Double> sortedItemPrices) {
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

    //------------------------------------------------------------------------------------------------------------
    // Getters
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

    public double getExalted() {
        return exalted;
    }

    public int getCount() {
        return count;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getInc() {
        return inc;
    }

    public int getDec() {
        return dec;
    }

    public String getSup() {
        return sup;
    }

    public String getSub() {
        return sub;
    }

    public List<DatabaseEntry> getDatabaseEntryListToRemove() {
        return databaseEntryListToRemove;
    }

    public List<DatabaseEntry> getDatabaseEntryListToAdd() {
        return databaseEntryListToAdd;
    }
}
