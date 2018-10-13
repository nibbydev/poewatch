package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Config;
import poe.db.Database;

import java.sql.*;

public class Calc {
    private static Logger logger = LoggerFactory.getLogger(Calc.class);
    private Database database;

    public Calc(Database database) {
        this.database = database;
    }

    /**
     * Calculates mean, median and mode price for items in table `league_items_rolling` based on approved entries in
     * `league_entries`
     *
     * @return True on success
     */
    public boolean calculatePrices() {
        String query =  "UPDATE league_items_rolling AS i " +
                        "JOIN ( " +
                        "  SELECT   id_l, id_d, " +
                        "           AVG(price)        AS mean, " +
                        "           MEDIAN(price)     AS median, " +
                        "           stats_mode(price) AS mode, " +
                        "           MIN(price)        AS min, " +
                        "           MAX(price)        AS max " +
                        "  FROM     league_entries " +
                        "  WHERE    approved = 1 " +
                        "  GROUP BY id_l, id_d " +
                        ") AS    e " +
                        "  ON    i.id_l = e.id_l " +
                        "    AND i.id_d = e.id_d " +
                        "SET     i.mean   = TRUNCATE(e.mean,   ?), " +
                        "        i.median = TRUNCATE(e.median, ?), " +
                        "        i.mode   = TRUNCATE(e.mode,   ?), " +
                        "        i.min    = TRUNCATE(e.min,    ?), " +
                        "        i.max    = TRUNCATE(e.max,    ?); ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, Config.precision);
                statement.setInt(2, Config.precision);
                statement.setInt(3, Config.precision);
                statement.setInt(4, Config.precision);
                statement.setInt(5, Config.precision);
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
     * Calculates median price for volatile items in table `league_items_rolling` based on any entries in
     * `league_entries`
     *
     * @return True on success
     */
    public boolean calculateVolatileMedian() {
        String query =  "UPDATE league_items_rolling AS i " +
                        "JOIN ( " +
                        "  SELECT   id_l, id_d, " +
                        "           MEDIAN(price) AS median " +
                        "  FROM     league_entries " +
                        "  GROUP BY id_l, id_d " +
                        ") AS    e " +
                        "  ON    i.id_l = e.id_l " +
                        "    AND i.id_d = e.id_d " +
                        "SET     i.median = TRUNCATE(e.median, ?) " +
                        "WHERE   i.volatile = 1;";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, Config.precision);
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
     * Calculates exalted price for items in table `league_items_rolling` based on exalted prices in same table
     *
     * @return True on success
     */
    public boolean calculateExalted() {
        String query =  "UPDATE    league_items_rolling AS i " +
                        "JOIN ( " +
                        "  SELECT  i.id_l, i.mean " +
                        "  FROM    league_items_rolling AS i " +
                        "  JOIN    data_itemData        AS did " +
                        "    ON    i.id_d = did.id " +
                        "  WHERE   did.frame = 5 " +
                        "    AND   did.name = 'Exalted Orb' " +
                        ") AS      ex " +
                        "  ON      i.id_l = ex.id_l " +
                        "SET       i.exalted = TRUNCATE(i.mean / ex.mean, ?) " +
                        "WHERE     ex.mean > 0 " +
                        "  AND     i.mean  > 0; ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, Config.precision);
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
     * Calculates quantity for items in table `league_items_rolling` based on history entries from table `league_history`
     *
     * @return True on success
     */
    public boolean calcQuantity() {
        String query =  "UPDATE league_items_rolling AS i  " +
                        "LEFT JOIN ( " +
                        "    SELECT   id_l, id_d, SUM(inc) AS quantity " +
                        "    FROM     league_history_hourly_quantity " +
                        "    WHERE    time > ADDDATE(NOW(), INTERVAL -24 HOUR) " +
                        "    GROUP BY id_l, id_d " +
                        ") AS    h " +
                        "  ON    h.id_l = i.id_l " +
                        "    AND h.id_d = i.id_d " +
                        "SET     i.quantity = IFNULL(h.quantity, 0) ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Calculates spark data for items in table `league_items_rolling` based on history entries
     *
     * @return True on success
     */
    public boolean calcSpark() {
        String query =  "UPDATE league_items_rolling AS i " +
                        "JOIN ( " +
                        "  SELECT    i.id_l, i.id_d, " +
                        "            SUBSTRING_INDEX(GROUP_CONCAT(lhdr.mean ORDER BY lhdr.time DESC SEPARATOR ','), ',', 6) AS history " +
                        "  FROM      league_items_rolling          AS i " +
                        "  JOIN      data_leagues                  AS l " +
                        "    ON      l.id = i.id_l " +
                        "  JOIN      league_history_daily_rolling  AS lhdr " +
                        "    ON      lhdr.id_d = i.id_d " +
                        "      AND   lhdr.id_l = l.id " +
                        "  WHERE     l.active = 1 " +
                        "    AND     i.count  > 1 " +
                        "  GROUP BY  i.id_l, i.id_d " +
                        ") AS    tmp " +
                        "  ON    i.id_l = tmp.id_l " +
                        "    AND i.id_d = tmp.id_d " +
                        "SET     i.spark = tmp.history ";

        return database.executeUpdateQueries(query);
    }
}
