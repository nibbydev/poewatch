package com.poestats.Pricer.Parcel;

import com.poestats.Main;
import com.poestats.Pricer.Entry;
import com.poestats.Pricer.Parcel.sub.JSONCategoryMap;
import com.poestats.Pricer.Parcel.sub.JSONItemList;
import com.poestats.Pricer.Parcel.sub.JSONLeagueMap;
import com.poestats.RelationManager.IndexedItem;

public class JSONParcel {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private JSONLeagueMap jsonLeagueMap = new JSONLeagueMap();

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    public void add(Entry entry) {
        if (entry.getIndex() == null) return;
        IndexedItem indexedItem = Main.RELATIONS.indexToGenericData(entry.getIndex());
        if (indexedItem == null) return;

        JSONCategoryMap jsonCategoryMap = jsonLeagueMap.getOrDefault(entry.getLeague(), new JSONCategoryMap());
        JSONItemList jsonItems = jsonCategoryMap.getOrDefault(indexedItem.parent, new JSONItemList());

        JSONItem jsonItem = new JSONItem();
        jsonItem.copy(entry);

        jsonItems.add(jsonItem);
        jsonCategoryMap.putIfAbsent(indexedItem.parent, jsonItems);
        jsonLeagueMap.putIfAbsent(entry.getLeague(), jsonCategoryMap);
    }

    public void sort() {
        for (String league : jsonLeagueMap.keySet()) {
            JSONCategoryMap jsonCategoryMap = jsonLeagueMap.get(league);

            for (String category : jsonCategoryMap.keySet()) {
                JSONItemList jsonItems = jsonCategoryMap.get(category);
                JSONItemList jsonItems_sorted = new JSONItemList();

                while (!jsonItems.isEmpty()) {
                    JSONItem mostExpensiveItem = null;

                    for (JSONItem item : jsonItems) {
                        if (mostExpensiveItem == null) {
                            mostExpensiveItem = item;
                        } else if (item.mean > mostExpensiveItem.mean) {
                            mostExpensiveItem = item;
                        }
                    }

                    jsonItems.remove(mostExpensiveItem);
                    jsonItems_sorted.add(mostExpensiveItem);
                }

                // Write jsonItems_sorted to category map
                jsonCategoryMap.put(category, jsonItems_sorted);
            }
        }
    }

    public void clear () {
        jsonLeagueMap.clear();
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public JSONLeagueMap getJsonLeagueMap() {
        return jsonLeagueMap;
    }
}
