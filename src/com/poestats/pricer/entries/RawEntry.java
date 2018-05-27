package com.poestats.pricer.entries;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.database.DatabaseItem;
import com.poestats.pricer.maps.CurrencyMap;

public class RawEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String account, priceType, id;
    private double price;
    private boolean discard;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void add (Item item, String accountName) {
        account = accountName;
        priceType = item.getPriceType();
        price = item.getPrice();
        id = item.id;
    }

    public boolean convertPrice(CurrencyMap currencyMap) {
        if (!priceType.equals("Chaos Orb")) {
            if (currencyMap == null) return true;
            DatabaseItem databaseItem = currencyMap.get(priceType);

            if (databaseItem == null) return true;
            else if (databaseItem.getCount() < 20) return true;

            price = Math.round(price * databaseItem.getMean() * Config.item_pricePrecision) / Config.item_pricePrecision;
            priceType = "Chaos Orb";
        }

        return price < 0.0001 || price > 120000;
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

    public String getPriceAsRoundedString() {
        return String.format("%."+ Config.item_pricePrecision2 +"f", price);
    }

    public String getAccount() {
        return account;
    }

    public String getPriceType() {
        return priceType;
    }

    public boolean isDiscard() {
        return discard;
    }
}
