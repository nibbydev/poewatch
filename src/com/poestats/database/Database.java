package com.poestats.database;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.ParcelEntry;
import com.poestats.pricer.StatusElement;
import com.poestats.pricer.entries.RawEntry;
import com.poestats.relations.IndexRelations;
import com.poestats.pricer.maps.CurrencyMaps.*;
import com.poestats.pricer.maps.RawMaps.*;
import com.poestats.relations.CategoryEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String query = "SELECT * FROM `leagues`";

        try {
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
        String query =  "INSERT INTO `leagues` (`name`, `display`, `start`, `end`) " +
                        "   VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "   `start` = VALUES(`start`), " +
                        "   `end` = VALUES(`end`), " +
                        "   `display` = VALUES(`display`)";

        try {
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
        String query =  "UPDATE `change_id` SET `id` = ?";

        try {
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
                        "FROM `category_parent` AS `cp` " +
                        "    LEFT JOIN `category_child` AS `cc` " +
                        "        ON `cp`.`id` = `cc`.`id_parent`";

        try {
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
        String query1 = "INSERT INTO `category_parent` (`name`) VALUES (?)";

        String query2 = "SELECT `id` FROM `category_parent` WHERE `name` = ?";

        try {
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
        String query1 = "INSERT INTO `category_child` (`id_parent`, `name`,) VALUES (?, ?)";

        String query2 = "SELECT `id` FROM `category_child` WHERE `id_parent` = ? AND `name` = ?";

        try {
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
            for (LeagueEntry leagueEntry : leagueEntries) {
                String league = leagueEntry.getName();
                String queryLeague = formatLeague(leagueEntry.getName());

                String query =  "SELECT " +
                                "    `i`.`id`, `idc`.`key` " +
                                "FROM `#_item_"+ queryLeague +"` AS `i` " +
                                "JOIN `item_data_child` AS `idc` " +
                                "    ON `i`.`id_data_child` = `idc`.`id`";

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
        String query =  "SELECT `key`, `id` FROM `item_data_parent`";

        try {
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
        String query =  "SELECT `key`, `id` FROM `item_data_child`";

        try {
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

        String query1 = "INSERT INTO `#_item_"+ league +"` (`id_data_parent`, `id_data_child`) VALUES (?, ?)";

        String query2 = "SELECT `id` FROM `#_item_"+ league +"` WHERE `id_data_parent` = ? AND `id_data_child` = ?";

        try {
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
        String query1 = "INSERT INTO `item_data_parent` (" +
                        "   `id_category_parent`, `id_category_child`, " +
                        "   `name`, `type`, `frame`, `key`) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        String query2 = "SELECT `id` FROM `item_data_parent` WHERE `key` = ?";

        try {
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
        String query1 = "INSERT INTO `item_data_child` (" +
                        "   `id_parent`, `tier`, `lvl`, `quality`, " +
                        "   `corrupted`, `links`, `var`, `key`, `icon`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String query2 = "SELECT `id` FROM `item_data_child` " +
                        "WHERE `id_parent` = ? AND `key` = ? " +
                        "LIMIT 1";

        try {
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
                statement.setInt(1, parentId);
                statement.setString(2, item.getUniqueKey());

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
    // Status
    //------------------------------------------------------------------------------------------------------------

    /**
     * Queries status timers from the database
     *
     * @param statusElement StatusElement to fill out
     * @return True on success
     */
    public boolean getStatus(StatusElement statusElement) {
        String query = "SELECT * FROM `status`";

        try {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);
                statusElement.load(resultSet);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query status data from database", 3);
            return false;
        }
    }

    /**
     * Adds or updates status records stored in the `status` table
     *
     * @param statusElement StatusElement to copy
     * @return True on success
     */
    public boolean updateStatus(StatusElement statusElement) {
        String query =  "UPDATE `status` SET `value` = ? WHERE `id` = ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setLong(1, statusElement.lastRunTime);
                statement.setString(2, "lastRunTime");
                statement.addBatch();

                statement.setLong(1, statusElement.twentyFourCounter);
                statement.setString(2, "twentyFourCounter");
                statement.addBatch();

                statement.setLong(1, statusElement.sixtyCounter);
                statement.setString(2, "sixtyCounter");
                statement.addBatch();

                statement.setLong(1, statusElement.tenCounter);
                statement.setString(2, "tenCounter");
                statement.addBatch();

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update status", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Entry management
    //------------------------------------------------------------------------------------------------------------

    /**
     * Queries database and fills out provided CurrencyMap with CurrencyItems
     *
     * @param league League where to get currency
     * @param currencyMap CurrencyMap to fill out
     * @return True on success
     */
    public boolean getCurrency(String league, CurrencyMap currencyMap) {
        league = formatLeague(league);

        String query =  "SELECT " +
                        "   `idp`.`name`, `i`.`id_data_parent`, `i`.`id_data_child`, " +
                        "   `i`.`mean`, `i`.`median`, `i`.`mode`, `i`.`exalted`, " +
                        "   `i`.`count`, `i`.`quantity`, `i`.`inc`, `i`.`dec` " +
                        "FROM `#_item_"+ league +"` AS `i` " +
                        "    INNER JOIN `item_data_parent` AS `idp` " +
                        "        ON `i`.`id_data_parent` = `idp`.`id` " +
                        "WHERE `idp`.`id_category_parent` = 4 AND `idp`.`frame` = 5";

        try {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String name = resultSet.getString("name");

                    CurrencyItem currencyItem = new CurrencyItem();
                    currencyItem.loadItem(resultSet);
                    currencyMap.put(name, currencyItem);
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
                        "FROM `currency_item` AS `ci` " +
                        "    JOIN `currency_alias` AS `ca` " +
                        "        ON `ci`.`id` = `ca`.`id_parent`";

        try {
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


    public boolean uploadRaw(String league, IndexMap idToAccountToRawEntry) {
        league = formatLeague(league);

        String query =  "INSERT INTO `#_entry_"+ league +"` (`id_item`, `price`, `account`, `item_id`) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `price` = `price`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idToAccountToRawEntry.keySet()) {
                    AccountMap accountToRawEntry = idToAccountToRawEntry.get(id);

                    for (String account : accountToRawEntry.keySet()) {
                        RawEntry rawEntry = accountToRawEntry.get(account);

                        statement.setInt(1, id);
                        statement.setString(2, rawEntry.getPriceAsRoundedString());
                        statement.setString(3, account);
                        statement.setString(4, rawEntry.getItemId());
                        statement.addBatch();
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

    public boolean updateCounters(String league, IndexMap idToAccountToRawEntry) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `count` = `count` + ?, `inc` = `inc` + ? " +
                        "WHERE `id`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idToAccountToRawEntry.keySet()) {
                    AccountMap accountToRawEntry = idToAccountToRawEntry.get(id);

                    statement.setInt(1, accountToRawEntry.size());
                    statement.setInt(2, accountToRawEntry.size());
                    statement.setInt(3, id);

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update counters in database", 3);
            return false;
        }
    }

    public boolean calculateMean(String league, List<Integer> idList, List<Integer> ignoreList) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `mean` = (" +
                        "    SELECT IFNULL(AVG(`price`), 0.0) " +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `id_item`= ?" +
                        ") WHERE `id`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idList) {
                    if (ignoreList.contains(id)) continue;

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
            Main.ADMIN.log_("Could not calculate mean", 3);
            return false;
        }
    }

    public boolean calculateMedian(String league, List<Integer> idList, List<Integer> ignoreList) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `median` = IFNULL((" +
                        "    SELECT AVG(t1.`price`) as median_val FROM (" +
                        "        SELECT @rownum:=@rownum+1 as `row_number`, d.`price`" +
                        "        FROM `#_entry_"+ league +"` d,  (SELECT @rownum:=0) r" +
                        "        WHERE `id_item` = ?" +
                        "        ORDER BY d.`price`" +
                        "    ) as t1, (" +
                        "        SELECT count(*) as total_rows" +
                        "        FROM `#_entry_"+ league +"` d" +
                        "        WHERE `id_item` = ?" +
                        "    ) as t2 WHERE 1" +
                        "    AND t1.row_number in ( floor((total_rows+1)/2), floor((total_rows+2)/2) )" +
                        "), 0.0) WHERE `id` = ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idList) {
                    if (ignoreList.contains(id)) continue;

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

    public boolean calculateMode(String league, List<Integer> idList, List<Integer> ignoreList) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `mode` = IFNULL(( " +
                        "    SELECT `price` FROM `#_entry_"+ league +"`" +
                        "    WHERE `id_item` = ?" +
                        "    GROUP BY `price` " +
                        "    ORDER BY COUNT(*) DESC " +
                        "    LIMIT 1" +
                        "), 0.0) " +
                        "WHERE `id` = ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idList) {
                    if (ignoreList.contains(id)) continue;

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

        String query1 = "SET @exVal = (" +
                        "    SELECT `i`.`mean` FROM `#_item_"+ league +"` AS `i` " +
                        "    JOIN `item_data_parent` AS `idp` ON `i`.`id_data_parent` = `idp`.`id` " +
                        "    WHERE `idp`.`frame` = 5 AND `idp`.`name` = 'Exalted Orb');";

        String query2 = "UPDATE `#_item_"+ league +"` " +
                        "SET `exalted` = IFNULL(`mean` / @exVal, 0.0)";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query1);
                statement.execute(query2);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate exalted", 3);
            return false;
        }
    }

    //--------------------------
    // Output data management
    //--------------------------

    public boolean getOutputItems(String league, String category, Map<String, ParcelEntry> parcel) {
        league = formatLeague(league);

        String query =  "SELECT " +
                        "  i.`sup`, i.`sub`, d.`child`, " +
                        "  d.`name`, d.`type`, d.`frame`, d.`icon`, " +
                        "  d.`var`, d.`tier`, d.`lvl`, d.`quality`, d.`corrupted`, d.`links`," +
                        "  i.`mean`, i.`median`, i.`mode`, i.`exalted`, " +
                        "  i.`count`, i.`quantity`, d.`supKey`, d.`subKey`" +
                        "FROM `#_item_"+ league +"` AS i " +
                        "JOIN (" +
                        "  SELECT " +
                        "      p.`sup`, b.`sub`," +
                        "      p.`child`," +
                        "      p.`name`, p.`type`," +
                        "      p.`frame`, b.`icon`," +
                        "      p.`key` AS 'supKey', b.`key` AS 'subKey'," +
                        "      b.`var`, b.`tier`, b.`lvl`, b.`quality`, b.`corrupted`, b.`links` " +
                        "  FROM `item_data_sub` AS b" +
                        "  JOIN `item_data_sup` AS p" +
                        "      ON b.`sup` = p.`sup`" +
                        "  WHERE p.`parent` = ?" +
                        ") AS d ON i.`sup` = d.`sup` AND i.`sub` = d.`sub`" +
                        "ORDER BY i.`mean` DESC ";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, category);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    ParcelEntry parcelEntry = new ParcelEntry();
                    parcelEntry.loadItem(resultSet);
                    parcel.put(parcelEntry.getIndex(), parcelEntry);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not get output items", 3);
            return false;
        }
    }

    public boolean getOutputHistory(String league, Map<String, ParcelEntry> parcel) {
        league = formatLeague(league);

        String query  = "SELECT `sup`, `sub`, `mean` FROM `#_history_"+ league +"` " +
                        "WHERE `type`='daily' " +
                        "ORDER BY `time` DESC ";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String index = resultSet.getString("sup") + resultSet.getString("sub");
                    ParcelEntry parcelEntry = parcel.get(index);

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

        String query =  "INSERT INTO `output_files` (`league`, `category`, `path`) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "   `path` = VALUES(`path`)";

        try {
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
        String query =  "SELECT * FROM `output_files`";

        try {
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

        String query =  "INSERT INTO `#_history_"+ league +"` " +
                        "   (`id`, `id_type`, `mean`, `median`, `mode`, `exalted`, `inc`, `dec`, `count`, `quantity`) " +
                        "SELECT `id`, 1, `mean`, `median`, `mode`, `exalted`, `inc`, `dec`, `count`, `quantity` " +
                        "FROM `#_item_"+ league +"`";

        try {
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

        String query =  "INSERT INTO `#_history_"+ league +"` (" +
                        "   `id`, `id_type`, " +
                        "   `mean`, `median`, `mode`, `exalted`, " +
                        "   `count`, `quantity`, `inc`, `dec`)" +
                        "SELECT " +
                        "   `id`, 2, " +
                        "   AVG(`mean`), AVG(`median`), AVG(`mode`), AVG(`exalted`), " +
                        "   MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                        "FROM `#_history_"+ league +"` " +
                        "WHERE `type` = 1 " +
                        "GROUP BY `id`";

        try {
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

        String query =  "INSERT INTO `#_history_"+ league +"` (" +
                        "   `id`, `id_type`, " +
                        "   `mean`, `median`, `mode`, `exalted`, " +
                        "   `count`, `quantity`, `inc`, `dec`)" +
                        "SELECT " +
                        "   `id`, 3, " +
                        "   AVG(`mean`), AVG(`median`), AVG(`mode`), AVG(`exalted`), " +
                        "   MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                        "FROM `#_history_"+ league +"` " +
                        "WHERE `type` = 2 " +
                        "GROUP BY `id`";

        try {
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

        String query =  "UPDATE `#_item_"+ league +"` as `i` " +
                        "SET `quantity` = (" +
                        "    SELECT SUM(`inc`) / COUNT(`inc`) FROM `#_history_"+ league +"` " +
                        "    WHERE `id` = `i`.`id` AND `type`=2 " +
                        "), `inc` = 0, `dec` = 0;";

        try {
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

        String query =  "DELETE FROM `#_history_"+ league +"` " +
                        "WHERE `id_type` = "+ type+ " " +
                        "AND `time` < ADDDATE(NOW(), INTERVAL -"+ interval +")";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
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

        String query =  "DELETE FROM `#_entry_"+ league +"`" +
                        "WHERE `id_item` = ? AND `account` NOT IN (" +
                        "    SELECT `account`" +
                        "    FROM (" +
                        "        SELECT `account`, `time` " +
                        "        FROM `#_entry_"+ league +"`" +
                        "        WHERE `id_item` = ? " +
                        "        ORDER BY `time` DESC" +
                        "        LIMIT ?" +
                        "    ) foo )";

        try {
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

    public boolean removeItemOutliers(String league, List<Integer> idList, List<Integer> ignoreList){
        league = formatLeague(league);

        String queryX = "SELECT SUM(`dec`) / SUM(`inc`) AS 'discardRatio' " +
                        "FROM `#_history_"+ league +"` " +
                        "WHERE `id` = ? AND `id_type` = ? " +
                        "ORDER BY `time` DESC " +
                        "LIMIT ?";

        String query0 = "SET @entryCount = (" +
                        "    SELECT COUNT(*)" +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `id_item` = ?);";

        String query1 = "SET @stddevPrice = (" +
                        "    SELECT STDDEV(`price`)" +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `id_item` = ?" +
                        ") * ?;";

        String query2 = "SET @medianPrice = (" +
                        "    SELECT `median`" +
                        "    FROM `#_item_"+ league +"`" +
                        "    WHERE `id` = ?);";

        String query3 = "SET @numberOfElementsToRemove = (" +
                        "    SELECT COUNT(`price`)" +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `id_item` = ? " +
                        "    AND (`price` > @medianPrice + @stddevPrice && `price` > @medianPrice * ? || " +
                        "        `price` < @medianPrice - @stddevPrice && `price` < @medianPrice / ?) &&" +
                        "        @entryCount > ? && @medianPrice > ?);";

        String query4 = "UPDATE `#_item_"+ league +"` " +
                        "SET `dec` = `dec` + @numberOfElementsToRemove " +
                        "WHERE `id` = ?;";

        String query5 = "DELETE FROM `#_entry_"+ league +"` " +
                        "WHERE `id_item` = ? " +
                        "AND (`price` > @medianPrice + @stddevPrice && `price` > @medianPrice * ? || " +
                        "    `price` < @medianPrice - @stddevPrice && `price` < @medianPrice / ?) && " +
                        "    @entryCount > ? && @medianPrice > ?;";

        try {
            for (Integer id : idList) {
                try (PreparedStatement statement = connection.prepareStatement(queryX)) {
                    statement.setInt(1, id);
                    statement.setInt(2, Config.sql_id_category_history_hourly);
                    statement.setInt(3, Config.outlier_hoursCalculated);

                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        double discardRatio = resultSet.getDouble("discardRatio");

                        if (discardRatio > Config.outlier_discardRatio) {
                            System.out.printf("Bad ratio (%f) for %s %d\n", discardRatio, league, id);
                            ignoreList.add(id);
                            continue;
                        }
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(query0)) {
                    statement.setInt(1, id);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query1)) {
                    statement.setInt(1, id);
                    statement.setDouble(2, Config.outlier_devMulti);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query2)) {
                    statement.setInt(1, id);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query3)) {
                    statement.setInt(1, id);
                    statement.setDouble(2, Config.outlier_priceMulti);
                    statement.setDouble(3, Config.outlier_priceMulti);
                    statement.setInt(4, Config.outlier_minCount);
                    statement.setDouble(5, Config.outlier_minPrice);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query4)) {
                    statement.setInt(1, id);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query5)) {
                    statement.setInt(1, id);
                    statement.setDouble(2, Config.outlier_priceMulti);
                    statement.setDouble(3, Config.outlier_priceMulti);
                    statement.setInt(4, Config.outlier_minCount);
                    statement.setDouble(5, Config.outlier_minPrice);
                    statement.execute();
                }
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not remove outliers", 3);
            return false;
        }
    }

    public boolean createLeagueTables(String league) {
        league = formatLeague(league);

        String query1 = "CREATE TABLE `#_item_"+ league +"` (" +
                        "    FOREIGN KEY (`id_data_child`)" +
                        "        REFERENCES `item_data_child` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    FOREIGN KEY (`id_data_parent`)" +
                        "        REFERENCES `item_data_parent` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT," +
                        "    `id_data_parent`      int             unsigned NOT NULL," +
                        "    `id_data_child`       int             unsigned NOT NULL," +
                        "    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `mean`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `median`              decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `mode`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `exalted`             decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `count`               int(16)         unsigned NOT NULL DEFAULT 0," +
                        "    `quantity`            int(8)          unsigned NOT NULL DEFAULT 0," +
                        "    `inc`                 int(8)          unsigned NOT NULL DEFAULT 0," +
                        "    `dec`                 int(8)          unsigned NOT NULL DEFAULT 0" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String query3 = "CREATE TABLE `#_entry_"+ league +"` (" +
                        "    FOREIGN KEY (`id_item`)" +
                        "        REFERENCES `#_item_"+ league +"` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    `id_item`             int             unsigned NOT NULL," +
                        "    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `price`               decimal(10,4)   unsigned NOT NULL," +
                        "    `account`             varchar(32)     NOT NULL UNIQUE," +
                        "    `item_id`             varchar(32)     NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String query4 = "CREATE TABLE `#_history_"+ league +"` (" +
                        "    FOREIGN KEY (`id`)" +
                        "        REFERENCES `#_item_"+ league +"` (`id`)" +
                        "        ON DELETE CASCADE," +
                        "    FOREIGN KEY (`id_type`)" +
                        "        REFERENCES `category_history` (`id`)" +
                        "        ON DELETE RESTRICT," +
                        "    `id`                  int             unsigned NOT NULL," +
                        "    `id_type`             int             unsigned NOT NULL," +
                        "    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `mean`                decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `median`              decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `mode`                decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `exalted`             decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `inc`                 int(8)          unsigned DEFAULT NULL," +
                        "    `dec`                 int(8)          unsigned DEFAULT NULL," +
                        "    `count`               int(16)         unsigned DEFAULT NULL," +
                        "    `quantity`            int(8)          unsigned DEFAULT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        try {
            getTables(tables);

            if (!tables.contains("#_item_" + league)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query1);
                }
            }

            if (!tables.contains("#_entry_" + league)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query3);
                }
            }

            if (!tables.contains("#_history_" + league)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query4);
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
                .replace(" ", "-")
                .replace("(", "")
                .replace(")", "")
                .toLowerCase();
    }
}
