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
        account = result.getString("account");
        price = result.getDouble("price");
        time = result.getString("time");
        id = result.getString("id");
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
