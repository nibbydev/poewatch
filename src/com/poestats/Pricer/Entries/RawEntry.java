package com.poestats.Pricer.Entries;

import com.poestats.Item;

public class RawEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String accountName, priceType, id;
    private double price;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void add (Item item, String accountName) {
        this.price = item.getPrice();
        this.id = item.id;
        this.accountName = accountName;
        this.priceType = item.getPriceType();
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
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

    public String getPriceType() {
        return priceType;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }
}
