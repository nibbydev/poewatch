package com.poestats.parcel;

public class HistoryItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private double[] spark;
    private double[] mean;
    private double[] median;
    private double[] mode;
    private int[] quantity;
    private double change;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    HistoryItem(int size) {
        spark       = new double[size];
        mean        = new double[size];
        median      = new double[size];
        mode        = new double[size];
        quantity    = new int[size];
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public double getChange() {
        return change;
    }

    public double[] getMean() {
        return mean;
    }

    public double[] getMedian() {
        return median;
    }

    public double[] getMode() {
        return mode;
    }

    public double[] getSpark() {
        return spark;
    }

    public int[] getQuantity() {
        return quantity;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public void setMean(double[] mean) {
        this.mean = mean;
    }

    public void setMedian(double[] median) {
        this.median = median;
    }

    public void setMode(double[] mode) {
        this.mode = mode;
    }

    public void setQuantity(int[] quantity) {
        this.quantity = quantity;
    }

    public void setSpark(double[] spark) {
        this.spark = spark;
    }
}


