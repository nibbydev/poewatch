package com.poestats.relations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexRelations {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Map<String, Map<String, Integer>> leagueToKeyToId = new HashMap<>();
    private Map<String, List<Integer>> leagueToIds = new HashMap<>();
    private Map<String, Integer> parentKeyToParentItemDataId = new HashMap<>();
    private Map<String, Integer> childKeyToChildItemDataId = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------
    // Methods
    //------------------------------------------------------------------------------------------------------------

    public void loadItemIds(ResultSet resultSet, String league) throws SQLException {
        Map<String, Integer> keyToId = leagueToKeyToId.getOrDefault(league, new HashMap<>());
        List<Integer> idList = leagueToIds.getOrDefault(league, new ArrayList<>());

        while (resultSet.next()) {
            Integer id = resultSet.getInt("id");
            String key = resultSet.getString("key");

            keyToId.put(key, id);
            idList.add(id);
        }

        leagueToKeyToId.putIfAbsent(league, keyToId);
        leagueToIds.putIfAbsent(league, idList);
    }

    public void loadItemDataParentIds(ResultSet resultSet) throws SQLException {
        while (resultSet.next()) {
            Integer id = resultSet.getInt("id");
            String key = resultSet.getString("key");

            parentKeyToParentItemDataId.put(key, id);
        }
    }

    public void loadItemDataChildIds(ResultSet resultSet) throws SQLException {
        while (resultSet.next()) {
            Integer id = resultSet.getInt("id");
            String key = resultSet.getString("key");

            childKeyToChildItemDataId.put(key, id);
        }
    }



    public boolean isEmpty_childKeyToChildId() {
        return childKeyToChildItemDataId.isEmpty();
    }

    public boolean isEmpty_parentKeyToParentId() {
        return parentKeyToParentItemDataId.isEmpty();
    }

    public boolean isEmpty_leagueToKeyToId() {
        return leagueToKeyToId.isEmpty();
    }



    public Integer getItemId(String league, String key) {
        Map<String, Integer> keyToId = leagueToKeyToId.get(league);
        if (keyToId == null) return null;

        return keyToId.get(key);
    }

    public Integer getParentItemDataId(String key) {
        return parentKeyToParentItemDataId.get(key);
    }

    public Integer getChildItemDataId(String key) {
        return childKeyToChildItemDataId.get(key);
    }



    public void addLeagueToKeyToId(String league, String key, int id) {
        Map<String, Integer> keyToId = leagueToKeyToId.getOrDefault(league, new HashMap<>());
        keyToId.put(key, id);
        leagueToKeyToId.putIfAbsent(league, keyToId);
    }

    public void addLeagueToIds(String league, int id) {
        List<Integer> idList = leagueToIds.getOrDefault(league, new ArrayList<>());
        idList.add(id);
        leagueToIds.putIfAbsent(league, idList);
    }

    public void addParentKeyToParentItemDataId(String key, int id) {
        parentKeyToParentItemDataId.put(key, id);
    }

    public void addChildKeyToChildItemDataId(String key, int id) {
        childKeyToChildItemDataId.put(key, id);
    }
}
