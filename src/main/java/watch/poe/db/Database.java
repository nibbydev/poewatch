package poe.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Config;
import poe.manager.account.AccountRelation;
import poe.manager.entry.AccountEntry;
import poe.manager.entry.RawEntry;
import poe.manager.entry.StatusElement;
import poe.manager.entry.item.Item;
import poe.manager.entry.item.Key;
import poe.manager.entry.timer.Timer;
import poe.manager.entry.timer.TimerList;
import poe.manager.league.LeagueEntry;
import poe.manager.relation.CategoryEntry;

import java.sql.*;
import java.util.*;
//todo: split into some helper which offers connection/execution methods and classes that have it as parent or helper class to break it up into smaller chunks.
public class Database {
    private Connection connection;
    private static Logger logger = LoggerFactory.getLogger(Database.class);

    //------------------------------------------------------------------------------------------------------------
    // DB controllers
    //------------------------------------------------------------------------------------------------------------

    /**
     * Initializes connection to the MySQL database
     *
     * @return True on success
     */
    public boolean connect() {
        try {
            connection = DriverManager.getConnection(Config.db_address, Config.db_username, Config.getDb_password());
            connection.setCatalog(Config.db_database);
            connection.setAutoCommit(false);

            return true;
        } catch (SQLException ex) {
            logger.error(String.format("Failed to connect to databases ex=%s", ex.getMessage()), ex);
        }

        return false;
    }

    /**
     * Disconnects from the MySQL database
     */
    public void disconnect() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
     * Execute a update queries (each given as a String)
     *
     * @param queries query update strings to execute
     * @return true if success, else false
     */
    private boolean executeUpdateQueries(String... queries) {
        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                for (String query :
                        queries) {
                    //todo: add a new pojo that can represent the statement params - (index, value, type) & apply here

                    statement.executeUpdate(query);
                }
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
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
            logger.error(ex.getMessage(), ex);

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
        String query = "SELECT oldAcc.id         AS oldAccountId, " +
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
                "HAVING    matches > 1 " +
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
            logger.error(ex.getMessage(), ex);

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
            logger.error(ex.getMessage(), ex);

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

                // Empty provided list just in case
                leagueEntries.clear();

                while (resultSet.next()) {
                    leagueEntries.add(new LeagueEntry(resultSet));
                }
            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

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

        String query = "SELECT    cp.name AS parentName, " +
                "          cp.id   AS parentId, " +
                "          GROUP_CONCAT(cc.name) AS childNames, " +
                "          GROUP_CONCAT(cc.id) AS childIds " +
                "FROM      category_parent AS cp " +
                "JOIN      category_child  AS cc " +
                "  ON      cp.id = cc.id_cp " +
                "GROUP BY  cp.id; ";

        try {
            if (connection.isClosed()) {
                return false;
            }

            if (categoryRelations == null) {
                throw new SQLException("Provided map was null");
            }

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String[] childIds = resultSet.getString("childIds").split(",");
                    String[] childNames = resultSet.getString("childNames").split(",");

                    CategoryEntry categoryEntry = new CategoryEntry();
                    categoryEntry.setId(resultSet.getInt("parentId"));

                    for (int i = 0; i < childIds.length; i++) {
                        categoryEntry.addChild(childNames[i], Integer.parseInt(childIds[i]));
                    }

                    tmpCategoryRelations.putIfAbsent(resultSet.getString("parentName"), categoryEntry);
                }
            }

            categoryRelations.clear();
            categoryRelations.putAll(tmpCategoryRelations);

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Loads provided Maps with item ID data from database
     *
     * @param keyToId Empty map that will contain item Key - item ID relations
     * @return True on success
     */
    public boolean getItemData(Map<Key, Integer> keyToId) {
        Map<Key, Integer> tmpKeyToId = new HashMap<>();

        String query = "SELECT * FROM data_itemData; ";

        try {
            if (connection.isClosed()) {
                return false;
            }

            if (keyToId == null) {
                throw new SQLException("Provided map was null");
            }

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    tmpKeyToId.put(new Key(resultSet), resultSet.getInt("id"));
                }
            }

            keyToId.clear();
            keyToId.putAll(tmpKeyToId);

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }


    /**
     * Loads provided Maps with item ID data from database
     *
     * @param leagueIds Empty map that will contain league ID - list of item IDs relations
     * @return True on success
     */
    public boolean getLeagueItemIds(Map<Integer, List<Integer>> leagueIds) {
        Map<Integer, List<Integer>> tmpLeagueIds = new HashMap<>();

        String query = "SELECT   i.id_l, i.id_d " +
                "FROM     league_items_rolling AS i " +
                "JOIN     data_leagues AS l " +
                "  ON     i.id_l = l.id " +
                "WHERE    l.active = 1 " +
                "ORDER BY i.id_l ASC; ";

        try {
            if (connection.isClosed()) {
                return false;
            }

            if (leagueIds == null) {
                throw new SQLException("Provided map was null");
            }


            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    Integer leagueId = resultSet.getInt("id_l");
                    Integer itemId = resultSet.getInt("id_d");

                    List<Integer> idList = tmpLeagueIds.getOrDefault(leagueId, new ArrayList<>());
                    idList.add(itemId);
                    tmpLeagueIds.putIfAbsent(leagueId, idList);
                }
            }

            leagueIds.clear();
            leagueIds.putAll(tmpLeagueIds);

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

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

        String query = "SELECT   i.id_l, did.name, i.median " +
                "FROM     league_items_rolling  AS i " +
                "JOIN     data_itemData AS did " +
                "  ON     i.id_d = did.id " +
                "WHERE    did.id_cc = 11 " +
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
            logger.error(ex.getMessage(), ex);

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
        String query = "SELECT ci.name AS name, " +
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
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Loads provided Map with timer delay entries from database
     *
     * @param timeLog Empty map that will be filled with key - TimerList relations
     * @return True on success
     */
    public boolean getTimers(Map<String, TimerList> timeLog) {
        String query = "SELECT `key`, delay, type FROM data_timers ORDER BY time ASC;";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);
                int counter = 0;

                while (resultSet.next()) {
                    String key = resultSet.getString("key");
                    long delay = resultSet.getLong("delay");
                    Integer type = resultSet.getInt("type");

                    if (resultSet.wasNull()) type = null;
                    Timer.TimerType timerType = Timer.translate(type);

                    TimerList timerList = timeLog.getOrDefault(key, new TimerList(timerType));

                    // Truncate list if entry count exceeds limit
                    if (timerList.list.size() >= Config.timerLogHistoryLength) {
                        timerList.list.remove(0);
                    }

                    counter++;
                    timerList.list.add(delay);
                    timeLog.putIfAbsent(key, timerList);
                }


            }

            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

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
        String query = "INSERT INTO league_items_rolling (id_l, id_d) VALUES (?, ?) " +
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
            if (connection.isClosed()) return null;

            Key key = item.getKey();

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
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

            connection.commit();

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query2);
                return resultSet.next() ? resultSet.getInt(1) : null;
            }

        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

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
                        "  id_l, id_d, price, account, identifier) " +
                        "VALUES (?, ?, ?, ?, ?) " +
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
                    statement.setString(3, rawEntry.getPrice());
                    statement.setString(4, rawEntry.getAccountName());
                    statement.setString(5, rawEntry.getIdentifier());
                    statement.addBatch();

                    if (++count % 500 == 0) statement.executeBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

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
        String query1 = "INSERT INTO " +
                "  data_leagues (name) " +
                "SELECT ? " +
                "FROM   DUAL " +
                "WHERE  NOT EXISTS ( " +
                "  SELECT 1 " +
                "  FROM   data_leagues " +
                "  WHERE  name = ? " +
                "  LIMIT  1); ";

        String query2 = "UPDATE data_leagues " +
                "SET    start    = ?, " +
                "       end      = ?, " +
                "       upcoming = 0, " +
                "       active   = 1, " +
                "       event    = ?," +
                "       hardcore = ? " +
                "WHERE  name     = ? " +
                "LIMIT  1; ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getName());
                    statement.setString(2, leagueEntry.getName());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getStartAt());
                    statement.setString(2, leagueEntry.getEndAt());
                    statement.setInt(3, leagueEntry.isEvent() ? 1 : 0);
                    statement.setInt(4, leagueEntry.isHardcore() ? 1 : 0);
                    statement.setString(5, leagueEntry.getName());
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

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
        String query = "UPDATE data_changeId SET changeId = ?, time = CURRENT_TIMESTAMP; ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Uploads all latest timer delays to database
     *
     * @param timeLog Valid map of key - TimerList relations
     * @return True on success
     */
    public boolean uploadTimers(Map<String, TimerList> timeLog, StatusElement statusElement) {
        String query1 = "DELETE  del " +
                "FROM    data_timers AS del " +
                "JOIN ( " +
                "  SELECT   `key`, ( " +
                "    SELECT   t.time " +
                "    FROM     data_timers AS t " +
                "    WHERE    t.`key` = d.`key` " +
                "    ORDER BY t.time DESC " +
                "    LIMIT    4, 1 " +
                "  ) AS     time " +
                "  FROM     data_timers AS d " +
                "  GROUP BY d.`key` " +
                "  HAVING   time IS NOT NULL " +
                ") AS    tmp " +
                "  ON    del.`key` = tmp.`key` " +
                "    AND del.time <= tmp.time; ";

        String query = "INSERT INTO data_timers (`key`, type, delay) " +
                "VALUES (?, ?, ?)  ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query1);
            }

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String key : timeLog.keySet()) {
                    TimerList timerList = timeLog.get(key);

                    if (timerList.type.equals(Timer.TimerType.NONE)) {
                        continue;
                    } else if (timerList.list.isEmpty()) {
                        continue;
                    }

                    // Very nice
                    if (!statusElement.isTenBool() || !statusElement.isSixtyBool() || !statusElement.isTwentyFourBool()) {
                        if (timerList.type.equals(Timer.TimerType.TEN)) continue;
                        if (timerList.type.equals(Timer.TimerType.SIXTY)) continue;
                        if (timerList.type.equals(Timer.TimerType.TWENTY)) continue;
                    } else if (statusElement.isTenBool()) {
                        if (timerList.type.equals(Timer.TimerType.SIXTY)) continue;
                        if (timerList.type.equals(Timer.TimerType.TWENTY)) continue;
                    } else if (statusElement.isSixtyBool()) {
                        if (timerList.type.equals(Timer.TimerType.TWENTY)) continue;
                    }

                    Integer type = Timer.translate(timerList.type);

                    statement.setString(1, key);
                    if (type == null) statement.setNull(2, 0);
                    else statement.setInt(2, type);
                    statement.setLong(3, timerList.list.get(timerList.list.size() - 1));

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Data calculation
    //------------------------------------------------------------------------------------------------------------

    /**
     * Calculates mean, median and mode price for items in table `league_items_rolling` based on approved entries in
     * `league_entries`
     *
     * @return True on success
     */
    public boolean calculatePrices() {
        String query = "UPDATE league_items_rolling AS i " +
                "JOIN ( " +
                "  SELECT   id_l, id_d, " +
                "           AVG(price)        AS mean, " +
                "           MEDIAN(price)     AS median, " +
                "           stats_mode(price) AS mode, " +
                "           MIN(price)        AS min, " +
                "           MAX(price)        AS max " +
                "  FROM     league_entries " +
                "  WHERE    approved = 1 " +
                "  GROUP BY id_l, id_d " +
                ") AS    e " +
                "  ON    i.id_l = e.id_l " +
                "    AND i.id_d = e.id_d " +
                "SET     i.mean   = TRUNCATE(e.mean,   ?), " +
                "        i.median = TRUNCATE(e.median, ?), " +
                "        i.mode   = TRUNCATE(e.mode,   ?), " +
                "        i.min    = TRUNCATE(e.min,    ?), " +
                "        i.max    = TRUNCATE(e.max,    ?); ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, Config.precision);
                statement.setInt(2, Config.precision);
                statement.setInt(3, Config.precision);
                statement.setInt(4, Config.precision);
                statement.setInt(5, Config.precision);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Calculates median price for volatile items in table `league_items_rolling` based on any entries in
     * `league_entries`
     *
     * @return True on success
     */
    public boolean calculateVolatileMedian() {
        String query = "UPDATE league_items_rolling AS i " +
                "JOIN ( " +
                "  SELECT   id_l, id_d, " +
                "           MEDIAN(price) AS median " +
                "  FROM     league_entries " +
                "  GROUP BY id_l, id_d " +
                ") AS    e " +
                "  ON    i.id_l = e.id_l " +
                "    AND i.id_d = e.id_d " +
                "SET     i.median = TRUNCATE(e.median, ?) " +
                "WHERE   i.volatile = 1;";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, Config.precision);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Calculates exalted price for items in table `league_items_rolling` based on exalted prices in same table
     *
     * @return True on success
     */
    public boolean calculateExalted() {
        String query = "UPDATE    league_items_rolling AS i " +
                "JOIN ( " +
                "  SELECT  i.id_l, i.mean " +
                "  FROM    league_items_rolling AS i " +
                "  JOIN    data_itemData        AS did " +
                "    ON    i.id_d = did.id " +
                "  WHERE   did.frame = 5 " +
                "    AND   did.name = 'Exalted Orb' " +
                ") AS      ex " +
                "  ON      i.id_l = ex.id_l " +
                "SET       i.exalted = TRUNCATE(i.mean / ex.mean, ?) " +
                "WHERE     ex.mean > 0 " +
                "  AND     i.mean  > 0; ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, Config.precision);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Calculates quantity for items in table `league_items_rolling` based on history entries from table `league_history`
     *
     * @return True on success
     */
    public boolean calcQuantity() {
        String query = "UPDATE league_items_rolling AS i  " +
                "LEFT JOIN ( " +
                "    SELECT   id_l, id_d, SUM(inc) AS quantity " +
                "    FROM     league_history_hourly_quantity " +
                "    WHERE    time > ADDDATE(NOW(), INTERVAL -24 HOUR) " +
                "    GROUP BY id_l, id_d " +
                ") AS    h " +
                "  ON    h.id_l = i.id_l " +
                "    AND h.id_d = i.id_d " +
                "SET     i.quantity = IFNULL(h.quantity, 0) ";

        return executeUpdateQueries(query);
    }

    /**
     * Calculates spark data for items in table `league_items_rolling` based on history entries
     *
     * @return True on success
     */
    public boolean calcSpark() {
        String query = "UPDATE league_items_rolling AS i " +
                "JOIN ( " +
                "  SELECT    i.id_l, i.id_d, " +
                "            SUBSTRING_INDEX(GROUP_CONCAT(lhdr.mean ORDER BY lhdr.time DESC SEPARATOR ','), ',', 6) AS history " +
                "  FROM      league_items_rolling          AS i " +
                "  JOIN      data_leagues                  AS l " +
                "    ON      l.id = i.id_l " +
                "  JOIN      league_history_daily_rolling  AS lhdr " +
                "    ON      lhdr.id_d = i.id_d " +
                "      AND   lhdr.id_l = l.id " +
                "  WHERE     l.active = 1 " +
                "    AND     i.count  > 1 " +
                "  GROUP BY  i.id_l, i.id_d " +
                ") AS    tmp " +
                "  ON    i.id_l = tmp.id_l " +
                "    AND i.id_d = tmp.id_d " +
                "SET     i.spark = tmp.history ";

        return executeUpdateQueries(query);
    }

    //------------------------------------------------------------------------------------------------------------
    // Flag updates
    //------------------------------------------------------------------------------------------------------------

    /**
     * Updates volatile state for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateVolatile() {
        String query1 = "UPDATE league_items_rolling " +
                "SET volatile = IF(`dec` > 0 && `dec` >= inc * ?, 1, 0);";

        String query2 = "UPDATE  league_entries       AS e " +
                "JOIN    league_items_rolling AS i " +
                "  ON    e.id_d = i.id_d " +
                "    AND e.id_l = i.id_l " +
                "SET     e.approved = 0 " +
                "WHERE   i.volatile = 1 " +
                "  AND   e.approved = 1;";

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
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Updates approved state for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateApproved() {
        String query1 = "UPDATE  league_entries       AS e " +
                "JOIN    league_items_rolling AS i " +
                "  ON    i.id_l = e.id_l " +
                "    AND i.id_d = e.id_d " +
                "SET     e.approved = 1 " +
                "WHERE   e.approved = 0 " +
                "  AND   i.median   = 0 ";

        String query2 = "UPDATE  league_entries       AS e " +
                "JOIN    league_items_rolling AS i " +
                "  ON    i.id_l = e.id_l " +
                "    AND i.id_d = e.id_d " +
                "SET     e.approved = 1 " +
                "WHERE   e.approved = 0 " +
                "  AND   e.price > i.median / i.multiplier " +
                "  AND   e.price < i.median * i.multiplier ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query1);
                statement.executeUpdate(query2);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Updates multipliers for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateMultipliers() {
        String query = "UPDATE league_items_rolling " +
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
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Updates counters for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean updateCounters() {
        /*
        Possible as a single query, but led to some interesting and non-consistent changes.
        Needs further testing.

            UPDATE  league_items_rolling AS i
            JOIN (
              SELECT   approved, id_l, id_d, COUNT(*) AS count
              FROM     league_entries
              WHERE    time > ADDDATE(NOW(), INTERVAL -60 SECOND)
              GROUP BY approved, id_l, id_d
            ) AS    tmp
              ON    tmp.id_l = i.id_l
                AND tmp.id_d = i.id_d
            SET     i.count = i.count + tmp.count,
                    i.inc   = i.inc   + tmp.count,
                    i.dec   = IF(tmp.approved, i.dec, i.dec + tmp.count)
         */

        String query1 = "UPDATE league_items_rolling AS i " +
                "    JOIN ( " +
                "        SELECT id_l, id_d, COUNT(*) AS count " +
                "        FROM league_entries " +
                "        WHERE approved = 1 " +
                "           AND time > ADDDATE(NOW(), INTERVAL -60 SECOND)" +
                "        GROUP BY id_l, id_d" +
                "    ) AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                "SET " +
                "    i.count = i.count + e.count, " +
                "    i.inc = i.inc + e.count ";

        String query2 = "UPDATE league_items_rolling AS i " +
                "    JOIN ( " +
                "        SELECT id_l, id_d, COUNT(*) AS count " +
                "        FROM league_entries " +
                "        WHERE approved = 0 " +
                "           AND time > ADDDATE(NOW(), INTERVAL -60 SECOND) " +
                "        GROUP BY id_l, id_d" +
                "    ) AS e ON e.id_l = i.id_l AND e.id_d = i.id_d " +
                "SET " +
                "    i.count = i.count + e.count, " +
                "    i.inc = i.inc + e.count, " +
                "    i.dec = i.dec + e.count ";


        return executeUpdateQueries(query1,query2);
    }

    /**
     * Resets counters for entries in table `league_items_rolling`
     *
     * @return True on success
     */
    public boolean resetCounters() {
        String query = "UPDATE league_items_rolling SET inc = 0, `dec` = 0 ";

        return executeUpdateQueries(query);
    }

    //------------------------------------------------------------------------------------------------------------
    // History entry management
    //------------------------------------------------------------------------------------------------------------

    /**
     * Copies data from table `league_items_rolling` to table `league_history_hourly_quantity` every hour
     * on a rolling basis with a history of 24 hours
     *
     * @return True on success
     */
    public boolean addHourly() {
        String query = "INSERT " +
                "INTO   league_history_hourly_quantity (" +
                "       id_l, id_d, inc) " +
                "SELECT id_l, id_d, inc " +
                "FROM   league_items_rolling AS i " +
                "JOIN   data_leagues         AS l " +
                "  ON   i.id_l = l.id " +
                "WHERE  l.active = 1 " +
                "  AND  i.inc > 0 ";

        return executeUpdateQueries(query);
    }

    /**
     * Copies data from table `league_items_rolling` to table `league_history_daily_rolling` every 24h
     * on a rolling basis with a history of 120 days for standard and hardcore
     *
     * @return True on success
     */
    public boolean addDaily() {
        String query = "INSERT INTO league_history_daily_rolling ( " +
                "  id_l, id_d, volatile, mean, median, mode, " +
                "  min, max, exalted, count, quantity, inc, `dec`) " +
                "SELECT " +
                "  id_l, id_d, volatile, mean, median, mode, " +
                "  min, max, exalted, count, quantity, inc, `dec` " +
                "FROM   league_items_rolling AS i " +
                "JOIN   data_leagues AS l " +
                "  ON   i.id_l = l.id " +
                "WHERE  l.active = 1 ";

        return executeUpdateQueries(query);

    }

    /**
     * Removes entries from table `league_entries` that exceed the specified capacity
     *
     * @return True on success
     */
    public boolean removeOldItemEntries() {
        String query = "DELETE    del " +
                "FROM      league_entries AS del " +
                "JOIN ( " +
                "  SELECT   id_l, id_d, ( " +
                "    SELECT   e.time " +
                "    FROM     league_entries AS e " +
                "    WHERE    e.id_l = d.id_l " +
                "      AND    e.id_d = d.id_d " +
                "    ORDER BY e.time DESC " +
                "    LIMIT    ?, 1 " +
                "  ) AS     time " +
                "  FROM     league_entries AS d " +
                "  GROUP BY id_l, id_d " +
                "  HAVING time IS NOT NULL " +
                ") AS      tmp " +
                "  ON      del.id_l = tmp.id_l " +
                "    AND   del.id_d = tmp.id_d " +
                "      AND del.time <= tmp.time; ";

        try {
            if (connection.isClosed()) return false;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, Config.entry_maxCount);
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }

    /**
     * Moves rows from table `league_items_rolling` to table `league_items_inactive` based on league active status
     *
     * @return True on success
     */
    public boolean moveInactiveItemEntries() {
        String query1 = "INSERT INTO league_items_inactive ( " +
                "              id_l, id_d, time, " +
                "              mean, median, mode, " +
                "              min, max, exalted, count)" +
                "SELECT      id_l, id_d, time, " +
                "            mean, median, mode, " +
                "            min, max, exalted, count " +
                "FROM        league_items_rolling AS i " +
                "JOIN        data_leagues         AS l " +
                "  ON        l.id = i.id_l " +
                "WHERE       l.active = 0 ";

        String query2 = "DELETE i " +
                "FROM   league_items_rolling AS i " +
                "JOIN   data_leagues         AS l " +
                "  ON   l.id = i.id_l " +
                "WHERE  l.active = 0 ";

        String query3 = "UPDATE league_items_inactive AS i " +
                "JOIN ( " +
                "  SELECT  id, TIMESTAMPDIFF( " +
                "                DAY, " +
                "                STR_TO_DATE(start, '%Y-%m-%dT%H:%i:%sZ'), " +
                "                STR_TO_DATE(end,   '%Y-%m-%dT%H:%i:%sZ') " +
                "              ) AS diff " +
                "  FROM    data_leagues " +
                "  WHERE   active = 0 " +
                "  HAVING  diff IS NOT NULL " +
                ") AS l ON i.id_l = l.id " +
                "SET i.quantity = FLOOR(i.count / l.diff) ";

        try {
            if (connection.isClosed()) return false;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query1);
                statement.executeUpdate(query2);
                statement.executeUpdate(query3);
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);

            return false;
        }
    }
}
