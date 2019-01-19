package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Key;
import poe.Worker.Timer.Timer;
import poe.Worker.Timer.TimerList;
import poe.Managers.League.LeagueEntry;
import poe.Managers.Relation.CategoryEntry;

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

        logger.info("Getting leagues from database");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                leagueEntries.clear();

                while (resultSet.next()) {
                    leagueEntries.add(new LeagueEntry(resultSet));
                }
            }

            logger.info("Got leagues from database");
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            logger.error("Could not get leagues from database");
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
        String query =  "SELECT    dc.name AS categoryName, " +
                        "          dc.id   AS categoryId, " +
                        "          GROUP_CONCAT(dg.name) AS groupNames, " +
                        "          GROUP_CONCAT(dg.id)   AS groupIds " +
                        "FROM      data_categories AS dc " +
                        "JOIN      data_groups     AS dg " +
                        "  ON      dc.id = dg.id_cat " +
                        "GROUP BY  dc.id; ";

        logger.info("Getting categories from database");

        if (categoryRelations == null) {
            throw new NullPointerException("Provided map was null");
        }

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            Map<String, CategoryEntry> tmpCategoryRelations = new HashMap<>();

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String[] groupIds = resultSet.getString("groupIds").split(",");
                    String[] groupNames = resultSet.getString("groupNames").split(",");

                    CategoryEntry categoryEntry = new CategoryEntry(resultSet.getInt("categoryId"));

                    for (int i = 0; i < groupIds.length; i++) {
                        categoryEntry.addGroup(groupNames[i], Integer.parseInt(groupIds[i]));
                    }

                    tmpCategoryRelations.putIfAbsent(resultSet.getString("categoryName"), categoryEntry);
                }
            }

            categoryRelations.clear();
            categoryRelations.putAll(tmpCategoryRelations);
            logger.info("Got categories from database");

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
        String query = "SELECT * FROM data_itemData; ";

        logger.info("Getting item data from database");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            if (keyToId == null) {
                throw new SQLException("Provided map was null");
            }

            Map<Key, Integer> tmpKeyToId = new HashMap<>();

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    tmpKeyToId.put(new Key(resultSet), resultSet.getInt("id"));
                }
            }

            keyToId.clear();
            keyToId.putAll(tmpKeyToId);
            logger.info("Got item data from database");

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
    public boolean getLeagueItemIds(Map<Integer, Set<Integer>> leagueIds) {
        String query =  "SELECT   i.id_l, i.id_d " +
                        "FROM     league_items AS i " +
                        "JOIN     data_leagues AS l " +
                        "  ON     i.id_l = l.id " +
                        "WHERE    l.active = 1 " +
                        "ORDER BY i.id_l ASC; ";

        logger.info("Getting league item IDs from database");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            if (leagueIds == null) {
                throw new SQLException("Provided map was null");
            }

            Map<Integer, Set<Integer>> tmpLeagueIds = new HashMap<>();

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    int id_l = resultSet.getInt(1);

                    Set<Integer> idList = tmpLeagueIds.getOrDefault(id_l, new HashSet<>());
                    idList.add(resultSet.getInt(2));
                    tmpLeagueIds.putIfAbsent(id_l, idList);
                }
            }

            leagueIds.clear();
            leagueIds.putAll(tmpLeagueIds);
            logger.info("Got league item IDs from database");

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
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String key = resultSet.getString("key");
                    long delay = resultSet.getLong("delay");
                    Integer type = resultSet.getInt("type");

                    if (resultSet.wasNull()) type = null;
                    poe.Worker.Timer.Timer.TimerType timerType = Timer.translate(type);

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
     * Gets the local changeId
     *
     * @return Local changeId or null on error
     */
    public String getChangeID() {
        String query = "SELECT changeId FROM data_changeId; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);
                resultSet.next();
                return resultSet.getString("changeId");
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return null;
    }

    public boolean getStashIds(Set<Long> set) {
        String query = "SELECT DISTINCT stash_crc FROM league_entries WHERE stash_crc IS NOT NULL; ";

        logger.info("Getting stash IDs from database");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    set.add(resultSet.getLong(1));
                }

                logger.info("Got stash IDs from database");
                return true;
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
