package com.poestats.database;

import com.poestats.Config;
import com.poestats.Main;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.StatusElement;
import com.poestats.pricer.entries.RawEntry;
import com.poestats.pricer.maps.CurrencyMap;
import com.poestats.pricer.maps.IndexMap;
import com.poestats.pricer.maps.RawList;
import com.poestats.relations.entries.SupIndexedItem;
import com.poestats.relations.entries.SubIndexedItem;

import java.sql.*;
import java.util.ArrayList;
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
        String query = "SELECT * FROM `leagues`";

        try {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(query);

                leagueEntries.clear();

                while (resultSet.next()) {
                    LeagueEntry leagueEntry = new LeagueEntry();

                    // TODO: SQL database has additional field display
                    leagueEntry.setId(resultSet.getString("id"));
                    leagueEntry.setEndAt(resultSet.getString("start"));
                    leagueEntry.setStartAt(resultSet.getString("end"));

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
        String query1 = "DELETE FROM `leagues`";
        String query2 = "INSERT INTO `leagues` (`id`, `start`, `end`) VALUES (?, ?, ?)";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query1);
            }

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
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
     * @param changeID New changeID string to store
     */
    public boolean updateChangeID(String changeID) {
        String query1 = "DELETE FROM `changeid`";
        String query2 = "INSERT INTO `changeid` (`changeid`) VALUES (?)";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query1);
            }

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                statement.setString(1, changeID);
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
    public boolean getItemData(Map<String, SupIndexedItem> relations) {
        String query =  "SELECT " +
                        "    `item_data_sup`.`sup`, " +
                        "    `item_data_sup`.`parent`, " +
                        "    `item_data_sup`.`child`, " +
                        "    `item_data_sup`.`name`, " +
                        "    `item_data_sup`.`type`, " +
                        "    `item_data_sup`.`frame`, " +
                        "    `item_data_sup`.`key`, " +

                        "    `item_data_sub`.`sub`, " +
                        "    `item_data_sub`.`tier`, " +
                        "    `item_data_sub`.`lvl`, " +
                        "    `item_data_sub`.`quality`, " +
                        "    `item_data_sub`.`corrupted`, " +
                        "    `item_data_sub`.`links`, " +
                        "    `item_data_sub`.`var`, " +
                        "    `item_data_sub`.`key`, " +
                        "    `item_data_sub`.`icon` " +
                        "FROM `item_data_sub`" +
                        "    JOIN `item_data_sup`" +
                        "        ON `item_data_sub`.`sup` = `item_data_sup`.`sup`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String sup = resultSet.getString(1);
                    String sub = resultSet.getString(8);

                    String supKey = resultSet.getString(7);
                    String subKey = resultSet.getString(15);

                    String parent = resultSet.getString(2);
                    String child = resultSet.getString(3);

                    String name = resultSet.getString(4);
                    String type = resultSet.getString(5);
                    int frame = resultSet.getInt(6);

                    String tier = resultSet.getString(9);
                    String lvl = resultSet.getString(10);
                    String quality = resultSet.getString(11);
                    String corrupted = resultSet.getString(12);
                    String links = resultSet.getString(13);
                    String var = resultSet.getString(14);
                    String icon = resultSet.getString(16);

                    SupIndexedItem supIndexedItem = relations.getOrDefault(sup, new SupIndexedItem());

                    if (!relations.containsKey(sup)) {
                        if (child != null)  supIndexedItem.setChild(child);
                        if (type != null)   supIndexedItem.setType(type);

                        supIndexedItem.setParent(parent);
                        supIndexedItem.setName(name);
                        supIndexedItem.setFrame(frame);
                        supIndexedItem.setKey(supKey);
                    }

                    SubIndexedItem subIndexedItem = new SubIndexedItem();
                    if (tier != null)       subIndexedItem.setTier(tier);
                    if (lvl != null)        subIndexedItem.setLvl(lvl);
                    if (quality != null)    subIndexedItem.setQuality(quality);
                    if (corrupted != null)  subIndexedItem.setCorrupted(corrupted);
                    if (links != null)      subIndexedItem.setLinks(links);
                    if (var != null)        subIndexedItem.setVar(var);
                    subIndexedItem.setKey(subKey);
                    subIndexedItem.setIcon(icon);
                    subIndexedItem.setSupIndexedItem(supIndexedItem);

                    supIndexedItem.getSubIndexes().put(sub, subIndexedItem);
                    relations.putIfAbsent(sup, supIndexedItem);
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query item data", 3);
            return false;
        }
    }

    public boolean addFullItemData(SupIndexedItem supIndexedItem, String sup, String sub) {
        String query =  "INSERT INTO `item_data_sup` (`sup`,`parent`,`child`,`name`,`type`,`frame`,`key`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `sup`=`sup`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, supIndexedItem.getParent());
                statement.setString(3, supIndexedItem.getChild());
                statement.setString(4, supIndexedItem.getName());
                statement.setString(5, supIndexedItem.getType());
                statement.setInt(6, supIndexedItem.getFrame());
                statement.setString(7, supIndexedItem.getKey());

                statement.execute();
            }

            addSubItemData(supIndexedItem, sup, sub);

            // Commit changes
            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update full item data in database", 3);
            return false;
        }
    }

    public boolean addSubItemData(SupIndexedItem supIndexedItem, String sup, String sub) {
        SubIndexedItem subIndexedItem = supIndexedItem.getSubIndexes().get(sup + sub);

        String query =  "INSERT INTO `item_data_sub`" +
                        "   (`sup`,`sub`,`tier`,`lvl`,`quality`,`corrupted`,`links`,`var`,`key`,`icon`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `sup`=`sup`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, sub);
                statement.setString(3, subIndexedItem.getTier());
                statement.setString(4, subIndexedItem.getLvl());
                statement.setString(5, subIndexedItem.getQuality());
                statement.setString(6, subIndexedItem.getCorrupted());
                statement.setString(7, subIndexedItem.getLinks());
                statement.setString(8, subIndexedItem.getVar());
                statement.setString(9, subIndexedItem.getKey());
                statement.setString(10, subIndexedItem.getIcon());

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
     * @return True if successful
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
     * Removes any previous and updates the status records in table `status`
     *
     * @param statusElement StatusElement to copy
     * @return True on success
     */
    public boolean updateStatus(StatusElement statusElement) {
        String query1 = "DELETE FROM `status`";
        String query2 = "INSERT INTO `status` (`val`, `name`) VALUES (?, ?)";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query1);
            }

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
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
            Main.ADMIN.log_("Could not update status data", 3);
            return false;
        }
    }

    //--------------------
    // Entry management
    //--------------------

    public boolean getCurrency(String league, CurrencyMap currencyMap) {
        league = formatLeague(league);

        String query =  "SELECT " +
                        "    i.`sup`," +
                        "    i.`sub`," +
                        "    d.`name`," +
                        "    i.`time`," +
                        "    i.`mean`," +
                        "    i.`median`," +
                        "    i.`mode`," +
                        "    i.`exalted`," +
                        "    i.`count`," +
                        "    i.`quantity`," +
                        "    i.`inc`," +
                        "    i.`dec` " +
                        "FROM `$_"+ league +"_item` AS i" +
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

        String query =  "INSERT INTO `$_"+ league +"_entry` (`sup`, `sub`, `price`, `account`, `id`) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `sup`=`sup`";

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

    public boolean createItem(String league, String index) {
        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);
        league = formatLeague(league);

        String query =  "INSERT INTO `$_"+ league +"_item` (`sup`, `sub`) " +
                        "VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE `sup`=`sup`";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, sub);
                statement.execute();
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

        String query =  "UPDATE `$_"+ league +"_item` " +
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

    public boolean calculateMean(String league, String index) {
        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);
        league = formatLeague(league);

        String query =  "UPDATE `$_"+ league +"_item` " +
                        "SET `mean` = (" +
                        "    SELECT AVG(`price`) FROM `$_"+ league +"_entry`" +
                        "    WHERE `sup`= ? AND `sub`= ?" +
                        ") WHERE `sup`= ? AND `sub`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, sub);
                statement.setString(3, sup);
                statement.setString(4, sub);
                statement.execute();
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate mean", 3);
            return false;
        }
    }

    public boolean calculateMedian(String league, String index) {
        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);
        league = formatLeague(league);

        String query =  "UPDATE `$_"+ league +"_item` " +
                        "SET `median` = (" +
                        "    SELECT AVG(t1.`price`) as median_val FROM (" +
                        "        SELECT @rownum:=@rownum+1 as `row_number`, d.`price`" +
                        "        FROM `$_"+ league +"_entry` d,  (SELECT @rownum:=0) r" +
                        "        WHERE `sup`= ? AND `sub`= ?" +
                        "        ORDER BY d.`price`" +
                        "    ) as t1, (" +
                        "        SELECT count(*) as total_rows" +
                        "        FROM `$_"+ league +"_entry` d" +
                        "        WHERE `sup`= ? AND `sub`= ?" +
                        "    ) as t2 WHERE 1" +
                        "    AND t1.row_number in ( floor((total_rows+1)/2), floor((total_rows+2)/2) )" +
                        ") WHERE `sup`= ? AND `sub`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, sub);
                statement.setString(3, sup);
                statement.setString(4, sub);
                statement.setString(5, sup);
                statement.setString(6, sub);
                statement.execute();
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate median", 3);
            return false;
        }
    }

    public boolean calculateMode(String league, String index) {
        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);
        league = formatLeague(league);

        String query =  "UPDATE `$_"+ league +"_item`  " +
                        "SET `mode` = ( " +
                        "    SELECT `price` FROM `$_"+ league +"_entry` " +
                        "    WHERE `sup`= ? AND `sub`= ?" +
                        "    GROUP BY `price` " +
                        "    ORDER BY COUNT(*) DESC " +
                        "    LIMIT 1" +
                        ") WHERE `sup`= ? AND `sub`= ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, sub);
                statement.setString(3, sup);
                statement.setString(4, sub);
                statement.execute();
            }

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
                        "    FROM (SELECT `sup`, `mean` FROM `$_"+ league +"_item`) AS i" +
                        "        INNER JOIN `item_data_sup` AS d" +
                        "            ON i.`sup` = d.`sup`" +
                        "    WHERE EXISTS (" +
                        "        SELECT * FROM `item_data_sup` AS a " +
                        "        WHERE a.`sup` = i.`sup` " +
                        "        AND a.`parent` = 'currency'" +
                        "        AND a.`name` = 'Exalted Orb'))";

        String query2 = "UPDATE `$_"+ league +"_item` " +
                        "SET `exalted` = IFNULL(`mean` / @exVal, 0.0)";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query1);
                statement.execute(query2);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate exalted", 3);
            return false;
        }
    }

    //--------------------------
    // History entry management
    //--------------------------

    public boolean addMinutely(String league) {
        league = formatLeague(league);

        String query =  "INSERT INTO `$_"+ league +"_history` (`sup`,`sub`,`type`,`mean`,`median`,`mode`," +
                        "                                            `exalted`,`inc`,`dec`,`count`,`quantity`)" +
                        "   SELECT `sup`,`sub`,'minutely',`mean`,`median`,`mode`," +
                        "           `exalted`,`inc`,`dec`,`count`,`quantity`" +
                        "   FROM `$_"+ league +"_item`";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add minutely", 3);
            return false;
        }
    }

    public boolean addHourly(String league) {
        league = formatLeague(league);

        String query =  "INSERT INTO `$_"+ league +"_history` (`sup`, `sub`, `type`, `mean`, `median`, `mode`, " +
                        "                                        `exalted`, `count`, `quantity`, `inc`, `dec`)" +
                        "    SELECT `sup`, `sub`, 'hourly', AVG(`mean`), AVG(`median`), AVG(`mode`), " +
                        "           AVG(`exalted`), MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                        "    FROM `$_"+ league +"_history`" +
                        "    WHERE `type`='minutely'" +
                        "    GROUP BY `sup`, `sub`";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add hourly", 3);
            return false;
        }
    }

    public boolean addDaily(String league) {
        league = formatLeague(league);

        String query =  "INSERT INTO `$_"+ league +"_history` (`sup`, `sub`, `type`, `mean`, `median`, `mode`, " +
                        "                                        `exalted`, `count`, `quantity`, `inc`, `dec`)" +
                        "    SELECT `sup`, `sub`, 'daily', AVG(`mean`), AVG(`median`), AVG(`mode`), " +
                        "           AVG(`exalted`), MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                        "    FROM `$_"+ league +"_history`" +
                        "    WHERE `type`='hourly'" +
                        "    GROUP BY `sup`, `sub`";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not add daily", 3);
            return false;
        }
    }

    public boolean calcQuantity(String league) {
        league = formatLeague(league);

        String query =  "UPDATE `$_"+ league +"_item` as i " +
                        "SET `quantity` = (" +
                        "    SELECT SUM(`inc`) / COUNT(`inc`) FROM `$_"+ league +"_history` " +
                        "    WHERE `sup`=i.`sup` AND `sub`=i.`sub` AND `type`='hourly'" +
                        "), `inc`=0, `dec`=0";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not calculate quantity", 3);
            return false;
        }
    }

    public boolean removeOldHistoryEntries(String league, String type, String interval) {
        league = formatLeague(league);

        String query =  "DELETE FROM `$_"+ league +"_history` " +
                        "WHERE `type` = '"+ type+ "' " +
                        "AND `time` < ADDDATE(NOW(), INTERVAL -"+ interval +")";

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(query);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not remove old history entries", 3);
            return false;
        }
    }

    public boolean removeOldItemEntries(String league, String index) {
        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);
        league = formatLeague(league);

        String query =  "DELETE FROM `$_"+ league +"_entry`" +
                        "WHERE `sup`= ? AND `sub`= ? AND `id` NOT IN (" +
                        "    SELECT `id`" +
                        "    FROM (" +
                        "        SELECT `id`, `time`" +
                        "        FROM `$_"+ league +"_entry`" +
                        "        WHERE `sup`= ? AND `sub`= ?" +
                        "        ORDER BY `time` DESC" +
                        "        LIMIT ?" +
                        "    ) foo )";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, sup);
                statement.setString(2, sub);
                statement.setString(3, sup);
                statement.setString(4, sub);
                statement.setInt(5, Config.entry_itemsSize);
                statement.execute();
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not remove old item entries", 3);
            return false;
        }
    }

    public boolean removeItemOutliers(String league, IndexMap indexMap){
        league = formatLeague(league);

        String query1 = "DELETE FROM `$_"+ league +"_entry` " +
                        "WHERE `sup`= ? AND `sub`= ? " +
                        "AND `price` > (" +
                        "    SELECT AVG(`price`) + ? * STDDEV(`price`) " +
                        "    FROM `$_"+ league +"_entry`  " +
                        "    WHERE `sup`= ? AND `sub`= ?)";

        String query2 = "DELETE FROM `$_"+ league +"_entry` " +
                        "WHERE `sup`= ? AND `sub`= ? " +
                        "AND `price` < (" +
                        "    SELECT AVG(`price`) - ? * STDDEV(`price`) " +
                        "    FROM `$_"+ league +"_entry`  " +
                        "    WHERE `sup`= ? AND `sub`= ?) " +
                        "AND `price` < (" +
                        "    SELECT `median` FROM `$_"+ league +"_item` " +
                        "    WHERE `sup`= ? AND `sub`= ?) / ?";

        try {
            try (PreparedStatement statement = connection.prepareStatement(query1)) {
                for (String index : indexMap.keySet()) {
                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setDouble(3, Config.db_outlierMulti);
                    statement.setString(4, sup);
                    statement.setString(5, sub);
                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement = connection.prepareStatement(query2)) {
                for (String index : indexMap.keySet()) {
                    String sup = index.substring(0, Config.index_superSize);
                    String sub = index.substring(Config.index_superSize);

                    statement.setString(1, sup);
                    statement.setString(2, sub);
                    statement.setDouble(3, Config.db_outlierMulti);
                    statement.setString(4, sup);
                    statement.setString(5, sub);
                    statement.setString(6, sup);
                    statement.setString(7, sub);
                    statement.setDouble(8, Config.db_outlierMulti2);

                    statement.addBatch();
                }

                statement.executeBatch();
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

        String query1 = "CREATE TABLE `$_"+ league +"_item` (" +
                        "    CONSTRAINT `"+ league +"_sup-sub`" +
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

        String query3 = "CREATE TABLE `$_"+ league +"_entry` (" +
                        "    CONSTRAINT `"+ league +"_entry`" +
                        "        PRIMARY KEY (`sup`,`sub`,`id`)," +
                        "        FOREIGN KEY (`sup`,`sub`)" +
                        "        REFERENCES `item_data_sub` (`sup`,`sub`)" +
                        "        ON DELETE CASCADE," +

                        "    `sup`       varchar(5)      NOT NULL," +
                        "    `sub`       varchar(2)      NOT NULL," +

                        "    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "    `price`     decimal(10,4)   NOT NULL," +
                        "    `account`   varchar(32)     NOT NULL," +
                        "    `id`        varchar(32)     NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String query4 = "CREATE TABLE `$_"+ league +"_history` (" +
                        "    CONSTRAINT `"+ league +"_history`" +
                        "        FOREIGN KEY (`sup`,`sub`)" +
                        "        REFERENCES `item_data_sub` (`sup`,`sub`)" +
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

            if (!tables.contains("$_"+ league +"_item")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query1);
                }
            }

            if (!tables.contains("$_"+ league +"_entry")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(query3);
                }
            }

            if (!tables.contains("$_"+ league +"_history")) {
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
