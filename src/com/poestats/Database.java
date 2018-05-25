package com.poestats;

import com.poestats.league.LeagueEntry;
import com.poestats.relations.entries.IndexedItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final String address = "jdbc:mysql://localhost:3306?serverTimezone=UTC";
    private final String db_username = "root";
    private final String db_password = "";
    private final String database = "ps_test_database";
    private Connection connection;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------
    // DB controllers
    //------------------------------------------------------------------------------------------------------------

    public void connect() {
        try {
            connection = DriverManager.getConnection(address, db_username, db_password);
            connection.setCatalog(database);
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
    // Initial DB setup
    //------------------------------------------------------------------------------------------------------------

    private ArrayList<String> listAllTables() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SHOW tables");
        ResultSet result = statement.executeQuery();

        ArrayList<String> tables = new ArrayList<>();

        while (result.next()) {
            tables.add(result.getString("Tables_in_" + database));
            System.out.println(result.getString("Tables_in_" + database));
        }

        return tables;
    }

    //------------------------------------------------------------------------------------------------------------
    // Access methods
    //------------------------------------------------------------------------------------------------------------

    public List<LeagueEntry> getLeagues() {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `leagues`");
            ResultSet result = statement.executeQuery();

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
        }
    }

    /**
     * Compares provided league entires to ones present in database, updates any changes and adds missing leagues
     *
     * @param leagueEntries List of the most recent LeagueEntry objects
     */
    public void updateLeagues(List<LeagueEntry> leagueEntries) {
        if (leagueEntries == null) {
            Main.ADMIN.log_("Could not update database league list (null passed)", 3);
            return;
        }

        try {
            String query1 = "SELECT * FROM `leagues`";
            String query2 = "UPDATE TABLE `leagues` SET (`start`, `end`) VALUES (?, ?) WHERE `id`=?";
            String query3 = "INSERT INTO `leagues` (`id`, `start`, `end`) VALUES (?, ?, ?)";

            PreparedStatement statement1 = connection.prepareStatement(query1);
            PreparedStatement statement2 = connection.prepareStatement(query2);
            PreparedStatement statement3 = connection.prepareStatement(query3);

            ResultSet result = statement1.executeQuery();

            // Loop though database's league entries
            while (result.next()) {
                // Loop though provided league entries
                for (int i = 0; i < leagueEntries.size(); i++) {
                    // If there's a match and the info has changed, update the database entry
                    if (result.getString("id").equals(leagueEntries.get(i).getId())) {
                        String start = result.getString("start");
                        String end = result.getString("end");

                        String startNew = leagueEntries.get(i).getStartAt();
                        String endNew = leagueEntries.get(i).getEndAt();

                        boolean update = false;

                        if (start == null) {
                            if (startNew != null) update = true;
                        } else if (!start.equals(startNew)) update = true;

                        if (end == null) {
                            if (endNew != null) update = true;
                        } else if (!end.equals(endNew)) update = true;

                        if (update) {
                            if (startNew == null) statement2.setNull(1, 0);
                            else statement2.setString(1, startNew);

                            if (endNew == null) statement2.setNull(2, 0);
                            else statement2.setString(2, endNew);

                            statement2.setString(3, leagueEntries.get(i).getId());
                            statement2.addBatch();
                        }

                        leagueEntries.remove(i);
                        break;
                    }
                }
            }

            // Loop though entries that were not present in the database
            for (LeagueEntry leagueEntry : leagueEntries) {
                statement3.setString(1, leagueEntry.getId());

                if (leagueEntry.getStartAt() == null) statement2.setNull(1, 0);
                else statement2.setString(2, leagueEntry.getStartAt());

                if (leagueEntry.getEndAt() == null) statement2.setNull(2, 0);
                else statement2.setString(3, leagueEntry.getEndAt());

                statement3.addBatch();
            }

            // Execute the batches
            statement2.executeBatch();
            statement3.executeBatch();

            // Commit changes
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update database league list", 3);
        }
    }

    /**
     * Removes any previous and updates the changeID record in table `changeid`
     *
     * @param changeID New changeID string to store
     */
    public void updateChangeID(String changeID) {
        try {
            String query1 = "DELETE FROM `changeid`";
            String query2 = "INSERT INTO `changeid` (`changeid`) VALUES (?)";

            PreparedStatement statement1 = connection.prepareStatement(query1);
            PreparedStatement statement2 = connection.prepareStatement(query2);

            statement2.setString(1, changeID);

            statement1.execute();
            statement2.execute();

            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not update database change id", 3);
        }
    }

    // Read in categories
    public Map<String, List<String>> getCategories() {
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
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet result = statement.executeQuery();

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
        }

        return categories;
    }

    // Read in itemData
    public void getItemData() {

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

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------
}
