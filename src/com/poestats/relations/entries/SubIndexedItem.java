package com.poestats.relations.entries;

import com.poestats.Item;
import com.poestats.Misc;
import com.poestats.relations.RelationManager;

public class SubIndexedItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String var, key, lvl, quality, links, corrupted, name;
    private String icon, tier;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    public SubIndexedItem () {

    }

    public SubIndexedItem (Item item) {
        key = item.getKey();

        if (item.icon != null) icon = Misc.formatIconURL(item.icon);
        if (item.getTier() != null) tier = item.getTier();

        if (item.getVariation() != null) {
            var = item.getVariation();

            if (item.frameType == -1) {
                name = RelationManager.resolveSpecificKey(item.getKey());

                // Replace all instances of "#" with the associated value
                for (String value : var.split("-")) {
                    name = name.replaceFirst("#", value);
                }
            }
        } else {
            if (item.frameType == -1) {
                name = RelationManager.resolveSpecificKey(item.getKey());
            }
        }

        if (item.getLinks() > 4) links = Integer.toString(item.getLinks());

        if (item.frameType == 4) {
            // Gson wants to serialize uninitialized integers and booleans
            quality = Integer.toString(item.getQuality());
            lvl = Integer.toString(item.getLevel());
            corrupted = Boolean.toString(item.corrupted);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

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

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }

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
}
