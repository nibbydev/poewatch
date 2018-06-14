package com.poestats.pricer.maps;

import com.poestats.database.CurrencyItem;

import java.util.HashMap;

public class CurrencyMaps {
    public static class CurrencyMap extends HashMap<String, CurrencyItem> {

    }

    public static class CurrencyLeagueMap extends HashMap<String, CurrencyMap> {
    }
}
