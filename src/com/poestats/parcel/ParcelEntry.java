package com.poestats.parcel;

import com.poestats.Config;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.pricer.entries.DailyEntry;
import com.poestats.pricer.Entry;
import com.poestats.pricer.maps.*;
import com.poestats.relations.entries.SupIndexedItem;
import com.poestats.relations.entries.SubIndexedItem;

import java.util.List;

public class ParcelEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private double mean, median, mode, exalted;
    private int count, quantity, frame;
    private String index;
    private String corrupted, lvl, quality, links;
    private String key, parent, child, name, type, var, tier, icon;
    private HistoryItem history;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void copy (Entry entry) {
        mean = entry.getMean();
        median = entry.getMedian();
        mode = entry.getMode();
        count = entry.getCount() + entry.getInc();
        quantity = entry.getQuantity();
        index = entry.getIndex();

        // Find the item's price in exalted
        IndexMap tmp_currencyMap = Main.ENTRY_MANAGER.getCurrencyMap(entry.getLeague());
        if (tmp_currencyMap != null) {
            String exaltedIndex = Main.RELATIONS.getCurrencyNameToIndex().getOrDefault("Exalted Orb", null);
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
        if (Main.RELATIONS.getSupIndexToData().containsKey(superIndex)) {
            SupIndexedItem supIndexedItem = Main.RELATIONS.getSupIndexToData().get(superIndex);
            frame = supIndexedItem.getFrame();
            parent = supIndexedItem.getParent();
            child = supIndexedItem.getChild();
            name = supIndexedItem.getName();
            type = supIndexedItem.getType();

            String subIndex = index.substring(index.indexOf("-") + 1);
            SubIndexedItem subIndexedItem = supIndexedItem.getSubIndexes().get(subIndex);

            icon = subIndexedItem.getIcon();
            key = subIndexedItem.getKey();

            if (subIndexedItem.getCorrupted() != null) corrupted = subIndexedItem.getCorrupted();
            if (subIndexedItem.getQuality() != null) quality = subIndexedItem.getQuality();
            if (subIndexedItem.getLinks() != null) links = subIndexedItem.getLinks();
            if (subIndexedItem.getLvl() != null) lvl = subIndexedItem.getLvl();
            if (subIndexedItem.getVar() != null) var = subIndexedItem.getVar();
            if (subIndexedItem.getTier() != null) tier = subIndexedItem.getTier();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getVar() {
        return var;
    }

    public String getQuality() {
        return quality;
    }

    public String getLvl() {
        return lvl;
    }

    public String getLinks() {
        return links;
    }

    public String getCorrupted() {
        return corrupted;
    }

    public int getFrame() {
        return frame;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getParent() {
        return parent;
    }

    public String getIcon() {
        return icon;
    }

    public String getChild() {
        return child;
    }

    public String getTier() {
        return tier;
    }

    public String getKey() {
        return key;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getExalted() {
        return exalted;
    }

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public double getMode() {
        return mode;
    }

    public HistoryItem getHistory() {
        return history;
    }

    public int getCount() {
        return count;
    }

    public String getIndex() {
        return index;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setVar(String var) {
        this.var = var;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setLvl(String lvl) {
        this.lvl = lvl;
    }

    public void setLinks(String links) {
        this.links = links;
    }

    public void setCorrupted(String corrupted) {
        this.corrupted = corrupted;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public void setChild(String child) {
        this.child = child;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public void setMode(double mode) {
        this.mode = mode;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setExalted(double exalted) {
        this.exalted = exalted;
    }

    public void setHistory(HistoryItem history) {
        this.history = history;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
