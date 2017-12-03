package MainPack;

import MainPack.MapperClasses.APIReply;
import MainPack.MapperClasses.Item;
import MainPack.MapperClasses.Stash;
import MainPack.PricerClasses.PricerController;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker extends Thread {
    //  Name: Worker
    //  Date created: 21.11.2017
    //  Last modified: 02.12.2017
    //  Description: Contains a worker used to download and parse a batch from the PoE API. Runs in a separate loop.

    private boolean flagLocalRun = true;
    private int index;
    private String nextChangeID = "";
    private String job = "";
    private PricerController pricerController;

    /////////////////////////////
    // Actually useful methods //
    /////////////////////////////

    public void run() {
        //  Name: run()
        //  Date created: 21.11.2017
        //  Last modified: 02.12.2017
        //  Description: Contains the main loop of the worker
        //  Child methods:
        //      downloadData()
        //      deSerializeJSONString()
        //      parseItems()
        //      sleep()

        String replyJSONString;
        APIReply reply;

        // Run while flag is true
        while (flagLocalRun) {
            // Check for new jobs
            if (!job.equals("")) {
                // Download and parse data according to the changeID.
                replyJSONString = downloadData();

                // If download was unsuccessful, stop
                if (replyJSONString.equals(""))
                    continue;

                // Seems good, deserialize the JSON string
                reply = deSerializeJSONString(replyJSONString);

                // Check if object has info in it
                if (reply.getNext_change_id().equals(""))
                    continue;

                // Parse the deserialized JSON
                parseItems(reply);

                // Clear the job
                setJob("");
            }
            sleep(100);
        }
    }

    private String downloadData() {
        //  Name: downloadData()
        //  Date created: 21.11.2017
        //  Last modified: 02.12.2017
        //  Description: Method that downloads data from the API
        //  Parent methods:
        //      run()
        //  Child methods:
        //      trimPartialByteBuffer()
        //      sleep()

        StringBuilder stringBuilderBuffer = new StringBuilder();
        int chunkSize = 128;
        byte[] byteBuffer = new byte[chunkSize];
        boolean regexLock = true;
        int byteCount;

        // Sleep for 500ms
        sleep(500);

        try {
            // Define the request
            URL request = new URL("http://www.pathofexile.com/api/public-stash-tabs?id=" + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();
            InputStream stream = connection.getInputStream();

            // Handle a bad response
            if (connection.getResponseCode() != 200) {
                if (connection.getResponseCode() == 429) {
                    System.out.println("[ERROR] Client was timed out");
                    sleep(5000);
                }

                // Set uncompleted job as new job, allowing another worker to finish it
                this.nextChangeID = this.job;
                // Return empty value
                return "";
            }

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, chunkSize)) != -1) {
                // Check if run flag is lowered
                if (!this.flagLocalRun)
                    return "";

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

        } catch (Exception ex) {
            System.out.println("[ERROR] Caught worker download error:");
            ex.printStackTrace();
            this.nextChangeID = this.job;
            return "";
        }

        // Return the downloaded mess of a JSON string
        // If the script reached this point, something probably went wrong
        return stringBuilderBuffer.toString();
    }

    private String trimPartialByteBuffer(byte[] buffer, int length) {
        //  Name: trimPartialByteBuffer()
        //  Date created: 22.11.2017
        //  Last modified: 29.11.2017
        //  Description: Copies over contents of fixed-size byte array and adds contents to ArrayList, which converts it
        //      into string. Reason: first iteration of get reply returns 26 bytes instead of 128
        //  Parent methods:
        //      downloadData()

        byte[] bufferBuffer = new byte[length];

        // TODO: improve this
        for (int i = 0; i < length; i++) {
            bufferBuffer[i] = buffer[i];
        }

        return new String(bufferBuffer);
    }

    private APIReply deSerializeJSONString(String stringBuffer) {
        //  Name: deSerializeJSONString()
        //  Date created: 22.11.2017
        //  Last modified: 29.11.2017
        //  Description: Maps a JSON string to an object
        //  Parent methods:
        //      run()

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
        //  Name: parseItems()
        //  Date created: 23.11.2017
        //  Last modified: 29.11.2017
        //  Description: Checks every item the script has found against preset filters
        //  Parent methods:
        //      run()

        // Loop through every single item, checking every single one of them
        for (Stash stash : reply.getStashes()) {
            for (Item item : stash.getItems()) {
                // Run through pricing service
                pricerController.checkItem(item);
            }
        }
    }

    private void sleep(int timeMS) {
        //  Name: sleep()
        //  Date created: 02.12.2017
        //  Last modified: 02.12.2017
        //  Description: Sleeps for <timeMS> ms
        //  Parent methods:
        //      run()
        //      downloadData()

        try {
            Thread.sleep(timeMS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void setNextChangeID(String nextChangeID) {
        this.nextChangeID = nextChangeID;
    }

    public void setJob(String job) {
        this.job = job;
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

    public void setPricerController(PricerController pricerController) {
        this.pricerController = pricerController;
    }

}
