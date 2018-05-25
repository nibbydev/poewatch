package com.poestats.parcel;

import com.poestats.Main;
import com.poestats.pricer.Entry;
import com.poestats.parcel.ParcelMaps.*;
import com.poestats.relations.entries.SupIndexedItem;

public class Parcel {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private ParcelLeagueMap parcelLeagueMap = new ParcelLeagueMap();

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    public void add(Entry entry) {
        if (entry.getIndex() == null) return;
        SupIndexedItem supIndexedItem = Main.RELATIONS.indexToGenericData(entry.getIndex());
        if (supIndexedItem == null) return;

        ParcelCategoryMap parcelCategoryMap = parcelLeagueMap.getOrDefault(entry.getLeague(), new ParcelCategoryMap());
        ParcelItemList parcelItemList = parcelCategoryMap.getOrDefault(supIndexedItem.getParent(), new ParcelItemList());

        ParcelEntry parcelEntry = new ParcelEntry();
        parcelEntry.copy(entry);

        parcelItemList.add(parcelEntry);
        parcelCategoryMap.putIfAbsent(supIndexedItem.getParent(), parcelItemList);
        parcelLeagueMap.putIfAbsent(entry.getLeague(), parcelCategoryMap);
    }

    public void sort() {
        for (String league : parcelLeagueMap.keySet()) {
            ParcelCategoryMap parcelCategoryMap = parcelLeagueMap.get(league);

            for (String category : parcelCategoryMap.keySet()) {
                ParcelItemList parcelItemList = parcelCategoryMap.get(category);
                ParcelItemList parcelItemList_sorted = new ParcelItemList();

                while (!parcelItemList.isEmpty()) {
                    ParcelEntry mostExpensiveItem = null;

                    for (ParcelEntry item : parcelItemList) {
                        if (mostExpensiveItem == null) {
                            mostExpensiveItem = item;
                        } else if (item.getMean() > mostExpensiveItem.getMean()) {
                            mostExpensiveItem = item;
                        }
                    }

                    parcelItemList.remove(mostExpensiveItem);
                    parcelItemList_sorted.add(mostExpensiveItem);
                }

                // Write parcelItemList_sorted to category map
                parcelCategoryMap.put(category, parcelItemList_sorted);
            }
        }
    }

    public void clear () {
        parcelLeagueMap.clear();
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public ParcelLeagueMap getParcelLeagueMap() {
        return parcelLeagueMap;
    }
}
