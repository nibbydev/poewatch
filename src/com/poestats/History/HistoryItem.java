package com.poestats.History;

public class HistoryItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private double[] mean;
    private double[] median;
    private double[] mode;
    private int[] quantity;
    private int[] count;

    //------------------------------------------------------------------------------------------------------------
    // Init class
    //------------------------------------------------------------------------------------------------------------

    public HistoryItem(int baseSize) {
        mean        = new double[baseSize];
        median      = new double[baseSize];
        mode        = new double[baseSize];
        quantity    = new int[baseSize];
        count       = new int[baseSize];
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public int[] getQuantity() {
        return quantity;
    }

    public double[] getMode() {
        return mode;
    }

    public double[] getMedian() {
        return median;
    }

    public double[] getMean() {
        return mean;
    }

    public int[] getCount() {
        return count;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------


    public void setQuantity(int[] quantity) {
        this.quantity = quantity;
    }

    public void setMode(double[] mode) {
        this.mode = mode;
    }

    public void setMedian(double[] median) {
        this.median = median;
    }

    public void setMean(double[] mean) {
        this.mean = mean;
    }

    public void setCount(int[] count) {
        this.count = count;
    }
}
