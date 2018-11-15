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
        String query =  "DELETE    del " +
                        "FROM      league_entries AS del " +
                        "JOIN ( " +
                        "  SELECT   id_l, id_d, ( " +
                        "    SELECT   e.time " +
                        "    FROM     league_entries AS e " +
                        "    WHERE    e.id_l = d.id_l " +
                        "      AND    e.id_d = d.id_d " +
                        "    ORDER BY e.time DESC " +
                        "    LIMIT    ?, 1 " +
                        "  ) AS     time " +
                        "  FROM     league_entries AS d " +
                        "  GROUP BY id_l, id_d " +
                        "  HAVING time IS NOT NULL " +
                        ") AS      tmp " +
                        "  ON      del.id_l = tmp.id_l " +
                        "    AND   del.id_d = tmp.id_d " +
                        "      AND del.time <= tmp.time; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, database.config.getInt("entry.maxCount"));
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
