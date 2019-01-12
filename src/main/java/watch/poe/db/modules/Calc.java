package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;

import java.sql.*;

public class Calc {
    private static Logger logger = LoggerFactory.getLogger(Calc.class);
    private Database database;

    public Calc(Database database) {
        this.database = database;
    }

    /**
     * Calculates mean, median and mode price for items in table `league_items` based on entries in `league_entries`
     *
     * @return True on success
     */
    public boolean calculatePrices() {
        String query  = "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "  SELECT   le.id_l, le.id_d, " +
                        "           AVG(le.price)        AS mean, " +
                        "           MEDIAN(le.price)     AS median, " +
                        "           stats_mode(le.price) AS mode, " +
                        "           MIN(le.price)        AS min, " +
                        "           MAX(le.price)        AS max " +
                        "  FROM     league_entries AS le" +
                        "  JOIN (" +
                        "    SELECT DISTINCT account_crc AS crc FROM league_accounts " +
                        "    WHERE updated > DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                        "  ) AS active_accounts ON le.account_crc = active_accounts.crc " +
                        "  WHERE    le.stash_crc IS NOT NULL " +
                        "    AND    le.outlier = 0 " +
                        "  GROUP BY le.id_l, le.id_d " +
                        ") AS    e " +
                        "  ON    i.id_l = e.id_l " +
                        "    AND i.id_d = e.id_d " +
                        "SET     i.mean   = e.mean, " +
                        "        i.median = e.median, " +
                        "        i.mode   = e.mode, " +
                        "        i.min    = e.min, " +
                        "        i.max    = e.max ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Calculates exalted price for items in table `league_items` based on exalted prices in same table
     *
     * @return True on success
     */
    public boolean calculateExalted() {
        String query =  "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "  SELECT i.id_l, i.mean " +
                        "  FROM league_items AS i " +
                        "  JOIN data_itemData AS did ON i.id_d = did.id " +
                        "  WHERE did.name = 'Exalted Orb' " +
                        ") AS ex ON i.id_l = ex.id_l " +
                        "SET i.exalted = i.mean / ex.mean " +
                        "WHERE ex.mean > 0 AND i.mean > 0; ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Calculates the daily total for items in table `league_items` based on history entries from table `league_history`
     *
     * @return True on success
     */
    public boolean calcDaily() {
        String query =  "UPDATE league_items AS i  " +
                        "LEFT JOIN ( " +
                        "  SELECT id_l, id_d, SUM(inc) AS daily " +
                        "  FROM league_history_hourly " +
                        "  WHERE time > ADDDATE(NOW(), INTERVAL -24 HOUR) " +
                        "  GROUP BY id_l, id_d " +
                        ") AS h ON h.id_l = i.id_l AND h.id_d = i.id_d " +
                        "SET i.daily = IFNULL(h.daily, 0) ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Calculates spark data for items in table `league_items` based on history entries
     *
     * @return True on success
     */
    public boolean calcSpark() {
        String query =  "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "  SELECT    i.id_l, i.id_d, " +
                        "            SUBSTRING_INDEX(GROUP_CONCAT(lhd.mean ORDER BY lhd.time DESC SEPARATOR ','), ',', 6) AS history " +
                        "  FROM      league_items  AS i " +
                        "  JOIN      data_leagues  AS l " +
                        "    ON      l.id = i.id_l " +
                        "  JOIN      league_history_daily  AS lhd " +
                        "    ON      lhd.id_d = i.id_d " +
                        "      AND   lhd.id_l = l.id " +
                        "  WHERE     l.active = 1 " +
                        "    AND     i.total  > 1 " +
                        "  GROUP BY  i.id_l, i.id_d " +
                        ") AS    tmp " +
                        "  ON    i.id_l = tmp.id_l " +
                        "    AND i.id_d = tmp.id_d " +
                        "SET     i.spark = tmp.history ";

        return database.executeUpdateQueries(query);
    }
}
