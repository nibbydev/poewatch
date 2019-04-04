package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Calc {
    private static Logger logger = LoggerFactory.getLogger(Calc.class);
    private Database database;

    public Calc(Database database) {
        this.database = database;
    }

    /**
     * Queries a list of league+item pairs that need to have their prices recalculated
     *
     * @return
     */
    public ArrayList<IdBundle> getNewItemIdBundles() {
        String query =  "select distinct id_l, id_d " +
                        "from league_entries " +
                        "where stash_crc is not null " +
                        "  and price is not null " +
                        "  and discovered > date_sub(now(), interval 10 minute) ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                ArrayList<IdBundle> idBundles = new ArrayList<>();

                while (resultSet.next()) {
                    idBundles.add(new IdBundle(resultSet.getInt(1), resultSet.getInt(2)));
                }

                return idBundles;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Queries currency rates from the database
     *
     * @return
     */
    public ArrayList<PriceBundle> getPriceBundles() {
        String query =  "select li.id_l, li.id_d, li.mean " +
                        "from league_items as li " +
                        "join data_itemData as did on li.id_d = did.id " +
                        "where did.id_cat = 4 " +
                        "  and did.id_grp = 11 " +
                        "  and did.frame = 5 " +
                        "  and li.mean > 0 ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                ArrayList<PriceBundle> priceBundles = new ArrayList<>();

                while (resultSet.next()) {
                    priceBundles.add(new PriceBundle(
                            resultSet.getInt(1),
                            resultSet.getInt(2),
                            resultSet.getDouble(3)
                    ));
                }

                return priceBundles;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Queries entries for the specified item
     *
     * @param idBundle
     * @return
     */
    public List<EntryBundle> getEntryBundles(IdBundle idBundle) {
        String query =  "select le.price, le.id_price " +
                        "from league_entries as le " +
                        "join ( " +
                        "  select distinct account_crc from league_accounts " +
                        "  where updated > date_sub(now(), interval 1 hour) " +
                        ") as foo2 on le.account_crc = foo2.account_crc " +
                        "where le.id_l = ? " +
                        "  and le.id_d = ? " +
                        "  and le.stash_crc is not null " +
                        "  and le.price is not null ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, idBundle.getLeagueId());
                statement.setInt(2, idBundle.getItemId());
                ResultSet resultSet = statement.executeQuery();

                List<EntryBundle> entryBundles = new ArrayList<>();

                while (resultSet.next()) {
                    EntryBundle eb = new EntryBundle(resultSet.getDouble(1));

                    eb.setCurrencyId(resultSet.getInt(2));
                    if (resultSet.wasNull()) eb.setCurrencyId(null);
                    entryBundles.add(eb);
                }

                return entryBundles;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }











/*


    public ResultSet getEntryStream() {
        String query =  "select le.id_l, le.id_d, " +
                        "  truncate(le.price * ifnull(foo3.val, 1.0), 8) as price " +
                        "from league_entries as le " +
                        "join ( " +
                        "  select distinct id_l, id_d from league_entries " +
                        "  where stash_crc is not null " +
                        "    and price is not null " +
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
                        "  and le.price is not null " +
                        "  and !(foo4.id is not null && " +
                        "    le.id_price is not null && " +
                        "    le.id_price != (select id from data_itemData where name = 'Exalted Orb' limit 1)) " +
                        "having price > 0 and price < 96000 " +
                        "order by le.id_l asc, le.id_d asc; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                ArrayList<IdBundle> idBundles = new ArrayList<>();

                while (resultSet.next()) {
                    idBundles.add(new IdBundle(resultSet.getInt(1), resultSet.getInt(2)));
                }

                return idBundles;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }


*/


    public ResultSet getEntryStream() {
        String query =  "select le.id_l, le.id_d, " +
                        "  truncate(le.price * ifnull(foo3.val, 1.0), 8) as price " +
                        "from league_entries as le " +
                        "join ( " +
                        "  select distinct id_l, id_d from league_entries " +
                        "  where stash_crc is not null " +
                        "    and price is not null " +
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
                        "  and le.price is not null " +
                        "  and !(foo4.id is not null && " +
                        "    le.id_price is not null && " +
                        "    le.id_price != (select id from data_itemData where name = 'Exalted Orb' limit 1)) " +
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
                and price is not null
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
              and le.price is not null
            -- if (is currency) and (is not in chaos) and (is not in exalted), return FALSE,
            -- otherwise return TRUE. This restrict currency price calculation to only use
            -- entries listed in chaos to avoid circular dependencies. Eg exalted orbs are
            -- listed for divines and divines are listed in exalted orbs, causing a circular
            -- effect which messes up the prices.
              and !(foo4.id is not null &&
                le.id_price is not null &&
                le.id_price != (select id from data_itemData where name = 'Exalted Orb' limit 1))
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
    public boolean calcExalted() {
        String query = "UPDATE league_items AS i " +
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
     * Calculates the daily for items
     *
     * @return True on success
     */
    public boolean calcDaily() {
        String query = "update league_items as foo " +
                "left join ( " +
                "  select id_l, id_d, count(*) as count " +
                "  from league_entries " +
                "  where discovered > date_sub(now(), interval 24 hour) " +
                "  group by id_l, id_d " +
                ") as bar on foo.id_l = bar.id_l and foo.id_d = bar.id_d " +
                "set foo.daily = ifnull(bar.count, 0) ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Calculates the total for items
     *
     * @return True on success
     */
    public boolean calcTotal() {
        String query = "update league_items as foo " +
                "left join ( " +
                "  select id_l, id_d, count(*) as count " +
                "  from league_entries " +
                "  where discovered > date_sub(now(), interval 1 hour) " +
                "  group by id_l, id_d " +
                ") as bar on foo.id_l = bar.id_l and foo.id_d = bar.id_d " +
                "set foo.total = foo.total + ifnull(bar.count, 0) ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Calculates how many of each item there are currently on sale
     *
     * @return True on success
     */
    public boolean calcCurrent() {
        String query = "update league_items as li " +
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
     * Calculates spark data for items in table `league_items` based on history entries
     *
     * @return True on success
     */
    public boolean calcSpark() {
        String query = "UPDATE league_items AS i " +
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
