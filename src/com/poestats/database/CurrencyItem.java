package com.poestats.database;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CurrencyItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private int count, quantity, inc, dec;
    private double mean, median, mode, exalted;

    //------------------------------------------------------------------------------------------------------------
    // I/O
    //------------------------------------------------------------------------------------------------------------

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

    public void setMean(double mean) {
        this.mean = mean;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public void addCount(int val) {
        count += val;
    }
}
