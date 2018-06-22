package com.poestats.pricer.entries;

import com.poestats.Config;
import com.poestats.Item;

import java.util.Map;

public class RawEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String priceType, id;
    private double price;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void load(Item item) {
        priceType = item.getPriceType();
        price = item.getPrice();
        id = item.getId();
    }

    public boolean convertPrice(Map<String, Double> currencyMap) {
        if (!priceType.equals("Chaos Orb")) {
            if (currencyMap == null) return true;
            Double chaosValue = currencyMap.get(priceType);

            if (chaosValue == null) return true;

            price = Math.round(price * chaosValue * Config.item_pricePrecision) / Config.item_pricePrecision;
            priceType = "Chaos Orb";
        }

        return price < 0.0001 || price > 120000;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getItemId() {
        return id;
    }

    public String getPriceAsRoundedString() {
        return String.format("%."+ Config.item_pricePrecision2 +"f", price);
    }
}
