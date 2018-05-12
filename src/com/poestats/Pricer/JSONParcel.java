package com.poestats.Pricer;

import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.RelationManager.IndexedItem;
import com.poestats.RelationManager.SubIndexedItem;

import java.util.*;

public class JSONParcel {
    // League map. Has mappings of: [league id - category map]
    static class JSONLeagueMap extends HashMap<String, JSONCategoryMap> { }
    // Category map. Has mappings of: [category id - item map]
    static class JSONCategoryMap extends HashMap<String, JSONItemList> { }
    // Index map. Has list of: [JSONItem]
    static class JSONItemList extends ArrayList<JSONItem> { }

    private static class HistoryItem {
        private double[] spark;
        private double[] mean;
        private double[] median;
        private double[] mode;
        private int[] quantity;
        private double change;

        private HistoryItem(int size) {
            spark       = new double[size];
            mean        = new double[size];
            median      = new double[size];
            mode        = new double[size];
            quantity    = new int[size];
        }
    }

    private static class JSONItem {
        public double mean, median, mode, exalted;
        public int count, quantity, frame;
        public String index;
        public String corrupted, lvl, quality, links;
        public String key, parent, child, name, type, var, tier, icon;
        public HistoryItem history;

        public void copy (Entry entry) {
            mean = entry.getMean();
            median = entry.getMedian();
            mode = entry.getMode();
            count = entry.getCount() + entry.getInc();
            quantity = entry.getQuantity();
            index = entry.getIndex();

            // Find the item's price in exalted
            EntryController.IndexMap tmp_currencyMap = Main.ENTRY_CONTROLLER.getCurrencyMap(entry.getLeague());
            if (tmp_currencyMap != null) {
                String exaltedIndex = Main.RELATIONS.getCurrencyNameToFullIndex().getOrDefault("Exalted Orb", null);
                Entry tmp_exaltedEntry = tmp_currencyMap.getOrDefault(exaltedIndex, null);

                if (tmp_exaltedEntry != null) {
                    double tmp_exaltedPrice = tmp_exaltedEntry.getMean();

                    // If the currency the item was listed in has very few listings then ignore this item
                    if (tmp_exaltedPrice > 0) {
                        double tempExaltedMean = mean / tmp_exaltedPrice;
                        exalted = Math.round(tempExaltedMean * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;
                    }
                }
            }

            int dbDailySize = entry.getDb_daily().size();

            if (dbDailySize < 7) history = new HistoryItem(dbDailySize + 1);
            else history = new HistoryItem(dbDailySize);

            double lowestSpark = 99999;
            List<Entry.DailyEntry> dailyEntries = entry.getDb_daily();

            for (int i = 0; i < dbDailySize; i++) {
                Entry.DailyEntry dailyEntry = dailyEntries.get(i);

                // Add all values to history
                history.mean[i]     = dailyEntry.getMean();
                history.median[i]   = dailyEntry.getMedian();
                history.mode[i]     = dailyEntry.getMode();
                history.quantity[i] = dailyEntry.getQuantity();

                // Find the lowest mean entry for sparkline
                if (lowestSpark > dailyEntry.getMean()) lowestSpark = dailyEntry.getMean();
            }

            // Add current mean/median/mode values to history (but not quantity as that's the mean quantity)
            if (dbDailySize < 7) {
                history.mean[dbDailySize]     = mean;
                history.median[dbDailySize]   = median;
                history.mode[dbDailySize]     = mode;
                history.quantity[dbDailySize] = quantity;
            } else {
                Misc.shiftArrayLeft(history.mean,       1);
                Misc.shiftArrayLeft(history.median,     1);
                Misc.shiftArrayLeft(history.mode,       1);
                Misc.shiftArrayLeft(history.quantity,   1);

                history.mean[dbDailySize - 1]     = mean;
                history.median[dbDailySize - 1]   = median;
                history.mode[dbDailySize - 1]     = mode;
                history.quantity[dbDailySize - 1] = quantity;
            }

            // TODO: change order so this wouldn't be required
            // Again, find the lowest mean entry for sparkline
            if (mean < lowestSpark) lowestSpark = mean;

            // Get the absolute value of lowestSpark as the JS sparkline plugin can't handle negative values
            if (lowestSpark < 0) lowestSpark *= -1;

            // Get variation from lowest value
            for (int i = 0; i < dbDailySize; i++) {
                Entry.DailyEntry dailyEntry = dailyEntries.get(i);
                double newSpark = lowestSpark == 0 ? 0.0 : dailyEntry.getMean() / lowestSpark - 1;
                history.spark[i] = Math.round(newSpark * 10000.0) / 100.0;
            }

            // Add current mean values to history sparkline
            if (dbDailySize < 7) {
                double newSpark = lowestSpark == 0 ? 0.0 : mean / lowestSpark - 1;
                history.spark[dbDailySize] = Math.round(newSpark * 10000.0) / 100.0;
            } else {
                Misc.shiftArrayLeft(history.spark, 1);
                double newSpark = lowestSpark == 0 ? 0.0 : mean / lowestSpark - 1;
                history.spark[dbDailySize - 1] = Math.round(newSpark * 10000.0) / 100.0;
            }

            // Set change
            if (history.spark.length > 1) {
                if (dbDailySize < 7) {
                    history.change = Math.round( (history.spark[dbDailySize] - history.spark[0]) * 100.0) / 100.0;
                } else {
                    history.change = Math.round( (history.spark[dbDailySize - 1] - history.spark[0]) * 100.0) / 100.0;
                }
            }

            // Check if there's a match for the specific index
            String superIndex = index.substring(0, index.indexOf("-"));
            if (Main.RELATIONS.getItemSubIndexToData().containsKey(superIndex)) {
                IndexedItem indexedItem = Main.RELATIONS.getItemSubIndexToData().get(superIndex);
                frame = indexedItem.frame;
                key = indexedItem.genericKey;
                parent = indexedItem.parent;
                child = indexedItem.child;
                icon = indexedItem.icon;
                name = indexedItem.name;
                type = indexedItem.type;
                tier = indexedItem.tier;

                String subIndex = index.substring(index.indexOf("-") + 1);
                SubIndexedItem subIndexedItem = indexedItem.subIndexes.get(subIndex);

                if (subIndexedItem.corrupted != null) corrupted = subIndexedItem.corrupted.equals("true") ? "1" : "0";
                if (subIndexedItem.quality != null) quality = subIndexedItem.quality;
                if (subIndexedItem.links != null) links = subIndexedItem.links;
                if (subIndexedItem.lvl != null) lvl = subIndexedItem.lvl;
                if (subIndexedItem.var != null) var = subIndexedItem.var;

                // Enchantments override the id here
                if (subIndexedItem.name != null) name = subIndexedItem.name;
            }
        }
    }

    private JSONLeagueMap jsonLeagueMap = new JSONLeagueMap();

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    public void add(Entry entry) {
        if (entry.getIndex() == null) return;
        IndexedItem indexedItem = Main.RELATIONS.indexToGenericData(entry.getIndex());
        if (indexedItem == null) return;

        JSONCategoryMap jsonCategoryMap = jsonLeagueMap.getOrDefault(entry.getLeague(), new JSONCategoryMap());
        JSONItemList jsonItems = jsonCategoryMap.getOrDefault(indexedItem.parent, new JSONItemList());

        JSONItem jsonItem = new JSONItem();
        jsonItem.copy(entry);

        jsonItems.add(jsonItem);
        jsonCategoryMap.putIfAbsent(indexedItem.parent, jsonItems);
        jsonLeagueMap.putIfAbsent(entry.getLeague(), jsonCategoryMap);
    }

    public void sort() {
        for (String league : jsonLeagueMap.keySet()) {
            JSONCategoryMap jsonCategoryMap = jsonLeagueMap.get(league);

            for (String category : jsonCategoryMap.keySet()) {
                JSONItemList jsonItems = jsonCategoryMap.get(category);
                JSONItemList jsonItems_sorted = new JSONItemList();

                while (!jsonItems.isEmpty()) {
                    JSONItem mostExpensiveItem = null;

                    for (JSONItem item : jsonItems) {
                        if (mostExpensiveItem == null) {
                            mostExpensiveItem = item;
                        } else if (item.mean > mostExpensiveItem.mean) {
                            mostExpensiveItem = item;
                        }
                    }

                    jsonItems.remove(mostExpensiveItem);
                    jsonItems_sorted.add(mostExpensiveItem);
                }

                // Write jsonItems_sorted to category map
                jsonCategoryMap.put(category, jsonItems_sorted);
            }
        }
    }

    public void clear () {
        jsonLeagueMap.clear();
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public JSONLeagueMap getJsonLeagueMap() {
        return jsonLeagueMap;
    }
}
