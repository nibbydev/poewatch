package com.poestats.parcel;

import java.util.ArrayList;
import java.util.HashMap;

public class JSONMaps {
    // Category map. Has mappings of: [category id - item map]
    public static class JSONCategoryMap extends HashMap<String, JSONItemList> {

    }

    // Index map. Has list of: [JSONItem]
    public static class JSONItemList extends ArrayList<JSONItem> {

    }

    // league map. Has mappings of: [league id - category map]
    public static class JSONLeagueMap extends HashMap<String, JSONCategoryMap> {

    }
}
