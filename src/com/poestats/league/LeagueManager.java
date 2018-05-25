package com.poestats.league;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poestats.Config;
import com.poestats.Main;
import com.poestats.Misc;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class LeagueManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Gson gson = Main.getGson();
    private List<LeagueEntry> leagues;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Prepares league data lists on program start.
     *
     * @return False on failure
     */
    public boolean loadLeaguesOnStartup() {
        List<LeagueEntry> tmpLeagueList = downloadLeagueList();

        // Download failed. Load leagues from database
        if (tmpLeagueList == null) {
            tmpLeagueList = Main.DATABASE.getLeagues();

            // Download failed AND database doesn't have league data. Shut down the program.
            if (tmpLeagueList == null) return false;
        }

        leagues = sortLeagues(tmpLeagueList);

        // Update database leagues
        Main.DATABASE.updateLeagues(leagues);

        return true;
    }

    /**
     * Downloads leagues, updates database and local arrays
     *
     * @return False of failure
     */
    public boolean download() {
        List<LeagueEntry> tmpLeagueList = downloadLeagueList();
        if (tmpLeagueList == null) return false;

        leagues = sortLeagues(tmpLeagueList);

        // Update database leagues if there have been any changes
        Main.DATABASE.updateLeagues(leagues);

        return true;
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    private List<LeagueEntry> sortLeagues(List<LeagueEntry> leagueList) {
        // Get rid of SSF
        List<LeagueEntry> leagueListNoSSF = new ArrayList<>();
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getId().contains("SSF")) continue;
            leagueListNoSSF.add(leagueEntry);
        }

        List<LeagueEntry> tmpLeagueList = new ArrayList<>(leagueListNoSSF.size());

        // Add softcore events
        for (LeagueEntry leagueEntry : leagueListNoSSF) {
            if (leagueEntry.getId().contains("Event") || leagueEntry.getId().contains("(")) {
                if (!leagueEntry.getId().contains("Hardcore") && !leagueEntry.getId().contains("HC")) {
                    tmpLeagueList.add(leagueEntry);
                }
            }
        }

        // Add hardcore events
        for (LeagueEntry leagueEntry : leagueListNoSSF) {
            if (leagueEntry.getId().contains("Event") || leagueEntry.getId().contains("(")) {
                if (leagueEntry.getId().contains("Hardcore") || leagueEntry.getId().contains("HC")) {
                    tmpLeagueList.add(leagueEntry);
                }
            }
        }

        // Add main softcore league
        for (LeagueEntry leagueEntry : leagueListNoSSF) {
            if (leagueEntry.getId().contains("Event") || leagueEntry.getId().contains("(")) continue;
            if (leagueEntry.getId().equals("Hardcore") || leagueEntry.getId().equals("Standard")) continue;

            if (!leagueEntry.getId().contains("Hardcore") && !leagueEntry.getId().contains("HC")) {
                tmpLeagueList.add(leagueEntry);
            }
        }

        // Add main hardcore league
        for (LeagueEntry leagueEntry : leagueListNoSSF) {
            if (leagueEntry.getId().contains("Event") || leagueEntry.getId().contains("(")) continue;
            if (leagueEntry.getId().equals("Hardcore") || leagueEntry.getId().equals("Standard")) continue;

            if (leagueEntry.getId().contains("Hardcore") || leagueEntry.getId().contains("HC")) {
                tmpLeagueList.add(leagueEntry);
            }
        }

        // Add Standard
        for (LeagueEntry leagueEntry : leagueListNoSSF) {
            if (leagueEntry.getId().equals("Standard")) tmpLeagueList.add(leagueEntry);
        }

        // Add Hardcore
        for (LeagueEntry leagueEntry : leagueListNoSSF) {
            if (leagueEntry.getId().equals("Hardcore")) tmpLeagueList.add(leagueEntry);
        }

        return tmpLeagueList;
    }

    //------------------------------------------------------------------------------------------------------------
    // I/O
    //------------------------------------------------------------------------------------------------------------

    /**
     * Downloads a list of active leagues from `api.pathofexile.com/leagues`
     *
     * @return List of valid LeagueEntry'ies or null on exception
     */
    private List<LeagueEntry> downloadLeagueList() {
        InputStream stream = null;

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
            return gson.fromJson(stringBuilderBuffer.toString(), listType);
        } catch (Exception ex) {
            Main.ADMIN.log_("Failed to download league list", 3);
            Main.ADMIN._log(ex, 3);
            return null;
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
