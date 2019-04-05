package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Parser.DbItemEntry;
import poe.Item.Parser.RawUsernameEntry;
import poe.Managers.League.BaseLeague;
import poe.Managers.Price.Bundles.ResultBundle;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class Upload {
    private static Logger logger = LoggerFactory.getLogger(Upload.class);
    private Database database;

    public Upload(Database database) {
        this.database = database;
    }

    public boolean uploadEntries(Set<DbItemEntry> set) {
        String query = "INSERT INTO league_entries (id_l, id_d, account_crc, stash_crc, item_crc, stack, price, id_price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "  updates = IF(price = VALUES(price) | stack = VALUES(stack) | id_price = VALUES(id_price), updates, updates + 1)," +
                "  updated = IF(price = VALUES(price) | stack = VALUES(stack) | id_price = VALUES(id_price), updated, now())," +
                "  stash_crc = VALUES(stash_crc), " +
                "  stack = VALUES(stack), " +
                "  price = VALUES(price), " +
                "  id_price = VALUES(id_price); ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (DbItemEntry raw : set) {
                    statement.setInt(1, raw.id_l);
                    statement.setInt(2, raw.id_d);
                    statement.setLong(3, raw.account_crc);
                    statement.setLong(4, raw.stash_crc);
                    statement.setLong(5, raw.item_crc);

                    if (raw.stackSize == null) {
                        statement.setNull(6, 0);
                    } else statement.setInt(6, raw.stackSize);

                    if (raw.price == null) {
                        statement.setNull(7, 0);
                        statement.setNull(8, 0);
                    } else {
                        String price = String.format("%.8f", raw.price).replace(',', '.');
                        statement.setString(7, price);

                        if (raw.id_price == null) {
                            statement.setNull(8, 0);
                        } else statement.setInt(8, raw.id_price);
                    }

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

    public boolean updateItem(ResultBundle result) {
        String query = "update league_items " +
                "set mean = ?, median = ?, mode = ?, `min` = ?, `max` = ?, accepted = ? " +
                "where id_l = ? and id_d = ? " +
                "limit 1; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setDouble(1, result.getMean());
                statement.setDouble(2, result.getMedian());
                statement.setDouble(3, result.getMode());
                statement.setDouble(4, result.getMin());
                statement.setDouble(5, result.getMax());
                statement.setDouble(6, result.getAccepted());
                statement.setInt(7, result.getIdBundle().getLeagueId());
                statement.setInt(8, result.getIdBundle().getItemId());
                statement.execute();
            }

            database.connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    public boolean uploadAccounts(Set<Long> set) {
        String query = "INSERT INTO league_accounts (account_crc) " +
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

    public boolean updateLeagueStates() {
        String[] queries = {
                // League ended
                "update data_leagues " +
                        "set active = 0 " +
                        "where active = 1 " +
                        "  and end is not null " +
                        "  and STR_TO_DATE(end, '%Y-%m-%dT%H:%i:%sZ') < now()"
        };

        return database.executeUpdateQueries(queries);
    }

    /**
     * Adds/updates league entries in table `data_leagues`
     *
     * @param leagueEntries List of LeagueEntry objects to upload
     * @return True on success
     */
    public boolean updateLeagues(List<BaseLeague> leagueEntries) {
        // Create league entry if it does not exist without incrementing the auto_increment value
        String query1 = "insert into data_leagues (name) " +
                "select ? from dual " +
                "where not exists (select 1 from data_leagues where name = ? limit 1);";

        // Update data for inserted league entry
        String query2 = "UPDATE data_leagues " +
                "SET    start     = ?, " +
                "       end       = ?, " +
                "       upcoming  = 0, " +
                "       active    = 1, " +
                "       event     = ?, " +
                "       hardcore  = ?, " +
                "       challenge = IF(id > 2, ?, 0) " +
                "WHERE  name      = ? " +
                "LIMIT  1; ";

        logger.info("Updating database leagues");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                for (BaseLeague league : leagueEntries) {
                    statement.setString(1, league.getName());
                    statement.setString(2, league.getName());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query2)) {
                for (BaseLeague league : leagueEntries) {
                    statement.setString(1, league.getStartAt());
                    statement.setString(2, league.getEndAt());
                    statement.setInt(3, league.isEvent() ? 1 : 0);
                    statement.setInt(4, league.isHardcore() ? 1 : 0);
                    statement.setInt(5, league.isEvent() ? 0 : 1);
                    statement.setString(6, league.getName());
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
