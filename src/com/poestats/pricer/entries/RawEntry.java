package com.poestats.pricer.entries;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.database.CurrencyItem;
import com.poestats.pricer.maps.CurrencyMap;

public class RawEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String account, priceType, id;
    private double price;

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
            CurrencyItem currencyItem = currencyMap.get(priceType);

            if (currencyItem == null) return true;
            else if (currencyItem.getCount() < 20) return true;

            price = Math.round(price * currencyItem.getMean() * Config.item_pricePrecision) / Config.item_pricePrecision;
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

    public String getPriceAsRoundedString() {
        return String.format("%."+ Config.item_pricePrecision2 +"f", price);
    }

    public String getAccount() {
        return account;
    }
}
