package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.StatType;
import poe.Managers.StatisticsManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Calc {
    private static Logger logger = LoggerFactory.getLogger(Calc.class);
    private Database database;

    public Calc(Database database) {
        this.database = database;
    }

    public boolean countActiveAccounts(StatisticsManager statisticsManager) {
        String query =  "select distinct count(*) from league_accounts " +
                        "where updated > date_sub(now(), interval 1 hour) ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                // Get first and only entry
                if (resultSet.next()) {
                    statisticsManager.addValue(StatType.ACTIVE_ACCOUNTS, resultSet.getInt(1));
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return false;
    }

    public ResultSet getEntryStream() {
        String query =  "select le.id_l, le.id_d, " +
                        "  truncate(le.price * ifnull(foo3.val, 1.0), 8) as price " +
                        "from league_entries as le " +
                        "join ( " +
                        "  select distinct id_l, id_d from league_entries " +
                        "  where stash_crc is not null " +
                        "    and updated > date_sub(now(), interval 65 second) " +
                        ") as foo1 on le.id_l = foo1.id_l and le.id_d = foo1.id_d " +
                        "join ( " +
                        "  select distinct account_crc from league_accounts " +
                        "  where updated > date_sub(now(), interval 6 hour) " +
                        ") as foo2 on le.account_crc = foo2.account_crc " +
                        "left join ( " +
                        "  select id_l, id_d, mean as val from league_items " +
                        "  where mean > 0 " +
                        ") as foo3 on le.id_l = foo3.id_l and le.id_price = foo3.id_d " +
                        "left join ( " +
                        "  select id from data_itemData where frame = 5 " +
                        ") as foo4 on le.id_d = foo4.id " +
                        "where le.stash_crc is not null " +
                        "  and !(foo4.id is not null && le.id_price is not null) " +
                        "having price > 0 and price < 96000 " +
                        "order by le.id_l asc, le.id_d asc; ";

        /*
        Here's the query somewhat explained. Might not match 1:1 due to fixes/changes.
        Warning: not for the faint of heart.

            -- Select leagueID, itemID and price from every valid entry.
            -- When buyout note is in chaos, `foo3.val` is null. Otherwise
            -- it's the mean chaos value of the currency used
            select le.id_l, le.id_d,
              truncate(le.price * ifnull(foo3.val, 1.0), 8) as price
            from league_entries as le
            -- get all items that have had entries added since last calculation cycle
            join (
              select distinct id_l, id_d from league_entries
              where stash_crc is not null
                and updated > date_sub(now(), interval 65 second)
            ) as foo1 on le.id_l = foo1.id_l and le.id_d = foo1.id_d
            -- get all accounts that have been active in trade recently
            join (
              select distinct account_crc from league_accounts
              where updated > date_sub(now(), interval 6 hour)
            ) as foo2 on le.account_crc = foo2.account_crc
            -- get currency prices in chaos
            left join (
              select id_l, id_d, mean as val from league_items
              where mean > 0
            ) as foo3 on le.id_l = foo3.id_l and le.id_price = foo3.id_d
            -- get all itemIDs that are currency
            left join (
              select id from data_itemData where frame = 5
            ) as foo4 on le.id_d = foo4.id
            -- if item is currently in a public stash tab
            where le.stash_crc is not null
            -- if (is currency) and (is not in chaos), return FALSE, otherwise return TRUE.
            -- This restrict currency price calculation to only use entries listed in chaos
            -- to avoid circular dependencies. Eg exalted orbs are listed for divines and
            -- divines are listed in exalted orbs, causing a circular effect which messes up
            -- the prices.
              and !(foo4.id is not null && le.id_price is not null)
            -- Hard-filter out any entries that have ridiculous prices after being converted
            -- to chaos.
            having price > 0 and price < 96000
            order by le.id_l asc, le.id_d asc;
         */

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            // Return open connection
            return database.connection.createStatement().executeQuery(query);

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Calculates exalted price for items in table `league_items` based on exalted prices in same table
     *
     * @return True on success
     */
    public boolean calcCurrent() {
        String query =  "update league_items as li " +
                        "join ( " +
                        "  select le.id_l, le.id_d, count(*) as count " +
                        "  from league_entries as le " +
                        "  join ( " +
                        "    select distinct account_crc from league_accounts " +
                        "    where updated > date_sub(now(), interval 6 hour) " +
                        "  ) as foo2 on le.account_crc = foo2.account_crc " +
                        "  where le.stash_crc is not null " +
                        "  group by le.id_l, le.id_d " +
                        ") as foo1 on foo1.id_l = li.id_l and foo1.id_d = li.id_d " +
                        "set li.current = foo1.count; ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Calculates exalted price for items in table `league_items` based on exalted prices in same table
     *
     * @return True on success
     */
    public boolean calcExalted() {
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
