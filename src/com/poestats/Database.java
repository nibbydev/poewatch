package com.poestats;

import com.poestats.league.LeagueEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        String query = "SHOW tables";
        PreparedStatement statement = connection.prepareStatement(query);
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
            String query = "SELECT * FROM `leagues`";
            PreparedStatement statement = connection.prepareStatement(query);
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
            return null;
        }
    }

    /**
     * Compares provided league entires to ones present in database, updates any changes and adds missing leagues
     *
     * @param leagueEntries List of the most recent LeagueEntry objects
     */
    public void updateLeagues(List<LeagueEntry> leagueEntries) {
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
                        if (!result.getString("start").equals(leagueEntries.get(i).getStartAt()) ||
                                !result.getString("end").equals(leagueEntries.get(i).getEndAt())) {
                            statement2.setString(1, leagueEntries.get(i).getStartAt());
                            statement2.setString(2, leagueEntries.get(i).getEndAt());
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
                statement3.setString(2, leagueEntry.getStartAt());
                statement3.setString(3, leagueEntry.getEndAt());
                statement3.addBatch();
            }

            // Execute the batches
            statement2.executeBatch();
            statement3.executeBatch();

            // Commit changes
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------
}
