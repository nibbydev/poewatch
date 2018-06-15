package com.poestats.league;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poestats.Config;
import com.poestats.Main;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;

public class LeagueManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Gson gson = Main.getGson();
    private List<LeagueEntry> leagues = new ArrayList<>();

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Prepares league data lists on program start.
     *
     * @return False on failure
     */
    public boolean loadLeaguesOnStartup() {
        boolean success;

        success = downloadLeagueList(leagues);

        // Download failed. Load leagues from database
        if (!success) {
            success = Main.DATABASE.getLeagues(leagues);

            // Download failed AND database doesn't have league data. Shut down the program.
            if (!success) {
                Main.ADMIN.log_("Failed to query leagues from API and database. Shutting down...", 5);
                return false;
            } else if (leagues.isEmpty()) {
                Main.ADMIN.log_("Failed to query leagues from API and database did not contain any records. Shutting down...", 5);
                return false;
            }
        }

        sortLeagues(leagues);

        // Update database leagues
        Main.DATABASE.updateLeagues(leagues);

        for (LeagueEntry leagueEntry : leagues) {
            Main.DATABASE.createLeagueTables(leagueEntry.getName());
        }

        return true;
    }

    /**
     * Downloads leagues, updates database and local arrays
     *
     * @return False of failure
     */
    public boolean download() {
        List<LeagueEntry> tmpLeagueList = new ArrayList<>();
        boolean success;

        success = downloadLeagueList(tmpLeagueList);

        // Download failed. Load leagues from database
        if (!success) {
            success = Main.DATABASE.getLeagues(tmpLeagueList);

            // Download failed AND database doesn't have league data. Shut down the program.
            if (!success) return false;
        }

        sortLeagues(tmpLeagueList);
        leagues = tmpLeagueList;

        // Update database leagues if there have been any changes
        Main.DATABASE.updateLeagues(leagues);

        for (LeagueEntry leagueEntry : leagues) {
            Main.DATABASE.createLeagueTables(leagueEntry.getName());
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    private void sortLeagues(List<LeagueEntry> leagueList) {
        List<LeagueEntry> sortedLeagueList = new ArrayList<>();

        // Get rid of SSF
        for (LeagueEntry leagueEntry : new ArrayList<>(leagueList)) {
            if (leagueEntry.getName().contains("SSF")) {
                leagueList.remove(leagueEntry);
            }
        }

        // Add softcore events
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().contains("Event") || leagueEntry.getName().contains("(")) {
                if (!leagueEntry.getName().contains("Hardcore") && !leagueEntry.getName().contains("HC")) {
                    sortedLeagueList.add(leagueEntry);
                }
            }
        }

        // Add hardcore events
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().contains("Event") || leagueEntry.getName().contains("(")) {
                if (leagueEntry.getName().contains("Hardcore") || leagueEntry.getName().contains("HC")) {
                    sortedLeagueList.add(leagueEntry);
                }
            }
        }

        // Add main softcore league
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().contains("Event") || leagueEntry.getName().contains("(")) continue;
            if (leagueEntry.getName().equals("Hardcore") || leagueEntry.getName().equals("Standard")) continue;

            if (!leagueEntry.getName().contains("Hardcore") && !leagueEntry.getName().contains("HC")) {
                sortedLeagueList.add(leagueEntry);
            }
        }

        // Add main hardcore league
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().contains("Event") || leagueEntry.getName().contains("(")) continue;
            if (leagueEntry.getName().equals("Hardcore") || leagueEntry.getName().equals("Standard")) continue;

            if (leagueEntry.getName().contains("Hardcore") || leagueEntry.getName().contains("HC")) {
                sortedLeagueList.add(leagueEntry);
            }
        }

        // Add Standard
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().equals("Standard")) {
                sortedLeagueList.add(leagueEntry);
            }
        }

        // Add Hardcore
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().equals("Hardcore")) {
                sortedLeagueList.add(leagueEntry);
            }
        }

        leagueList.clear();
        leagueList.addAll(sortedLeagueList);
    }

    //------------------------------------------------------------------------------------------------------------
    // I/O
    //------------------------------------------------------------------------------------------------------------

    /**
     * Downloads a list of active leagues from `api.pathofexile.com/leagues`
     *
     * @return List of valid LeagueEntry'ies or null on exception
     */
    private boolean downloadLeagueList(List<LeagueEntry> leagueEntryList) {
        InputStream stream = null;
        leagueEntryList.clear();

        try {
            // Define the request
            URL request = new URL(Config.league_APIBaseURL);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            connection.setReadTimeout(Config.league_readTimeoutMS);
            connection.setConnectTimeout(Config.league_connectTimeoutMS);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Define some elements
            StringBuilder stringBuilderBuffer = new StringBuilder();
            byte[] byteBuffer = new byte[Config.league_downloadBufferSize];
            int byteCount;

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0,Config.league_downloadBufferSize)) != -1) {
                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != Config.league_downloadBufferSize) {
                    byte[] trimmedByteBuffer = new byte[byteCount];
                    System.arraycopy(byteBuffer, 0, trimmedByteBuffer, 0, byteCount);

                    // Trim byteBuffer, convert it into string and add to string buffer
                    stringBuilderBuffer.append(new String(trimmedByteBuffer));
                } else {
                    stringBuilderBuffer.append(new String(byteBuffer));
                }
            }

            // Attempt to parse league list
            Type listType = new TypeToken<List<LeagueEntry>>(){}.getType();
            leagueEntryList.addAll(gson.fromJson(stringBuilderBuffer.toString(), listType));

            return true;
        } catch (SocketTimeoutException ex) {
            Main.ADMIN.log_("Failed to download league list (timeout)", 3);
            return false;
        } catch (Exception ex) {
            Main.ADMIN.log_("Failed to download league list", 3);
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException ex) {
                Main.ADMIN._log(ex, 3);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public List<LeagueEntry> getLeagues() {
        return leagues;
    }
}
