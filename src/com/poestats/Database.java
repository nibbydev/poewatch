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
        }

        disconnect();
        System.exit(1);
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

    private List<LeagueEntry> getLeagues() {
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
