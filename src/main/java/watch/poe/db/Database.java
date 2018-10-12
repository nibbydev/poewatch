package poe.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Config;
import poe.db.modules.*;
import java.sql.*;

public class Database {
    private static Logger logger = LoggerFactory.getLogger(Database.class);

    public History history = new History(this);
    public Account account = new Account(this);
    public Upload upload = new Upload(this);
    public Index index = new Index(this);
    public Flag flag = new Flag(this);
    public Init init = new Init(this);
    public Calc calc = new Calc(this);

    public Connection connection;

    /**
     * Initializes connection to the MySQL database
     *
     * @return True on success
     */
    public boolean connect() {
        try {
            connection = DriverManager.getConnection(Config.db_address, Config.db_username, Config.getDb_password());
            connection.setCatalog(Config.db_database);
            connection.setAutoCommit(false);

            return true;
        } catch (SQLException ex) {
            logger.error(String.format("Failed to connect to database ex=%s", ex.getMessage()), ex);
        }

        return false;
    }

    /**
     * Disconnects from the MySQL database
     */
    public void disconnect() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ex) {
            logger.error(String.format("Failed to disconnect from database ex=%s", ex.getMessage()), ex);
        }
    }

    /**
     * Execute a update queries (each given as a String)
     *
     * @param queries Query update strings to execute
     * @return True if success, else false
     */
    public boolean executeUpdateQueries(String... queries) {
        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                for (String query : queries) {
                    //todo: add a new pojo that can represent the statement params - (index, value, type) & apply here
                    statement.executeUpdate(query);
                }
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
