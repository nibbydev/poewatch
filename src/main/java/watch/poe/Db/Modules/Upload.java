package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.PriceManager;
import poe.Managers.StatisticsManager.ValueEntry;
import poe.Managers.StatisticsManager.StatType;
import poe.Worker.Entry.*;
import poe.Managers.League.LeagueEntry;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Upload {
    private static Logger logger = LoggerFactory.getLogger(Upload.class);
    private Database database;

    public Upload(Database database) {
        this.database = database;
    }

    public boolean uploadEntries(Set<RawItemEntry> set) {
        String query =  "INSERT INTO league_entries (id_l, id_d, account_crc, stash_crc, item_crc, id_price, price) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  updates = IF(price = VALUES(price), updates, updates + 1)," +
                        "  updated = IF(price = VALUES(price), updated, now())," +
                        "  price = VALUES(price), " +
                        "  id_price = VALUES(id_price), " +
                        "  stash_crc = VALUES(stash_crc); ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (RawItemEntry raw : set) {
                    // Convert price to string to avoid mysql truncation and rounding errors
                    String priceStr = Double.toString(raw.price);
                    int index = priceStr.indexOf('.');

                    if (priceStr.length() - index > 8) {
                        priceStr = priceStr.substring(0, index + 9);
                    }

                    statement.setInt(1, raw.id_l);
                    statement.setInt(2, raw.id_d);
                    statement.setLong(3, raw.account_crc);
                    statement.setLong(4, raw.stash_crc);
                    statement.setLong(5, raw.item_crc);

                    if (raw.id_price == null) {
                        statement.setNull(6, 0);
                    } else statement.setInt(6, raw.id_price);

                    statement.setString(7, priceStr);
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

    public boolean updateItems(Map<Integer, Map<Integer, PriceManager.Result>> results) {
        String query =  "update league_items " +
                        "set mean = ?, median = ?, mode = ?, `min` = ?, `max` = ?, `current` = ? " +
                        "where id_l = ? and id_d = ? " +
                        "limit 1; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (int id_l : results.keySet()) {
                    Map<Integer, PriceManager.Result> tmpMap = results.get(id_l);

                    for (int id_d : tmpMap.keySet()) {
                        PriceManager.Result result = tmpMap.get(id_d);

                        statement.setDouble(1, result.mean);
                        statement.setDouble(2, result.median);
                        statement.setDouble(3, result.mode);
                        statement.setDouble(4, result.min);
                        statement.setDouble(5, result.max);
                        statement.setDouble(6, result.current);
                        statement.setInt(7, id_l);
                        statement.setInt(8, id_d);

                        statement.addBatch();
                    }
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

    public boolean uploadAccounts(Set<Long> set) {
        String query =  "INSERT INTO league_accounts (account_crc) " +
                        "VALUES (?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  updates = updates + 1, " +
                        "  updated = now(); ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (Long crc : set) {
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
     * Adds/updates league entries in table `data_leagues`
     *
     * @param leagueEntries List of LeagueEntry objects to upload
     * @return True on success
     */
    public boolean updateLeagues(List<LeagueEntry> leagueEntries) {
        String query1 = "insert into data_leagues (`name`) " +
                        "select ? from dual " +
                        "where not exists ( " +
                        "  select 1 from data_leagues " +
                        "  where name = ? limit 1);";

        String query2 = "UPDATE data_leagues " +
                        "SET    start    = ?, " +
                        "       end      = ?, " +
                        "       upcoming = 0, " +
                        "       active   = 1, " +
                        "       event    = ?, " +
                        "       hardcore = ? " +
                        "WHERE  name     = ? " +
                        "LIMIT  1; ";

        logger.info("Updating database leagues");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getName());
                    statement.setString(2, leagueEntry.getName());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query2)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getStartAt());
                    statement.setString(2, leagueEntry.getEndAt());
                    statement.setInt(3, leagueEntry.isEvent() ? 1 : 0);
                    statement.setInt(4, leagueEntry.isHardcore() ? 1 : 0);
                    statement.setString(5, leagueEntry.getName());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            database.connection.commit();
            logger.info("Database leagues updated");
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            logger.error("Could not update database leagues");
            return false;
        }
    }

    /**
     * Updates the change ID in table `data_changeId`
     *
     * @param id Change ID to upload
     * @return True on success
     */
    public boolean updateChangeID(String id) {
        String query = "UPDATE data_changeId SET changeId = ?; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setString(1, id);
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
     * Uploads all statistics values to database
     *
     * @param values
     * @return True on success
     */
    public boolean uploadStatistics(Map<StatType, List<ValueEntry>> values) {
        String query =  "INSERT INTO data_statistics (type, time, value) VALUES (?, ?, ?); ";

        if (values == null || values.isEmpty()) {
            logger.error("Invalid map provided");
            return false;
        }

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (StatType type : values.keySet()) {
                    List<ValueEntry> valueList = values.get(type);

                    for (ValueEntry entry : valueList) {
                        statement.setString(1, type.name());
                        statement.setTimestamp(2, entry.getTime());

                        if (entry.getValue() == null) {
                            statement.setNull(3, 0);
                        } else statement.setLong(3, entry.getValue());

                        statement.addBatch();
                    }
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
     * Uploads gathered account and character names to the account database
     *
     * @param usernameSet Contains account, character and league name
     * @return True on success
     */
    public boolean uploadUsernames(Set<RawUsernameEntry> usernameSet) {
        String query1 = "INSERT INTO account_accounts   (name) VALUES (?) ON DUPLICATE KEY UPDATE seen = NOW(); ";
        String query2 = "INSERT INTO account_characters (name) VALUES (?) ON DUPLICATE KEY UPDATE seen = NOW(); ";

        String query3 = "INSERT INTO account_relations (id_l, id_a, id_c) " +
                        "SELECT ?, " +
                        "  (SELECT id FROM account_accounts   WHERE name = ? LIMIT 1), " +
                        "  (SELECT id FROM account_characters WHERE name = ? LIMIT 1) " +
                        "ON DUPLICATE KEY UPDATE seen = NOW(); ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            int counter;

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                counter = 0;

                for (RawUsernameEntry rawUsernameEntry : usernameSet) {
                    statement.setString(1, rawUsernameEntry.account);
                    statement.addBatch();

                    if (++counter % 500 == 0) statement.executeBatch();
                }

                statement.executeBatch();
            }


            try (PreparedStatement statement = database.connection.prepareStatement(query2)) {
                counter = 0;

                for (RawUsernameEntry rawUsernameEntry : usernameSet) {
                    statement.setString(1, rawUsernameEntry.character);
                    statement.addBatch();

                    if (++counter % 500 == 0) statement.executeBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query3)) {
                counter = 0;

                for (RawUsernameEntry rawUsernameEntry : usernameSet) {
                    statement.setInt(1, rawUsernameEntry.league);
                    statement.setString(2, rawUsernameEntry.account);
                    statement.setString(3, rawUsernameEntry.character);
                    statement.addBatch();

                    if (++counter % 500 == 0) statement.executeBatch();
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
}
