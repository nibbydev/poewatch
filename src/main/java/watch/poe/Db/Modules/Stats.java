package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.League.LeagueEntry;
import poe.Managers.PriceManager;
import poe.Managers.Stat.Collector;
import poe.Worker.Entry.RawItemEntry;
import poe.Worker.Entry.RawUsernameEntry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Stats {
    private static Logger logger = LoggerFactory.getLogger(Stats.class);
    private Database database;

    public Stats(Database database) {
        this.database = database;
    }

    /**
     * Deletes all tmp statistics from database
     *
     * @return True on success
     */
    public boolean truncateTmpStatistics() {
        return database.executeUpdateQueries("truncate data_statistics_tmp");
    }

    /**
     * Deletes expired statistics from database
     *
     * @param collectors
     * @return True on success
     */
    public boolean deleteTmpStatistics(Set<Collector> collectors) {
        String query = "delete from data_statistics_tmp where statType = ?; ";

        if (collectors == null) {
            logger.error("Invalid list provided");
            throw new RuntimeException();
        }

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (Collector collector : collectors) {
                    statement.setString(1, collector.getStatType().name());
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
     * Uploads all statistics values to database
     *
     * @param collectors
     * @return True on success
     */
    public boolean uploadStatistics(Set<Collector> collectors) {
        String query =  "INSERT INTO data_statistics (statType, time, value) VALUES (?, ?, ?); ";

        if (collectors == null) {
            logger.error("Invalid list provided");
            throw new RuntimeException();
        }

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (Collector collector : collectors) {
                    statement.setString(1, collector.getStatType().name());
                    statement.setTimestamp(2, new Timestamp(collector.getCreationTime()));

                    if (collector.getValue() == null) {
                        statement.setNull(3, 0);
                    } else statement.setInt(3, collector.getValue());

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
     * Uploads all statistics values to database
     *
     * @param collectors
     * @return True on success
     */
    public boolean uploadTempStatistics(Set<Collector> collectors) {
        String query =  "INSERT INTO data_statistics_tmp (statType, created, sum, count) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  created = VALUES(created), " +
                        "  count = VALUES(count), " +
                        "  sum = VALUES(sum); ";

        if (collectors == null) {
            logger.error("Invalid list provided");
            throw new RuntimeException();
        }

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (Collector collector : collectors) {
                    statement.setString(1, collector.getStatType().name());
                    statement.setLong(2, collector.getCreationTime());

                    if (collector.getSum() == null) {
                        statement.setNull(3, 0);
                    } else statement.setLong(3, collector.getSum());

                    statement.setInt(4, collector.getCount());

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
     * Deletes old stat entries from database
     *
     * @param collectors
     * @return True on success
     */
    public boolean trimStatHistory(Collector[] collectors) {
        String query =  "delete foo from data_statistics as foo " +
                        "join ( " +
                        "  select statType, time " +
                        "  from data_statistics " +
                        "  where statType = ? " +
                        "  order by time desc " +
                        "  limit ?, 1 " +
                        ") as bar on foo.statType = bar.statType " +
                        "where foo.time <= bar.time ";

        if (collectors == null) {
            logger.error("Invalid set provided");
            throw new RuntimeException();
        }

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (Collector collector : collectors) {
                    if (collector.getHistorySize() == null) {
                        continue;
                    }

                    statement.setString(1, collector.getStatType().name());
                    statement.setInt(2, collector.getHistorySize());
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
}
