package ovh.poe.Pricer;

import ovh.poe.Main;
import ovh.poe.RelationManager.IndexedItem;
import ovh.poe.RelationManager.SubIndexedItem;

import java.util.*;

public class JSONParcel {
    private static class HistoryItem {
        public List<Double> spark = new ArrayList<>();
        public List<Double> mean = new ArrayList<>();
        public List<Double> median = new ArrayList<>();
        public List<Double> mode = new ArrayList<>();
        public List<Integer> quantity = new ArrayList<>();
        public double change;
    }

    public static class JSONItem {
        public double mean, median, mode, exalted;
        public int count, quantity, frame;
        public String index;
        public String corrupted, lvl, quality, links;
        public String key, parent, child, name, type, var, tier, icon;
        public HistoryItem history = new HistoryItem();

        public void copy (Entry entry) {
            mean = entry.getMean();
            median = entry.getMedian();
            mode = entry.getMode();
            count = entry.getCount() + entry.getInc_counter();
            quantity = entry.getQuantity();
            index = entry.getIndex();

            // Find the item's price in exalted
            EntryController.IndexMap tmp_currencyMap = Main.ENTRY_CONTROLLER.getCurrencyMap(entry.getLeague());
            if (tmp_currencyMap != null) {
                String exaltedIndex = Main.RELATIONS.currencyNameToFullIndex.getOrDefault("Exalted Orb", null);
                Entry tmp_exaltedEntry = tmp_currencyMap.getOrDefault(exaltedIndex, null);

                if (tmp_exaltedEntry != null) {
                    double tmp_exaltedPrice = tmp_exaltedEntry.getMean();

                    // If the currency the item was listed in has very few listings then ignore this item
                    if (tmp_exaltedEntry.getCount() > 20 && tmp_exaltedPrice > 0) {
                        double tempExaltedMean = mean / tmp_exaltedPrice;
                        exalted = Math.round(tempExaltedMean * Main.CONFIG.pricePrecision) / Main.CONFIG.pricePrecision;
                    }
                }
            }

            // Copy the history over
            if (entry.getDb_daily().size() > 0) {
                double lowestSpark = 99999;

                for (Entry.DailyEntry dailyEntry : entry.getDb_daily()) {
                    // Add all values to history
                    history.mean.add(dailyEntry.mean);
                    history.median.add(dailyEntry.median);
                    history.mode.add(dailyEntry.mode);
                    history.quantity.add(dailyEntry.quantity);

                    // Find the lowest mean entry for sparkline
                    if (dailyEntry.mean < lowestSpark) lowestSpark = dailyEntry.mean;
                }

                // Add current mean/median/mode values to history (but not quantity as that's the mean quantity)
                if (Main.CONFIG.addCurrenctPricesToHistory) {
                    history.mean.add(mean);
                    history.median.add(median);
                    history.mode.add(mode);
                    history.quantity.add(quantity);
                    // Remove excess elements
                    if (history.mean.size() > 7) history.mean.subList(0, history.mean.size() - 7).clear();
                    if (history.median.size() > 7) history.median.subList(0, history.median.size() - 7).clear();
                    if (history.mode.size() > 7) history.mode.subList(0, history.mode.size() - 7).clear();
                    if (history.quantity.size() > 7) history.quantity.subList(0, history.quantity.size() - 7).clear();
                    // Again, find the lowest mean entry for sparkline
                    if (mean < lowestSpark) lowestSpark = mean;
                }

                // Get the absolute value of lowestSpark as the JS sparkline plugin can't handle negative values
                if (lowestSpark < 0) lowestSpark *= -1;

                // Get variation from lowest value
                for (Entry.DailyEntry dailyEntry : entry.getDb_daily()) {
                    double newSpark = lowestSpark != 0 ? dailyEntry.mean / lowestSpark - 1 : 0.0;
                    newSpark = Math.round(newSpark * 10000.0) / 100.0;
                    history.spark.add(newSpark);
                }

                // Set change
                if (history.spark.size() > 0) {
                    history.change = history.spark.get(history.spark.size() - 1) - history.spark.get(0);
                    history.change = Math.round(history.change * 100.0) / 100.0;
                }
            }

            // Check if there's a match for the specific index
            String superIndex = index.substring(0, index.indexOf("-"));
            if (Main.RELATIONS.itemSubIndexToData.containsKey(superIndex)) {
                IndexedItem indexedItem = Main.RELATIONS.itemSubIndexToData.get(superIndex);
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

                // Enchantments override the name here
                if (subIndexedItem.name != null) name = subIndexedItem.name;
            }
        }
    }

    public Map<String, Map<String, List<JSONItem>>> leagues = new HashMap<>();

    public void add(Entry entry) {
        if (entry.getIndex() == null) return;

        IndexedItem indexedItem = Main.RELATIONS.genericIndexToData(entry.getIndex());
        if (indexedItem == null) return;

        Map<String, List<JSONItem>> league = leagues.getOrDefault(entry.getLeague(), new TreeMap<>());
        List<JSONItem> category = league.getOrDefault(indexedItem.parent, new ArrayList<>());

        JSONItem jsonItem = new JSONItem();
        jsonItem.copy(entry);

        category.add(jsonItem);
        league.putIfAbsent(indexedItem.parent, category);
        leagues.putIfAbsent(entry.getLeague(), league);
    }

    public void sort() {
        for (String leagueKey : leagues.keySet()) {
            Map<String, List<JSONItem>> league = leagues.get(leagueKey);

            for (String categoryKey : league.keySet()) {
                List<JSONItem> category = league.get(categoryKey);
                List<JSONItem> sortedCategory = new ArrayList<>();

                while (!category.isEmpty()) {
                    JSONItem mostExpensiveItem = null;

                    for (JSONItem item : category) {
                        if (mostExpensiveItem == null)
                            mostExpensiveItem = item;
                        else if (item.mean > mostExpensiveItem.mean)
                            mostExpensiveItem = item;
                    }

                    category.remove(mostExpensiveItem);
                    sortedCategory.add(mostExpensiveItem);
                }

                // Write sortedCategory to league map
                league.put(categoryKey, sortedCategory);
            }
        }
    }

    public void clear () {
        leagues.clear();
    }
}
