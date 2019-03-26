package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Key;
import poe.Managers.League.League;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    public boolean getLeagues(List<League> leagueEntries) {
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
                    leagueEntries.add(new League(resultSet));
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
     * Loads provided Maps with item ID data from database
     *
     * @param keyToId Empty map that will contain item Key - item ID relations
     * @return True on success
     */
    public boolean getItemData(Map<Key, Integer> keyToId, Set<Integer> reindexSet) {
        String query = "SELECT * FROM data_itemData; ";

        logger.info("Getting item data from database");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            if (keyToId == null || !keyToId.isEmpty()) {
                throw new SQLException("Invalid provided map");
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    keyToId.put(new Key(resultSet), resultSet.getInt("id"));

                    // If entry was marked to be reindexed
                    if (resultSet.getInt("reindex") == 1) {
                        int id_d = resultSet.getInt("id");

                        reindexSet.add(id_d);
                        //logger.debug(String.format("Item %d queued for reindexing", id_d));
                    }
                }
            }

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
