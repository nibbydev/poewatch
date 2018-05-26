package com.poestats.database;

import com.poestats.Config;
import com.poestats.pricer.entries.RawEntry;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String time, account, id;
    private double price;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void loadFromDatabase(ResultSet result) throws SQLException {
        time = result.getString("time");
        account = result.getString("account");
        id = result.getString("id");
        price = result.getDouble("price");
    }

    public void loadFromRaw(RawEntry rawEntry) {
        account = rawEntry.getAccount();
        id = rawEntry.getId();
        price = rawEntry.getPrice();
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getAccount() {
        return account;
    }

    public String getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }

    public String getPriceAsRoundedString() {
        return String.format("%."+ Config.item_pricePrecision2 +"f", price);
    }
}
