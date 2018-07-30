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

        private String priceType, accountName;
        private double price;
        private int id_l, id_d;

        //------------------------------------------------------------------------------------------------------------
        // Main methods
        //------------------------------------------------------------------------------------------------------------

        public void load(Item item) {
            priceType = item.getPriceType();
            price = item.getPrice();
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

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!RawEntry.class.isAssignableFrom(obj.getClass())) {
                return false;
            }

            final RawEntry other = (RawEntry) obj;

            if (this.accountName == null ? (other.accountName != null) : !this.accountName.equals(other.accountName)) {
                return false;
            }

            if (this.id_l != other.id_l) {
                return false;
            }

            if (this.id_d != other.id_d) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;

            hash = 53 * hash + (this.accountName != null ? this.accountName.hashCode() : 0);
            hash = 53 * hash + this.id_l;
            hash = 53 * hash + this.id_d;

            return hash;
        }

        //------------------------------------------------------------------------------------------------------------
        // Getters and Setters
        //------------------------------------------------------------------------------------------------------------

        public String getPriceAsRoundedString() {
            return String.format("%."+ Config.item_pricePrecision2 +"f", price);
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public void setLeagueId(int id) {
            this.id_l = id;
        }

        public void setItemId(int id) {
            this.id_d = id;
        }

        public int getLeagueId() {
            return id_l;
        }

        public int getItemId() {
            return id_d;
        }

        public String getAccountName() {
            return accountName;
        }
    }
}
