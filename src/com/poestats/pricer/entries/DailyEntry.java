package com.poestats.pricer.entries;

public class DailyEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private double mean, median, mode;
    private int quantity;
    private String raw;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void add(String raw) {
        // Eg "8.241,5.0,5.0,3283", mean median mode quantity respectively
        this.raw = raw;

        String[] splitRaw = raw.split(",");
        mean = Double.parseDouble(splitRaw[0]);
        median = Double.parseDouble(splitRaw[1]);
        mode = Double.parseDouble(splitRaw[2]);
        quantity = Integer.parseInt(splitRaw[3]);
    }

    public void add(double mean, double median, double mode, int quantity) {
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

    public String getRaw() {
        return raw;
    }
}

