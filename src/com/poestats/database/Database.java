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
        try {
            connection = DriverManager.getConnection(Config.db_address, Config.db_username, Config.getDb_password());
            connection.setCatalog(Config.db_database);
            connection.setAutoCommit(false);

            tables = listAllTables();
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

    private ArrayList<String> listAllTables() throws SQLException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SHOW tables");
            result = statement.executeQuery();

            ArrayList<String> tables = new ArrayList<>();

            while (result.next()) {
                tables.add(result.getString("Tables_in_" + Config.db_database));
            }

            return tables;
        } finally {
            if (statement != null) statement.close();
            if (result != null) result.close();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Access methods
    //------------------------------------------------------------------------------------------------------------

    public List<LeagueEntry> getLeagues() {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SELECT * FROM `leagues`");
            result = statement.executeQuery();

            List<LeagueEntry> leagueEntries = new ArrayList<>();

            while (result.next()) {
                LeagueEntry leagueEntry = new LeagueEntry();

                // TODO: SQL database has additional field display
                leagueEntry.setId(result.getString("id"));
                leagueEntry.setEndAt(result.getString("start"));
                leagueEntry.setStartAt(result.getString("end"));

                leagueEntries.add(leagueEntry);
            }

            return leagueEntries;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query database league list", 3);
            return null;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (result != null) result.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * Compares provided league entries to ones present in database, updates any changes and adds missing leagues
     *
     * @param leagueEntries List of the most recent LeagueEntry objects
     */
    public void updateLeagues(List<LeagueEntry> leagueEntries) {
        PreparedStatement statement = null;

        try {
            String query =  "INSERT INTO `leagues`" +
                            "    (`id`, `start`, `end`)" +
                            "VALUES" +
                            "    (?, ?, ?)" +
                            "ON DUPLICATE KEY UPDATE" +
                            "    `start`     = VALUES(`start`)," +
                            "    `end`       = VALUES(`end`)";
            statement = connection.prepareStatement(query);

            for (LeagueEntry leagueEntry : leagueEntries) {
                statement.setString(1, leagueEntry.getId());
                statement.setString(2, leagueEntry.getStartAt());
                statement.setString(3, leagueEntry.getEndAt());
                statement.addBatch();
            }

            statement.executeBatch();

            // Commit changes
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update database league list", 3);
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * Removes any previous and updates the changeID record in table `changeid`
     *
     * @param changeID New changeID string to store
     */
    public void updateChangeID(String changeID) {
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;

        try {
            String query1 = "DELETE FROM `changeid`";
            String query2 = "INSERT INTO `changeid` (`changeid`) VALUES (?)";

            statement1 = connection.prepareStatement(query1);
            statement2 = connection.prepareStatement(query2);

            statement2.setString(1, changeID);

            statement1.execute();
            statement2.execute();

            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update database change id", 3);
        } finally {
            try { if (statement1 != null) statement1.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statement2 != null) statement2.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * Gets a list of parent and child categories and their display names from the database
     *
     * @return Map of categories or null on error
     */
    public Map<String, List<String>> getCategories() {
        PreparedStatement statement = null;
        ResultSet result = null;

        Map<String, List<String>> categories = new HashMap<>();

        try {
            String query =  "SELECT " +
                                "`category_parent`.`parent`, " +
                                "`category_parent`.`display`, " +
                                "`category_child`.`child`, " +
                                "`category_child`.`display` " +
                            "FROM `category_child`" +
                                "JOIN `category_parent`" +
                                    "ON `category_child`.`parent` = `category_parent`.`parent`";
            statement = connection.prepareStatement(query);
            result = statement.executeQuery();

            // Get parent categories
            while (result.next()) {
                String parent = result.getString(1);
                String parentDisplay = result.getString(2);

                String child = result.getString(3);
                String childDisplay = result.getString(4);

                List<String> childCategories = categories.getOrDefault(parent, new ArrayList<>());
                childCategories.add(child);
                categories.putIfAbsent(parent, childCategories);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query categories", 3);
            return null;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (result != null) result.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }

        return categories;
    }

    //--------------------
    // Item data
    //--------------------

    /**
     * Get item data relations from database
     *
     * @return Map of indexed items or null on error
     */
    public Map<String, SupIndexedItem> getItemData() {
        PreparedStatement statement = null;
        ResultSet result = null;

        Map<String, SupIndexedItem> relations = new HashMap<>();

        try {
            String query =  "SELECT " +
                                "`item_data_sup`.`sup`, " +
                                "`item_data_sup`.`parent`, " +
                                "`item_data_sup`.`child`, " +
                                "`item_data_sup`.`name`, " +
                                "`item_data_sup`.`type`, " +
                                "`item_data_sup`.`frame`, " +
                                "`item_data_sup`.`key`, " +

                                "`item_data_sub`.`sub`, " +
                                "`item_data_sub`.`tier`, " +
                                "`item_data_sub`.`lvl`, " +
                                "`item_data_sub`.`quality`, " +
                                "`item_data_sub`.`corrupted`, " +
                                "`item_data_sub`.`links`, " +
                                "`item_data_sub`.`var`, " +
                                "`item_data_sub`.`key`, " +
                                "`item_data_sub`.`icon` " +
                            "FROM `item_data_sub`" +
                                "JOIN `item_data_sup`" +
                                    "ON `item_data_sub`.`sup` = `item_data_sup`.`sup`";
            statement = connection.prepareStatement(query);
            result = statement.executeQuery();

            // Get parent categories
            while (result.next()) {
                String sup = result.getString(1);
                String sub = result.getString(8);

                String supKey = result.getString(7);
                String subKey = result.getString(15);

                String parent = result.getString(2);
                String child = result.getString(3);

                String name = result.getString(4);
                String type = result.getString(5);
                int frame = result.getInt(6);

                String tier = result.getString(9);
                String lvl = result.getString(10);
                String quality = result.getString(11);
                String corrupted = result.getString(12);
                String links = result.getString(13);
                String var = result.getString(14);
                String icon = result.getString(16);

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
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not query item data", 3);
            return null;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (result != null) result.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }

        return relations;
    }

    public boolean addFullItemData(SupIndexedItem supIndexedItem, String sup, String sub) {
        PreparedStatement statement = null;

        try {
            String query =  "INSERT INTO `item_data_sup` " +
                                "(`sup`,`parent`,`child`,`name`,`type`,`frame`,`key`) " +
                            "VALUES " +
                                "(?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE" +
                                "`sup`=`sup`";
            statement = connection.prepareStatement(query);

            statement.setString(1, sup);
            statement.setString(2, supIndexedItem.getParent());
            statement.setString(3, supIndexedItem.getChild());
            statement.setString(4, supIndexedItem.getName());
            statement.setString(5, supIndexedItem.getType());
            statement.setInt(6, supIndexedItem.getFrame());
            statement.setString(7, supIndexedItem.getKey());

            statement.execute();

            addSubItemData(supIndexedItem, sup, sub);

            // Commit changes
            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update item data in database", 3);
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean addSubItemData(SupIndexedItem supIndexedItem, String sup, String sub) {
        PreparedStatement statement = null;
        SubIndexedItem subIndexedItem = supIndexedItem.getSubIndexes().get(sup + sub);

        try {
            String query =  "INSERT INTO `item_data_sub`" +
                                "(`sup`,`sub`,`tier`,`lvl`,`quality`,`corrupted`,`links`,`var`,`key`,`icon`) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE `sup`=`sup`";
            statement = connection.prepareStatement(query);

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

            // Commit changes
            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update item data in database", 3);
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
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
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SELECT * FROM `status`");
            result = statement.executeQuery();

            while (result.next()) {
                switch (result.getString("name")) {
                    case "twentyFourCounter":
                        statusElement.twentyFourCounter = result.getLong("val");
                        break;
                    case "sixtyCounter":
                        statusElement.sixtyCounter = result.getLong("val");
                        break;
                    case "tenCounter":
                        statusElement.tenCounter = result.getLong("val");
                        break;
                }
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (result != null) result.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * Removes any previous and updates the status records in table `status`
     *
     * @param statusElement StatusElement to copy
     * @return True on success
     */
    public boolean updateStatus(StatusElement statusElement) {
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;

        try {
            String query1 = "DELETE FROM `status`";
            String query2 = "INSERT INTO `status` (`val`, `name`) VALUES (?, ?)";

            statement1 = connection.prepareStatement(query1);
            statement2 = connection.prepareStatement(query2);

            statement2.setLong(1, statusElement.twentyFourCounter);
            statement2.setString(2, "twentyFourCounter");
            statement2.addBatch();

            statement2.setLong(1, statusElement.sixtyCounter);
            statement2.setString(2, "sixtyCounter");
            statement2.addBatch();

            statement2.setLong(1, statusElement.tenCounter);
            statement2.setString(2, "tenCounter");
            statement2.addBatch();

            statement2.setLong(1, statusElement.lastRunTime);
            statement2.setString(2, "lastRunTime");
            statement2.addBatch();

            statement1.execute();
            statement2.executeBatch();
            connection.commit();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement1 != null) statement1.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statement2 != null) statement2.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    //--------------------
    // Entry management
    //--------------------

    public boolean getCurrency(String league, CurrencyMap currencyMap) {
        PreparedStatement statement = null;
        ResultSet result = null;
        league = formatLeague(league);

        try {
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
                            "    i.`dec`" +
                            "FROM `!_"+ league +"_item` AS i" +
                            "    INNER JOIN `item_data_sup` AS d" +
                            "        ON i.`sup` = d.`sup`" +
                            "WHERE EXISTS (" +
                            "    SELECT * FROM `item_data_sup` AS a " +
                            "    WHERE a.`sup` = i.`sup` " +
                            "    AND a.`parent` = 'currency'" +
                            "    AND a.`frame` = 5)";
            statement = connection.prepareStatement(query);
            result = statement.executeQuery();

            while (result.next()) {
                String name = result.getString("name");

                CurrencyItem currencyItem = new CurrencyItem();
                currencyItem.loadItem(result);
                currencyMap.put(name, currencyItem);
            }

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (result != null) result.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
/*
    public CurrencyItem getFullItem(String index, String league) {
        PreparedStatement statementItem = null;
        PreparedStatement statementEntry = null;
        ResultSet resultItem = null;
        ResultSet resultEntry = null;
        league = formatLeague(league);

        try {
            String sup = index.substring(0, Config.index_superSize);
            String sub = index.substring(Config.index_superSize);

            String queryItem = "SELECT * FROM `!_"+ league +"_item` WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";
            String queryEntry = "SELECT * FROM `!_"+ league +"_item_entry` WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";

            statementItem = connection.prepareStatement(queryItem);
            resultItem = statementItem.executeQuery();

            CurrencyItem currencyItem = new CurrencyItem(sup, sub);

            if (resultItem.next()) {
                currencyItem.loadItem(resultItem);

                statementEntry = connection.prepareStatement(queryEntry);
                resultEntry = statementEntry.executeQuery();

                currencyItem.loadEntries(resultEntry);
            }

            return currencyItem;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try { if (statementItem != null) statementItem.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statementEntry != null) statementEntry.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (resultItem != null) resultItem.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (resultEntry != null) resultEntry.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
*/
/*
    public boolean updateFullItem(String league, String index, DatabaseEntryHolder entryHolder) {
        PreparedStatement statement = null;
        league = formatLeague(league);

        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        String query = "INSERT INTO `!_"+ league +"_item`" +
                        "    (`sup`, `sub`, `mean`, `median`, `mode`, `exalted`, `inc`, `dec`, `count`, `quantity`)" +
                        "VALUES" +
                        "    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                        "ON DUPLICATE KEY UPDATE" +
                        "    `mean`      = VALUES(`mean`)," +
                        "    `median`    = VALUES(`median`)," +
                        "    `mode`      = VALUES(`mode`)," +
                        "    `exalted`   = VALUES(`exalted`)," +
                        "    `inc`       = VALUES(`inc`)," +
                        "    `dec`       = VALUES(`dec`)," +
                        "    `count`     = VALUES(`count`)," +
                        "    `quantity`  = VALUES(`quantity`)";

        try {
            statement = connection.prepareStatement(query);
            statement.setString(1, sup);
            statement.setString(2, sub);
            statement.setDouble(3, entryHolder.getMean());
            statement.setDouble(4, entryHolder.getMedian());
            statement.setDouble(5, entryHolder.getMode());
            statement.setDouble(6, entryHolder.getExalted());

            statement.setInt(7, entryHolder.getInc());
            statement.setInt(8, entryHolder.getDec());
            statement.setInt(9, entryHolder.getCount());
            statement.setInt(10, entryHolder.getQuantity());

            statement.execute();

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
*/
    public boolean uploadRaw(String league, IndexMap indexMap) {
        PreparedStatement statement = null;
        league = formatLeague(league);

        String query =  "INSERT INTO `!_"+ league +"_item_entry` (`sup`, `sub`, `price`, `account`, `id`) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `sup`=`sup`";

        try {
            statement = connection.prepareStatement(query);

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
            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean updateCounters(String league, IndexMap indexMap) {
        PreparedStatement statement = null;
        league = formatLeague(league);

        String query =  "UPDATE `!_"+ league +"_item` " +
                        "SET `count`=`count` + ?, `inc`=`inc` + ? " +
                        "WHERE `sup`=? AND `sub`=?";

        try {
            statement = connection.prepareStatement(query);

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
            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /*
    public boolean calculatePrices(String league, String index, RawList rawList) {
        PreparedStatement statementX = null;
        PreparedStatement statementY = null;
        PreparedStatement statementZ = null;
        PreparedStatement statementW = null;
        ResultSet resultSetW = null;
        ResultSet resultSetZ = null;
        ResultSet resultSetY = null;
        league = formatLeague(league);

        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        CurrencyItem currencyItem = new CurrencyItem(sup, sub);

        // Get current data
        String queryW = "SELECT * FROM `!_"+ league +"_item` " +
                        "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";

        // Calc mean
        String queryZ = "SELECT AVG(`price`) FROM `!_"+ league +"_item_entry` " +
                        "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";

        // Calc median
        String queryY = "SELECT AVG(t1.`price`) as median_val FROM (" +
                        "    SELECT @rownum:=@rownum+1 as `row_number`, d.`price`" +
                        "    FROM `!_"+ league +"_item_entry` d,  (SELECT @rownum:=0) r" +
                        "    WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                        "    ORDER BY d.`price`" +
                        ") as t1, (" +
                        "    SELECT count(*) as total_rows" +
                        "    FROM `!_"+ league +"_item_entry` d" +
                        "    WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                        ") as t2 WHERE 1" +
                        "AND t1.row_number in ( floor((total_rows+1)/2), floor((total_rows+2)/2) )";

        // Update item
        String queryX = "INSERT INTO `!_"+ league +"_item`" +
                        "    (`sup`, `sub`, `mean`, `median`, `mode`, `exalted`, `inc`, `dec`, `count`, `quantity`)" +
                        "VALUES" +
                        "    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                        "ON DUPLICATE KEY UPDATE" +
                        "    `mean`      = VALUES(`mean`)," +
                        "    `median`    = VALUES(`median`)," +
                        "    `mode`      = VALUES(`mode`)," +
                        "    `exalted`   = VALUES(`exalted`)," +
                        "    `inc`       = VALUES(`inc`)," +
                        "    `dec`       = VALUES(`dec`)," +
                        "    `count`     = VALUES(`count`)," +
                        "    `quantity`  = VALUES(`quantity`)";

        try {
            statementW = connection.prepareStatement(queryW);
            statementZ = connection.prepareStatement(queryZ);
            statementY = connection.prepareStatement(queryY);

            // Get current data
            resultSetW = statementW.executeQuery();
            if (resultSetW.next()) currencyItem.loadItem(resultSetW);

            // Calculate mean
            resultSetZ = statementZ.executeQuery();
            if (resultSetZ.next()) currencyItem.setMean(resultSetZ.getDouble(1));

            // Calculate median
            resultSetY = statementY.executeQuery();
            if (resultSetY.next()) currencyItem.setMedian(resultSetY.getDouble(1));

            // Add values together
            currencyItem.addCount(rawList.size());

            statementX = connection.prepareStatement(queryX);
            statementX.setString(1, sup);
            statementX.setString(2, sub);
            statementX.setDouble(3, currencyItem.getMean());
            statementX.setDouble(4, currencyItem.getMedian());
            statementX.setDouble(5, currencyItem.getMode());
            statementX.setDouble(6, currencyItem.getExalted());
            statementX.setInt(7, currencyItem.getInc());
            statementX.setInt(8, currencyItem.getDec());
            statementX.setInt(9, currencyItem.getCount());
            statementX.setInt(10, currencyItem.getQuantity());
            statementX.execute();

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statementX != null) statementX.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statementY != null) statementY.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statementZ != null) statementZ.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statementW != null) statementW.close(); } catch (SQLException ex) { ex.printStackTrace(); }

            try { if (resultSetW != null) resultSetW.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (resultSetZ != null) resultSetZ.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (resultSetY != null) resultSetY.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
    */
/*
    public boolean getItem(String league, String index, DatabaseEntryHolder entryHolder) {
        PreparedStatement statement = null;
        ResultSet result = null;

        league = formatLeague(league);

        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        String query =  "SELECT * FROM `!_"+ league +"_item` " +
                        "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";

        try {
            statement = connection.prepareStatement(query);
            result = statement.executeQuery();
            if (result.next()) entryHolder.loadItem(result);

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (result != null) result.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
    */
/*
    public boolean getEntries(String league, String index, DatabaseEntryHolder entryHolder) {
        PreparedStatement statement = null;
        ResultSet result = null;

        league = formatLeague(league);

        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        String query =  "SELECT `price` FROM `!_"+ league +"_item_entry` " +
                        "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";

        try {
            statement = connection.prepareStatement(query);
            result = statement.executeQuery();

            entryHolder.loadEntries(result);

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (result != null) result.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
*/
    public boolean calculateMean(String league, String index) {
        PreparedStatement statement = null;

        league = formatLeague(league);

        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        String query =  "UPDATE `!_"+ league +"_item` " +
                        "SET `mean` = (" +
                        "    SELECT AVG(`price`) FROM `!_"+ league +"_item_entry` " +
                        "    WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                        ") WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";

        try {
            statement = connection.prepareStatement(query);
            statement.execute();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean calculateMedian(String league, String index) {
        PreparedStatement statement = null;

        league = formatLeague(league);

        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        String query =  "UPDATE `!_"+ league +"_item` " +
                        "SET `median` = (" +
                        "    SELECT AVG(t1.`price`) as median_val FROM (" +
                        "        SELECT @rownum:=@rownum+1 as `row_number`, d.`price`" +
                        "        FROM `!_"+ league +"_item_entry` d,  (SELECT @rownum:=0) r" +
                        "        WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                        "        ORDER BY d.`price`" +
                        "    ) as t1, (" +
                        "        SELECT count(*) as total_rows" +
                        "        FROM `!_"+ league +"_item_entry` d" +
                        "        WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                        "    ) as t2 WHERE 1" +
                        "    AND t1.row_number in ( floor((total_rows+1)/2), floor((total_rows+2)/2) )" +
                        ") WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";

        try {
            statement = connection.prepareStatement(query);
            statement.execute();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    //--------------------------
    // History entry management
    //--------------------------

    public boolean addMinutely(String league) {
        PreparedStatement statement = null;
        league = formatLeague(league);

        try {
            String query =  "INSERT INTO `!_"+ league +"_history_entry` " +
                            "    (`sup`,`sub`,`type`,`mean`,`median`,`mode`,`exalted`,`inc`,`dec`,`count`,`quantity`)" +
                            "SELECT " +
                            "    `sup`,`sub`,'minutely',`mean`,`median`,`mode`,`exalted`,`inc`,`dec`,`count`,`quantity`" +
                            "FROM `!_"+ league +"_item`";
            statement = connection.prepareStatement(query);
            statement.execute();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean addHourly(String league, String index) {
        PreparedStatement statement = null;

        league = formatLeague(league);
        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        try {
            String query =  "INSERT INTO `!_"+ league +"_history_entry`" +
                            "    (`sup`, `sub`, `type`, `mean`, `median`, `mode`, " +
                            "     `exalted`, `count`, `quantity`, `inc`, `dec`)" +
                            "SELECT" +
                            "    '"+ sup +"', '"+ sub +"', 'hourly', AVG(`mean`), AVG(`median`), AVG(`mode`), " +
                            "    AVG(`exalted`), MAX(`count`), MAX(`quantity`),  MAX(`inc`),  MAX(`dec`)" +
                            "FROM `!_"+ league +"_history_entry`" +
                            "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"' AND `type`='minutely'";
            statement = connection.prepareStatement(query);
            statement.execute();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean calcQuantity(String league, String index) {
        PreparedStatement statement = null;

        league = formatLeague(league);
        String sup = index.substring(0, Config.index_superSize);
        String sub = index.substring(Config.index_superSize);

        try {
            String query =  "UPDATE `!_"+ league +"_item` " +
                            "SET `inc`=0, `dec`=0, `quantity` = (" +
                            "    SELECT SUM(`inc`) FROM `!_"+ league +"_history_entry` " +
                            "    WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"' AND `type`='hourly'" +
                            ") WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'";
            statement = connection.prepareStatement(query);
            statement.execute();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean removeOldHistoryEntries(String league, String type, String interval) {
        PreparedStatement statement = null;
        league = formatLeague(league);

        try {
            String query =  "DELETE FROM `!_"+ league +"_history_entry` " +
                            "WHERE `type` = '"+ type+ "' " +
                            "AND `time` < ADDDATE(NOW(), INTERVAL -"+ interval +")";
            statement = connection.prepareStatement(query);
            statement.execute();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean removeOldItemEntries(String league, String index) {
        PreparedStatement statement = null;
        league = formatLeague(league);

        try {
            String sup = index.substring(0, Config.index_superSize);
            String sub = index.substring(Config.index_superSize);

            String query =  "DELETE FROM `!_"+ league +"_item_entry`" +
                            "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"' AND `id` NOT IN (" +
                            "    SELECT `id`" +
                            "    FROM (" +
                            "        SELECT `id`, `time`" +
                            "        FROM `!_"+ league +"_item_entry`" +
                            "        WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                            "        ORDER BY `time` DESC" +
                            "        LIMIT "+ Config.entry_itemsSize +
                            "    ) foo" +
                            ")";
            statement = connection.prepareStatement(query);
            statement.execute();

            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement != null) statement.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean removeItemOutliers(String league, IndexMap indexMap){
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        league = formatLeague(league);

        try {
            for (String index : indexMap.keySet()) {
                String sup = index.substring(0, Config.index_superSize);
                String sub = index.substring(Config.index_superSize);

                String query1 = "DELETE FROM `!_"+ league +"_item_entry` " +
                                "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"' " +
                                "AND `price` > (" +
                                "    SELECT * FROM (" +
                                "        SELECT AVG(`price`) + "+ Config.db_outlierMulti +"*STDDEV(`price`) " +
                                "        FROM `!_"+ league +"_item_entry`  " +
                                "        WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                                "    ) foo )";

                String query2 = "DELETE FROM `!_"+ league +"_item_entry` " +
                                "WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"' " +
                                "AND `price` < (" +
                                "    SELECT * FROM (" +
                                "        SELECT AVG(`price`) - "+ Config.db_outlierMulti +"*STDDEV(`price`) " +
                                "        FROM `!_"+ league +"_item_entry`  " +
                                "        WHERE `sup`='"+ sup +"' AND `sub`='"+ sub +"'" +
                                "    ) foo )";

                statement1 = connection.prepareStatement(query1);
                statement2 = connection.prepareStatement(query2);

                statement1.execute();
                statement2.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement1 != null) statement1.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statement2 != null) statement2.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public boolean createLeagueTables(String league) {
        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        PreparedStatement statement3 = null;
        PreparedStatement statement4 = null;
        league = formatLeague(league);

        try {
            String query1 = "CREATE TABLE `!_"+ league +"_item` (" +
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

            String query2 = "CREATE TABLE `!_"+ league +"_history` (" +
                            "    CONSTRAINT `"+ league +"_history`" +
                            "        PRIMARY KEY (`sup`,`sub`)," +
                            "        FOREIGN KEY (`sup`,`sub`)" +
                            "        REFERENCES `item_data_sub` (`sup`,`sub`)" +
                            "        ON DELETE CASCADE," +

                            "    `sup`       varchar(5)      NOT NULL," +
                            "    `sub`       varchar(2)      NOT NULL," +

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

            String query3 = "CREATE TABLE `!_"+ league +"_item_entry` (" +
                            "    CONSTRAINT `"+ league +"_item_entry`" +
                            "        PRIMARY KEY (`sup`,`sub`,`account`)," +
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

            String query4 = "CREATE TABLE `!_"+ league +"_history_entry` (" +
                            "    CONSTRAINT `"+ league +"_history_entry`" +
                            "        FOREIGN KEY (`sup`,`sub`)" +
                            "        REFERENCES `item_data_sub` (`sup`,`sub`)" +
                            "        ON DELETE CASCADE," +
                            "    FOREIGN KEY (`type`)" +
                            "        REFERENCES `history_entry_category` (`type`)" +
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

            tables = listAllTables();

            if (!tables.contains("!_"+ league +"_item")) {
                statement1 = connection.prepareStatement(query1);
                statement1.execute();
            }

            if (!tables.contains("!_"+ league +"_history")) {
                statement2 = connection.prepareStatement(query2);
                statement2.execute();
            }

            if (!tables.contains("!_"+ league +"_item_entry")) {
                statement3 = connection.prepareStatement(query3);
                statement3.execute();
            }

            if (!tables.contains("!_"+ league +"_history_entry")) {
                statement4 = connection.prepareStatement(query4);
                statement4.execute();
            }

            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try { if (statement1 != null) statement1.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statement2 != null) statement2.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statement3 != null) statement3.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            try { if (statement4 != null) statement4.close(); } catch (SQLException ex) { ex.printStackTrace(); }
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
