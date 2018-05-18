package com.poestats.parcel;

import java.util.ArrayList;
import java.util.HashMap;

public class ParcelMaps {
    // Category map. Has mappings of: [category id - item map]
    public static class ParcelCategoryMap extends HashMap<String, ParcelItemList> {

    }

    // Index map. Has list of: [ParcelEntry]
    public static class ParcelItemList extends ArrayList<ParcelEntry> {

    }

    // league map. Has mappings of: [league id - category map]
    public static class ParcelLeagueMap extends HashMap<String, ParcelCategoryMap> {

    }
}
