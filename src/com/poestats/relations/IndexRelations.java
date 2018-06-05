package com.poestats.relations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexRelations {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final List<String> completeIndexList = new ArrayList<>();
    private final Map<String, List<String>> supIndexToSubs = new HashMap<>();
    private final Map<String, String> uniqueKeyToFullIndex = new HashMap<>();
    private final Map<String, String> genericKeyToSupIndex = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public List<String> getCompleteIndexList() {
        return completeIndexList;
    }

    public Map<String, List<String>> getSupIndexToSubs() {
        return supIndexToSubs;
    }

    public Map<String, String> getGenericKeyToSupIndex() {
        return genericKeyToSupIndex;
    }

    public Map<String, String> getUniqueKeyToFullIndex() {
        return uniqueKeyToFullIndex;
    }
}
