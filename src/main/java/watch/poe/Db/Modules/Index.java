package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Item;
import poe.Item.Key;

import java.sql.*;

public class Index {
    private static Logger logger = LoggerFactory.getLogger(Index.class);
    private Database database;

    public Index(Database database) {
        this.database = database;
    }

    /**
     * Creates an item entry in table `league_items`
     *
     * @param id_l ID of item's league
     * @param id_d ID of item
     * @return True on success
     */
    public boolean createLeagueItem(int id_l, int id_d) {
        String query =  "INSERT INTO league_items (id_l, id_d) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE id_l = id_l; ";
        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
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
            return false;
        }
    }

    /**
     * Creates an item data entry in table `data_itemData`
     *
     * @param item Item object to index
     * @return ID of created item data entry on success, null on failure
     */
    public Integer indexItemData(Item item) {
        String query =  "INSERT INTO data_itemData (" +
                        "  id_cat, id_grp, `name`, `type`, frame, stack, " +
                        "  tier, lvl,  quality, corrupted, links, ilvl, var, icon) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE id = id; ; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            Key key = item.getKey();

            try (PreparedStatement statement = database.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, item.getCategory().ordinal());

                if (item.getGroup() == null) statement.setNull(2, 0);
                else statement.setInt(2, item.getGroup().ordinal());

                statement.setString(3, key.getName());
                statement.setString(4, key.getTypeLine());
                statement.setInt(5, key.getFrameType());

                if (item.getMaxStackSize() == null) {
                    statement.setNull(6, 0);
                } else statement.setInt(6, item.getMaxStackSize());

                if (key.getTier() == null) {
                    statement.setNull(7, 0);
                } else statement.setInt(7, key.getTier());

                if (key.getLevel() == null) {
                    statement.setNull(8, 0);
                } else statement.setInt(8, key.getLevel());

                if (key.getQuality() == null) {
                    statement.setNull(9, 0);
                } else statement.setInt(9, key.getQuality());

                if (key.getCorrupted() == null) {
                    statement.setNull(10, 0);
                } else statement.setBoolean(10, key.getCorrupted());

                if (key.getLinks() == null) {
                    statement.setNull(11, 0);
                } else statement.setInt(11, key.getLinks());

                if (key.getiLvl() == null) {
                    statement.setNull(12, 0);
                } else statement.setInt(12, key.getiLvl());

                statement.setString(13, key.getVariation());
                statement.setString(14, item.formatIconURL());

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

    public boolean reindexItemData(int id_d, Item item) {
        String query =  "update data_itemData set " +
                        "  id_cat = ?, id_grp = ?, name = ?, type = ?, reindex = 0, " +
                        "  frame = ?, stack = ?, tier = ?, lvl = ?, quality = ?," +
                        "  corrupted = ?, links = ?, ilvl = ?, var = ?, icon = ? " +
                        "where id = ? " +
                        "limit 1 ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            Key key = item.getKey();

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, item.getCategory().ordinal());
                statement.setInt(2, item.getGroup().ordinal());
                statement.setString(3, key.getName());
                statement.setString(4, key.getTypeLine());
                statement.setInt(5, key.getFrameType());

                if (item.getMaxStackSize() == null) {
                    statement.setNull(6, 0);
                } else statement.setInt(6, item.getMaxStackSize());

                if (key.getTier() == null) {
                    statement.setNull(7, 0);
                } else statement.setInt(7, key.getTier());

                if (key.getLevel() == null) {
                    statement.setNull(8, 0);
                } else statement.setInt(8, key.getLevel());

                if (key.getQuality() == null) {
                    statement.setNull(9, 0);
                } else statement.setInt(9, key.getQuality());

                if (key.getCorrupted() == null) {
                    statement.setNull(10, 0);
                } else statement.setBoolean(10, key.getCorrupted());

                if (key.getLinks() == null) {
                    statement.setNull(11, 0);
                } else statement.setInt(11, key.getLinks());

                if (key.getiLvl() == null) {
                    statement.setNull(12, 0);
                } else statement.setInt(12, key.getiLvl());

                statement.setString(13, key.getVariation());
                statement.setString(14, item.formatIconURL());
                statement.setInt(15, id_d);

                statement.executeUpdate();
                database.connection.commit();
                return true;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
