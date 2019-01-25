package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.Collector;
import poe.Managers.Stat.StatType;
import poe.Managers.StatisticsManager;

import java.sql.*;
import java.util.Arrays;
import java.util.Set;

public class Stats {
    private static Logger logger = LoggerFactory.getLogger(Stats.class);
    private Database database;

    public Stats(Database database) {
        this.database = database;
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
                    statement.setTimestamp(2, new Timestamp(collector.getInsertTime()));

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
                    statement.setTimestamp(2, new Timestamp(collector.getCreationTime()));

                    if (collector.isValueNull()) {
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


    public boolean countActiveAccounts(StatisticsManager statisticsManager) {
        String query =  "select count(*) from account_accounts where seen > date_sub(now(), interval 1 hour)  ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                // Get first and only entry
                if (resultSet.next()) {
                    statisticsManager.addValue(StatType.COUNT_ACTIVE_ACCOUNTS, resultSet.getInt(1));
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return false;
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


    public boolean getTmpStatistics(Collector[] collectors) {
        String query = "SELECT * FROM data_statistics_tmp; ";

        if (collectors == null) {
            logger.error("Provided list was null");
            throw new RuntimeException();
        }

        logger.info("Getting statistics from database");

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    StatType statType = StatType.valueOf(resultSet.getString("statType"));

                    // Find first collector
                    Collector collector = Arrays.stream(collectors)
                            .filter(i -> i.getStatType().equals(statType))
                            .findFirst()
                            .orElse(null);

                    // If it didn't exist
                    if (collector == null) {
                        logger.error("The specified collector could not be found");
                        continue;
                    }

                    collector.setCount(resultSet.getInt("count"));

                    collector.setSum(resultSet.getLong("sum"));
                    if (resultSet.wasNull()) collector.setSum(null);

                    collector.setCreationTime(resultSet.getTimestamp("created").getTime());
                }
            }

            logger.info("Got statistics from database");
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            logger.error("Could not get statistics from database");
            return false;
        }
    }
}
