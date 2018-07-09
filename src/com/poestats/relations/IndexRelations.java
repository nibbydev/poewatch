package com.poestats.relations;

import com.poestats.league.LeagueEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexRelations {
    private Map<Integer, List<Integer>> leagueToIds = new HashMap<>();
    private Map<String, Integer> keyToId = new HashMap<>();

    public void loadLeagues(List<LeagueEntry> leagueEntries) {
        for (LeagueEntry leagueEntry : leagueEntries) {
            leagueToIds.put(leagueEntry.getId(), new ArrayList<>());
        }
    }

    public void loadItemIds(ResultSet resultSet) throws SQLException {
        while (resultSet.next()) {
            Integer leagueId = resultSet.getInt("id_l");
            Integer dataId = resultSet.getInt("id_d");
            String key = resultSet.getString("key");

            List<Integer> idList = leagueToIds.getOrDefault(leagueId, new ArrayList<>());
            idList.add(dataId);
            leagueToIds.putIfAbsent(leagueId, idList);

            keyToId.putIfAbsent(key, dataId);
        }
    }

    public Integer getItemId(String key) {
        return keyToId.get(key);
    }

    public boolean leagueHasId(Integer leagueId, Integer itemId) {
        return leagueToIds.get(leagueId) != null || leagueToIds.get(leagueId).contains(itemId);
    }

    public void addItemIdToLeague(Integer leagueId, Integer itemId) {
        leagueToIds.get(leagueId).add(itemId);
    }

    public void addItemIdToKeyMap(String key, Integer itemId) {
        keyToId.put(key, itemId);
    }

    public boolean isEmpty() {
        return keyToId.isEmpty();
    }
}
