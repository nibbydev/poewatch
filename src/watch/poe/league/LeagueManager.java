package watch.poe.league;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import watch.poe.Config;
import watch.poe.Main;

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
    private Map<String, Integer> leagueIds = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Prepares league data on program start.
     *
     * @return True on success
     */
    public boolean loadLeaguesOnStartup() {
        boolean success = downloadLeagueList(leagues);

        if (success) {
            sortLeagues(leagues);
            Main.DATABASE.updateLeagues(leagues);
        }

        success = Main.DATABASE.getLeagues(leagues);

        // Download failed AND database doesn't have league data. Shut down the program.
        if (!success) {
            Main.ADMIN.log_("Failed to query leagues from API and database. Shutting down...", 5);
            return false;
        } else if (leagues.isEmpty()) {
            Main.ADMIN.log_("Failed to query leagues from API and database did not contain any records. Shutting down...", 5);
            return false;
        }

        for (LeagueEntry leagueEntry : leagues) {
            leagueIds.put(leagueEntry.getName(), leagueEntry.getId());
        }

        return true;
    }

    /**
     * Control method for connecting to league API, updating local objects and entries in database
     *
     * @return True on success
     */
    public boolean download() {
        List<LeagueEntry> tmpLeagueList = new ArrayList<>();

        // Attempt to connect to league API
        boolean success = downloadLeagueList(tmpLeagueList);

        // If connection succeeded, filter, sort and upload leagues to database
        if (success) {
            sortLeagues(tmpLeagueList);
            Main.DATABASE.updateLeagues(tmpLeagueList);
        } else return false;

        // Since connection succeeded, get leagues from database (including their IDs!)
        success = Main.DATABASE.getLeagues(tmpLeagueList);

        // Could not connect to database, return
        if (!success || tmpLeagueList.isEmpty()) {
            Main.ADMIN.log_("Failed to query leagues from database", 5);
            return false;
        }

        // Recreate league name to ID relations map
        leagueIds.clear();
        for (LeagueEntry leagueEntry : tmpLeagueList) {
            leagueIds.put(leagueEntry.getName(), leagueEntry.getId());
        }

        leagues = tmpLeagueList;

        return true;
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Filters out and sorts list of league objects.
     * Removes any SSF leagues and puts the remaining in a certain order
     *
     * @param leagueList ArrayList of LeagueEntry objects to be filtered and sorted
     */
    private void sortLeagues(List<LeagueEntry> leagueList) {
        List<LeagueEntry> sortedLeagueList = new ArrayList<>();

        // Get rid of SSF
        for (LeagueEntry leagueEntry : new ArrayList<>(leagueList)) {
            if (leagueEntry.getName().contains("SSF") || leagueEntry.getName().contains("Solo")) {
                leagueList.remove(leagueEntry);
            }
        }

        // Add Hardcore
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().equals("Hardcore")) {
                sortedLeagueList.add(leagueEntry);
                leagueList.remove(leagueEntry);
                break;
            }
        }

        // Add Standard
        for (LeagueEntry leagueEntry : leagueList) {
            if (leagueEntry.getName().equals("Standard")) {
                sortedLeagueList.add(leagueEntry);
                leagueList.remove(leagueEntry);
                break;
            }
        }

        // Add main hardcore leagues
        for (LeagueEntry leagueEntry : new ArrayList<>(leagueList)) {
            if (leagueEntry.isEvent()) continue;

            if (leagueEntry.getName().contains("Hardcore") || leagueEntry.getName().contains("HC")) {
                sortedLeagueList.add(leagueEntry);
                leagueList.remove(leagueEntry);
            }
        }

        // Add main softcore leagues
        for (LeagueEntry leagueEntry : new ArrayList<>(leagueList)) {
            if (leagueEntry.isEvent()) continue;

            if (!leagueEntry.getName().contains("Hardcore") && !leagueEntry.getName().contains("HC")) {
                sortedLeagueList.add(leagueEntry);
                leagueList.remove(leagueEntry);
            }
        }

        // Add hardcore events
        for (LeagueEntry leagueEntry : new ArrayList<>(leagueList)) {
            if (!leagueEntry.isEvent()) continue;

            if (leagueEntry.getName().contains("Hardcore") || leagueEntry.getName().contains("HC")) {
                sortedLeagueList.add(leagueEntry);
                leagueList.remove(leagueEntry);
            }
        }

        // Add softcore events
        for (LeagueEntry leagueEntry : new ArrayList<>(leagueList)) {
            if (!leagueEntry.isEvent()) continue;

            if (!leagueEntry.getName().contains("Hardcore") && !leagueEntry.getName().contains("HC")) {
                sortedLeagueList.add(leagueEntry);
                leagueList.remove(leagueEntry);
            }
        }

        leagueList.clear();
        leagueList.addAll(sortedLeagueList);
    }

    //------------------------------------------------------------------------------------------------------------
    // I/O
    //------------------------------------------------------------------------------------------------------------

    /**
     * Downloads a list of active leagues from the league API
     *
     * @return List of valid LeagueEntry'es or null on exception
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

    public Integer getLeagueId(String league) {
        return leagueIds.get(league);
    }

    public String getLeagueName(Integer id) {
        for (Map.Entry<String, Integer> entry : leagueIds.entrySet()) {
            if (entry.getValue().equals(id)) {
                return entry.getKey();
            }
        }

        return null;
    }
}
