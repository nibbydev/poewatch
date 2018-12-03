package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.entry.RawEntry;
import poe.manager.entry.StatusElement;
import poe.manager.entry.timer.Timer;
import poe.manager.entry.timer.TimerList;
import poe.manager.league.LeagueEntry;

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

    /**
     * Adds an item entry in table `league_entries`
     *
     * @param entrySet Set of RawEntry objects to upload
     * @return True on success
     */
    public boolean uploadRaw(Set<RawEntry> entrySet) {
        String query =  "INSERT INTO league_entries ( " +
                        "  id, id_l, id_d, price, account) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  approved = 0, " +
                        "  price = VALUES(price) ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            int count = 0;

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (RawEntry rawEntry : entrySet) {
                    statement.setLong(1, rawEntry.getId());
                    statement.setInt(2, rawEntry.getLeagueId());
                    statement.setInt(3, rawEntry.getItemId());
                    statement.setString(4, rawEntry.getPrice());
                    statement.setString(5, rawEntry.getAccountName());
                    statement.addBatch();

                    if (++count % 100 == 0) statement.executeBatch();
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
        String query1 = "INSERT INTO " +
                        "  data_leagues (name) " +
                        "SELECT ? " +
                        "FROM   DUAL " +
                        "WHERE  NOT EXISTS ( " +
                        "  SELECT 1 " +
                        "  FROM   data_leagues " +
                        "  WHERE  name = ? " +
                        "  LIMIT  1); ";

        String query2 = "UPDATE data_leagues " +
                        "SET    start    = ?, " +
                        "       end      = ?, " +
                        "       upcoming = 0, " +
                        "       active   = 1, " +
                        "       event    = ?," +
                        "       hardcore = ? " +
                        "WHERE  name     = ? " +
                        "LIMIT  1; ";

        try {
            if (database.connection.isClosed()) {
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
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
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
        String query = "UPDATE data_changeId SET changeId = ?, time = CURRENT_TIMESTAMP; ";

        try {
            if (database.connection.isClosed()) {
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
     * Uploads all latest timer delays to database
     *
     * @param timeLog Valid map of key - TimerList relations
     * @return True on success
     */
    public boolean uploadTimers(Map<String, TimerList> timeLog, StatusElement statusElement) {
        String query1 = "DELETE  del " +
                        "FROM    data_timers AS del " +
                        "JOIN ( " +
                        "  SELECT   `key`, ( " +
                        "    SELECT   t.time " +
                        "    FROM     data_timers AS t " +
                        "    WHERE    t.`key` = d.`key` " +
                        "    ORDER BY t.time DESC " +
                        "    LIMIT    4, 1 " +
                        "  ) AS     time " +
                        "  FROM     data_timers AS d " +
                        "  GROUP BY d.`key` " +
                        "  HAVING   time IS NOT NULL " +
                        ") AS    tmp " +
                        "  ON    del.`key` = tmp.`key` " +
                        "    AND del.time <= tmp.time; ";

        String query =  "INSERT INTO data_timers (`key`, type, delay) " +
                        "VALUES (?, ?, ?)  ";

        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                statement.executeUpdate(query1);
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (String key : timeLog.keySet()) {
                    TimerList timerList = timeLog.get(key);

                    if (timerList.type.equals(Timer.TimerType.NONE)) {
                        continue;
                    } else if (timerList.list.isEmpty()) {
                        continue;
                    }

                    // Very nice
                    if (!statusElement.isTenBool() || !statusElement.isSixtyBool() || !statusElement.isTwentyFourBool()) {
                        if (timerList.type.equals(Timer.TimerType.TEN)) continue;
                        if (timerList.type.equals(Timer.TimerType.SIXTY)) continue;
                        if (timerList.type.equals(Timer.TimerType.TWENTY)) continue;
                    } else if (statusElement.isTenBool()) {
                        if (timerList.type.equals(Timer.TimerType.SIXTY)) continue;
                        if (timerList.type.equals(Timer.TimerType.TWENTY)) continue;
                    } else if (statusElement.isSixtyBool()) {
                        if (timerList.type.equals(Timer.TimerType.TWENTY)) continue;
                    }

                    Integer type = Timer.translate(timerList.type);

                    statement.setString(1, key);
                    if (type == null) statement.setNull(2, 0);
                    else statement.setInt(2, type);
                    statement.setLong(3, timerList.list.get(timerList.list.size() - 1));

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
