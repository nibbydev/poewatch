package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Category.CategoryEnum;
import poe.Item.Category.GroupEnum;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Setup {
    private static Logger logger = LoggerFactory.getLogger(Setup.class);
    private Database database;

    public Setup(Database database) {
        this.database = database;
    }

    /**
     * Get a list of unique category names
     */
    private boolean getCategories(ArrayList<TmpGrouping> categories) {
        String query = "SELECT id, name from data_categories; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    categories.add(new TmpGrouping() {{
                        id = resultSet.getInt("id");
                        name = resultSet.getString("name");
                    }});
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Get a list of unique group names
     */
    private boolean getGroups(ArrayList<TmpGrouping> groups) {
        String query = "SELECT id, name from data_groups; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    groups.add(new TmpGrouping() {{
                        id = resultSet.getInt("id");
                        name = resultSet.getString("name");
                    }});
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Uploads all missing categories to the database
     */
    public boolean verifyCategories() {
        // Get current categories from the database
        ArrayList<TmpGrouping> categories = new ArrayList<>();
        if (!getCategories(categories)) {
            return false;
        }

        // Find entries where name matches but ID doesn't
        List<CategoryEnum> invalidIds = Arrays.stream(CategoryEnum.values())
                .filter(t -> categories.stream().anyMatch(u -> u.name.equals(t.getName())))
                .filter(t -> categories.stream().noneMatch(u -> u.id == t.getId()))
                .collect(Collectors.toList());

        // If there were any invalid IDs
        if (!invalidIds.isEmpty()) {
            invalidIds.forEach(t -> logger.error("Invalid ID declared in database for category '" + t.getName() + "'"));
            return false;
        }

        // Filter categories that are not in the database
        List<CategoryEnum> missingCategories = Arrays.stream(CategoryEnum.values())
                .filter(t -> categories.stream().noneMatch(u -> u.name.equals(t.getName())))
                .collect(Collectors.toList());


        // If there were any missing categories
        if (!missingCategories.isEmpty()) {
            missingCategories.forEach(t -> logger.warn("Database was missing category '" + t.getName() + "'"));
        }

        String query = "INSERT INTO data_categories (id, name, display) VALUES (?, ?, ?); ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (CategoryEnum categoryEnum : missingCategories) {
                    statement.setInt(1, categoryEnum.getId());
                    statement.setString(2, categoryEnum.getName());
                    statement.setString(3, categoryEnum.getDisplay());
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
     * Uploads all missing groups to the database
     */
    public boolean verifyGroups() {
        // Get current groups from the database
        ArrayList<TmpGrouping> groups = new ArrayList<>();
        if (!getGroups(groups)) {
            return false;
        }

        // Find entries where name matches but ID doesn't
        List<GroupEnum> invalidIds = Arrays.stream(GroupEnum.values())
                .filter(t -> groups.stream().anyMatch(u -> u.name.equals(t.getName())))
                .filter(t -> groups.stream().noneMatch(u -> u.id == t.getId()))
                .collect(Collectors.toList());

        // If there were any invalid IDs
        if (!invalidIds.isEmpty()) {
            invalidIds.forEach(t -> logger.error("Invalid ID declared in database for group '" + t.getName() + "'"));
            return false;
        }

        // Filter groups that are not in the database
        List<GroupEnum> missingGroups = Arrays.stream(GroupEnum.values())
                .filter(t -> groups.stream().noneMatch(u -> u.name.equals(t.getName())))
                .collect(Collectors.toList());

        // If there were any missing groups
        if (!missingGroups.isEmpty()) {
            missingGroups.forEach(t -> logger.warn("Database was missing group '" + t.getName() + "'"));
        }

        String query = "INSERT INTO data_groups (id, name, display) VALUES (?, ?, ?); ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                for (GroupEnum groupEnum : missingGroups) {
                    statement.setInt(1, groupEnum.getId());
                    statement.setString(2, groupEnum.getName());
                    statement.setString(3, groupEnum.getDisplay());
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
     * Just for returning two values from a method
     */
    private class TmpGrouping {
        public String name;
        public int id;
    }
}

