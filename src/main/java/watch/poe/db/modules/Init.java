package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.entry.item.Key;
import poe.manager.entry.timer.Timer;
import poe.manager.entry.timer.TimerList;
import poe.manager.league.LeagueEntry;
import poe.manager.relation.CategoryEntry;

import java.sql.*;
import java.util.*;

public class Init {
    private static Logger logger = LoggerFactory.getLogger(Init.class);
    private Database database;

    public Init(Database database) {
        this.database = database;
    }

    /**
     * Loads provided List with league entries from database
     *
     * @param leagueEntries List that will contain LeagueEntry entries
     * @return True on success
     */
    public boolean getLeagues(List<LeagueEntry> leagueEntries) {
        String query = "SELECT * FROM data_leagues WHERE active = 1; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                leagueEntries.clear();

                while (resultSet.next()) {
                    leagueEntries.add(new LeagueEntry(resultSet));
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Loads provided Map with category entries from database
     *
     * @param categoryRelations Empty map that will contain category - CategoryEntry relations
     * @return True on success
     */
    public boolean getCategories(Map<String, CategoryEntry> categoryRelations) {
        Map<String, CategoryEntry> tmpCategoryRelations = new HashMap<>();

        String query =  "SELECT    dc.name AS categoryName, " +
                        "          dc.id   AS categoryId, " +
                        "          GROUP_CONCAT(dg.name) AS groupNames, " +
                        "          GROUP_CONCAT(dg.id)   AS groupIds " +
                        "FROM      data_categories AS dc " +
                        "JOIN      data_groups     AS dg " +
                        "  ON      dc.id = dg.id_cat " +
                        "GROUP BY  dc.id; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            if (categoryRelations == null) {
                throw new SQLException("Provided map was null");
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String[] groupIds = resultSet.getString("groupIds").split(",");
                    String[] groupNames = resultSet.getString("groupNames").split(",");

                    CategoryEntry categoryEntry = new CategoryEntry();
                    categoryEntry.setId(resultSet.getInt("categoryId"));

                    for (int i = 0; i < groupIds.length; i++) {
                        categoryEntry.addGroup(groupNames[i], Integer.parseInt(groupIds[i]));
                    }

                    tmpCategoryRelations.putIfAbsent(resultSet.getString("categoryName"), categoryEntry);
                }
            }

            categoryRelations.clear();
            categoryRelations.putAll(tmpCategoryRelations);

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Loads provided Maps with item ID data from database
     *
     * @param keyToId Empty map that will contain item Key - item ID relations
     * @return True on success
     */
    public boolean getItemData(Map<Key, Integer> keyToId) {
        Map<Key, Integer> tmpKeyToId = new HashMap<>();

        String query = "SELECT * FROM data_itemData; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            if (keyToId == null) {
                throw new SQLException("Provided map was null");
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    tmpKeyToId.put(new Key(resultSet), resultSet.getInt("id"));
                }
            }

            keyToId.clear();
            keyToId.putAll(tmpKeyToId);

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Loads provided Maps with item ID data from database
     *
     * @param leagueIds Empty map that will contain league ID - list of item IDs relations
     * @return True on success
     */
    public boolean getLeagueItemIds(Map<Integer, List<Integer>> leagueIds) {
        Map<Integer, List<Integer>> tmpLeagueIds = new HashMap<>();

        String query =  "SELECT   i.id_l, i.id_d " +
                        "FROM     league_items_rolling AS i " +
                        "JOIN     data_leagues AS l " +
                        "  ON     i.id_l = l.id " +
                        "WHERE    l.active = 1 " +
                        "ORDER BY i.id_l ASC; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            if (leagueIds == null) {
                throw new SQLException("Provided map was null");
            }


            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    Integer leagueId = resultSet.getInt("id_l");
                    Integer itemId = resultSet.getInt("id_d");

                    List<Integer> idList = tmpLeagueIds.getOrDefault(leagueId, new ArrayList<>());
                    idList.add(itemId);
                    tmpLeagueIds.putIfAbsent(leagueId, idList);
                }
            }

            leagueIds.clear();
            leagueIds.putAll(tmpLeagueIds);

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Creates a map containing currency price data from database in the format of:
     * {leagueID: {currencyName: chaosValue}}
     *
     * @return Generated Map
     */
    public Map<Integer, Map<String, Double>> getCurrencyMap() {
        Map<Integer, Map<String, Double>> tmpCurrencyLeagueMap = new HashMap<>();

        String query =  "SELECT   i.id_l, did.name, i.median " +
                        "FROM     league_items_rolling  AS i " +
                        "JOIN     data_itemData AS did " +
                        "  ON     i.id_d = did.id " +
                        "WHERE    did.id_grp = 11 " +
                        "ORDER BY i.id_l; ";

        try {
            if (database.connection.isClosed()) {
                return null;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    Integer leagueId = resultSet.getInt("id_l");
                    String name = resultSet.getString("name");
                    Double price = resultSet.getDouble("median");

                    Map<String, Double> currencyMap = tmpCurrencyLeagueMap.getOrDefault(leagueId, new HashMap<>());
                    currencyMap.put(name, price);
                    tmpCurrencyLeagueMap.putIfAbsent(leagueId, currencyMap);
                }
            }

            return tmpCurrencyLeagueMap;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Loads provided Map with currency alias data from database
     *
     * @param currencyAliasToName Map that will contain currency alias - currency name relations
     * @return True on success
     */
    public boolean getCurrencyAliases(Map<String, String> currencyAliasToName) {
        String query =  "SELECT ci.name AS name, " +
                        "       ca.name AS alias " +
                        "FROM   data_currencyItems   AS ci " +
                        "JOIN   data_currencyAliases AS ca " +
                        "  ON   ci.id = ca.id_ci; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String alias = resultSet.getString("alias");

                    currencyAliasToName.put(alias, name);
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Loads provided Map with timer delay entries from database
     *
     * @param timeLog Empty map that will be filled with key - TimerList relations
     * @return True on success
     */
    public boolean getTimers(Map<String, TimerList> timeLog) {
        String query = "SELECT `key`, delay, type FROM data_timers ORDER BY time ASC;";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String key = resultSet.getString("key");
                    long delay = resultSet.getLong("delay");
                    Integer type = resultSet.getInt("type");

                    if (resultSet.wasNull()) type = null;
                    poe.manager.entry.timer.Timer.TimerType timerType = Timer.translate(type);

                    TimerList timerList = timeLog.getOrDefault(key, new TimerList(timerType));

                    // Truncate list if entry count exceeds limit
                    if (timerList.list.size() >= database.config.getInt("misc.timerLogHistoryLength")) {
                        timerList.list.remove(0);
                    }

                    timerList.list.add(delay);
                    timeLog.putIfAbsent(key, timerList);
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Loads provided Map with item base names from database
     *
     * @param baseMap Empty map that will contain category - base name relations
     * @return True on success
     */
    public boolean getBaseItems(Map<String, Set<String>> baseMap) {
        Map<String, Set<String>> tmpBaseMap = new HashMap<>();

        String query = "SELECT * FROM data_baseNames; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            if (baseMap == null) {
                throw new SQLException("Provided map was null");
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String category = resultSet.getString("category");
                    Set<String> tmpBaseSet = tmpBaseMap.getOrDefault(category, new HashSet<>());
                    tmpBaseSet.add(resultSet.getString("base"));
                    tmpBaseMap.putIfAbsent(category, tmpBaseSet);
                }
            }

            baseMap.clear();
            baseMap.putAll(tmpBaseMap);

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
