package com.poestats.relations.entries;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.relations.RelationManager;

import java.util.HashMap;
import java.util.Map;

public class SupIndexedItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Map<String, SubIndexedItem> subIndexes = new HashMap<>();
    private String name, type, parent, child, key;
    private int frame;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    public SupIndexedItem() {

    }

    public SupIndexedItem(Item item) {
        name = item.name;
        parent = item.getParentCategory();
        frame = item.frameType;
        key = RelationManager.resolveSpecificKey(item.getKey());

        if (item.typeLine != null) type = item.typeLine;
        if (item.getChildCategory() != null) child = item.getChildCategory();
    }

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public String subIndex(Item item, String sup) {
        String sub = Integer.toHexString(subIndexes.size());
        sub = (Config.index_subBase + sub).substring(sub.length());

        SubIndexedItem subIndexedItem = new SubIndexedItem(item, this);
        subIndexes.put(sup + sub, subIndexedItem);

        return sub;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
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

    public String getParent() {
        return parent;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
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

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
