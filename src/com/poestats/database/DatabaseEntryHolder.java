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

public class DatabaseEntryHolder {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private int count, quantity, inc, dec;
    private double mean, median, mode, exalted;

    private List<Double> prices = new ArrayList<>();

    //------------------------------------------------------------------------------------------------------------
    // I/O
    //------------------------------------------------------------------------------------------------------------

    public void loadEntries(ResultSet result) throws SQLException {
        while (result.next()) prices.add(result.getDouble("price"));
    }

    public void loadItem(ResultSet result) throws SQLException {
        mean = result.getDouble("mean");
        median = result.getDouble("median");
        mode = result.getDouble("mode");
        exalted = result.getDouble("exalted");
        count = result.getInt("count");
        quantity = result.getInt("quantity");
        inc = result.getInt("inc");
        dec = result.getInt("dec");
    }

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void calculate() {
        prices = sortItemPrices();

        if (prices.isEmpty()) {
            mean = median = mode = 0;
        } else {
            mean = findMean();
            median = findMedian();
            mode = findMode();
        }
    }

    public void incCounters(int count) {
        this.count += count;
        inc += count;
    }

    //------------------------------------------------------------------------------------------------------------
    // Internal workings
    //------------------------------------------------------------------------------------------------------------

    private List<Double> sortItemPrices() {
        Collections.sort(prices);

        if (prices.size() > Config.entry_itemsSize * 3 / 4) {
            if (count < 20) {
                int startIndex = prices.size() * Config.entry_shiftPercent / 100;
                return prices.subList(0, startIndex);
            } else {
                int endIndex = prices.size() * Config.entry_shiftPercentNew / 100;
                int startIndex = (prices.size() - endIndex) / 2;
                return prices.subList(startIndex, endIndex);
            }
        } else {
            return prices;
        }
    }

    private double findMean() {
        double mean = 0.0;
        for (Double entry : prices) mean += entry;

        return Math.round(mean / prices.size() * Config.item_pricePrecision) / Config.item_pricePrecision;
    }

    private double findMedian() {
        int medianIndex = prices.size() / 2;
        return Math.round(prices.get(medianIndex) * Config.item_pricePrecision) / Config.item_pricePrecision;
    }

    private double findMode() {
        double maxValue = 0, maxCount = 0;

        for (Double entry1 : prices) {
            int count = 0;

            for (Double entry2 : prices) {
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
}
