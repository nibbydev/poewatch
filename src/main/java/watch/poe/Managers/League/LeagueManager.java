package poe.Managers.League;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.League.Derserializer.BaseLeague;
import poe.Managers.League.Derserializer.Rule;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeagueManager {
    private Gson gson = new Gson();
    private List<LeagueEntry> leagues;
    private Map<String, Integer> leagueIds;
    private Database database;

    private static Logger logger = LoggerFactory.getLogger(LeagueManager.class);
    private Config config;

    public LeagueManager(Database database,  Config config) {
        this.database = database;
        this.config = config;
    }

    /**
     * Control method for connecting to league API, updating local leagues and entries in database
     *
     * @return True on success
     */
    public boolean cycle() {
        List<BaseLeague> baseLeagues = new ArrayList<>();
        boolean success;

        logger.info("Starting league cycle");

        // Get list of leagues from official API
        success = downloadBaseLeagues(baseLeagues);
        if (success) {
            // If download was successful, parse the downloaded leagues and create/update database entries
            List<LeagueEntry> sortedLeagues = sortLeagues(baseLeagues);
            database.upload.updateLeagues(sortedLeagues);
        }

        // Get active league entries from database
        List<LeagueEntry> leagues = new ArrayList<>();
        success = database.init.getLeagues(leagues);
        if (!success) {
            // Download failed AND database doesn't have league data. Shut down the program.
            logger.error("Failed to query leagues from API and database");
            return false;
        } else if (leagues.isEmpty()) {
            logger.error("Failed to query leagues from API and database did not contain any records");
            return false;
        }

        // Populate small Map of league names : league ids
        Map<String, Integer> leagueIds = new HashMap<>();
        for (LeagueEntry leagueEntry : leagues) {
            leagueIds.put(leagueEntry.getName(), leagueEntry.getId());
        }

        // Assign variables by reference
        this.leagues = leagues;
        this.leagueIds = leagueIds;

        logger.info("League cycle finished successfully");

        // Return true to mark the league list was successfully obtained
        return true;
    }

    /**
     * Filters and sorts list of provided league objects.
     *
     * @param unsortedList ArrayList of LeagueEntry objects to be filtered and sorted
     * @return List of filtered, sorted and ordered LeagueEntry objects
     */
    private List<LeagueEntry> sortLeagues(List<BaseLeague> unsortedList) {
        List<BaseLeague> sortedList = new ArrayList<>();

        // Remove SSF
        for (BaseLeague baseLeague : new ArrayList<>(unsortedList)) {
            for (Rule rule : baseLeague.getRules()) {
                if (rule.getName().equals("Solo")) {
                    unsortedList.remove(baseLeague);
                }
            }
        }

        // Add Hardcore
        for (BaseLeague baseLeague : new ArrayList<>(unsortedList)) {
            if (baseLeague.getName().equals("Hardcore")) {
                unsortedList.remove(baseLeague);
                sortedList.add(baseLeague);
            }
        }

        // Add Standard
        for (BaseLeague baseLeague : new ArrayList<>(unsortedList)) {
            if (baseLeague.getName().equals("Standard")) {
                unsortedList.remove(baseLeague);
                sortedList.add(baseLeague);
            }
        }

        // Add main hardcore leagues
        for (BaseLeague baseLeague : new ArrayList<>(unsortedList)) {
            if (baseLeague.isEvent()) {
                continue;
            }

            boolean match = false;
            for (Rule rule : baseLeague.getRules()) {
                if (rule.getName().equals("Hardcore")) {
                    match = true;
                    break;
                }
            }

            if (match) {
                unsortedList.remove(baseLeague);
                sortedList.add(baseLeague);
            }
        }

        // Add main softcore leagues
        for (BaseLeague baseLeague : new ArrayList<>(unsortedList)) {
            if (baseLeague.isEvent()) {
                continue;
            }

            boolean match = false;
            for (Rule rule : baseLeague.getRules()) {
                if (rule.getName().equals("Hardcore")) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                unsortedList.remove(baseLeague);
                sortedList.add(baseLeague);
            }
        }


        // Add main hardcore events
        for (BaseLeague baseLeague : new ArrayList<>(unsortedList)) {
            if (!baseLeague.isEvent()) {
                continue;
            }

            boolean match = false;
            for (Rule rule : baseLeague.getRules()) {
                if (rule.getName().equals("Hardcore")) {
                    match = true;
                    break;
                }
            }

            if (match) {
                unsortedList.remove(baseLeague);
                sortedList.add(baseLeague);
            }
        }

        // Add main softcore events
        for (BaseLeague baseLeague : new ArrayList<>(unsortedList)) {
            if (!baseLeague.isEvent()) {
                continue;
            }

            boolean match = false;
            for (Rule rule : baseLeague.getRules()) {
                if (rule.getName().equals("Hardcore")) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                unsortedList.remove(baseLeague);
                sortedList.add(baseLeague);
            }
        }

        // Add whatever is left
        for (BaseLeague baseLeague : unsortedList) {
            unsortedList.remove(baseLeague);
            sortedList.add(baseLeague);
        }

        // Convert BaseLeagues into LeagueEntries
        List<LeagueEntry> leagueEntries = new ArrayList<>();
        for (BaseLeague baseLeague : sortedList) {
            leagueEntries.add(new LeagueEntry(baseLeague));
        }

        return leagueEntries;
    }

    /**
     * Downloads a list of active leagues from the official league API
     *
     * @param baseLeagueEntryList Empty or filled List to be filled with valid LeagueEntry objects
     * @return True on success
     */
    private boolean downloadBaseLeagues(List<BaseLeague> baseLeagueEntryList) {
        InputStream stream = null;
        baseLeagueEntryList.clear();

        logger.info("Getting league list from official API");

        try {
            // Define the request
            URL request = new URL("http://api.pathofexile.com/leagues?type=main");
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            connection.setReadTimeout(config.getInt("league.readTimeout"));
            connection.setConnectTimeout(config.getInt("league.connectTimeout"));

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Define some elements
            StringBuilder stringBuilderBuffer = new StringBuilder();
            byte[] byteBuffer = new byte[config.getInt("league.bufferSize")];
            int byteCount;

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, config.getInt("league.bufferSize"))) != -1) {
                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != config.getInt("league.bufferSize")) {
                    byte[] trimmedByteBuffer = new byte[byteCount];
                    System.arraycopy(byteBuffer, 0, trimmedByteBuffer, 0, byteCount);

                    // Trim byteBuffer, convert it into string and add to string buffer
                    stringBuilderBuffer.append(new String(trimmedByteBuffer));
                } else {
                    stringBuilderBuffer.append(new String(byteBuffer));
                }
            }

            // Attempt to parse league list
            Type listType = new TypeToken<List<BaseLeague>>() {
            }.getType();
            baseLeagueEntryList.addAll(gson.fromJson(stringBuilderBuffer.toString(), listType));

            logger.info("Got league list");

            return true;
        } catch (SocketTimeoutException ex) {
            logger.error("Failed to download league list (timeout)");
            return false;
        } catch (Exception ex) {
            logger.error("Failed to download league list", ex);
            return false;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public List<LeagueEntry> getLeagues() {
        return leagues;
    }

    public Integer getLeagueId(String league) {
        return leagueIds.get(league);
    }
}
