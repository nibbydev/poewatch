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

    public void convertPrice(CurrencyMap currencyMap) {
        if (priceType.equals("Chaos Orb")) return;

        if (currencyMap == null) {
            discard = true;
            return;
        }

        DatabaseItem databaseItem = currencyMap.get(priceType);

        if (databaseItem == null) {
            discard = true;
            return;
        } else if (databaseItem.getCount() < 20) {
            discard = true;
            return;
        }

        price = Math.round(price * databaseItem.getMean() * Config.item_pricePrecision) / Config.item_pricePrecision;
        priceType = "Chaos Orb";

        if (price < 0.0001) discard = true;
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
