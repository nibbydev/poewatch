package poe.db.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.entry.item.Item;
import poe.manager.entry.item.Key;

import java.sql.*;

public class Index {
    private static Logger logger = LoggerFactory.getLogger(Index.class);
    private Database database;

    public Index(Database database) {
        this.database = database;
    }

    /**
     * Creates a parent category entry in table `category_parent`
     *
     * @param parentName Name of parent category
     * @return ID of created category on success, null on failure
     */
    public Integer addParentCategory(String parentName) {
        String query1 = "INSERT INTO category_parent (name) VALUES (?); ";

        String query2 = "SELECT id " +
                        "FROM   category_parent " +
                        "WHERE  name = ? " +
                        "LIMIT  1;";

        try {
            if (database.connection.isClosed()) {
                return null;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                statement.setString(1, parentName);
                statement.executeUpdate();
            }

            database.connection.commit();

            try (PreparedStatement statement = database.connection.prepareStatement(query2)) {
                statement.setString(1, parentName);
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Creates a child category entry in table `category_child`
     *
     * @param parentId  ID of child's parent category
     * @param childName Name of child category
     * @return ID of created category on success, null on failure
     */
    public Integer addChildCategory(Integer parentId, String childName) {
        String query1 = "INSERT INTO category_child (id_cp, name) VALUES (?, ?)";

        String query2 = "SELECT id " +
                        "FROM   category_child " +
                        "WHERE  id_cp = ? " +
                        "  AND  name = ? " +
                        "LIMIT  1; ";

        try {
            if (database.connection.isClosed()) {
                return null;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                statement.setInt(1, parentId);
                statement.setString(2, childName);
                statement.executeUpdate();
            }

            database.connection.commit();

            try (PreparedStatement statement = database.connection.prepareStatement(query2)) {
                statement.setInt(1, parentId);
                statement.setString(2, childName);
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Creates an item entry in table `league_items_rolling`
     *
     * @param leagueId ID of item's league
     * @param dataId   ID of item
     * @return True on success
     */
    public boolean createLeagueItem(Integer leagueId, Integer dataId) {
        String query =  "INSERT INTO league_items_rolling (id_l, id_d) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE id_l = id_l; ";
        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, leagueId);
                statement.setInt(2, dataId);
                statement.executeUpdate();
            }

            database.connection.commit();
            return true;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Creates an item data entry in table `data_itemData`
     *
     * @param item             Item object to index
     * @param parentCategoryId ID of item's parent category
     * @param childCategoryId  ID of item's child category
     * @return ID of created item data entry on success, null on failure
     */
    public Integer indexItemData(Item item, Integer parentCategoryId, Integer childCategoryId) {
        String query1 = "INSERT INTO data_itemData (" +
                        "  id_cp, id_cc, name, type, frame, tier, lvl, " +
                        "  quality, corrupted, links, ilvl, var, icon) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";
        String query2 = "SELECT LAST_INSERT_ID(); ";

        try {
            if (database.connection.isClosed()) {
                return null;
            }

            Key key = item.getKey();

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                statement.setInt(1, parentCategoryId);

                if (childCategoryId == null) statement.setNull(2, 0);
                else statement.setInt(2, childCategoryId);

                statement.setString(3, key.getName());
                statement.setString(4, key.getTypeLine());
                statement.setInt(5, key.getFrameType());

                if (key.getTier() == null) {
                    statement.setNull(6, 0);
                } else statement.setInt(6, key.getTier());

                if (key.getLevel() == null) {
                    statement.setNull(7, 0);
                } else statement.setInt(7, key.getLevel());

                if (key.getQuality() == null) {
                    statement.setNull(8, 0);
                } else statement.setInt(8, key.getQuality());

                if (key.getCorrupted() == null) {
                    statement.setNull(9, 0);
                } else statement.setInt(9, key.getCorrupted());

                if (key.getLinks() == null) {
                    statement.setNull(10, 0);
                } else statement.setInt(10, key.getLinks());

                if (key.getiLvl() == null) {
                    statement.setNull(11, 0);
                } else statement.setInt(11, key.getiLvl());

                statement.setString(12, key.getVariation());
                statement.setString(13, Item.formatIconURL(item.getIcon()));

                statement.executeUpdate();
            }

            database.connection.commit();

            try (Statement statement = database.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query2);
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }
}
