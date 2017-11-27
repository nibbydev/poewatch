package MainPack;

import MainPack.MapperClasses.APIReply;
import MainPack.MapperClasses.Item;
import MainPack.MapperClasses.Stash;
import MainPack.StatClasses.League;
import MainPack.StatClasses.StatController;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker extends Thread {
    /*   Name: Worker
     *   Date created: 21.11.2017
     *   Last modified: 27.11.2017
     *   Description: Contains a worker used to download and parse a batch from the PoE API. Runs in a separate loop.
     */

    private boolean flagLocalRun = true;
    private int index;
    private ArrayList<String> searchParameters;
    private String nextChangeID = "";
    private String job = "";
    private StatController statController;

    /*
     * Methods that get/set values from outside the class
     */

    public void setNextChangeID(String nextChangeID) {
        this.nextChangeID = nextChangeID;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public void setSearchParameters(ArrayList<String> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setFlagLocalRun(boolean flagLocalRun) {
        this.flagLocalRun = flagLocalRun;
    }

    public int getIndex() {
        return index;
    }

    public String getNextChangeID() {
        return nextChangeID;
    }

    public String getJob() {
        return job;
    }

    public void setStatController(StatController statController) {
        this.statController = statController;
    }

    /*
     * Methods that actually do something
     */

    public void run() {
        /*  Name: run()
        *   Date created: 21.11.2017
        *   Last modified: 23.11.2017
        *   Description: Contains the main loop of the worker
        *   Child methods:
        *       downloadData()
        *       deSerializeJSONString()
        *       parseItems()
        */

        String replyJSONString;
        APIReply reply;

        // Run while flag is true
        while (this.flagLocalRun) {
            // Check for new jobs
            if (!this.job.equals("")) {
                // Download and parse data according to the changeID.
                replyJSONString = this.downloadData();

                // If download was unsuccessful, stop
                if (replyJSONString.equals(""))
                    return;

                // Seems good, deserialize the JSON string
                reply = this.deSerializeJSONString(replyJSONString);

                // Check if object has info in it
                if (reply.getNext_change_id().equals(""))
                    return;

                // Parse the deserialized JSON
                this.parseItems(reply);

                // Clear the job
                this.job = "";
            }

            // Sleep for 100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String downloadData() {
        /*  Name: downloadData()
        *   Date created: 21.11.2017
        *   Last modified: 27.11.2017
        *   Description: Contains the method that downloads data from the API
        *   Parent methods:
        *       run()
        *   Child methods:
        *       trimPartialByteBuffer()
        */

        StringBuilder stringBuilderBuffer = new StringBuilder();
        int chunkSize = 128;
        byte[] byteBuffer = new byte[chunkSize];
        boolean regexLock = true;
        int byteCount;

        // Sleep for 500ms
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        // Add to statistics
        statController.incTotalPullCount();

        try {
            // Define the request
            URL request = new URL("http://www.pathofexile.com/api/public-stash-tabs?id=" + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();
            InputStream stream = connection.getInputStream();

            // Handle a bad response
            if (connection.getResponseCode() != 200) {
                if (connection.getResponseCode() == 429) {
                    System.out.println("[ERROR] Client was timed out");
                    // Sleep for 5000ms
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Set uncompleted job as new job, allowing another worker to finish it
                this.nextChangeID = this.job;
                // Return empty value
                return "";
            }

            // The loop that downloads data in chunks
            while (true) {
                // Stream data and count bytes
                byteCount = stream.read(byteBuffer, 0, chunkSize);
                // Check if run flag is lowered
                if (!this.flagLocalRun)
                    return "";
                // Transmission has finished
                else if (byteCount == -1)
                    break;

                // Add to statistics
                statController.addBytesDownloaded(byteCount);

                // Trim the byteBuffer, convert it into string and add to string buffer
                stringBuilderBuffer.append(trimPartialByteBuffer(byteBuffer, byteCount));

                // Try to find new job number using regex
                if (regexLock) {
                    Pattern pattern = Pattern.compile("\\d*-\\d*-\\d*-\\d*-\\d*");
                    Matcher matcher = pattern.matcher(stringBuilderBuffer.toString());
                    if (matcher.find()) {
                        this.nextChangeID = matcher.group();
                        regexLock = false;
                    }
                }
            }

            // Add to statistics
            statController.incSuccessfulPullCount();

            // Return the downloaded mess of a JSON string
            return stringBuilderBuffer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // If the script reached this point, something probably went wrong
        return "";
    }

    private String trimPartialByteBuffer(byte[] buffer, int length) {
        /*  Name: trimPartialByteBuffer()
        *   Date created: 22.11.2017
        *   Last modified: 27.11.2017
        *   Description: Copies over contents of fixed-size byte array and adds contents to ArrayList, which converts it
        *       into string. Reason: first iteration of get reply returns 26 bytes instead of 128
        *   Parent methods:
        *       downloadData()
        */

        byte[] bufferBuffer = new byte[length];

        // TODO: improve this
        for (int i = 0; i < length; i++) {
            bufferBuffer[i] = buffer[i];
        }

        return new String(bufferBuffer);
    }

    private APIReply deSerializeJSONString(String stringBuffer) {
        /*  Name: deSerializeJSONString()
        *   Date created: 22.11.2017
        *   Last modified: 23.11.2017
        *   Description: Map a JSON string to an object
        *   Parent methods:
        *       run()
        */

        APIReply reply;

        try {
            // Define the mapper
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

            // Map the JSON string to an object
            reply = mapper.readValue(stringBuffer, APIReply.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new APIReply();
        }

        return reply;

    }

    private void parseItems(APIReply reply) {
        /*  Name: parseItems()
        *   Date created: 23.11.2017
        *   Last modified: 26.11.2017
        *   Description: Checks every item the script has found against preset filters
        *   Parent methods:
        *       run()
        */

        String parameterConstructor;
        League leagueStats;

        // Loop through every single item, checking every single one of them
        for (Stash stash : reply.getStashes()) {
            for (Item item : stash.getItems()) {
                // Check search parameters
                parameterConstructor = item.getLeague() + "|" + item.getFrameType() + "|" + item.getName();
                for (String parameter : searchParameters) {
                    if (parameter.equalsIgnoreCase(parameterConstructor)) {
                        System.out.println("(" + stash.getAccountName() + ") - " + parameterConstructor);
                    }
                }

                // Get the league-based statistics object
                leagueStats = statController.getLeague(item.getLeague());

                // Increment item frame type stats
                switch (item.getFrameType()) {
                    case 0:
                        leagueStats.incNormalCount();
                        break;
                    case 1:
                        leagueStats.incMagicCount();
                        break;
                    case 2:
                        leagueStats.incRareCount();
                        break;
                    case 3:
                        leagueStats.incUniqueCount();
                        break;
                    case 4:
                        leagueStats.incGemCount();
                        break;
                    case 5:
                        leagueStats.incCurrencyCount();
                        break;
                    default:
                        leagueStats.incOtherCount();
                }

                // Check item properties
                if(item.isCorrupted())
                    leagueStats.incCorruptedCount();
                if(!item.isIdentified())
                    leagueStats.incUnidentifiedCount();

            }
        }
    }
}
