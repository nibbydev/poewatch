package poe.Managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.League.BaseLeague;
import poe.Managers.League.League;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;

public class LeagueManager {
    private static final Logger logger = LoggerFactory.getLogger(LeagueManager.class);
    private static final Gson gson = new Gson();
    private List<League> leagues;
    private final Database database;
    private final Config config;

    public LeagueManager(Database database,  Config config) {
        this.database = database;
        this.config = config;
    }

    public boolean cycle() {
        List<BaseLeague> baseLeagues = new ArrayList<>();
        List<League> leagues = new ArrayList<>();
        boolean success;

        logger.info("Starting league cycle");

        // Get list of leagues from official API
        success = downloadBaseLeagues(baseLeagues);
        if (success) {
            // Remove any SSF leagues from the list
            baseLeagues.removeIf(i -> Arrays.stream(i.getRules()).anyMatch(j -> j.getName().equals("Solo")));
            database.upload.updateLeagues(baseLeagues);
        }

        // Get active league entries from database
        success = database.init.getLeagues(leagues);
        if (!success || leagues.isEmpty()) {
            // Download failed AND database doesn't have league data. Shut down the program.
            logger.error("Failed to query leagues from database");
            return false;
        }

        this.leagues = leagues;
        database.upload.updateLeagueStates();

        logger.info("League cycle finished successfully");
        return true;
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

    public Integer getLeagueId(String name) {
        League league = leagues.stream()
                .filter(i -> i.getName().equals(name))
                .findFirst()
                .orElse(null);

        if (league == null) {
            return null;
        }

        return league.getId();
    }
}
