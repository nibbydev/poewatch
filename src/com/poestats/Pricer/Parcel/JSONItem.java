package com.poestats.Pricer.Parcel;

import com.poestats.Config;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.Pricer.Entries.DailyEntry;
import com.poestats.Pricer.Entry;
import com.poestats.Pricer.Maps.*;
import com.poestats.RelationManager;

import java.util.List;

public class JSONItem {
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
        IndexMap tmp_currencyMap = Main.ENTRY_CONTROLLER.getCurrencyMap(entry.getLeague());
        if (tmp_currencyMap != null) {
            String exaltedIndex = Main.RELATIONS.getCurrencyNameToFullIndex().getOrDefault("Exalted Orb", null);
            Entry tmp_exaltedEntry = tmp_currencyMap.getOrDefault(exaltedIndex, null);

            if (tmp_exaltedEntry != null) {
                double tmp_exaltedPrice = tmp_exaltedEntry.getMean();

                // If the currency the item was listed in has very few listings then ignore this item
                if (tmp_exaltedPrice > 0) {
                    double tempExaltedMean = mean / tmp_exaltedPrice;
                    exalted = Math.round(tempExaltedMean * Config.item_pricePrecision) / Config.item_pricePrecision;
                }
            }
        }

        int dbDailySize = entry.getDb_daily().size();

        if (dbDailySize < 7) history = new HistoryItem(dbDailySize + 1);
        else history = new HistoryItem(dbDailySize);

        double lowestSpark = 99999;
        List<DailyEntry> dailyEntries = entry.getDb_daily();

        for (int i = 0; i < dbDailySize; i++) {
            DailyEntry dailyEntry = dailyEntries.get(i);

            // Add all values to history
            history.getMean()[i]     = dailyEntry.getMean();
            history.getMedian()[i]   = dailyEntry.getMedian();
            history.getMode()[i]     = dailyEntry.getMode();
            history.getQuantity()[i] = dailyEntry.getQuantity();

            // Find the lowest mean entry for sparkline
            if (lowestSpark > dailyEntry.getMean()) lowestSpark = dailyEntry.getMean();
        }

        // Add current mean/median/mode values to history (but not quantity as that's the mean quantity)
        if (dbDailySize < 7) {
            history.getMean()[dbDailySize]     = mean;
            history.getMedian()[dbDailySize]   = median;
            history.getMode()[dbDailySize]     = mode;
            history.getQuantity()[dbDailySize] = quantity;
        } else {
            Misc.shiftArrayLeft(history.getMean(),       1);
            Misc.shiftArrayLeft(history.getMedian(),     1);
            Misc.shiftArrayLeft(history.getMode(),       1);
            Misc.shiftArrayLeft(history.getQuantity(),   1);

            history.getMean()[dbDailySize - 1]     = mean;
            history.getMedian()[dbDailySize - 1]   = median;
            history.getMode()[dbDailySize - 1]     = mode;
            history.getQuantity()[dbDailySize - 1] = quantity;
        }

        // TODO: change order so this wouldn't be required
        // Again, find the lowest mean entry for sparkline
        if (mean < lowestSpark) lowestSpark = mean;

        // Get the absolute value of lowestSpark as the JS sparkline plugin can't handle negative values
        if (lowestSpark < 0) lowestSpark *= -1;

        // Get variation from lowest value
        for (int i = 0; i < dbDailySize; i++) {
            DailyEntry dailyEntry = dailyEntries.get(i);
            double newSpark = lowestSpark == 0 ? 0.0 : dailyEntry.getMean() / lowestSpark - 1;
            history.getSpark()[i] = Math.round(newSpark * 10000.0) / 100.0;
        }

        // Add current mean values to history sparkline
        if (dbDailySize < 7) {
            double newSpark = lowestSpark == 0 ? 0.0 : mean / lowestSpark - 1;
            history.getSpark()[dbDailySize] = Math.round(newSpark * 10000.0) / 100.0;
        } else {
            Misc.shiftArrayLeft(history.getSpark(), 1);
            double newSpark = lowestSpark == 0 ? 0.0 : mean / lowestSpark - 1;
            history.getSpark()[dbDailySize - 1] = Math.round(newSpark * 10000.0) / 100.0;
        }

        // Set change
        if (history.getSpark().length > 1) {
            if (dbDailySize < 7) {
                history.setChange(Math.round( (history.getSpark()[dbDailySize] - history.getSpark()[0]) * 100.0) / 100.0);
            } else {
                history.setChange(Math.round( (history.getSpark()[dbDailySize - 1] - history.getSpark()[0]) * 100.0) / 100.0);
            }
        }

        // Check if there's a match for the specific index
        String superIndex = index.substring(0, index.indexOf("-"));
        if (Main.RELATIONS.getItemSubIndexToData().containsKey(superIndex)) {
            RelationManager.IndexedItem indexedItem = Main.RELATIONS.getItemSubIndexToData().get(superIndex);
            frame = indexedItem.frame;
            key = indexedItem.genericKey;
            parent = indexedItem.parent;
            child = indexedItem.child;
            icon = indexedItem.icon;
            name = indexedItem.name;
            type = indexedItem.type;
            tier = indexedItem.tier;

            String subIndex = index.substring(index.indexOf("-") + 1);
            RelationManager.SubIndexedItem subIndexedItem = indexedItem.subIndexes.get(subIndex);

            if (subIndexedItem.corrupted != null) corrupted = subIndexedItem.corrupted.equals("true") ? "1" : "0";
            if (subIndexedItem.quality != null) quality = subIndexedItem.quality;
            if (subIndexedItem.links != null) links = subIndexedItem.links;
            if (subIndexedItem.lvl != null) lvl = subIndexedItem.lvl;
            if (subIndexedItem.var != null) var = subIndexedItem.var;
            if (subIndexedItem.specificKey != null) key = subIndexedItem.specificKey;

            // Enchantments override the id here
            if (subIndexedItem.name != null) name = subIndexedItem.name;
        }
    }
}
