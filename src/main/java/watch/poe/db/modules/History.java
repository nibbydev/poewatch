package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;

import java.sql.*;

public class History {
    private static Logger logger = LoggerFactory.getLogger(History.class);
    private Database database;

    public History(Database database) {
        this.database = database;
    }

    /**
     * Removes entries from table `league_entries` that exceed the specified capacity
     *
     * @return True on success
     */
    public boolean removeOldItemEntries() {
        String query =  "DELETE e3 FROM league_entries AS e3 " +
                        "JOIN ( " + // Get group ids and their nth oldest time
                        "  SELECT   e1.id_l, e1.id_d, ( " + // Get the nth time for each group
                        "    SELECT time " +
                        "    FROM league_entries " +
                        "    WHERE id_l = e1.id_l " +
                        "      AND id_d = e1.id_d " +
                        "    ORDER BY time DESC " +
                        "    LIMIT ?, 1 " +
                        "  ) AS time " +
                        "  FROM league_entries AS e1 " +
                        "  JOIN ( " + // Get ids of all groups that have more than n entries
                        "    SELECT id_l, id_d " +
                        "    FROM league_entries " +
                        "    GROUP BY id_l, id_d " +
                        "    having count(*) > ? " +
                        "  ) AS e2 on e1.id_l = e2.id_l and e1.id_d = e2.id_d " +
                        "  GROUP BY id_l, id_d " +
                        ") AS tmp " +
                        "ON  e3.id_l = tmp.id_l " +
                        "AND e3.id_d = tmp.id_d " +
                        "AND e3.time <= tmp.time; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, database.config.getInt("entry.maxCount") - 1);
                statement.setInt(2, database.config.getInt("entry.maxCount"));
                statement.executeUpdate();
            }

            database.connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Copies data from table `league_items` to table `league_history_hourly` every hour
     * on a rolling basis with a history of 24 hours
     *
     * @return True on success
     */
    public boolean addHourly() {
        String query =  "INSERT INTO league_history_hourly (" +
                        "  id_l, id_d, inc) " +
                        "SELECT id_l, id_d, inc " +
                        "FROM   league_items AS i " +
                        "JOIN   data_leagues AS l " +
                        "  ON   i.id_l = l.id " +
                        "WHERE  l.active = 1 " +
                        "  AND  i.inc > 0 ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Copies data from table `league_items` to table `league_history_daily` every 24h
     * on a rolling basis
     *
     * @return True on success
     */
    public boolean addDaily() {
        String query =  "INSERT INTO league_history_daily ( " +
                        "  id_l, id_d, mean, median, mode, " +
                        "  min, max, exalted, count, quantity) " +
                        "SELECT " +
                        "  id_l, id_d, mean, median, mode, " +
                        "  min, max, exalted, count, quantity " +
                        "FROM   league_items AS i " +
                        "JOIN   data_leagues AS l " +
                        "  ON   i.id_l = l.id " +
                        "WHERE  l.active = 1 ";

        return database.executeUpdateQueries(query);
    }
}
