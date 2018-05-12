package com.poestats.League;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
    private String[] stringLeagues;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Prepares league data lists on program start.
     *
     * @return False on failure.
     */
    public boolean loadLeaguesOnStartup() {
        if (stringLeagues != null) return false;

        download();

        if (stringLeagues == null) {
            readFromFile();
        }

        return stringLeagues != null;
    }

    public void readFromFile() {
        readLeaguesFromFile();
    }

    public void writeToFile() {
        saveDataToFiles();
    }

    public void download() {
        List<LeagueEntry> rawLeagueList = downloadLeagueList();
        if (rawLeagueList == null) return;

        leagues = sortLeagues(rawLeagueList);

        // Fill stringLeague list
        stringLeagues = new String[leagues.size()];
        for (int i = 0; i < leagues.size(); i++) {
            stringLeagues[i] = leagues.get(i).getId();
        }
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
            URL request = new URL("http://api.pathofexile.com/leagues?type=main&compact=1");
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(Main.CONFIG.readTimeOut);
            connection.setConnectTimeout(Main.CONFIG.connectTimeOut);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Define some elements
            StringBuilder stringBuilderBuffer = new StringBuilder();
            byte[] byteBuffer = new byte[64];
            int byteCount;

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0,64)) != -1) {
                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != 64) {
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

    /**
     * Saves league-related data to files
     */
    private void saveDataToFiles() {
        File dataFile = new File("./data/leagueData.json");
        try (Writer writer = Misc.defineWriter(dataFile)) {
            if (writer == null) throw new IOException();
            gson.toJson(leagues, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to '"+dataFile.getName()+"'", 3);
            Main.ADMIN._log(ex, 3);
        }

        File stringFile = new File("./data/leagueList.json");
        try (Writer writer = Misc.defineWriter(stringFile)) {
            if (writer == null) throw new IOException();
            gson.toJson(stringLeagues, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to '"+stringFile.getName()+"'", 3);
            Main.ADMIN._log(ex, 3);
        }
    }

    /**
     * Reads league-related data from files
     */
    private void readLeaguesFromFile() {
        File dataFile = new File("./data/leagueData.json");

        // Open up the reader
        try (Reader reader = Misc.defineReader(dataFile)) {
            if (reader == null) throw new IOException("File '"+dataFile.getName()+"' not found");

            Type listType = new TypeToken<List<LeagueEntry>>(){}.getType();
            leagues = gson.fromJson(reader, listType);

            // Fill stringLeagues
            for (int i = 0; i < leagues.size(); i++) {
                stringLeagues[i] = leagues.get(i).getId();
            }
        } catch (IOException ex) {
            Main.ADMIN.log_("Couldn't load '" + dataFile.getName() + "'", 3);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public List<LeagueEntry> getLeagues() {
        return leagues;
    }

    public String[] getStringLeagues() {
        return stringLeagues;
    }
}
