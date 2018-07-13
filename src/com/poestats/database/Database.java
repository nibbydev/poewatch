package com.poestats.database;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.Misc;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.AccountEntry;
import com.poestats.pricer.itemdata.ItemdataEntry;
import com.poestats.pricer.ParcelEntry;
import com.poestats.pricer.RawMaps.*;
import com.poestats.relations.CategoryEntry;

import java.sql.*;
import java.util.*;

public class Database {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Connection connection;

    //------------------------------------------------------------------------------------------------------------
    // DB controllers
    //------------------------------------------------------------------------------------------------------------

    /**
     * Initializes connection to the MySQL database
     */
    public void connect() {
        try {
            connection = DriverManager.getConnection(Config.db_address, Config.db_username, Config.getDb_password());
            connection.setCatalog(Config.db_database);
            connection.setAutoCommit(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Failed to connect to databases", 5);
            System.exit(0);
        }
    }

    /**
     * Disconnects from the MySQL database
     */
    public void disconnect() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Account database
    //------------------------------------------------------------------------------------------------------------

    /**
     * Uploads gathered account and character names to the account database
     *
     * @param accountSet Set of AccountEntries, containing accountName and characterName
     * @return True on success
     */
    public boolean uploadAccountNames(Set<AccountEntry> accountSet) {
        String query =  "BEGIN; " +
                        "    INSERT INTO account_accounts (name) VALUES(?) " +
                        "    ON DUPLICATE KEY UPDATE seen = NOW(); " +

                        "    INSERT INTO account_characters (name) VALUES(?) " +
                        "    ON DUPLICATE KEY UPDATE seen = NOW(); " +

                        "    INSERT INTO account_relations (id_l, id_a, id_c) " +
                        "        SELECT ?, a.id, c.id " +
                        "        FROM account_accounts AS a " +
                        "        INNER JOIN account_characters AS c " +
                        "        WHERE a.name = ? AND c.name = ? " +
                        "    ON DUPLICATE KEY UPDATE seen = NOW(); " +
                        "COMMIT; ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (AccountEntry accountEntry : accountSet) {
                    statement.setString(1, accountEntry.account);
                    statement.setString(2, accountEntry.character);
                    statement.setInt(3, accountEntry.league);
                    statement.setString(4, accountEntry.account);
                    statement.setString(5, accountEntry.character);

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not upload account names", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Data retrieval upon initialization
    //------------------------------------------------------------------------------------------------------------

    /**
     * Loads provided List with league entries from database
     *
     * @param leagueEntries List that will contain LeagueEntry entries
     * @return True on success
     */
    public boolean getLeagues(List<LeagueEntry> leagueEntries) {
        String query =  "SELECT * FROM data_leagues WHERE active = 1 ";

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
     * Loads provided Map with category entries from database
     *
     * @param categoryRelations Empty map that will contain parentCategory - CategoryEntry relations
     * @return True on success
     */
    public boolean getCategories(Map<String, CategoryEntry> categoryRelations) {
        Map<String, CategoryEntry> tmpCategoryRelations = new HashMap<>();

        String query =  "SELECT " +
                        "    cp.name AS parentName, " +
                        "    cc.name AS childName, " +
                        "    cp.id AS parentId, " +
                        "    cc.id AS childId " +
                        "FROM category_parent AS cp " +
                        "    LEFT JOIN category_child AS cc " +
                        "        ON cp.id = cc.id_cp ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String parentName = resultSet.getString("parentName");
                    String childName = resultSet.getString("childName");
                    Integer parentId = resultSet.getInt("parentId");
                    Integer childId = resultSet.getInt("childId");

                    CategoryEntry categoryEntry = tmpCategoryRelations.getOrDefault(parentName, new CategoryEntry());
                    categoryEntry.setId(parentId);
                    if (childName != null) categoryEntry.addChild(childName, childId);
                    tmpCategoryRelations.putIfAbsent(parentName, categoryEntry);
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

    /**
     * Loads provided Maps with item ID data from database
     *
     * @param leagueToIds Empty map that will contain league ID - list of item IDs relations
     * @param keyToId Empty map that will contain item key - item ID relations
     * @return True on success
     */
    public boolean getItemIds(Map<Integer, List<Integer>> leagueToIds, Map<String, Integer> keyToId) {
        String query =  "SELECT i.id_l, did.id as id_d, did.key " +
                        "FROM league_items AS i " +
                        "JOIN data_itemData AS did " +
                        "    ON i.id_d = did.id " +
                        "ORDER BY i.id_l, did.id ASC";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    Integer leagueId = resultSet.getInt("id_l");
                    Integer dataId = resultSet.getInt("id_d");
                    String key = resultSet.getString("key");

                    List<Integer> idList = leagueToIds.getOrDefault(leagueId, new ArrayList<>());
                    idList.add(dataId);
                    leagueToIds.putIfAbsent(leagueId, idList);

                    keyToId.putIfAbsent(key, dataId);
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
     * Loads provided Map with currency price data from database
     *
     * @param currencyLeagueMap Map that will contain league id - currency name - chaos value relations
     * @return True on success
     */
    public boolean getCurrency(Map<Integer, Map<String, Double>> currencyLeagueMap) {
        Map<Integer, Map<String, Double>> tmpCurrencyLeagueMap = new HashMap<>();

        String query =  "SELECT i.id_l, did.name, i.median " +
                        "FROM league_items AS i " +
                        "   JOIN data_itemData AS did " +
                        "      ON i.id_d = did.id " +
                        "WHERE did.id_cp = 4 AND did.frame = 5 " +
                        "ORDER BY i.id_l";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    Integer leagueId = resultSet.getInt("id_l");
                    String name = resultSet.getString("name");
                    Double price = resultSet.getDouble("median");

                    Map<String, Double> currencyMap = tmpCurrencyLeagueMap.getOrDefault(leagueId, new HashMap<>());
                    currencyMap.put(name, price);
                    tmpCurrencyLeagueMap.putIfAbsent(leagueId, currencyMap);
                }
            }

            currencyLeagueMap.clear();
            currencyLeagueMap.putAll(tmpCurrencyLeagueMap);

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query currency rates from database", 3);
            return false;
        }
    }

    /**
     * Loads provided Map with currency alias data from database
     *
     * @param currencyAliasToName Map that will contain currency alias - currency name relations
     * @return True on success
     */
    public boolean getCurrencyAliases(Map<String, String> currencyAliasToName) {
        String query =  "SELECT " +
                        "    ci.name AS name, " +
                        "    ca.name AS alias " +
                        "FROM data_currencyItems AS ci " +
                        "    JOIN data_currencyAliases AS ca " +
                        "        ON ci.id = ca.id_ci";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String alias = resultSet.getString("alias");

                    currencyAliasToName.put(alias, name);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query currency aliases from database", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Item data indexing
    //------------------------------------------------------------------------------------------------------------

    /**
     * Creates a parent category entry in table `category_parent`
     *
     * @param parentName Name of parent category
     * @return ID of created category on success, null on failure
     */
    public Integer addParentCategory(String parentName) {
        String query1 = "INSERT INTO category_parent (name) VALUES (?)";
        String query2 = "SELECT LAST_INSERT_ID()";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setString(1, parentName);
                statement.executeUpdate();
            }

            connection.commit();

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query2);
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add parent category to database", 3);
            return null;
        }
    }

    /**
     * Creates a child category entry in table `category_child`
     *
     * @param parentId ID of child's parent category
     * @param childName Name of child category
     * @return ID of created category on success, null on failure
     */
    public Integer addChildCategory(Integer parentId, String childName) {
        String query1 = "INSERT INTO category_child (id_cp, name) VALUES (?, ?)";
        String query2 = "SELECT LAST_INSERT_ID()";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentId);
                statement.setString(2, childName);
                statement.executeUpdate();
            }

            connection.commit();

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query2);
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add child category to database", 3);
            return null;
        }
    }

    /**
     * Creates an item entry in table `league_items`
     *
     * @param leagueId ID of item's league
     * @param dataId ID of item
     * @return True on success
     */
    public boolean createLeagueItem(Integer leagueId, Integer dataId) {
        String query1 = "INSERT INTO league_items (id_l, id_d) VALUES (?, ?)" +
                        "ON DUPLICATE KEY UPDATE id_l = id_l";
        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, leagueId);
                statement.setInt(2, dataId);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not create item in database", 3);
            return false;
        }
    }

    /**
     * Creates an item data entry in table `data_itemData`
     *
     * @param item Item object to index
     * @param parentCategoryId ID of item's parent category
     * @param childCategoryId ID of item's child category
     * @return ID of created item data entry on success, null on failure
     */
    public Integer indexItemData(Item item, Integer parentCategoryId, Integer childCategoryId) {
        String query1 = "INSERT INTO data_itemData (" +
                        "    id_cp, id_cc, name, type, frame, tier, lvl, " +
                        "    quality, corrupted, links, var, `key`, icon) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String query2 = "SELECT LAST_INSERT_ID()";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentCategoryId);

                if (childCategoryId == null) statement.setNull(2, 0);
                else statement.setInt(2, childCategoryId);

                statement.setString(3, item.getName());
                statement.setString(4, item.getType());
                statement.setInt(5, item.getFrame());

                statement.setString(6, item.getTier());
                statement.setString(7, item.getLevel());
                statement.setString(8, item.getQuality());
                statement.setString(9, item.isCorrupted());
                statement.setString(10, item.getLinks());
                statement.setString(11, item.getVariation());
                statement.setString(12, item.getUniqueKey());
                statement.setString(13, Misc.formatIconURL(item.getIcon()));

                statement.executeUpdate();
            }

            connection.commit();

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query2);
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add item data to database", 3);
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Data uploads
    //------------------------------------------------------------------------------------------------------------

    /**
     * Adds an item entry in table `league_entries`
     *
     * @param entrySet Set of RawEntry objects to upload
     * @return True on success
     */
    public boolean uploadRaw(Set<RawEntry> entrySet) {
        String query =  "INSERT INTO league_entries ( " +
                        "    id_l, id_d, price, account, itemid) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "    approved = 0, " +
                        "    price = VALUES(price) ";

        try {
            if (connection.isClosed()) return false;
            int count = 0;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (RawEntry rawEntry : entrySet) {
                    statement.setInt(1, rawEntry.getLeagueId());
                    statement.setInt(2, rawEntry.getItemId());
                    statement.setString(3, rawEntry.getPriceAsRoundedString());
                    statement.setString(4, rawEntry.getAccountName());
                    statement.setString(5, rawEntry.getId());
                    statement.addBatch();

                    if (++count % 100 == 0) statement.executeBatch();
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

    /**
     * Adds/updates league entries in table `data_leagues`
     *
     * @param leagueEntries List of LeagueEntry objects to upload
     * @return True on success
     */
    public boolean updateLeagues(List<LeagueEntry> leagueEntries) {
        // Use transactions to avoid incrementing the auto incremented id row with "ON DUPLICATE UPDATE" on every
        // update, which would result in size ~8,000 index gaps.

        String query =  "BEGIN; " +
                        "    SET @name = ?; " +
                        "    SET @start = ?; " +
                        "    SET @end = ?; " +
                        "    SET @id = (SELECT id FROM data_leagues WHERE name = @name);" +

                        "    INSERT IGNORE INTO data_leagues (id, name, display, start, end) " +
                        "    VALUES(@id, @name, @name, @start, @end);" +

                        "    UPDATE data_leagues SET " +
                        "        active = 1, " +
                        "        start = @start, " +
                        "        end = @end " +
                        "    WHERE name = @name; " +
                        "COMMIT; ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getName());
                    statement.setString(2, leagueEntry.getStartAt());
                    statement.setString(3, leagueEntry.getEndAt());
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
     * Updates the change ID in table `data_changeId`
     *
     * @param id Change ID to upload
     * @return True on success
     */
    public boolean updateChangeID(String id) {
        String query =  "UPDATE `data_changeId` SET `changeId` = ?, `time` = CURRENT_TIMESTAMP";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update database change id", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Data calculation
    //------------------------------------------------------------------------------------------------------------

    /**
     * Calculates mean price for items in table `league_item` based on approved entries in `league_entries`
     *
     * @return True on success
     */
    public boolean calculateMean() {
        String query =  "UPDATE league_items AS i" +
                        "    JOIN (" +
                        "        SELECT id_l, id_d, TRUNCATE(IFNULL(AVG(price), 0.0), 4) AS avg " +
                        "        FROM league_entries " +
                        "        WHERE approved = 1 " +
                        "        GROUP BY id_l, id_d " +
                        "    ) AS tmp ON tmp.id_l = i.id_l AND tmp.id_d = i.id_d " +
                        "SET i.mean = tmp.avg ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate mean", 3);
            return false;
        }
    }

    /**
     * Calculates median price for items in table `league_item` based on approved entries in `league_entries`
     *
     * @return True on success
     */
    public boolean calculateMedian() {
        String query =  "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "    SELECT id_l, id_d, MEDIAN(price) AS median " +
                        "    FROM league_entries " +
                        "    WHERE approved = 1 " +
                        "    GROUP BY id_l, id_d " +
                        ") AS e ON i.id_l = e.id_l AND i.id_d = e.id_d " +
                        "SET i.median = e.median ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate median", 3);
            return false;
        }
    }

    /**
     * Calculates median price for items in table `league_item` based on any entries in `league_entries`
     *
     * @return True on success
     */
    public boolean calculateVolatileMedian() {
        String query =  "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "    SELECT id_l, id_d, MEDIAN(price) AS median " +
                        "    FROM league_entries " +
                        "    WHERE approved = 1 " +
                        "    GROUP BY id_l, id_d " +
                        ") AS e ON i.id_l = e.id_l AND i.id_d = e.id_d " +
                        "SET i.median = e.median " +
                        "WHERE volatile = 1 ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate median", 3);
            return false;
        }
    }

    /**
     * Calculates mode price for items in table `league_item` based on approved entries in `league_entries`
     *
     * @return True on success
     */
    public boolean calculateMode() {
        String query =  "UPDATE league_items AS i " +
                        "JOIN ( " +
                        "    SELECT id_l, id_d, stats_mode(price) AS mode " +
                        "    FROM league_entries " +
                        "    WHERE approved = 1 " +
                        "    GROUP BY id_l, id_d " +
                        ") AS e ON i.id_l = e.id_l AND i.id_d = e.id_d " +
                        "SET i.mode = e.mode ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate mode", 3);
            return false;
        }
    }

    /**
     * Calculates exalted price for items in table `league_item` based on exalted prices in same table
     *
     * @return True on success
     */
    public boolean calculateExalted() {
        String query =  "UPDATE league_items AS i1 " +
                        "JOIN ( " +
                        "    SELECT i2.id_l, i2.mean FROM league_items AS i2 " +
                        "    JOIN data_itemData AS did ON i2.id_d = did.id " +
                        "    WHERE did.frame = 5 AND did.name = 'Exalted Orb' " +
                        ") AS exPrice ON i1.id_l = exPrice.id_l " +
                        "SET i1.exalted = TRUNCATE(i1.mean / exPrice.mean, 4) " +
                        "WHERE i1.mean > 0; ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate exalted", 3);
            return false;
        }
    }


    /**
     * Calculates quantity for items in table `league_item` based on history entries from table `league_history`
     *
     * @return True on success
     */
    public boolean calcQuantity() {
        String query =  "UPDATE league_items AS i  " +
                        "JOIN ( " +
                        "    SELECT id_l, id_d, IFNULL(SUM(inc), 0) AS quantity " +
                        "    FROM league_history_hourly_rolling " +
                        "    WHERE time > ADDDATE(NOW(), INTERVAL -24 HOUR) " +
                        "    GROUP BY id_l, id_d " +
                        ") AS h ON h.id_l = i.id_l AND h.id_d = i.id_d " +
                        "SET i.quantity = h.quantity ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate quantity", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Flag updates
    //------------------------------------------------------------------------------------------------------------

    /**
     * Updates volatile state for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean updateVolatile() {
        String query1 = "UPDATE league_items " +
                        "SET volatile = IF(`dec` > 0 && inc > 0 && `dec` / inc > ?, 1, 0);";

        String query2 = "UPDATE league_entries AS e " +
                        "JOIN league_items AS i ON e.id_d = i.id_d " +
                        "SET e.approved = 0 " +
                        "WHERE i.volatile = 1 AND e.approved = 1";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setDouble(1, Config.entry_volatileRatio);
                statement.executeUpdate();
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query2);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update volatile status", 3);
            return false;
        }
    }

    /**
     * Updates approved state for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean updateApproved(){
        String query =  "UPDATE league_entries AS e " +
                        "JOIN league_items AS i ON i.id_l = e.id_l AND i.id_d = e.id_d " +
                        "SET e.approved = 1 " +
                        "WHERE e.approved = 0 " +
                        "    AND (" +
                        "        i.median = 0 " +
                        "        OR e.price BETWEEN i.median / i.multiplier AND i.median * i.multiplier " +
                        "    )";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update approved state", 3);
            return false;
        }
    }

    /**
     * Updates multipliers for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean updateMultipliers(){
        String query =  "UPDATE league_items " +
                        "SET multiplier = IF(? - quantity / ? < ?, ?, ? - quantity / ?)";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setDouble(1, Config.entry_approvedMax);
                statement.setDouble(2, Config.entry_approvedDiv);
                statement.setDouble(3, Config.entry_approvedMin);
                statement.setDouble(4, Config.entry_approvedMin);
                statement.setDouble(5, Config.entry_approvedMax);
                statement.setDouble(6, Config.entry_approvedDiv);

                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update multipliers", 3);
            return false;
        }
    }

    /**
     * Updates counters for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean updateCounters(){
        String query1 = "UPDATE league_items AS i " +
                        "    JOIN ( " +
                        "        SELECT id_l, id_d, COUNT(*) AS count " +
                        "        FROM league_entries " +
                        "        WHERE time > ADDDATE(NOW(), INTERVAL -60 SECOND)" +
                        "           AND approved = 1" +
                        "        GROUP BY id_l, id_d" +
                        "    ) AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                        "SET " +
                        "    i.count = i.count + e.count, " +
                        "    i.inc = i.inc + e.count ";

        String query2 = "UPDATE league_items AS i " +
                        "    JOIN ( " +
                        "        SELECT id_l, id_d, COUNT(*) AS count " +
                        "        FROM league_entries " +
                        "        WHERE time > ADDDATE(NOW(), INTERVAL -60 SECOND) " +
                        "           AND approved = 0" +
                        "        GROUP BY id_l, id_d" +
                        "    ) AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                        "SET " +
                        "    i.count = i.count + e.count, " +
                        "    i.inc = i.inc + e.count, " +
                        "    i.dec = i.dec + e.count ";


        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query1);
                statement.executeUpdate(query2);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update counters", 3);
            return false;
        }
    }

    /**
     * Resets counters for entries in table `league_items`
     *
     * @return True on success
     */
    public boolean resetCounters() {
        String query =  "UPDATE league_items SET inc = 0, `dec` = 0 ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not reset counters", 3);
            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Output file generation
    //------------------------------------------------------------------------------------------------------------

    /**
     * Loads provided List with item data from database
     *
     * @param parcel List that will contain ItemdataEntry entries
     * @return True on success
     */
    public boolean getItemdata(List<ItemdataEntry> parcel) {
        String query =  "SELECT " +
                        "    did.id , " +
                        "    did.name, did.type, did.frame, " +
                        "    did.tier, did.lvl, did.quality, did.corrupted, " +
                        "    did.links, did.var, did.key, did.icon, " +
                        "    cp.name AS cpName, cc.name AS ccName " +
                        "FROM data_itemData AS did " +
                        "LEFT JOIN category_parent AS cp ON cp.id = did.id_cp " +
                        "LEFT JOIN category_child AS cc ON cc.id = did.id_cc ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    ItemdataEntry entry = new ItemdataEntry();
                    entry.load(resultSet);
                    parcel.add(entry);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not get itemdata", 3);
            return false;
        }
    }

    /**
     * Loads provided List with item data and prices from database
     *
     * @param leagueId ID of target league
     * @param categoryId ID of target parent category
     * @param parcelEntryList List that will contain ParcelEntry entries
     * @return True on success
     */
    public boolean getOutputData(int leagueId, int categoryId, List<ParcelEntry> parcelEntryList) {
        String query =  "SELECT " +
                        "    i.id_l, i.id_d, i.mean, i.exalted, i.quantity, " +
                        "    GROUP_CONCAT(hdr.mean ORDER BY hdr.time ASC) AS history, " +
                        "    did.name, did.type, did.frame, " +
                        "    did.tier, did.lvl, did.quality, did.corrupted, " +
                        "    did.links, did.var, did.icon, " +
                        "    cc.name AS ccName " +
                        "FROM league_items AS i " +
                        "JOIN data_itemData AS did ON i.id_d = did.id " +
                        "LEFT JOIN category_child AS cc ON did.id_cc = cc.id " +
                        "JOIN league_history_daily_rolling AS hdr ON i.id_l = hdr.id_l AND i.id_d = hdr.id_d " +
                        "WHERE i.id_l = ? AND did.id_cp = ? " +
                        "GROUP BY i.id_l, i.id_d " +
                        "ORDER BY i.mean DESC ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, leagueId);
                statement.setInt(2, categoryId);

                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    ParcelEntry parcelEntry = new ParcelEntry(resultSet);
                    parcelEntryList.add(parcelEntry);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not get output items", 3);
            return false;
        }
    }

    /**
     * Adds/updates output file entries in table `data_outputFiles`
     *
     * @param league Name of target league
     * @param category Name of target parent category
     * @param path Path to output file
     * @return True on success
     */
    public boolean addOutputFile(String league, String category, String path) {
        String query =  "INSERT INTO data_outputFiles (league, category, path) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE path = VALUES(path)";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, league);
                statement.setString(2, category);
                statement.setString(3, path);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add output file to database", 3);
            return false;
        }
    }

    /**
     * Loads provided List with output file paths from database
     *
     * @param pathList List that will contain output file path entries
     * @return True on success
     */
    public boolean getOutputFiles(List<String> pathList) {
        String query =  "SELECT * FROM data_outputFiles";

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

    //------------------------------------------------------------------------------------------------------------
    // History entry management
    //------------------------------------------------------------------------------------------------------------

    /**
     * Copies data from table `league_items` to table `league_history_minutely_rolling` every minute
     * on a rolling basis with a history of 60 minutes only if those item entries belong to an active league
     *
     * @return True on success
     */
    public boolean addMinutely() {
        String query =  "INSERT INTO league_history_minutely_rolling ( " +
                        "    id_l, id_d, mean, median, mode, exalted) " +
                        "SELECT id_l, id_d, mean, median, mode, exalted  " +
                        "FROM league_items AS i " +
                        "JOIN data_leagues AS l ON i.id_l = l.id " +
                        "WHERE l.active = 1 ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add minutely", 3);
            return false;
        }
    }

    /**
     * Copies data from table `league_history_minutely_rolling` to table `league_history_hourly_rolling` every hour
     * on a rolling basis with a history of 24 hours
     *
     * @return True on success
     */
    public boolean addHourly() {
        String query =  "INSERT INTO league_history_hourly_rolling (" +
                        "    id_l, id_d, volatile, mean, median, mode, exalted, " +
                        "    count, quantity, inc, `dec`)" +
                        "SELECT " +
                        "    i.id_l, i.id_d, i.volatile, " +
                        "    TRUNCATE(AVG(h.mean), 4), TRUNCATE(AVG(h.median ), 4), " +
                        "    TRUNCATE(AVG(h.mode), 4), TRUNCATE(AVG(h.exalted), 4), " +
                        "    i.count, i.quantity, i.inc, i.dec " +
                        "FROM league_history_minutely_rolling AS h " +
                        "JOIN league_items AS i ON h.id_l = i.id_l AND h.id_d = i.id_d " +
                        "WHERE h.time > ADDDATE(NOW(), INTERVAL -60 MINUTE) " +
                        "GROUP BY h.id_l, h.id_d";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add hourly", 3);
            return false;
        }
    }

    /**
     * Copies data from table `league_history_hourly_rolling` to table `league_history_daily_rolling` every day
     * on a rolling basis with a history of 120 days
     *
     * @return True on success
     */
    public boolean addDaily() {
        String query =  "INSERT INTO league_history_daily_rolling (" +
                        "    id_l, id_d, volatile, mean, median, mode, exalted, " +
                        "    count, quantity, inc, `dec`)" +
                        "SELECT " +
                        "    h.id_l, h.id_d, i.volatile, " +
                        "    TRUNCATE(AVG(h.mean), 4), TRUNCATE(AVG(h.median ), 4), " +
                        "    TRUNCATE(AVG(h.mode), 4), TRUNCATE(AVG(h.exalted), 4), " +
                        "    i.count, i.quantity, i.inc, i.dec " +
                        "FROM league_history_hourly_rolling AS h " +
                        "JOIN league_items AS i ON h.id_l = i.id_l AND h.id_d = i.id_d " +
                        "WHERE h.time > ADDDATE(NOW(), INTERVAL -24 HOUR) " +
                        "GROUP BY h.id_l, h.id_d";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add daily", 3);
            return false;
        }
    }

    /**
     * Removes entries from table `league_entries` that exceed the specified capacity
     *
     * @return True on success
     */
    public boolean removeOldItemEntries() {
        String query =  "DELETE del FROM league_entries AS del " +
                        "JOIN (" +
                        "    SELECT id_l, id_d, (" +
                        "        SELECT e.time" +
                        "        FROM league_entries AS e" +
                        "        WHERE e.id_l = d.id_l AND e.id_d = d.id_d" +
                        "        ORDER BY e.time DESC" +
                        "        LIMIT 1 OFFSET ?" +
                        "   ) AS time" +
                        "    FROM league_entries AS d" +
                        "    GROUP BY id_l, id_d" +
                        "    HAVING time IS NOT NULL" +
                        ") AS tmp " +
                        "ON del.id_l = tmp.id_l AND del.id_d = tmp.id_d AND del.time <= tmp.time";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, Config.entry_maxCount);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not remove old item entries", 3);
            return false;
        }
    }
}
