package com.poestats.database;

import com.poestats.Config;
import com.poestats.Item;
import com.poestats.Main;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.ParcelEntry;
import com.poestats.pricer.StatusElement;
import com.poestats.pricer.entries.RawEntry;
import com.poestats.pricer.maps.CurrencyMap;
import com.poestats.pricer.maps.IndexMap;
import com.poestats.pricer.maps.RawList;
import com.poestats.relations.IndexRelations;

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

    public boolean getLeagues(List<LeagueEntry> leagueEntries) {
        String query = "SELECT `id`, `start`, `end` FROM `leagues`";

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
     */
    public boolean updateLeagues(List<LeagueEntry> leagueEntries) {
        String query =  "INSERT INTO `leagues` (`id`, `start`, `end`) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `start`=`start`, `end`=`end`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (LeagueEntry leagueEntry : leagueEntries) {
                    statement.setString(1, leagueEntry.getId());
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
     * Removes any previous and updates the changeID record in table `changeid`
     *
     * @param id New changeID string to store
     */
    public boolean updateChangeID(String id) {
        String query1 = "INSERT INTO `changeid` (`changeid`) VALUES (?)";

        String query2 = "DELETE FROM `changeid` " +
                        "WHERE `time` NOT IN ( " +
                        "    SELECT `time` FROM ( " +
                        "        SELECT `time` " +
                        "        FROM `changeid` " +
                        "        ORDER BY `time` DESC " +
                        "        LIMIT 1 " +
                        "    ) foo ); ";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                statement.setString(1, id);
                statement.execute();
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute(query2);
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
     */
    public boolean getCategories(Map<String, List<String>> tmpCategories) {
        String query =  "SELECT " +
                        "    `category_parent`.`parent`, " +
                        "    `category_parent`.`display`, " +
                        "    `category_child`.`child`, " +
                        "    `category_child`.`display` " +
                        "FROM `category_child`" +
                        "    JOIN `category_parent`" +
                        "        ON `category_child`.`parent` = `category_parent`.`parent`";

        try {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String parent = resultSet.getString(1);
                    String parentDisplay = resultSet.getString(2);

                    String child = resultSet.getString(3);
                    String childDisplay = resultSet.getString(4);

                    List<String> childCategories = tmpCategories.getOrDefault(parent, new ArrayList<>());
                    childCategories.add(child);
                    tmpCategories.putIfAbsent(parent, childCategories);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query categories", 3);
            return false;
        }
    }

    //--------------------
    // Item data
    //--------------------

    /**
     * Get item data relations from database
     * TODO: simplify resultSet operation
     */
    public boolean getItemData(IndexRelations indexRelations) {
        String query =  "SELECT " +
                        "    `item_data_sup`.`sup`, " +
                        "    `item_data_sub`.`sub`, " +
                        "    `item_data_sup`.`key` AS 'genericKey', " +
                        "    `item_data_sub`.`key` AS 'uniqueKey' " +
                        "FROM `item_data_sub`" +
                        "    JOIN `item_data_sup`" +
                        "        ON `item_data_sub`.`sup` = `item_data_sup`.`sup`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                List<String> completeIndexList = indexRelations.getCompleteIndexList();
                Map<String, List<String>> supIndexToSubs = indexRelations.getSupIndexToSubs();
                Map<String, String> uniqueKeyToFullIndex = indexRelations.getUniqueKeyToFullIndex();
                Map<String, String> genericKeyToSupIndex = indexRelations.getGenericKeyToSupIndex();

                while (resultSet.next()) {
                    String sup = resultSet.getString("sup");
                    String sub = resultSet.getString("sub");
                    String uniqueKey = resultSet.getString("uniqueKey");
                    String genericKey = resultSet.getString("genericKey");
                    String index = sup + sub;

                    completeIndexList.add(index);
                    uniqueKeyToFullIndex.put(uniqueKey, index);
                    genericKeyToSupIndex.putIfAbsent(genericKey, sup);

                    List<String> subIndexes = supIndexToSubs.getOrDefault(sup, new ArrayList<>());
                    subIndexes.add(sub);
                    supIndexToSubs.putIfAbsent(sup, subIndexes);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query item indexes", 3);
            return false;
        }
    }

    public boolean addSupItemData(String sup, Item item) {
        String query =  "INSERT INTO `item_data_sup` (`sup`,`parent`,`child`,`name`,`type`,`frame`,`key`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, item.getParentCategory());
                statement.setString(3, item.getChildCategory());
                statement.setString(4, item.getName());
                statement.setString(5, item.getType());
                statement.setInt(6, item.getFrame());
                statement.setString(7, item.getGenericKey());

                statement.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update full item data in database", 3);
            return false;
        }
    }

    public boolean addSubItemData(String sup, String sub, Item item) {
        String query =  "INSERT INTO `item_data_sub`" +
                        "    (`sup`,`sub`,`tier`,`lvl`,`quality`,`corrupted`,`links`,`var`,`key`,`icon`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, sub);
                statement.setString(3, item.getTier());
                statement.setString(4, item.getLevel());
                statement.setString(5, item.getQuality());
                statement.setString(6, item.isCorrupted());
                statement.setString(7, item.getLinks());
                statement.setString(8, item.getVariation());
                statement.setString(9, item.getUniqueKey());
                statement.setString(10, item.getIcon());

                statement.execute();
            }

            // Commit changes
            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update sub item data in database", 3);
            return false;
        }
    }


    //--------------------
    // Status
    //--------------------

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

                while (resultSet.next()) {
                    switch (resultSet.getString("name")) {
                        case "twentyFourCounter":
                            statusElement.twentyFourCounter = resultSet.getLong("val");
                            break;
                        case "sixtyCounter":
                            statusElement.sixtyCounter = resultSet.getLong("val");
                            break;
                        case "tenCounter":
                            statusElement.tenCounter = resultSet.getLong("val");
                            break;
                    }
                }
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
        String query =  "INSERT INTO `status` (`val`, `name`)" +
                        "    VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE" +
                        "    `val`= VALUES(`val`)";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setLong(1, statusElement.twentyFourCounter);
                statement.setString(2, "twentyFourCounter");
                statement.addBatch();

                statement.setLong(1, statusElement.sixtyCounter);
                statement.setString(2, "sixtyCounter");
                statement.addBatch();

                statement.setLong(1, statusElement.tenCounter);
                statement.setString(2, "tenCounter");
                statement.addBatch();

                statement.setLong(1, statusElement.lastRunTime);
                statement.setString(2, "lastRunTime");
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

    //--------------------
    // Entry management
    //--------------------

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
                        "    i.`sup`, i.`sub`, d.`name`," +
                        "    i.`mean`, i.`median`, i.`mode`, i.`exalted`," +
                        "    i.`count`, i.`quantity`, i.`inc`, i.`dec` " +
                        "FROM `#_item_"+ league +"` AS i" +
                        "    INNER JOIN `item_data_sup` AS d" +
                        "        ON i.`sup` = d.`sup` " +
                        "WHERE EXISTS (" +
                        "    SELECT * FROM `item_data_sup` AS a " +
                        "    WHERE a.`sup` = i.`sup` " +
                        "    AND a.`parent` = 'currency'" +
                        "    AND a.`frame` = 5)";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

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

    public boolean uploadRaw(String league, IndexMap indexMap) {
        league = formatLeague(league);

        String query =  "INSERT INTO `#_entry_"+ league +"` (`sup`, `sub`, `price`, `account`, `id`) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `price`=`price`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String index : indexMap.keySet()) {
                    RawList rawList = indexMap.get(index);

                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    for (RawEntry rawEntry : rawList) {
                        statement.setString(1, sup);
                        statement.setString(2, sub);
                        statement.setString(3, rawEntry.getPriceAsRoundedString());
                        statement.setString(4, rawEntry.getAccount());
                        statement.setString(5, rawEntry.getId());
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

    /**
     * Creates a new item entry in the league-specific item table if the item has not previously been indexed
     *
     * @param league League table to pick
     * @param indexMap IndexMap of indexes to check
     * @return True on success
     */
    public boolean createNewItems(String league, IndexMap indexMap) {
        league = formatLeague(league);

        String query =  "INSERT INTO `#_item_"+ league +"` (`sup`, `sub`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `sup`=`sup`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String index : indexMap.keySet()) {
                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not create item in database", 3);
            return false;
        }
    }

    public boolean updateCounters(String league, IndexMap indexMap) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `count`=`count` + ?, `inc`=`inc` + ? " +
                        "WHERE `sup`= ? AND `sub`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String index : indexMap.keySet()) {
                    RawList rawList = indexMap.get(index);

                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setInt(1, rawList.size());
                    statement.setInt(2, rawList.size());
                    statement.setString(3, sup);
                    statement.setString(4, sub);
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

    public boolean calculateMean(String league, IndexMap indexMap) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `mean` = (" +
                        "    SELECT IFNULL(AVG(`price`), 0.0) " +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `sup`= ? AND `sub`= ?" +
                        ") WHERE `sup`= ? AND `sub`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String index : indexMap.keySet()) {
                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setString(3, sup);
                    statement.setString(4, sub);
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

    public boolean calculateMedian(String league, IndexMap indexMap) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `median` = IFNULL((" +
                        "    SELECT AVG(t1.`price`) as median_val FROM (" +
                        "        SELECT @rownum:=@rownum+1 as `row_number`, d.`price`" +
                        "        FROM `#_entry_"+ league +"` d,  (SELECT @rownum:=0) r" +
                        "        WHERE `sup`= ? AND `sub`= ?" +
                        "        ORDER BY d.`price`" +
                        "    ) as t1, (" +
                        "        SELECT count(*) as total_rows" +
                        "        FROM `#_entry_"+ league +"` d" +
                        "        WHERE `sup`= ? AND `sub`= ?" +
                        "    ) as t2 WHERE 1" +
                        "    AND t1.row_number in ( floor((total_rows+1)/2), floor((total_rows+2)/2) )" +
                        "), 0.0) WHERE `sup`= ? AND `sub`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String index : indexMap.keySet()) {
                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setString(3, sup);
                    statement.setString(4, sub);
                    statement.setString(5, sup);
                    statement.setString(6, sub);
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

    public boolean calculateMode(String league, IndexMap indexMap) {
        league = formatLeague(league);

        String query =  "UPDATE `#_item_"+ league +"` " +
                        "SET `mode` = IFNULL(( " +
                        "    SELECT `price` FROM `#_entry_"+ league +"`" +
                        "    WHERE `sup`= ? AND `sub`= ?" +
                        "    GROUP BY `price` " +
                        "    ORDER BY COUNT(*) DESC " +
                        "    LIMIT 1" +
                        "), 0.0) " +
                        "WHERE `sup`= ? AND `sub`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String index : indexMap.keySet()) {
                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setString(3, sup);
                    statement.setString(4, sub);
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
                        "    SELECT  `mean`" +
                        "    FROM (SELECT `sup`, `mean` FROM `#_item_"+ league +"`) AS i" +
                        "        INNER JOIN `item_data_sup` AS d" +
                        "            ON i.`sup` = d.`sup`" +
                        "    WHERE EXISTS (" +
                        "        SELECT * FROM `item_data_sup` AS a " +
                        "        WHERE a.`sup` = i.`sup` " +
                        "        AND a.`parent` = 'currency'" +
                        "        AND a.`name` = 'Exalted Orb'))";

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

        String query  = "SELECT * FROM `#_history_"+ league +"` " +
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
                        "   (`sup`,`sub`,`type`,`mean`,`median`,`mode`,`exalted`,`inc`,`dec`,`count`,`quantity`) " +
                        "SELECT `sup`,`sub`,'minutely',`mean`,`median`,`mode`,`exalted`,`inc`,`dec`,`count`,`quantity` " +
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

        String query =  "INSERT INTO `#_history_"+ league +"` (`sup`, `sub`, `type`, `mean`, `median`, `mode`, " +
                        "                                        `exalted`, `count`, `quantity`, `inc`, `dec`)" +
                        "    SELECT `sup`, `sub`, 'hourly', AVG(`mean`), AVG(`median`), AVG(`mode`), " +
                        "           AVG(`exalted`), MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                        "    FROM `#_history_"+ league +"`" +
                        "    WHERE `type`='minutely'" +
                        "    GROUP BY `sup`, `sub`";

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

        String query =  "INSERT INTO `#_history_"+ league +"` (`sup`, `sub`, `type`, `mean`, `median`, `mode`, " +
                        "                                        `exalted`, `count`, `quantity`, `inc`, `dec`)" +
                        "    SELECT `sup`, `sub`, 'daily', AVG(`mean`), AVG(`median`), AVG(`mode`), " +
                        "           AVG(`exalted`), MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                        "    FROM `#_history_"+ league +"`" +
                        "    WHERE `type`='hourly'" +
                        "    GROUP BY `sup`, `sub`";

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

        String query =  "UPDATE `#_item_"+ league +"` as i " +
                        "SET `quantity` = (" +
                        "    SELECT SUM(`inc`) / COUNT(`inc`) FROM `#_history_"+ league +"` " +
                        "    WHERE `sup`=i.`sup` AND `sub`=i.`sub` AND `type`='hourly'" +
                        "), `inc`=0, `dec`=0";

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

    public boolean removeOldHistoryEntries(String league, String type, String interval) {
        league = formatLeague(league);

        String query =  "DELETE FROM `#_history_"+ league +"` " +
                        "WHERE `type` = '"+ type+ "' " +
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

    public boolean removeOldItemEntries(String league, IndexMap indexMap) {
        league = formatLeague(league);

        String query =  "DELETE FROM `#_entry_"+ league +"`" +
                        "WHERE `sup`= ? AND `sub`= ? AND `id` NOT IN (" +
                        "    SELECT `id`" +
                        "    FROM (" +
                        "        SELECT `id`, `time`" +
                        "        FROM `#_entry_"+ league +"`" +
                        "        WHERE `sup`= ? AND `sub`= ?" +
                        "        ORDER BY `time` DESC" +
                        "        LIMIT ?" +
                        "    ) foo )";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (String index : indexMap.keySet()) {
                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setString(3, sup);
                    statement.setString(4, sub);
                    statement.setInt(5, Config.entry_maxCount);
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

    public boolean removeItemOutliers(String league, IndexMap indexMap){
        league = formatLeague(league);

        String query0 = "SET @entryCount = (" +
                        "    SELECT COUNT(`id`)" +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `sup`=? AND `sub`=?);";

        String query1 = "SET @stddevPrice = (" +
                        "    SELECT STDDEV(`price`)" +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `sup`=? AND `sub`=?" +
                        ") * ?;";

        String query2 = "SET @medianPrice = (" +
                        "    SELECT `median`" +
                        "    FROM `#_item_"+ league +"`" +
                        "    WHERE `sup`=? AND `sub`=?);";

        String query3 = "SET @numberOfElementsToRemove = (" +
                        "    SELECT COUNT(`price`)" +
                        "    FROM `#_entry_"+ league +"`" +
                        "    WHERE `sup`=? AND `sub`=?" +
                        "    AND (`price` > @medianPrice + @stddevPrice && `price` > @medianPrice * ? || " +
                        "        `price` < @medianPrice - @stddevPrice && `price` < @medianPrice / ?) &&" +
                        "        @entryCount > ? && @medianPrice > ?);";

        String query4 = "UPDATE `#_item_"+ league +"` " +
                        "SET `dec` = `dec` + @numberOfElementsToRemove " +
                        "WHERE `sup`=? AND `sub`=?;";

        String query5 = "DELETE FROM `#_entry_"+ league +"` " +
                        "WHERE `sup`=? AND `sub`=? " +
                        "AND (`price` > @medianPrice + @stddevPrice && `price` > @medianPrice * ? || " +
                        "    `price` < @medianPrice - @stddevPrice && `price` < @medianPrice / ?) && " +
                        "    @entryCount > ? && @medianPrice > ?;";

        try {
            for (String index : indexMap.keySet()) {
                String sup = index.substring(0, Config.index_superSize);
                String sub = index.substring(Config.index_superSize);

                try (PreparedStatement statement = connection.prepareStatement(query0)) {
                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query1)) {
                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setDouble(3, Config.outlier_devMulti);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query2)) {
                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query3)) {
                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setDouble(3, Config.outlier_priceMulti);
                    statement.setDouble(4, Config.outlier_priceMulti);
                    statement.setInt(5, Config.outlier_minCount);
                    statement.setDouble(6, Config.outlier_minPrice);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query4)) {
                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.execute();
                }

                try (PreparedStatement statement = connection.prepareStatement(query5)) {
                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setDouble(3, Config.outlier_priceMulti);
                    statement.setDouble(4, Config.outlier_priceMulti);
                    statement.setInt(5, Config.outlier_minCount);
                    statement.setDouble(6, Config.outlier_minPrice);
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
                        "    CONSTRAINT `sup-sub_"+ league +"`" +
                        "        PRIMARY KEY (`sup`,`sub`), " +
                        "        FOREIGN KEY (`sup`,`sub`) " +
                        "        REFERENCES `item_data_sub` (`sup`,`sub`)" +
                        "        ON DELETE CASCADE," +

                        "    `sup`       varchar(5)      NOT NULL," +
                        "    `sub`       varchar(2)      NOT NULL," +

                        "    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `mean`      decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `median`    decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `mode`      decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `exalted`   decimal(10,4)   unsigned NOT NULL DEFAULT 0.0," +
                        "    `inc`       int(8)          unsigned NOT NULL DEFAULT 0," +
                        "    `dec`       int(8)          unsigned NOT NULL DEFAULT 0," +
                        "    `count`     int(16)         unsigned NOT NULL DEFAULT 0," +
                        "    `quantity`  int(8)          unsigned NOT NULL DEFAULT 0" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String query3 = "CREATE TABLE `#_entry_"+ league +"` (" +
                        "    CONSTRAINT `entry_"+ league +"`" +
                        "        PRIMARY KEY (`sup`,`sub`,`account`)," +
                        "        FOREIGN KEY (`sup`,`sub`)" +
                        "        REFERENCES `#_item_"+ league +"` (`sup`,`sub`)" +
                        "        ON DELETE CASCADE," +

                        "    `sup`       varchar(5)      NOT NULL," +
                        "    `sub`       varchar(2)      NOT NULL," +

                        "    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `price`     decimal(10,4)   NOT NULL," +
                        "    `account`   varchar(32)     NOT NULL," +
                        "    `id`        varchar(32)     NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String query4 = "CREATE TABLE `#_history_"+ league +"` (" +
                        "    CONSTRAINT `history_"+ league +"`" +
                        "        FOREIGN KEY (`sup`,`sub`)" +
                        "        REFERENCES `#_item_"+ league +"` (`sup`,`sub`)" +
                        "        ON DELETE CASCADE," +
                        "    FOREIGN KEY (`type`)" +
                        "        REFERENCES `history_category` (`type`)" +
                        "        ON DELETE CASCADE," +

                        "    `sup`       varchar(5)      NOT NULL," +
                        "    `sub`       varchar(2)      NOT NULL," +
                        "    `type`      varchar(32)     NOT NULL," +

                        "    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `mean`      decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `median`    decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `mode`      decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `exalted`   decimal(10,4)   unsigned DEFAULT NULL," +
                        "    `inc`       int(8)          unsigned DEFAULT NULL," +
                        "    `dec`       int(8)          unsigned DEFAULT NULL," +
                        "    `count`     int(16)         unsigned DEFAULT NULL," +
                        "    `quantity`  int(8)          unsigned DEFAULT NULL" +
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
