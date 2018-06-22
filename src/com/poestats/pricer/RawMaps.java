package com.poestats.pricer;

import com.poestats.Config;
import com.poestats.Item;

import java.util.HashMap;
import java.util.Map;

public class RawMaps {
    /**
     * League map. Has mappings of: [league name - id map]
     */
    public static class Le2Id2Ac2Raw extends HashMap<String, Id2Ac2Raw> {

    }

    /**
     * Id map. Has mappings of: [id - account map]
     */
    public static class Id2Ac2Raw extends HashMap<Integer, Ac2Raw> {

    }

    /**
     * Account map. Has mappings of: [account name - RawEntry]
     */
    public static class Ac2Raw extends HashMap<String, RawEntry> {

    }

    /**
     * The default format that new entries are stored as before uploading to database
     */
    public static class RawEntry {
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
}
