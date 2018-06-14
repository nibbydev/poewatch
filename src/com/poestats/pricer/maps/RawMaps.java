package com.poestats.pricer.maps;

import com.poestats.pricer.entries.RawEntry;

import java.util.HashMap;

public class RawMaps {
    public static class RawEntryLeagueMap extends HashMap<String, IndexMap> {
        // League map. Has mappings of: [league - index map]
    }

    public static class IndexMap extends HashMap<Integer, AccountMap> {
        // Index map. Has mappings of: [index - AccountMap]
    }

    public static class AccountMap extends HashMap<String, RawEntry> {
        // Index map. Has mappings of: [account name - RawEntry]
    }
}
