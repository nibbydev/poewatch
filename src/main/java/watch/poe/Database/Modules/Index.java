package poe.Database.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Database.Database;
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
                "  id_cat, id_grp, `name`, `type`, frame, stack, map_tier, map_series, shaper, elder, crusader, redeemer, hunter, warlord, " +
                "  enchant_min, enchant_max, gem_lvl, gem_quality, gem_corrupted, links, base_level, var, icon) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE id = id; ; ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return null;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                fillItemDataStatement(statement, item);

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
                "  frame = ?, stack = ?, map_tier = ?, map_series = ?, shaper = ?, " +
                "  elder = ?, crusader = ?, redeemer = ?, hunter = ?, warlord = ?, " +
                "  enchant_min = ?, enchant_max = ?, gem_lvl = ?, gem_quality = ?," +
                "  gem_corrupted = ?, links = ?, base_level = ?, var = ?, icon = ? " +
                "where id = ? " +
                "limit 1 ";

        try {
            if (database.connection.isClosed()) {
                logger.error("Database connection was closed");
                return false;
            }

            try (PreparedStatement statement = database.connection.prepareStatement(query)) {
                fillItemDataStatement(statement, item);
                statement.setInt(24, id_d);

                statement.executeUpdate();
                database.connection.commit();
                return true;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    private void fillItemDataStatement(PreparedStatement statement, Item item) throws SQLException {
        Key key = item.getKey();

        statement.setInt(1, item.getCategory().getId());
        statement.setInt(2, item.getGroup().getId());
        statement.setString(3, key.name);
        statement.setString(4, key.type);
        statement.setInt(5, key.frame);

        if (item.getMaxStackSize() == null) {
            statement.setNull(6, 0);
        } else statement.setInt(6, item.getMaxStackSize());

        if (key.mapTier == null) {
            statement.setNull(7, 0);
        } else statement.setInt(7, key.mapTier);

        if (key.mapSeries == null) {
            statement.setNull(8, 0);
        } else statement.setInt(8, key.mapSeries);

        if (key.shaper == null) {
            statement.setNull(9, 0);
        } else statement.setBoolean(9, key.shaper);

        if (key.elder == null) {
            statement.setNull(10, 0);
        } else statement.setBoolean(10, key.elder);

        if (key.crusader == null) {
            statement.setNull(11, 0);
        } else statement.setBoolean(11, key.crusader);

        if (key.redeemer == null) {
            statement.setNull(12, 0);
        } else statement.setBoolean(12, key.redeemer);

        if (key.hunter == null) {
            statement.setNull(13, 0);
        } else statement.setBoolean(13, key.hunter);

        if (key.warlord == null) {
            statement.setNull(14, 0);
        } else statement.setBoolean(14, key.warlord);

        if (key.enchantMin == null) {
            statement.setNull(15, 0);
        } else statement.setFloat(15, key.enchantMin);

        if (key.enchantMax == null) {
            statement.setNull(16, 0);
        } else statement.setFloat(16, key.enchantMax);

        if (key.gemLevel == null) {
            statement.setNull(17, 0);
        } else statement.setInt(17, key.gemLevel);

        if (key.gemQuality == null) {
            statement.setNull(18, 0);
        } else statement.setInt(18, key.gemQuality);

        if (key.gemCorrupted == null) {
            statement.setNull(19, 0);
        } else statement.setBoolean(19, key.gemCorrupted);

        if (key.links == null) {
            statement.setNull(20, 0);
        } else statement.setInt(20, key.links);

        if (key.baseItemLevel == null) {
            statement.setNull(21, 0);
        } else statement.setInt(21, key.baseItemLevel);

        if (key.variation == null) {
            statement.setNull(22, 0);
        } else statement.setString(22, key.variation.getVariation());

        statement.setString(23, item.getIcon());
    }
}
