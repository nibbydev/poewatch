package com.poestats.Pricer.Entries;

public class ItemEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private double price;
    private String accountName, id, raw;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void add (String raw) {
        this.raw = raw;
        String[] splitRaw = raw.split(",");

        this.price = Double.parseDouble(splitRaw[0]);
        this.accountName = splitRaw[1];
        this.id = splitRaw[2];
    }

    public void add (double price, String accountName, String id) {
        this.price = price;
        this.accountName = accountName;
        this.id = id;
    }

    @Override
    public String toString() {
        if (raw == null) return price + "," + accountName + "," + id;
        else return raw;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getRaw() {
        return raw;
    }
}
