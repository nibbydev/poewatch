package com.poestats.Pricer.Entries;

public class TenMinuteEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private double mean, median, mode;
    private String raw;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void add (double mean, double median, double mode) {
        this.mean = mean;
        this.median = median;
        this.mode = mode;
    }

    public void add(String raw) {
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

    public String getRaw() {
        return raw;
    }
}

