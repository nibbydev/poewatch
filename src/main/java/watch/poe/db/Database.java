package poe.db;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.modules.*;
import java.sql.*;
import java.util.List;

public class Database {
    private static Logger logger = LoggerFactory.getLogger(Database.class);
    public Config config;

    public History history = new History(this);
    public Account account = new Account(this);
    public Upload upload = new Upload(this);
    public Index index = new Index(this);
    public Flag flag = new Flag(this);
    public Init init = new Init(this);
    public Calc calc = new Calc(this);

    public Connection connection;
    private String address;

    public Database(Config config) {
        this.config = config;
    }

    /**
     * Initializes connection to the MySQL database
     *
     * @return True on success
     */
    public boolean connect() {
        StringBuilder address = new StringBuilder();
        address.append(config.getString("database.address"));

        List<String> parameters = config.getStringList("database.args");
        if (parameters != null && !parameters.isEmpty()) {
            address.append("?");

            for (String param : parameters) {
                address.append(param);
                address.append("&");
            }

            address.deleteCharAt(address.lastIndexOf("&"));
        }

        try {
            connection = DriverManager.getConnection(
                    address.toString(),
                    config.getString("database.username"),
                    config.getString("database.password"));
            connection.setCatalog(config.getString("database.database"));
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
