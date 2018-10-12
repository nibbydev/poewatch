package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Config;
import poe.db.Database;

import java.sql.*;

public class Flag {
    private static Logger logger = LoggerFactory.getLogger(Flag.class);
    private Database database;

    public Flag(Database database) {
        this.database = database;
    }

    /**
     * Updates volatile state for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateVolatile() {
        String query1 = "UPDATE league_items_rolling " +
                        "SET volatile = IF(`dec` > 0 && `dec` >= inc * ?, 1, 0);";

        String query2 = "UPDATE  league_entries       AS e " +
                        "JOIN    league_items_rolling AS i " +
                        "  ON    e.id_d = i.id_d " +
                        "    AND e.id_l = i.id_l " +
                        "SET     e.approved = 0 " +
                        "WHERE   i.volatile = 1 " +
                        "  AND   e.approved = 1;";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                statement.setDouble(1, Config.entry_volatileRatio);
                statement.executeUpdate();
            }

            try (Statement statement = database.connection.createStatement()) {
                statement.executeUpdate(query2);
            }

            database.connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Updates approved state for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateApproved() {
        String query1 = "UPDATE  league_entries       AS e " +
                        "JOIN    league_items_rolling AS i " +
                        "  ON    i.id_l = e.id_l " +
                        "    AND i.id_d = e.id_d " +
                        "SET     e.approved = 1 " +
                        "WHERE   e.approved = 0 " +
                        "  AND   i.median   = 0 ";

        String query2 = "UPDATE  league_entries       AS e " +
                        "JOIN    league_items_rolling AS i " +
                        "  ON    i.id_l = e.id_l " +
                        "    AND i.id_d = e.id_d " +
                        "SET     e.approved = 1 " +
                        "WHERE   e.approved = 0 " +
                        "  AND   e.price > i.median / i.multiplier " +
                        "  AND   e.price < i.median * i.multiplier ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                statement.executeUpdate(query1);
                statement.executeUpdate(query2);
            }

            database.connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Updates multipliers for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateMultipliers() {
        String query =  "UPDATE league_items_rolling " +
                        "SET multiplier = IF(? - quantity / ? < ?, ?, ? - quantity / ?)";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setDouble(1, Config.entry_approvedMax);
                statement.setDouble(2, Config.entry_approvedDiv);
                statement.setDouble(3, Config.entry_approvedMin);
                statement.setDouble(4, Config.entry_approvedMin);
                statement.setDouble(5, Config.entry_approvedMax);
                statement.setDouble(6, Config.entry_approvedDiv);

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
     * Updates counters for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateCounters() {
        String query1 = "UPDATE league_items_rolling AS i " +
                        "    JOIN ( " +
                        "        SELECT id_l, id_d, COUNT(*) AS count " +
                        "        FROM league_entries " +
                        "        WHERE approved = 1 " +
                        "           AND time > ADDDATE(NOW(), INTERVAL -60 SECOND)" +
                        "        GROUP BY id_l, id_d" +
                        "    ) AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                        "SET " +
                        "    i.count = i.count + e.count, " +
                        "    i.inc = i.inc + e.count ";

        String query2 = "UPDATE league_items_rolling AS i " +
                        "    JOIN ( " +
                        "        SELECT id_l, id_d, COUNT(*) AS count " +
                        "        FROM league_entries " +
                        "        WHERE approved = 0 " +
                        "           AND time > ADDDATE(NOW(), INTERVAL -60 SECOND) " +
                        "        GROUP BY id_l, id_d" +
                        "    ) AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                        "SET " +
                        "    i.count = i.count + e.count, " +
                        "    i.inc = i.inc + e.count, " +
                        "    i.dec = i.dec + e.count ";


        return database.executeUpdateQueries(query1, query2);
    }

    /**
     * Resets counters for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean resetCounters() {
        String query = "UPDATE league_items_rolling SET inc = 0, `dec` = 0 ";

        return database.executeUpdateQueries(query);
    }
}
