package poe.DB.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.DB.Database;
import poe.Managers.Entry.item.Item;
import poe.Managers.Entry.item.Key;

import java.sql.*;

public class Index {
    private static Logger logger = LoggerFactory.getLogger(Index.class);
    private Database database;

    public Index(Database database) {
        this.database = database;
    }

    /**
     * Creates a category entry in table `data_categories`
     *
     * @param categoryName Name of category
     * @return ID of created category on success, null on failure
     */
    public Integer createCategory(String categoryName) {
        String query1 = "INSERT INTO data_categories (`name`) VALUES (?); ";
        String query2 = "SELECT id FROM data_categories WHERE `name` = ? LIMIT 1;";

        try {
            if (database.connection.isClosed()) {
                return null;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                statement.setString(1, categoryName);
                statement.executeUpdate();
            }

            database.connection.commit();

            try (PreparedStatement statement = database.connection.prepareStatement(query2)) {
                statement.setString(1, categoryName);
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Creates a group entry in table `data_groups`
     *
     * @param categoryId  ID of group's category
     * @param groupName Name of group
     * @return ID of created category on success, null on failure
     */
    public Integer addGroup(int categoryId, String groupName) {
        String query1 = "INSERT INTO data_groups (id_cat, `name`) VALUES (?, ?)";
        String query2 = "SELECT id FROM data_groups WHERE id_cat = ? AND `name` = ? LIMIT 1; ";

        try {
            if (database.connection.isClosed()) {
                return null;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query1)) {
                statement.setInt(1, categoryId);
                statement.setString(2, groupName);
                statement.executeUpdate();
            }

            database.connection.commit();

            try (PreparedStatement statement = database.connection.prepareStatement(query2)) {
                statement.setInt(1, categoryId);
                statement.setString(2, groupName);
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Creates an item entry in table `league_items`
     *
     * @param id_l ID of item's league
     * @param id_d ID of item
     * @return True on success
     */
    public boolean createLeagueItem(int id_l, int id_d) {
        String query =  "INSERT INTO league_items (id_l, id_d) " +
                        "VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE id_l = id_l; ";
        try {
            if (database.connection.isClosed()) {
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, id_l);
                statement.setInt(2, id_d);
                statement.executeUpdate();
            }

            database.connection.commit();
            return true;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            System.out.printf("%d -- %d\n", id_l, id_d); // todo:remove
            return false;
        }
    }

    /**
     * Creates an item data entry in table `data_itemData`
     *
     * @param item Item object to index
     * @param categoryId ID of item's category
     * @param groupId  ID of item's group
     * @return ID of created item data entry on success, null on failure
     */
    public Integer indexItemData(Item item, Integer categoryId, Integer groupId) {
        String query =  "INSERT INTO data_itemData (" +
                        "  id_cat, id_grp, `name`, `type`, frame, tier, lvl, " +
                        "  quality, corrupted, links, ilvl, var, icon) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";

        try {
            if (database.connection.isClosed()) {
                return null;
            }

            Key key = item.getKey();

            try (PreparedStatement statement = database.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, categoryId);

                if (groupId == null) statement.setNull(2, 0);
                else statement.setInt(2, groupId);

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
                database.connection.commit();

                ResultSet resultSet = statement.getGeneratedKeys();
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }
}
