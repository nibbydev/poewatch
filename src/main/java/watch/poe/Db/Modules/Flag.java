package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.Collector;

import java.sql.*;
import java.util.Set;

public class Flag {
    private static Logger logger = LoggerFactory.getLogger(Flag.class);
    private Database database;

    public Flag(Database database) {
        this.database = database;
    }

    public boolean resetStashReferences(Set<Long> set) {
        String query =  "update league_entries " +
                        "set " +
                        "  stash_crc = NULL, " +
                        "  seen = now() " +
                        "where stash_crc = ?; ";
        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (long crc : set) {
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
}
