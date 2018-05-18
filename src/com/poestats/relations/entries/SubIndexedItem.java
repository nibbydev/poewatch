package com.poestats.relations.entries;

import com.poestats.Item;
import com.poestats.relations.RelationManager;

public class SubIndexedItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String var, specificKey, lvl, quality, links, corrupted, name;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    public SubIndexedItem (Item item) {
        specificKey = item.getKey();

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

    public String getSpecificKey() {
        return specificKey;
    }

    public String getVar() {
        return var;
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

    public void setSpecificKey(String specificKey) {
        this.specificKey = specificKey;
    }

    public void setVar(String var) {
        this.var = var;
    }
}
