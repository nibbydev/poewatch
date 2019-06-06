package poe.Db.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Item;
import poe.Item.Key;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
        String query = "INSERT INTO league_items (id_l, id_d) VALUES (?, ?) " +
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
     * Creates an item data entry
     *
     * @param item Item object to index
     * @return ID of created item data entry on success, null on failure
     */
    public Integer indexItemData(Item item) {
        String query = "INSERT INTO data_item_data (" +
                "  id_cat, id_grp, `name`, `type`, frame, stack, map_tier, map_series, base_shaper, base_elder, " +
                "  enchant_min, enchant_max, gem_lvl, gem_quality, gem_corrupted, links, base_level, var, icon) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE id = id; ; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            Key key = item.getKey();

            try (PreparedStatement statement = database.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, item.getCategory().getId());
                statement.setInt(2, item.getGroup().getId());

                statement.setString(3, key.getName());
                statement.setString(4, key.getTypeLine());
                statement.setInt(5, key.getFrameType());

                if (item.getMaxStackSize() == null) {
                    statement.setNull(6, 0);
                } else statement.setInt(6, item.getMaxStackSize());

                if (key.getTier() == null) {
                    statement.setNull(7, 0);
                } else statement.setInt(7, key.getTier());

                if (key.getSeries() == null) {
                    statement.setNull(8, 0);
                } else statement.setInt(8, key.getSeries());

                if (key.getShaper() == null) {
                    statement.setNull(9, 0);
                } else statement.setBoolean(9, key.getShaper());

                if (key.getElder() == null) {
                    statement.setNull(10, 0);
                } else statement.setBoolean(10, key.getElder());

                if (key.getEnchantMin() == null) {
                    statement.setNull(11, 0);
                } else statement.setFloat(11, key.getEnchantMin());

                if (key.getEnchantMax() == null) {
                    statement.setNull(12, 0);
                } else statement.setFloat(12, key.getEnchantMax());

                if (key.getLevel() == null) {
                    statement.setNull(13, 0);
                } else statement.setInt(13, key.getLevel());

                if (key.getQuality() == null) {
                    statement.setNull(14, 0);
                } else statement.setInt(14, key.getQuality());

                if (key.getCorrupted() == null) {
                    statement.setNull(15, 0);
                } else statement.setBoolean(15, key.getCorrupted());

                if (key.getLinks() == null) {
                    statement.setNull(16, 0);
                } else statement.setInt(16, key.getLinks());

                if (key.getiLvl() == null) {
                    statement.setNull(17, 0);
                } else statement.setInt(17, key.getiLvl());

                statement.setString(18, key.getVariation());
                statement.setString(19, item.formatIconURL());

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

    /**
     * Updates item data entry in database. Only immutable field is the id
     */
    public boolean reindexItemData(int id_d, Item item) {
        String query = "update data_item_data set " +
                "  id_cat = ?, id_grp = ?, name = ?, type = ?, reindex = 0, " +
                "  frame = ?, stack = ?, map_tier = ?, map_series = ?, base_shaper = ?, base_elder = ?," +
                "  enchant_min = ?, enchant_max = ?, gem_lvl = ?, gem_quality = ?," +
                "  gem_corrupted = ?, links = ?, base_level = ?, var = ?, icon = ? " +
                "where id = ? " +
                "limit 1 ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            Key key = item.getKey();

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                statement.setInt(1, item.getCategory().getId());
                statement.setInt(2, item.getGroup().getId());
                statement.setString(3, key.getName());
                statement.setString(4, key.getTypeLine());
                statement.setInt(5, key.getFrameType());

                if (item.getMaxStackSize() == null) {
                    statement.setNull(6, 0);
                } else statement.setInt(6, item.getMaxStackSize());

                if (key.getTier() == null) {
                    statement.setNull(7, 0);
                } else statement.setInt(7, key.getTier());

                if (key.getSeries() == null) {
                    statement.setNull(8, 0);
                } else statement.setInt(8, key.getSeries());

                if (key.getShaper() == null) {
                    statement.setNull(9, 0);
                } else statement.setBoolean(9, key.getShaper());

                if (key.getElder() == null) {
                    statement.setNull(10, 0);
                } else statement.setBoolean(10, key.getElder());

                if (key.getEnchantMin() == null) {
                    statement.setNull(11, 0);
                } else statement.setFloat(11, key.getEnchantMin());

                if (key.getEnchantMax() == null) {
                    statement.setNull(12, 0);
                } else statement.setFloat(12, key.getEnchantMax());

                if (key.getLevel() == null) {
                    statement.setNull(13, 0);
                } else statement.setInt(13, key.getLevel());

                if (key.getQuality() == null) {
                    statement.setNull(14, 0);
                } else statement.setInt(14, key.getQuality());

                if (key.getCorrupted() == null) {
                    statement.setNull(15, 0);
                } else statement.setBoolean(15, key.getCorrupted());

                if (key.getLinks() == null) {
                    statement.setNull(16, 0);
                } else statement.setInt(16, key.getLinks());

                if (key.getiLvl() == null) {
                    statement.setNull(17, 0);
                } else statement.setInt(17, key.getiLvl());

                statement.setString(18, key.getVariation());
                statement.setString(19, item.formatIconURL());
                statement.setInt(20, id_d);

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
