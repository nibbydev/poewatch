package com.poestats.relations.entries;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Misc;
import com.poestats.relations.RelationManager;

import java.util.Map;
import java.util.TreeMap;

public class IndexedItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Map<String, SubIndexedItem> subIndexes = new TreeMap<>();
    private String name, type, parent, child, tier, genericKey, icon;
    private int frame;

    //------------------------------------------------------------------------------------------------------------
    // Constructor
    //------------------------------------------------------------------------------------------------------------

    public IndexedItem(Item item) {
        if (item.frameType != -1) name = item.name;
        parent = item.getParentCategory();
        frame = item.frameType;

        genericKey = RelationManager.resolveSpecificKey(item.getKey());

        if (item.icon != null) icon = Misc.formatIconURL(item.icon);
        if (item.typeLine != null) type = item.typeLine;
        if (item.getChildCategory() != null) child = item.getChildCategory();
        if (item.getTier() != null) tier = item.getTier();
    }

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public String subIndex(Item item) {
        String subIndex = Integer.toHexString(subIndexes.size());
        subIndex = (Config.index_subBase + subIndex).substring(subIndex.length());

        SubIndexedItem subIndexedItem = new SubIndexedItem(item);
        subIndexes.put(subIndex, subIndexedItem);

        return subIndex;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getTier() {
        return tier;
    }

    public int getFrame() {
        return frame;
    }

    public Map<String, SubIndexedItem> getSubIndexes() {
        return subIndexes;
    }

    public String getChild() {
        return child;
    }

    public String getGenericKey() {
        return genericKey;
    }

    public String getIcon() {
        return icon;
    }

    public String getParent() {
        return parent;
    }

    public String getType() {
        return type;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setChild(String child) {
        this.child = child;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public void setGenericKey(String genericKey) {
        this.genericKey = genericKey;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setSubIndexes(Map<String, SubIndexedItem> subIndexes) {
        this.subIndexes = subIndexes;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public void setType(String type) {
        this.type = type;
    }
}
