package watch.poe.database;

import watch.poe.Config;
import watch.poe.item.Item;
import watch.poe.Main;
import watch.poe.Misc;
import watch.poe.account.AccountRelation;
import watch.poe.item.Key;
import watch.poe.league.LeagueEntry;
import watch.poe.pricer.AccountEntry;
import watch.poe.pricer.FileEntry;
import watch.poe.pricer.RawEntry;
import watch.poe.pricer.itemdata.ItemdataEntry;
import watch.poe.pricer.ParcelEntry;
import watch.poe.relations.CategoryEntry;

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
        String query1 = "INSERT INTO account_accounts   (name) VALUES (?) ON DUPLICATE KEY UPDATE seen = NOW(); ";
        String query2 = "INSERT INTO account_characters (name) VALUES (?) ON DUPLICATE KEY UPDATE seen = NOW(); ";

        String query3 = "INSERT INTO account_relations (id_l, id_a, id_c) " +
                        "SELECT ?, " +
                        "  (SELECT id FROM account_accounts   WHERE name = ? LIMIT 1), " +
                        "  (SELECT id FROM account_characters WHERE name = ? LIMIT 1) " +
                        "ON DUPLICATE KEY UPDATE seen = NOW(); ";

        try {
            if (connection.isClosed()) return false;
            int counter;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                counter = 0;

                for (AccountEntry accountEntry : accountSet) {
                    statement.setString(1, accountEntry.account);
                    statement.addBatch();

                    if (++counter % 500 == 0) statement.executeBatch();
                }

                statement.executeBatch();
            }


            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                counter = 0;

                for (AccountEntry accountEntry : accountSet) {
                    statement.setString(1, accountEntry.character);
                    statement.addBatch();

                    if (++counter % 500 == 0) statement.executeBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement = connection.prepareStatement(query3)) {
                counter = 0;

                for (AccountEntry accountEntry : accountSet) {
                    statement.setInt(1, accountEntry.league);
                    statement.setString(2, accountEntry.account);
                    statement.setString(3, accountEntry.character);
                    statement.addBatch();

                    if (++counter % 500 == 0) statement.executeBatch();
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

    /**
     * Gets potential accounts that might have changed names
     *
     * @param accountRelations Empty List of AccountRelation to be filled
     * @return True on success
     */
    public boolean getAccountRelations(List<AccountRelation> accountRelations) {
        String query =  "SELECT oldAcc.id         AS oldAccountId, " +
                        "       oldAcc.accName    AS oldAccountName, " +
                        "       newAcc.id         AS newAccountId, " +
                        "       newAcc.accName    AS newAccountName, " +
                        "       COUNT(oldAcc.idC) AS matches " +
                        "FROM ( " +
                        "    SELECT   a.id   AS id, " +
                        "             a.name AS accName, " +
                        "             c.id   AS idC, " +
                        "             a.seen AS seen " +
                        "    FROM     account_relations  AS r " +
                        "    INNER JOIN ( " +
                        "        SELECT   id_c " +
                        "        FROM     account_relations  " +
                        "        GROUP BY id_c  " +
                        "        HAVING   COUNT(*) > 1 " +
                        "    ) AS tmp1 ON tmp1.id_c = r.id_c " +
                        "    JOIN     account_accounts   AS a ON a.id = r.id_a " +
                        "    JOIN     account_characters AS c ON c.id = r.id_c " +
                        ") AS oldAcc " +
                        "JOIN ( " +
                        "    SELECT   a.id   AS id, " +
                        "             a.name AS accName, " +
                        "             c.id   AS idC, " +
                        "             a.seen AS seen " +
                        "    FROM     account_relations  AS r " +
                        "    INNER JOIN ( " +
                        "        SELECT   id_c " +
                        "        FROM     account_relations  " +
                        "        GROUP BY id_c  " +
                        "        HAVING   COUNT(*) > 1 " +
                        "    ) AS tmp1 ON tmp1.id_c = r.id_c " +
                        "    JOIN     account_accounts   AS a ON a.id = r.id_a " +
                        "    JOIN     account_characters AS c ON c.id = r.id_c " +
                        ") AS newAcc " +
                        "  ON      oldAcc.idC  = newAcc.idC " +
                        "    AND   oldAcc.seen < newAcc.seen " +
                        "    AND   oldAcc.id  != newAcc.id " +
                        "LEFT JOIN account_history AS h " +
                        "  ON      h.id_old    = oldAcc.id " +
                        "    AND   h.id_new    = newAcc.id " +
                        "WHERE     h.id_old IS NULL " +
                        "GROUP BY  oldAcc.id, newAcc.id " +
                        "HAVING    matches > 0 " +
                        "ORDER BY  oldAcc.seen ASC, newAcc.seen ASC; ";

        ArrayList<Long> filter = new ArrayList<>();

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    long id = resultSet.getLong("oldAccountId");
                    if (filter.contains(id)) continue;
                    filter.add(id);

                    AccountRelation accountRelation = new AccountRelation();
                    accountRelation.load(resultSet);
                    accountRelations.add(accountRelation);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not get account name relations", 3);
            return false;
        }
    }

    /**
     * Creates an entry in table `account_history`, indicating account name change
     *
     * @param accountRelations List of AccountRelation to be created
     * @return True on success
     */
    public boolean createAccountRelation(List<AccountRelation> accountRelations) {
        String query = "INSERT INTO account_history (id_old, id_new, moved) VALUES (?, ?, ?); ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (AccountRelation accountRelation : accountRelations) {
                    statement.setLong(1, accountRelation.oldAccountId);
                    statement.setLong(2, accountRelation.newAccountId);
                    statement.setInt(3, accountRelation.moved);
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not create account relation", 3);
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
        String query = "SELECT * FROM data_leagues WHERE active = 1; ";

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

        String query =  "SELECT    cp.name AS parentName, " +
                        "          cc.name AS childName, " +
                        "          cp.id AS parentId, " +
                        "          cc.id AS childId " +
                        "FROM      category_parent AS cp " +
                        "LEFT JOIN category_child  AS cc " +
                        "  ON      cp.id = cc.id_cp; ";

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
    public boolean getItemIds(Map<Integer, List<Integer>> leagueToIds, Map<Key, Integer> keyToId) {
        String query =  "SELECT  i.id_l, did.id, did.name, did.type, " +
                        "        did.frame, did.tier, did.lvl, " +
                        "        did.quality, did.corrupted, " +
                        "        did.links, did.ilvl, did.var " +
                        "FROM    league_items AS i " +
                        "JOIN    data_itemData AS did " +
                        "  ON    i.id_d = did.id ";

        try {
            if (connection.isClosed()) {
                return false;
            }

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    Integer leagueId = resultSet.getInt("id_l");
                    Integer dataId = resultSet.getInt("id");

                    List<Integer> idList = leagueToIds.getOrDefault(leagueId, new ArrayList<>());
                    idList.add(dataId);
                    leagueToIds.putIfAbsent(leagueId, idList);

                    keyToId.put(new Key(resultSet), dataId);
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
     * Creates a map containing currency price data from database in the format of:
     * {leagueID: {currencyName: chaosValue}}
     *
     * @return Generated Map
     */
    public Map<Integer, Map<String, Double>> getCurrencyMap() {
        Map<Integer, Map<String, Double>> tmpCurrencyLeagueMap = new HashMap<>();

        String query =  "SELECT   i.id_l, did.name, i.median " +
                        "FROM     league_items  AS i " +
                        "JOIN     data_itemData AS did " +
                        "  ON     i.id_d = did.id " +
                        "WHERE    did.id_cp = 4 " +
                        "  AND    did.frame = 5 " +
                        "ORDER BY i.id_l; ";

        try {
            if (connection.isClosed()) return null;

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

            return tmpCurrencyLeagueMap;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query currency rates from database", 3);
            return null;
        }
    }

    /**
     * Loads provided Map with currency alias data from database
     *
     * @param currencyAliasToName Map that will contain currency alias - currency name relations
     * @return True on success
     */
    public boolean getCurrencyAliases(Map<String, String> currencyAliasToName) {
        String query =  "SELECT ci.name AS name, " +
                        "       ca.name AS alias " +
                        "FROM   data_currencyItems   AS ci " +
                        "JOIN   data_currencyAliases AS ca " +
                        "  ON   ci.id = ca.id_ci; ";

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
        String query1 = "INSERT INTO category_parent (name) VALUES (?); ";

        String query2 = "SELECT id " +
                        "FROM   category_parent " +
                        "WHERE  name = ? " +
                        "LIMIT  1;";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setString(1, parentName);
                statement.executeUpdate();
            }

            connection.commit();

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setString(1, parentName);
                ResultSet resultSet = statement.executeQuery();
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

        String query2 = "SELECT id " +
                        "FROM   category_child " +
                        "WHERE  id_cp = ? " +
                        "  AND  name = ? " +
                        "LIMIT  1; ";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentId);
                statement.setString(2, childName);
                statement.executeUpdate();
            }

            connection.commit();

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setInt(1, parentId);
                statement.setString(2, childName);
                ResultSet resultSet = statement.executeQuery();
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
        String query  = "INSERT INTO league_items (id_l, id_d) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE id_l = id_l; ";
        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
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
                        "  id_cp, id_cc, name, type, frame, tier, lvl, " +
                        "  quality, corrupted, links, ilvl, var, icon) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";
        String query2 = "SELECT LAST_INSERT_ID(); ";

        try {
            if (connection.isClosed()) return null;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setInt(1, parentCategoryId);

                if (childCategoryId == null) statement.setNull(2, 0);
                else statement.setInt(2, childCategoryId);

                statement.setString(3, item.getName());
                statement.setString(4, item.getTypeLine());
                statement.setInt(5, item.getFrameType());

                if (item.getTier() == null) {
                    statement.setNull(6, 0);
                } else statement.setInt(6, item.getTier());

                if (item.getLevel() == null) {
                    statement.setNull(7, 0);
                } else statement.setInt(7, item.getLevel());

                if (item.getQuality() == null) {
                    statement.setNull(8, 0);
                } else statement.setInt(8, item.getQuality());

                if (item.getCorrupted() == null) {
                    statement.setNull(9, 0);
                } else statement.setInt(9, item.getCorrupted());

                if (item.getLinks() == null) {
                    statement.setNull(10, 0);
                } else statement.setInt(10, item.getLinks());

                if (item.getIlvl() == null) {
                    statement.setNull(11, 0);
                } else statement.setInt(11, item.getIlvl());

                statement.setString(12, item.getVariation());
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
                        "  id_l, id_d, price, account) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  approved = 0, " +
                        "  price = VALUES(price) ";

        try {
            if (connection.isClosed()) return false;
            int count = 0;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (RawEntry rawEntry : entrySet) {
                    statement.setInt(1, rawEntry.getLeagueId());
                    statement.setInt(2, rawEntry.getItemId());
                    statement.setString(3, rawEntry.getPriceAsRoundedString());
                    statement.setString(4, rawEntry.getAccountName());
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
        String query1 = "INSERT INTO data_leagues (" +
                        "  name, event) " +
                        "SELECT ?, ? " +
                        "FROM   DUAL " +
                        "WHERE  NOT EXISTS ( " +
                        "  SELECT 1 " +
                        "  FROM   data_leagues " +
                        "  WHERE  name = ? " +
                        "  LIMIT  1); ";

        String query2 = "UPDATE data_leagues " +
                        "SET    start    = ?, " +
                        "       end      = ?, " +
                        "       upcoming = 0," +
                        "       active   = 1 " +
                        "WHERE  name     = ? " +
                        "LIMIT  1; ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getName());
                    statement.setInt(2, leagueEntry.isEvent() ? 1 : 0);
                    statement.setString(3, leagueEntry.getName());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getStartAt());
                    statement.setString(2, leagueEntry.getEndAt());
                    statement.setString(3, leagueEntry.getName());
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
        String query =  "UPDATE data_changeId SET changeId = ?, time = CURRENT_TIMESTAMP; ";

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
        String query =  "UPDATE league_items AS i " +
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
                        "JOIN   league_items AS i " +
                        "  ON   e.id_d = i.id_d " +
                        "SET    e.approved = 0 " +
                        "WHERE  i.volatile = 1 " +
                        "  AND  e.approved = 1; ";

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
    // History entry management
    //------------------------------------------------------------------------------------------------------------

    /**
     * Copies data from table `league_items` to table `league_history_hourly_rolling` every hour
     * on a rolling basis with a history of 24 hours
     *
     * @return True on success
     */
    public boolean addHourly() {
        String query =  "INSERT INTO league_history_hourly_rolling ( " +
                        "  id_l, id_d, volatile, mean, median, mode, " +
                        "  exalted, count, quantity, inc, `dec`) " +
                        "SELECT id_l, id_d, volatile, mean, median, mode, " +
                        "       exalted, count, quantity, inc, `dec` " +
                        "FROM   league_items AS i " +
                        "JOIN   data_leagues AS l " +
                        "  ON   i.id_l = l.id " +
                        "WHERE  l.active = 1 ";

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
        String query =  "INSERT INTO league_history_daily_rolling ( " +
                        "  id_l, id_d, volatile, mean, median, mode, " +
                        "  exalted, count, quantity, inc, `dec`) " +
                        "SELECT id_l, id_d, volatile, mean, median, mode, " +
                        "       exalted, count, quantity, inc, `dec` " +
                        "FROM   league_items AS i " +
                        "JOIN   data_leagues AS l " +
                        "  ON   i.id_l = l.id " +
                        "WHERE  l.active = 1 ";

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
