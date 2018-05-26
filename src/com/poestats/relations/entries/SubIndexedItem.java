package com.poestats.relations.entries;

import com.poestats.Item;
import com.poestats.Misc;
import com.poestats.relations.RelationManager;

public class SubIndexedItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String var, key, lvl, quality, links, corrupted;
    private String icon, tier;
    private SupIndexedItem supIndexedItem;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    public SubIndexedItem() {

    }

    public SubIndexedItem (Item item, SupIndexedItem supItem) {
        key = item.getKey();
        supIndexedItem = supItem;

        icon = Misc.formatIconURL(item.icon);
        if (item.getTier() != null) tier = item.getTier();
        if (item.getVariation() != null) var = item.getVariation();

        if (item.getLinks() > 4) links = Integer.toString(item.getLinks());

        if (item.frameType == 4) {
            quality = Integer.toString(item.getQuality());
            lvl = Integer.toString(item.getLevel());
            corrupted = item.corrupted ? "1" : "0";
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getCorrupted() {
        return corrupted;
    }

    public String getLinks() {
        return links;
    }

    public String getLvl() {
        return lvl;
    }

    public String getQuality() {
        return quality;
    }

    public String getKey() {
        return key;
    }

    public String getVar() {
        return var;
    }

    public String getIcon() {
        return icon;
    }

    public String getTier() {
        return tier;
    }

    public SupIndexedItem getSupIndexedItem() {
        return supIndexedItem;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setCorrupted(String corrupted) {
        this.corrupted = corrupted;
    }

    public void setLinks(String links) {
        this.links = links;
    }

    public void setLvl(String lvl) {
        this.lvl = lvl;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public void setSupIndexedItem(SupIndexedItem supIndexedItem) {
        this.supIndexedItem = supIndexedItem;
    }
}
