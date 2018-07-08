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

    //------------------------------------------------------------------------------------------------------------
    // DB controllers
    //------------------------------------------------------------------------------------------------------------

    public void connect() {
        try {
            connection = DriverManager.getConnection(Config.db_address, Config.db_username, Config.getDb_password());
            connection.setCatalog(Config.db_database);
            connection.setAutoCommit(false);
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
    // Access methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Fills provided List with LeagueEntry objects created from data from database
     *
     * @param leagueEntries
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
     * Compares provided league entries to ones present in database, updates any changes and adds missing leagues
     *
     * @param leagueEntries List of the most recent LeagueEntry objects
     * @return True on success
     */
    public boolean updateLeagues(List<LeagueEntry> leagueEntries) {
        String query =  "INSERT INTO data_leagues (active, name, start, end) " +
                        "   VALUES (1, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "   active = VALUES(active), " +
                        "   start = VALUES(start), " +
                        "   end = VALUES(end)";

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
     * Removes any previous and updates the changeID record in table `change_id`
     *
     * @param id New changeID string to store
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

    /**
     * Gets a list of parent and child categories and their display names from the database
     *
     * @param categoryRelations
     * @return Id of category or null on failure
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

    //------------------------------------------------------------------------------------------------------------
    // Get item IDs
    //------------------------------------------------------------------------------------------------------------

    /**
     * Gets all item keys and IDs from every league item table
     *
     * @return True on success
     */
    public boolean getItemIds(Map<Integer, List<Integer>> leagueToIds, Map<String, Integer> keyToId) {
        String query =  "SELECT i.id_l, did.id as id_d, did.key " +
                        "FROM league_items AS i " +
                        "JOIN data_itemData AS did " +
                        "  ON i.id_d = did.id " +
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

    //------------------------------------------------------------------------------------------------------------
    // Indexing
    //------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new item entry in the league-specific item table if the item has not previously been indexed
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

    public Integer indexItemData(Item item, Integer parentCategoryId, Integer childCategoryId) {
        String query1 = "INSERT INTO data_itemData (" +
                        "  id_cp, id_cc, name, type, frame, tier, lvl, " +
                        "  quality, corrupted, links, var, `key`, icon) " +
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
    // Entry management
    //------------------------------------------------------------------------------------------------------------

    /**
     * Queries database and fills out provided CurrencyMap with CurrencyItems
     *
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
     * Fills provided Map with currency alias data from database
     *
     * @param currencyAliasToName
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

    public boolean uploadRaw(Map<Integer, Map<Integer, Map<String, RawEntry>>> mergedMap) {
        String query =  "INSERT INTO league_entries ( " +
                        "  id_l, id_d, price, account, itemid) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  approved = 0, " +
                        "  price = VALUES(price) ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                int count = 0;

                for (Integer leagueId : mergedMap.keySet()) {
                    Map<Integer, Map<String, RawEntry>> idMap = mergedMap.get(leagueId);

                    for (Integer itemId : idMap.keySet()) {
                        Map<String, RawEntry> accountToRawEntry = idMap.get(itemId);

                        for (String account : accountToRawEntry.keySet()) {
                            RawEntry rawEntry = accountToRawEntry.get(account);

                            statement.setInt(1, leagueId);
                            statement.setInt(2, itemId);
                            statement.setString(3, rawEntry.getPriceAsRoundedString());
                            statement.setString(4, account);
                            statement.setString(5, rawEntry.getItemId());
                            statement.addBatch();

                            if (++count % 1000 == 0) statement.executeBatch();
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

    public boolean calculateMean() {
        String query =  "UPDATE league_items AS i" +
                        "  JOIN (" +
                        "    SELECT id_l, id_d, TRUNCATE(IFNULL(AVG(price), 0.0), 4) AS avg " +
                        "    FROM league_entries " +
                        "    WHERE approved = 1 " +
                        "    GROUP BY id_l, id_d " +
                        "  ) AS tmp ON tmp.id_l = i.id_l AND tmp.id_d = i.id_d " +
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

    public boolean calculateMode(Integer leagueId, List<Integer> idList) {
        String query =  "UPDATE league_items " +
                        "SET mode = IFNULL(( " +
                        "    SELECT price FROM league_entries " +
                        "    WHERE id_l = ? AND id_d = ? AND approved = 1 " +
                        "    GROUP BY price " +
                        "    ORDER BY COUNT(*) DESC " +
                        "    LIMIT 1 " +
                        "), 0.0) " +
                        "WHERE id_l = ? AND id_d = ?";

        try {
            if (connection.isClosed()) return false;
            int count = 0;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Integer id : idList) {
                    statement.setInt(1, leagueId);
                    statement.setInt(2, id);
                    statement.setInt(3, leagueId);
                    statement.setInt(4, id);
                    statement.addBatch();

                    if (++count % 100 == 0) statement.executeBatch();
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

    public boolean updateApproved(){
        String query =  "UPDATE league_entries AS e " +
                        "JOIN league_items AS i ON i.id_l = e.id_l AND i.id_d = e.id_d " +
                        "SET e.approved = 1 " +
                        "WHERE e.approved = 0 " +
                        "  AND (" +
                        "    i.median = 0 " +
                        "    OR e.price BETWEEN i.median / i.multiplier AND i.median * i.multiplier " +
                        "  )";

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

    public boolean updateCounters(){
        String query =  "UPDATE league_items AS i " +
                        "  JOIN ( " +
                        "    SELECT id_l, id_d, approved, COUNT(*) AS count " +
                        "    FROM league_entries " +
                        "    WHERE time > ADDDATE(NOW(), INTERVAL -50 SECOND)" +
                        "    GROUP BY id_l, id_d, approved" +
                        "  ) AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                        "SET " +
                        "  i.count = i.count + e.count, " +
                        "  i.inc = i.inc + e.count, " +
                        "  i.dec = IF(e.approved = 0, i.dec, i.dec + e.count) ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
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
                        "  did.id , " +
                        "  did.name, did.type, did.frame, " +
                        "  did.tier, did.lvl, did.quality, did.corrupted, " +
                        "  did.links, did.var, did.key, did.icon, " +
                        "  cp.name AS cpName, cc.name AS ccName " +
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

    public boolean getOutputItems(Integer leagueId, Map<Integer, ParcelEntry> parcel, int category) {
        String query =  "SELECT " +
                        "  i.id_l, i.id_d, i.mean, i.exalted, i.quantity, " +
                        "  did.name, did.type, did.frame, " +
                        "  did.tier, did.lvl, did.quality, did.corrupted, " +
                        "  did.links, did.var, did.icon, " +
                        "  cc.name AS ccName " +
                        "FROM league_items AS i " +
                        "JOIN data_itemData AS did ON i.id_d = did.id " +
                        "LEFT JOIN category_child AS cc ON did.id_cc = cc.id " +
                        "WHERE i.id_l = ? AND did.id_cp = ? " +
                        "ORDER BY i.mean DESC ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, leagueId);
                statement.setInt(2, category);
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

    public boolean getOutputHistory(Integer leagueId, Map<Integer, ParcelEntry> parcel) {
        String query  = "SELECT id_d, mean FROM league_history " +
                        "WHERE id_l = ? AND id_ch = 3 " +
                        "ORDER BY time DESC ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, leagueId);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    Integer id = resultSet.getInt("id_d");
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

    //--------------------------
    // History entry management
    //--------------------------

    public boolean addMinutely() {
        String query =  "INSERT INTO league_history ( " +
                        "   id_l, id_ch, id_d, mean, median, mode, exalted) " +
                        "SELECT id_l, 1, id_d, mean, median, mode, exalted " +
                        "FROM league_items ";

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

    public boolean addHourly() {
        String query =  "INSERT INTO league_history (" +
                        "   id_l, id_ch, id_d, volatile, mean, median, mode, exalted, " +
                        "   count, quantity, inc, `dec`)" +
                        "SELECT " +
                        "   i.id_l, 2, i.id_d, i.volatile, " +
                        "   AVG(h.mean), AVG(h.median), AVG(h.mode), AVG(h.exalted), " +
                        "   i.count, i.quantity, i.inc, i.dec " +
                        "FROM league_history AS h " +
                        "JOIN league_items AS i ON h.id_l = i.id_l AND h.id_d = i.id_d " +
                        "WHERE h.id_ch = 1 " +
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

    public boolean addDaily() {
        String query =  "INSERT INTO league_history (" +
                        "   id_l, id_ch, id_d, volatile, mean, median, mode, exalted, " +
                        "   count, quantity, inc, `dec`)" +
                        "SELECT " +
                        "   h.id_l, 3, h.id_d, i.volatile, " +
                        "   AVG(h.mean), AVG(h.median), AVG(h.mode), AVG(h.exalted), " +
                        "   i.count, i.quantity, i.inc, i.dec " +
                        "FROM league_history AS h " +
                        "JOIN league_items AS i ON h.id_l = i.id_l AND h.id_d = i.id_d " +
                        "WHERE h.id_ch = 2 " +
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

    public boolean calcQuantity() {
        String query =  "UPDATE league_items AS i " +
                        "SET i.quantity = i.inc + (" +
                        "  SELECT IFNULL(SUM(h.inc), 0) FROM league_history AS h" +
                        "  WHERE h.id_ch = 2 AND h.id_l = i.id_l AND h.id_d = i.id_d" +
                        ")";

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

    public boolean removeOldHistoryEntries(int type, String interval) {
        String query =  "DELETE FROM league_history " +
                        "WHERE id_ch = ? " +
                        "AND time < ADDDATE(NOW(), INTERVAL -"+ interval +")";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, type);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not remove old history entries", 3);
            return false;
        }
    }

    public boolean removeOldItemEntries() {
        String query =  "DELETE del FROM league_entries AS del " +
                        "JOIN (" +
                        "  SELECT id_l, id_d, (" +
                        "    SELECT e.time" +
                        "    FROM league_entries AS e" +
                        "    WHERE e.id_l = d.id_l AND e.id_d = d.id_d" +
                        "    ORDER BY e.time DESC" +
                        "    LIMIT 1 OFFSET ?" +
                        "  ) AS time" +
                        "  FROM league_entries AS d" +
                        "  GROUP BY id_l, id_d" +
                        "  HAVING time IS NOT NULL" +
                        ") AS tmp " +
                        "ON del.id_l = tmp.id_l AND del.id_d = tmp.id_d AND del.time <= tmp.time";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, Config.entry_maxCount);
                int total = statement.executeUpdate();

                System.out.printf("Total %d old entries deleted\n", total);
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
