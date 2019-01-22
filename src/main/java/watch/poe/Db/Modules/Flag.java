package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.Collector;

import java.sql.*;
import java.util.Set;

public class Flag {
    private static Logger logger = LoggerFactory.getLogger(Flag.class);
    private Database database;

    public Flag(Database database) {
        this.database = database;
    }

    public boolean resetStashReferences(Set<Long> set) {
        String query =  "update league_entries " +
                        "set stash_crc = NULL " +
                        "where stash_crc = ?; ";
        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (long crc : set) {
                    statement.setLong(1, crc);
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            database.connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Updates totals for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean updateCounters() {
        String query1 = "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "  SELECT id_l, id_d, COUNT(*) AS total " +
                        "  FROM league_entries " +
                        "  WHERE discovered > date_sub(now(), interval 60 second) " +
                        "  GROUP BY id_l, id_d" +
                        ") AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                        "SET i.total = i.total + e.total, i.inc = i.inc + e.total ";

        return database.executeUpdateQueries(query1);
    }

    /**
     * Resets counters for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean resetCounters() {
        String query = "UPDATE league_items SET inc = 0; ";

        return database.executeUpdateQueries(query);
    }
}
