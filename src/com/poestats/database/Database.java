package com.poestats.database;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.itemdata.ItemdataEntry;
import com.poestats.pricer.ParcelEntry;
import com.poestats.relations.IndexRelations;
import com.poestats.pricer.RawMaps.*;
import com.poestats.relations.CategoryEntry;

import java.sql.*;
import java.util.*;

public class Database {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Connection connection;
    private ArrayList<String> tables;

    //------------------------------------------------------------------------------------------------------------
    // DB controllers
    //------------------------------------------------------------------------------------------------------------

    public void connect() {
        tables = new ArrayList<>();

        try {
            connection = DriverManager.getConnection(Config.db_address, Config.db_username, Config.getDb_password());
            connection.setCatalog(Config.db_database);
            connection.setAutoCommit(false);

            getTables(tables);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Failed to connect to database", 5);
            System.exit(0);
        }
    }

    public void disconnect() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Initial DB setup
    //------------------------------------------------------------------------------------------------------------

    private boolean getTables(ArrayList<String> tables) {
        String query = "SHOW tables";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                tables.clear();
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query table info", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Access methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Fills provided List with LeagueEntry objects created from data from database
     *
     * @param leagueEntries
     * @return True on success
     */
    public boolean getLeagues(List<LeagueEntry> leagueEntries) {
        String query = "SELECT * FROM `sys-leagues`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                leagueEntries.clear();

                while (resultSet.next()) {
                    LeagueEntry leagueEntry = new LeagueEntry();
                    leagueEntry.load(resultSet);
                    leagueEntries.add(leagueEntry);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query database league list", 3);
            return false;
        }
    }

    /**
     * Compares provided league entries to ones present in database, updates any changes and adds missing leagues
     *
     * @param leagueEntries List of the most recent LeagueEntry objects
     * @return True on success
     */
    public boolean updateLeagues(List<LeagueEntry> leagueEntries) {
        String query =  "INSERT INTO `sys-leagues` (`name`, `display`, `start`, `end`) " +
                        "   VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "   `start` = VALUES(`start`), " +
                        "   `end` = VALUES(`end`), " +
                        "   `display` = VALUES(`display`)";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getName());
                    statement.setString(2, leagueEntry.getDisplay());
                    statement.setString(3, leagueEntry.getStartAt());
                    statement.setString(4, leagueEntry.getEndAt());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update database league list", 3);
            return false;
        }
    }

    /**
     * Removes any previous and updates the changeID record in table `change_id`
     *
     * @param id New changeID string to store
     * @return True on success
     */
    public boolean updateChangeID(String id) {
        String query =  "UPDATE `sys-change_id` SET `change_id` = ?, `time` = CURRENT_TIMESTAMP";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, id);
                statement.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update database change id", 3);
            return false;
        }
    }

    /**
     * Gets a list of parent and child categories and their display names from the database
     *
     * @param categoryRelations
     * @return Id of category or null on failure
     */
    public boolean getCategories(Map<String, CategoryEntry> categoryRelations) {
        Map<String, CategoryEntry> tmpCategoryRelations = new HashMap<>();

        String query =  "SELECT " +
                        "    `cp`.`name` AS 'name_parent', " +
                        "    `cc`.`name` AS 'name_child', " +
                        "    `cp`.`id` AS 'id_parent', " +
                        "    `cc`.`id` AS 'id_child' " +
                        "FROM `category-parent` AS `cp` " +
                        "    LEFT JOIN `category-child` AS `cc` " +
                        "        ON `cp`.`id` = `cc`.`id-cp`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String name_parent = resultSet.getString("name_parent");
                    String name_child = resultSet.getString("name_child");

                    Integer id_parent = resultSet.getInt("id_parent");
                    Integer id_child = resultSet.getInt("id_child");

                    CategoryEntry categoryEntry = tmpCategoryRelations.getOrDefault(name_parent, new CategoryEntry());
                    categoryEntry.setId(id_parent);
                    if (name_child != null) categoryEntry.addChild(name_child, id_child);
                    tmpCategoryRelations.putIfAbsent(name_parent, categoryEntry);
                }
            }

            categoryRelations.clear();
            categoryRelations.putAll(tmpCategoryRelations);

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query categories", 3);
            return false;
        }
    }

    public Integer addParentCategory(String parentName) {
        String query1 = "INSERT INTO `category-parent` (`name`) VALUES (?)";

        String query2 = "SELECT `id` FROM `category-parent` WHERE `name` = ?";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setString(1, parentName);
                statement.execute();
            }

            connection.commit();

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setString(1, parentName);

                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt("id") : null;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add parent category to database", 3);
            return null;
        }
    }

    public Integer addChildCategory(int parentId, String childName) {
        String query1 = "INSERT INTO `category-child` (`id-cp`, `name`) VALUES (?, ?)";

        String query2 = "SELECT `id` FROM `category-child` WHERE `id-cp` = ? AND `name` = ?";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentId);
                statement.setString(2, childName);
                statement.execute();
            }

            connection.commit();

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setInt(1, parentId);
                statement.setString(2, childName);

                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt("id") : null;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add child category to database", 3);
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Get item IDs
    //------------------------------------------------------------------------------------------------------------

    /**
     * Gets all item keys and IDs from every league item table
     *
     * @param indexRelations
     * @param leagueEntries
     * @return True on success
     */
    public boolean getItemIds(IndexRelations indexRelations, List<LeagueEntry> leagueEntries) {
        try {
            if (connection.isClosed()) return false;

            for (LeagueEntry leagueEntry : leagueEntries) {
                String league = leagueEntry.getName();
                String queryLeague = formatLeague(leagueEntry.getName());

                String query =  "SELECT " +
                                "    `i`.`id`, `idc`.`key` " +
                                "FROM `#_"+ queryLeague +"-items` AS `i` " +
                                "JOIN `itemdata-child` AS `idc` " +
                                "    ON `i`.`id-idc` = `idc`.`id`";

                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery(query);
                    indexRelations.loadItemIds(resultSet, league);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query item ids", 3);
            return false;
        }
    }

    /**
     * Gets all item keys and IDs from the parent item data table
     *
     * @param indexRelations
     * @return True on success
     */
    public boolean getItemDataParentIds(IndexRelations indexRelations) {
        String query =  "SELECT `id`, `key` FROM `itemdata-parent`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);
                indexRelations.loadItemDataParentIds(resultSet);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query parent item data ids", 3);
            return false;
        }
    }

    /**
     * Gets all item keys and IDs from the parent item data table
     *
     * @param indexRelations
     * @return True on success loadItemDataChildIds
     */
    public boolean getItemDataChildIds(IndexRelations indexRelations) {
        String query =  "SELECT `id`, `key` FROM `itemdata-child`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);
                indexRelations.loadItemDataChildIds(resultSet);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query child item data ids", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Indexing
    //------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new item entry in the league-specific item table if the item has not previously been indexed
     *
     * @param league
     * @param parentId
     * @param childId
     * @return Id of created item entry or null on failure
     */
    public Integer indexItem(String league, int parentId, int childId) {
        league = formatLeague(league);

        String query1 = "INSERT INTO `#_"+ league +"-items` (`id-idp`, `id-idc`) VALUES (?, ?)";

        String query2 = "SELECT `id` FROM `#_"+ league +"-items` WHERE `id-idp` = ? AND `id-idc` = ?";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentId);
                statement.setInt(2, childId);

                statement.execute();
            }

            connection.commit();

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setInt(1, parentId);
                statement.setInt(2, childId);

                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt("id") : null;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not create item in database", 3);
            return null;
        }
    }

    /**
     * Saves generic item data to database
     *
     * @param item
     * @return Id of created entry or null on failure
     */
    public Integer indexParentItemData(Item item, int parentCategoryId, Integer childCategoryId) {
        String query1 = "INSERT INTO `itemdata-parent` (`id-cp`, `id-cc`, `name`, `type`, `frame`, `key`) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        String query2 = "SELECT `id` FROM `itemdata-parent` WHERE `key` = ?";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentCategoryId);

                if (childCategoryId == null) statement.setNull(2, 0);
                else statement.setInt(2, childCategoryId);

                statement.setString(3, item.getName());
                statement.setString(4, item.getType());
                statement.setInt(5, item.getFrame());
                statement.setString(6, item.getGenericKey());

                statement.execute();
            }

            connection.commit();

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setString(1, item.getGenericKey());
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) return resultSet.getInt("id");
                else return null;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not index parent item in database", 3);
            return null;
        }
    }

    /**
     * Saves specific item data to database
     *
     * @param item
     * @param parentId
     * @return Id of created entry or null on failure
     */
    public Integer indexChildItemData(Item item, int parentId) {
        String query1 = "INSERT INTO `itemdata-child` (" +
                        "   `id-idp`, `tier`, `lvl`, `quality`, " +
                        "   `corrupted`, `links`, `var`, `key`, `icon`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String query2 = "SELECT `id` FROM `itemdata-child` WHERE `key` = ?";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentId);

                statement.setString(2, item.getTier());
                statement.setString(3, item.getLevel());
                statement.setString(4, item.getQuality());
                statement.setString(5, item.isCorrupted());
                statement.setString(6, item.getLinks());
                statement.setString(7, item.getVariation());
                statement.setString(8, item.getUniqueKey());
                statement.setString(9, Misc.formatIconURL(item.getIcon()));

                statement.execute();
            }

            connection.commit();

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setString(1, item.getUniqueKey());

                ResultSet resultSet = statement.executeQuery();

                return resultSet.next() ? resultSet.getInt("id") : null;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not index child item in database", 3);
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Entry management
    //------------------------------------------------------------------------------------------------------------

    /**
     * Queries database and fills out provided CurrencyMap with CurrencyItems
     *
     * @param league League where to get currency
     * @param currencyMap A map of currency name to its price
     * @return True on success
     */
    public boolean getCurrency(String league, Map<String, Double> currencyMap) {
        league = formatLeague(league);

        String query =  "SELECT " +
                        "   `idp`.`name`, `i`.`median` " +
                        "FROM `#_"+ league +"-items` AS `i` " +
                        "   JOIN `itemdata-parent` AS `idp` " +
                        "      ON `i`.`id-idp` = `idp`.`id` " +
                        "WHERE `idp`.`id-cp` = 4 AND `idp`.`frame` = 5";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    currencyMap.put(name, resultSet.getDouble("median"));
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query currency rates from database", 3);
            return false;
        }
    }

    /**
     * Fills provided Map with currency alias data from database
     *
     * @param currencyAliasToName
     * @return True on success
     */
    public boolean getCurrencyAliases(Map<String, String> currencyAliasToName) {
        String query =  "SELECT " +
                        "    `ci`.`id` AS 'id_item', " +
                        "    `ca`.`id` AS 'id_alias', " +
                        "    `ci`.`name` AS 'name_item', " +
                        "    `ca`.`name` AS 'name_alias' " +
                        "FROM `sys-currency_items` AS `ci` " +
                        "    JOIN `sys-currency_aliases` AS `ca` " +
                        "        ON `ci`.`id` = `ca`.`id-ci`";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String name_item = resultSet.getString("name_item");
                    String name_alias = resultSet.getString("name_alias");
                    currencyAliasToName.put(name_alias, name_item);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query currency aliases from database", 3);
            return false;
        }
    }

    public boolean uploadRaw(String league, Id2Ac2Raw idToAccountToRawEntry) {
        league = formatLeague(league);

        String query =  "INSERT INTO `#_"+ league +"-entries` (`id-i`, `price`, `account`, `itemid`) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `approved` = 0, `price` = VALUES(`price`)";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                int count = 0;

                for (Integer id : idToAccountToRawEntry.keySet()) {
                    Ac2Raw accountToRawEntry = idToAccountToRawEntry.get(id);

                    for (String account : accountToRawEntry.keySet()) {
                        RawEntry rawEntry = accountToRawEntry.get(account);

                        statement.setInt(1, id);
                        statement.setString(2, rawEntry.getPriceAsRoundedString());
                        statement.setString(3, account);
                        statement.setString(4, rawEntry.getItemId());
                        statement.addBatch();

                        if (++count % 1000 == 0) {
                            statement.executeBatch();
                        }
                    }
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add raw values to database", 3);
            return false;
        }
    }

    public boolean calculateMean(String league) {
        league = formatLeague(league);

        String query =  "UPDATE `#_"+ league +"-items` AS `i`" +
                        "  JOIN (" +
                        "    SELECT `i`.`id`, TRUNCATE(IFNULL(AVG(`e`.price), 0.0), 4) AS 'avg' " +
                        "    FROM `#_"+ league +"-items` AS `i`" +
                        "      JOIN `#_"+ league +"-entries` AS `e` ON `e`.`id-i` = `i`.`id`" +
                        "    WHERE `approved` = 1" +
                        "    GROUP BY `e`.`id-i`" +
                        "  ) AS `tmp` ON `tmp`.`id` = `i`.`id`" +
                        "SET `i`.`mean` = `tmp`.`avg`" +
                        "WHERE `i`.`volatile` = 0";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate mean", 3);
            return false;
        }
    }

    public boolean calculateMedian(String league, List<Integer> idList) {
        league = formatLeague(league);

        String query =  "UPDATE `#_"+ league +"-items` " +
                        "SET `median` = IFNULL((" +
                        "    SELECT AVG(t1.`price`) as median_val FROM (" +
                        "        SELECT @rownum:=@rownum+1 as `row_number`, d.`price`" +
                        "        FROM `#_"+ league +"-entries` d,  (SELECT @rownum:=0) r" +
                        "        WHERE `id-i` = ? AND `approved` = 1" +
                        "        ORDER BY d.`price`" +
                        "    ) as t1, (" +
                        "        SELECT count(*) as total_rows" +
                        "        FROM `#_"+ league +"-entries` d" +
                        "        WHERE `id-i` = ? AND `approved` = 1" +
                        "    ) as t2 WHERE 1" +
                        "    AND t1.row_number in ( floor((total_rows+1)/2), floor((total_rows+2)/2) )" +
                        "), 0.0) WHERE `id` = ?";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idList) {
                    statement.setInt(1, id);
                    statement.setInt(2, id);
                    statement.setInt(3, id);
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate median", 3);
            return false;
        }
    }

    public boolean calculateMode(String league, List<Integer> idList) {
        league = formatLeague(league);

        String query =  "UPDATE `#_"+ league +"-items` " +
                        "SET `mode` = IFNULL(( " +
                        "    SELECT `price` FROM `#_"+ league +"-entries`" +
                        "    WHERE `id-i` = ? AND `approved` = 1" +
                        "    GROUP BY `price` " +
                        "    ORDER BY COUNT(*) DESC " +
                        "    LIMIT 1" +
                        "), 0.0) " +
                        "WHERE `id` = ? AND `volatile` = 0";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idList) {
                    statement.setInt(1, id);
                    statement.setInt(2, id);
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate mode", 3);
            return false;
        }
    }

    public boolean calculateExalted(String league) {
        league = formatLeague(league);

        String query =  "SET @exVal = (" +
                        "    SELECT `i`.`mean` FROM `#_"+ league +"-items` AS `i` " +
                        "    JOIN `itemdata-parent` AS `idp` ON `i`.`id-idp` = `idp`.`id` " +
                        "    WHERE `idp`.`frame` = 5 AND `idp`.`name` = 'Exalted Orb'); " +

                        "UPDATE `#_"+ league +"-items` " +
                        "SET `exalted` = TRUNCATE(`median` / @exVal, 4) " +
                        "WHERE @exVal > 0 AND `median` > 0";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate exalted", 3);
            return false;
        }
    }

    public boolean updateVolatile(String league) {
        league = formatLeague(league);

        String query =  "UPDATE `#_"+ league +"-items` AS `i`" +
                        "  JOIN `#_"+ league +"-entries` AS `e` ON `e`.`id-i` = `i`.`id`" +
                        "SET `i`.`volatile` = IF(`dec` > 1 && `inc` > ? && `dec` / `inc` > ?, 1, 0)," +
                        "`e`.`approved` = `i`.`volatile`";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setDouble(1, Config.entry_volatileFlat);
                statement.setDouble(2, Config.entry_volatileRatio);
                statement.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update volatile status", 3);
            return false;
        }
    }

    public boolean resetVolatile(String league) {
        league = formatLeague(league);

        String query =  "UPDATE `#_"+ league +"-items` AS `i`" +
                        "  JOIN `#_"+ league +"-entries` AS `e` ON `e`.`id-i` = `i`.`id`" +
                        "SET `i`.`volatile` = 0, `e`.`approved` = 0 " +
                        "WHERE `i`.`volatile` = 1 AND `e`.`approved` = 1";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not reset volatile status", 3);
            return false;
        }
    }

    public boolean updateApproved(String league){
        league = formatLeague(league);

        String query =  "UPDATE `#_"+ league +"-entries` AS `e` " +
                        "  JOIN `#_"+ league +"-items` AS `i` " +
                        "    ON `e`.`id-i` = `i`.`id` " +
                        "SET `e`.`approved` = 1 " +
                        "WHERE `e`.`approved` = 0 " +
                        "  AND `i`.`volatile` = 0" +
                        "  AND `e`.`price` BETWEEN `i`.`median` / ? AND `i`.`median` * ?";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setDouble(1, 2.5);
                statement.setDouble(2, 2.5);

                statement.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update approved state", 3);
            return false;
        }
    }

    public boolean updateCounters(String league){
        league = formatLeague(league);

        String query  = "UPDATE `#_"+ league +"-items` AS `i`" +
                        "    JOIN (" +
                        "        SELECT `id-i`, `approved`, count(*) AS 'count' " +
                        "        FROM `#_"+ league +"-entries`" +
                        "        WHERE `time` > ADDDATE(NOW(), INTERVAL -1 MINUTE)" +
                        "        GROUP BY `id-i`, `approved`" +
                        "    ) AS `e` ON `e`.`id-i` = `i`.`id`" +
                        "SET " +
                        "    `i`.`count` = IF(`e`.`approved` = 1, `i`.`count` + `e`.`count`, `i`.`count`), " +
                        "    `i`.`inc` = IF(`e`.`approved` = 1, `i`.`inc` + `e`.`count`, `i`.`inc`), " +
                        "    `i`.`dec` = IF(`e`.`approved` = 0, `i`.`dec` + `e`.`count`, `i`.`dec`) " +
                        "WHERE `i`.`volatile` = 0";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update counters", 3);
            return false;
        }
    }

    //--------------------------
    // Output data management
    //--------------------------

    public boolean getItemdata(List<ItemdataEntry> parcel) {
        String query =  "SELECT " +
                        "  `idp`.`id` AS 'idp-id', " +
                        "  `idp`.`name`, `idp`.`type`, `idp`.`frame`, `idp`.`key` AS 'parent-key', " +
                        "  `idc`.`id` AS 'idc-id', " +
                        "  `idc`.`tier`, `idc`.`lvl`, `idc`.`quality`, `idc`.`corrupted`, " +
                        "  `idc`.`links`, `idc`.`var`, `idc`.`key` AS 'child-key', `idc`.`icon`, " +
                        "  `cp`.`name` AS 'cp-name', `cc`.`name` AS 'cc-name' " +
                        "FROM `itemdata-parent` AS `idp` " +
                        "LEFT JOIN `itemdata-child` AS `idc` ON `idp`.`id` = `idc`.`id-idp` " +
                        "LEFT JOIN `category-parent` AS `cp` ON `cp`.`id` = `idp`.`id-cp` " +
                        "LEFT JOIN `category-child` AS `cc` ON `cc`.`id` = `idp`.`id-cc` ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                ItemdataEntry lastEntry = null;

                while (resultSet.next()) {
                    if (lastEntry == null) {
                        lastEntry = new ItemdataEntry();
                        lastEntry.load(resultSet);
                    } else if (!lastEntry.getId().equals(resultSet.getString("idp-id"))) {
                        parcel.add(lastEntry);

                        lastEntry = new ItemdataEntry();
                        lastEntry.load(resultSet);
                    }

                    if (resultSet.getString("idc-id") != null) {
                        lastEntry.loadMember(resultSet);
                    }
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not get itemdata", 3);
            return false;
        }
    }

    public boolean getOutputItems(String league, Map<Integer, ParcelEntry> parcel, int category) {
        league = formatLeague(league);

        String query =  "SELECT " +
                        "    `i`.`id` AS 'id_item', " +
                        "    `i`.`mean`, `i`.`median`, `i`.`mode`, `i`.`exalted`, " +
                        "    `i`.`count`, `i`.`quantity`, `i`.`inc`, `i`.`dec`, " +
                        "    `idp`.`name`, `idp`.`type`, `idp`.`frame`, `idp`.`key` AS 'key_parent', " +
                        "    `idc`.`tier`, `idc`.`lvl`, `idc`.`quality`, `idc`.`corrupted`, " +
                        "    `idc`.`links`, `idc`.`var`, `idc`.`key` AS 'key_child', `idc`.`icon`, " +
                        "    `cc`.`name` AS 'category_child' " +
                        "FROM `#_"+ league +"-items` AS `i` " +
                        "JOIN `itemdata-child` AS `idc` ON `i`.`id-idc` = `idc`.`id` " +
                        "JOIN `itemdata-parent` AS `idp` ON `i`.`id-idp` = `idp`.`id` " +
                        "LEFT JOIN `category-child` AS `cc` ON `idp`.`id-cc` = `cc`.`id` " +
                        "WHERE `idp`.`id-cp` = ? " +
                        "ORDER BY `i`.`mean` DESC;";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, category);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    ParcelEntry parcelEntry = new ParcelEntry();
                    parcelEntry.loadItem(resultSet);
                    parcel.put(parcelEntry.getId(), parcelEntry);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not get output items", 3);
            return false;
        }
    }

    public boolean getOutputHistory(String league, Map<Integer, ParcelEntry> parcel) {
        league = formatLeague(league);

        String query  = "SELECT `id-i`, `mean` FROM `#_"+ league +"-history` " +
                        "WHERE `id-ch` = 3 " +
                        "ORDER BY `time` DESC ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    Integer id = resultSet.getInt("id-i");
                    ParcelEntry parcelEntry = parcel.get(id);

                    if (parcelEntry == null) continue;
                    parcelEntry.loadHistory(resultSet);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not get output items", 3);
            return false;
        }
    }

    public boolean addOutputFile(String league, String category, String path) {
        league = formatLeague(league);

        String query =  "INSERT INTO `sys-output_files` (`league`, `category`, `path`) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "   `path` = VALUES(`path`)";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, league);
                statement.setString(2, category);
                statement.setString(3, path);
                statement.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add output file to database", 3);
            return false;
        }
    }

    public boolean getOutputFiles(List<String> pathList) {
        String query =  "SELECT * FROM `sys-output_files`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    pathList.add(resultSet.getString("path"));
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add output file to database", 3);
            return false;
        }
    }

    //--------------------------
    // History entry management
    //--------------------------

    public boolean addMinutely(String league) {
        league = formatLeague(league);

        String query =  "INSERT INTO `#_"+ league +"-history` (" +
                        "   `id-i`, `id-ch`, `mean`, `median`, `mode`, `exalted`)" +
                        "SELECT " +
                        "   `id`, 1, `mean`, `median`, `mode`, `exalted`" +
                        "FROM `#_"+ league +"-items`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add minutely", 3);
            return false;
        }
    }

    public boolean addHourly(String league) {
        league = formatLeague(league);

        String query =  "INSERT INTO `#_"+ league +"-history` (" +
                        "   `id-i`, `id-ch`, " +
                        "   `mean`, `median`, `mode`, `exalted`, " +
                        "   `count`, `quantity`, `inc`, `dec`)" +
                        "SELECT " +
                        "   `h`.`id-i`, 2, " +
                        "   AVG(`h`.`mean`), AVG(`h`.`median`), AVG(`h`.`mode`), AVG(`h`.`exalted`), " +
                        "   `i`.`count`, `i`.`quantity`, `i`.`inc`,  `i`.`dec` " +
                        "FROM `#_"+ league +"-history` AS `h` " +
                        "JOIN `#_"+ league +"-items` AS `i` ON `h`.`id-i` = `i`.`id` " +
                        "WHERE `h`.`id-ch` = 1 " +
                        "GROUP BY `h`.`id-i`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add hourly", 3);
            return false;
        }
    }

    public boolean addDaily(String league) {
        league = formatLeague(league);

        String query =  "INSERT INTO `#_"+ league +"-history` (" +
                        "   `id-i`, `id-ch`, " +
                        "   `mean`, `median`, `mode`, `exalted`, " +
                        "   `count`, `quantity`, `inc`, `dec`)" +
                        "SELECT " +
                        "   `id-i`, 3, " +
                        "   AVG(`mean`), AVG(`median`), AVG(`mode`), AVG(`exalted`), " +
                        "   MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                        "FROM `#_"+ league +"-history` " +
                        "WHERE `id-ch` = 2 " +
                        "GROUP BY `id-i`";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add daily", 3);
            return false;
        }
    }

    public boolean calcQuantity(String league) {
        league = formatLeague(league);

        String query =  "UPDATE `#_"+ league +"-items` as `i` " +
                        "SET `quantity` = (" +
                        "    SELECT SUM(`inc`) / COUNT(`inc`) FROM `#_"+ league +"-history` " +
                        "    WHERE `id-i` = `i`.`id` AND `id-ch` = 2 " +
                        "), `inc` = 0, `dec` = 0;";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate quantity", 3);
            return false;
        }
    }

    public boolean removeOldHistoryEntries(String league, int type, String interval) {
        league = formatLeague(league);

        String query =  "DELETE FROM `#_"+ league +"-history` " +
                        "WHERE `id-ch` = ? " +
                        "AND `time` < ADDDATE(NOW(), INTERVAL -"+ interval +")";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, type);
                statement.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not remove old history entries", 3);
            return false;
        }
    }

    public boolean removeOldItemEntries(String league, List<Integer> idList) {
        league = formatLeague(league);

        String query =  "DELETE FROM `#_"+ league +"-entries`" +
                        "WHERE `id-i` = ? AND `time` NOT IN (" +
                        "    SELECT `time`" +
                        "    FROM (" +
                        "        SELECT `time` " +
                        "        FROM `#_"+ league +"-entries`" +
                        "        WHERE `id-i` = ? " +
                        "        ORDER BY `time` DESC" +
                        "        LIMIT ?" +
                        "    ) foo )";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idList) {
                    statement.setInt(1, id);
                    statement.setInt(2, id);
                    statement.setInt(3, Config.entry_maxCount);
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not remove old item entries", 3);
            return false;
        }
    }

    public boolean createLeagueTables(String league) {
        league = formatLeague(league);

        String query1 = "CREATE TABLE `#_"+ league +"-items` (" +
                        "    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT," +
                        "    `id-idp`              int             unsigned NOT NULL," +
                        "    `id-idc`              int             unsigned NOT NULL," +
                        "    `time`                timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `volatile`            tinyint(1)      unsigned NOT NULL DEFAULT 0," +
                        "    `mean`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `median`              decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `mode`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `exalted`             decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `count`               int(16)         unsigned NOT NULL DEFAULT 0," +
                        "    `quantity`            int(8)          unsigned NOT NULL DEFAULT 0," +
                        "    `inc`                 int(8)          unsigned NOT NULL DEFAULT 0," +
                        "    `dec`                 int(8)          unsigned NOT NULL DEFAULT 0," +
                        "    FOREIGN KEY (`id-idp`)" +
                        "        REFERENCES `itemdata-parent` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    FOREIGN KEY (`id-idc`)" +
                        "        REFERENCES `itemdata-child` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    INDEX `ind-i-id`      (`id`)," +
                        "    INDEX `ind-i-id-idp`  (`id-idp`)," +
                        "    INDEX `ind-i-id-idc`  (`id-idc`)," +
                        "    INDEX `ind-i-mean`    (`mean`)," +
                        "    INDEX `ind-i-volatile`(`volatile`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String query2 = "CREATE TABLE `#_"+ league +"-entries` (" +
                        "    `id-i`                int             unsigned NOT NULL," +
                        "    `time`                timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `approved`            tinyint(1)      unsigned NOT NULL DEFAULT 0," +
                        "    `price`               decimal(10,4)   unsigned NOT NULL," +
                        "    `account`             varchar(32)     NOT NULL," +
                        "    `itemid`              varchar(32)     NOT NULL," +
                        "    FOREIGN KEY (`id-i`)" +
                        "        REFERENCES `#_"+ league +"-items` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    CONSTRAINT `pk-e`" +
                        "        PRIMARY KEY (`id-i`, `account`)," +
                        "    INDEX `ind-e-id-i`    (`id-i`)," +
                        "    INDEX `ind-e-time`    (`time`)," +
                        "    INDEX `ind-e-approved`(`approved`)," +
                        "    INDEX `ind-e-price`   (`price`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String query3 = "CREATE TABLE `#_"+ league +"-history` (" +
                        "    `id-i`                int             unsigned NOT NULL," +
                        "    `id-ch`               int             unsigned NOT NULL," +
                        "    `time`                timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `mean`                decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `median`              decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `mode`                decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `exalted`             decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `inc`                 int(8)          unsigned DEFAULT NULL," +
                        "    `dec`                 int(8)          unsigned DEFAULT NULL," +
                        "    `count`               int(16)         unsigned DEFAULT NULL," +
                        "    `quantity`            int(8)          unsigned DEFAULT NULL," +
                        "    FOREIGN KEY (`id-i`)" +
                        "        REFERENCES `#_"+ league +"-items` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    FOREIGN KEY (`id-ch`)" +
                        "        REFERENCES `category-history` (`id`)" +
                        "        ON DELETE RESTRICT," +
                        "    INDEX `ind-h-id`      (`id-i`)," +
                        "    INDEX `ind-h-id-ch`   (`id-ch`)," +
                        "    INDEX `ind-h-time`    (`time`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        try {
            if (connection.isClosed()) return false;

            getTables(tables);

            if (!tables.contains("#_" + league + "-items")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query1);
                }
            }

            if (!tables.contains("#_" + league + "-entries")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query2);
                }
            }

            if (!tables.contains("#_" + league + "-history")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query3);
                }
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not create league tables", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    private static void debug(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) System.out.print(",  ");
                String columnValue = rs.getString(i);
                System.out.print(columnValue + " (" + rsmd.getColumnName(i) + ")");
            }
            System.out.println();
        }
    }

    public static String formatLeague(String league) {
        return league
                .replace(" ", "_")
                .replace("(", "")
                .replace(")", "")
                .toLowerCase();
    }
}
