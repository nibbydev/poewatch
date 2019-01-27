package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.StatisticsManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class History {
    private static Logger logger = LoggerFactory.getLogger(History.class);
    private Database database;

    public History(Database database) {
        this.database = database;
    }

    /**
     * Removes entries that exceed the specified capacity
     *
     * @return True on success
     */
    public boolean removeOldItemEntries() {
        String query =  "delete from league_entries " +
                        "where stash_crc is null " +
                        "and updated < subdate(now(), interval 7 day); ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Copies data from table `league_items` to table `league_history_hourly` every hour
     * on a rolling basis with a history of 24 hours
     *
     * @return True on success
     */
    public boolean addHourly() {
        String query =  "INSERT INTO league_history_hourly (id_l, id_d, inc) " +
                        "SELECT id_l, id_d, inc " +
                        "FROM league_items AS i " +
                        "JOIN data_leagues AS l ON i.id_l = l.id " +
                        "WHERE l.active = 1 ";

        return database.executeUpdateQueries(query);
    }

    /**
     * Copies data from table `league_items` to table `league_history_daily` every 24h
     * on a rolling basis
     *
     * @return True on success
     */
    public boolean addDaily() {
        String query =  "INSERT INTO league_history_daily " +
                        "  (id_l, id_d, mean, median, mode, min, max, exalted, total, daily, current, accepted) " +
                        "SELECT id_l, id_d, mean, median, mode, min, max, exalted, total, daily, current, accepted " +
                        "FROM league_items AS i " +
                        "JOIN data_leagues AS l ON i.id_l = l.id " +
                        "WHERE l.active = 1 ";

        return database.executeUpdateQueries(query);
    }
}
