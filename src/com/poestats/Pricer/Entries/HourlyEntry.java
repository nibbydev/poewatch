package com.poestats.Pricer.Entries;

public class HourlyEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private double mean, median, mode;
    private int inc, dec;
    private String raw;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void add (double mean, double median, double mode, int inc, int dec) {
        this.mean = mean;
        this.median = median;
        this.mode = mode;
        this.inc = inc;
        this.dec = dec;
    }

    public void add(String raw) {
        // Eg "8.241,5.0,5.0", mean median mode respectively
        this.raw = raw;

        String[] splitRaw = raw.split(",");
        mean = Double.parseDouble(splitRaw[0]);
        median = Double.parseDouble(splitRaw[1]);
        mode = Double.parseDouble(splitRaw[2]);

        inc = Integer.parseInt(splitRaw[3]);
        dec = Integer.parseInt(splitRaw[4]);
    }

    @Override
    public String toString() {
        if (raw == null) return mean + "," + median + "," + mode + "," + inc + "," + dec;
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

    public int getInc() {
        return inc;
    }

    public int getDec() {
        return dec;
    }

    public String getRaw() {
        return raw;
    }
}

