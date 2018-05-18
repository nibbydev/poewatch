package com.poestats.parcel;

import com.poestats.Config;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.pricer.entries.DailyEntry;
import com.poestats.pricer.Entry;
import com.poestats.pricer.maps.*;
import com.poestats.relations.entries.IndexedItem;
import com.poestats.relations.entries.SubIndexedItem;

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

            // Find the lowest mean entries for sparkline
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
        // Again, find the lowest mean entries for sparkline
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
            IndexedItem indexedItem = Main.RELATIONS.getItemSubIndexToData().get(superIndex);
            frame = indexedItem.getFrame();
            key = indexedItem.getGenericKey();
            parent = indexedItem.getParent();
            child = indexedItem.getChild();
            icon = indexedItem.getIcon();
            name = indexedItem.getName();
            type = indexedItem.getType();
            tier = indexedItem.getTier();

            String subIndex = index.substring(index.indexOf("-") + 1);
            SubIndexedItem subIndexedItem = indexedItem.getSubIndexes().get(subIndex);

            if (subIndexedItem.getCorrupted() != null) corrupted = subIndexedItem.getCorrupted().equals("true") ? "1" : "0";
            if (subIndexedItem.getQuality() != null) quality = subIndexedItem.getQuality();
            if (subIndexedItem.getLinks() != null) links = subIndexedItem.getLinks();
            if (subIndexedItem.getLvl() != null) lvl = subIndexedItem.getLvl();
            if (subIndexedItem.getVar() != null) var = subIndexedItem.getVar();
            if (subIndexedItem.getSpecificKey() != null) key = subIndexedItem.getSpecificKey();

            // Enchantments override the id here
            if (subIndexedItem.getName() != null) name = subIndexedItem.getName();
        }
    }
}
