package poe.Database.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Database.Database;
import poe.Price.Bundles.EntryBundle;
import poe.Price.Bundles.IdBundle;
import poe.Price.Bundles.PriceBundle;

import java.sql.*;
import java.util.Set;

public class Calc {
    private static Logger logger = LoggerFactory.getLogger(Calc.class);
    private Database database;

    public Calc(Database database) {
        this.database = database;
    }

    /**
     * Queries a list of league+item pairs that need to have their prices recalculated
     *
     * @param idBundles Empty list to be filled
     * @param since Timestamp of last query
     * @return True on success
     */
    public boolean getIdBundles(Set<IdBundle> idBundles, Timestamp since) {
        if (idBundles == null || !idBundles.isEmpty()) {
            throw new RuntimeException("Invalid list provided");
        }

        String query =  "select b.id_l, b.id_d, li.mean, li.daily, did.id_grp " +
                        "from league_items as li " +
                        "join ( " +
                        "  select distinct id_l, id_d " +
                        "  from league_entries " +
                        "  where stash_crc is not null " +
                        "    and price is not null " +
                        "    and updated > ? " +
                        ") as b on li.id_l = b.id_l and li.id_d = b.id_d " +
                        "join data_item_data as did on did.id = b.id_d;";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setTimestamp(1, since);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    IdBundle ib = new IdBundle();

                    ib.setLeagueId(resultSet.getInt(1));
                    ib.setItemId(resultSet.getInt(2));
                    ib.setPrice(resultSet.getDouble(3));
                    ib.setDaily(resultSet.getInt(4));
                    ib.setGroup(resultSet.getInt(5));

                    idBundles.add(ib);
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }


    /**
     * Queries currency rates from the database
     *
     * @return True on success
     */
    public boolean getPriceBundles(Set<PriceBundle> priceBundles) {
        String query = "select li.id_l, li.id_d, li.mean " +
                "from league_items as li " +
                "join data_item_data as did on li.id_d = did.id " +
                "where did.id_cat = 4 " + // currency category
                "  and did.id_grp = 11 " + // currency group
                "  and did.frame = 5 " + // currency frame type
                "  and li.mean > 0 "; // actually has a price

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    priceBundles.add(new PriceBundle(
                            resultSet.getInt(1),
                            resultSet.getInt(2),
                            resultSet.getDouble(3)
                    ));
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Queries entries for the specified item
     *
     * @return True on success
     */
    public boolean getEntryBundles(Set<EntryBundle> entryBundles, IdBundle idBundle, int maxAge) {
        String query = "select le.id_a, le.price, le.id_price " +
                "from league_entries as le " +
                "join league_accounts as la " +
                "  on le.id_a = la.id " +
                "where le.id_l = ? " +
                "  and le.id_d = ? " +
                "  and la.seen > date_sub(now(), interval ? hour) " +
                "  and le.stash_crc is not null " +
                "  and le.price is not null";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, idBundle.getLeagueId());
                statement.setInt(2, idBundle.getItemId());
                statement.setInt(3, maxAge);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    EntryBundle eb = new EntryBundle();

                    eb.setAccountId(resultSet.getLong("id_a"));
                    eb.setPrice(resultSet.getDouble("price"));

                    eb.setCurrencyId(resultSet.getInt("id_price"));
                    if (resultSet.wasNull()) {
                        eb.setCurrencyId(null);
                    }

                    entryBundles.add(eb);
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
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
                "  JOIN data_item_data AS did ON i.id_d = did.id " +
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
