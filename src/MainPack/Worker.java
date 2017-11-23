package MainPack;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker extends Thread {
    /*  Name: Worker
    *   Date created: 21.11.2017
    *   Last modified: 23.11.2017
    *   Description: Contains a worker used to download and parse a batch from the PoE API. Runs in a separate loop.
    *   Example usage: *to be added*
    */

    private boolean flagLocalRun;
    private boolean flagLocalStop;
    private String baseAPIURL;
    private int chunkSize;
    private long pullDelay;
    private ArrayList<String> searchParameters;

    public int workerIndex;
    public String nextChangeID;
    public String job;

    public Worker(int workerIndex, ArrayList<String> searchParameters){
        this.flagLocalRun = true;
        this.flagLocalStop = false;
        this.pullDelay = 500;
        this.workerIndex = workerIndex;
        this.chunkSize = 128;
        this.baseAPIURL = "http://www.pathofexile.com/api/public-stash-tabs?id=";
        this.nextChangeID = "";
        this.job = "";
        this.searchParameters = searchParameters;
    }

    public void stopWorker(){
        /*  Name: stopWorker()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Method used to stop the worker safely
        */

        this.flagLocalRun = false;

        // Wait until process finishes safely
        while(!this.flagLocalStop)
            try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();}

    }

    public void addJob(String job){
        /*  Name: addJob()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Method used to add a job to the worker
        */

        // I could just do worker.job = "....", but I might need to implement some extra checks later
        this.job = job;
    }

    public void run() {
        /*  Name: run()
        *   Date created: 21.11.2017
        *   Last modified: 23.11.2017
        *   Description: Contains the main loop of the thread.
        *   Child methods:
        *       manageTheDownload()
        */

        // Run until stop flag is raised
        while(this.flagLocalRun){
            // Check for new jobs
            if(!this.job.equals("")) {
                this.manageTheDownload();

                // Empty the job string, indicating this worker is ready for another job
                this.job = "";
            }
            // Somehow sleep for 0.1 seconds
            try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
        }
        this.flagLocalStop = true;
    }

    private void manageTheDownload(){
        /*  Name: manageTheDownload()
        *   Date created: 23.11.2017
        *   Last modified: 23.11.2017
        *   Description: Contains functions that control the flow of downloaded data
        *   Parent methods:
        *       run()
        *   Child methods:
        *       downloadData()
        *       deSerializeDownloadedJSON()
        *       parseItems()
        */

        String stringBuffer;
        APIReply reply;

        // Download and parse data according to the changeID.
        stringBuffer = downloadData(this.job);

        // If download was unsuccessful, stop
        if(stringBuffer.equals(""))
            return;

        // Once everything has downloaded, turn that into a java object
        reply = this.deSerializeDownloadedJSON(stringBuffer);

        // Check if object has info in it
        if(reply.getNext_change_id().equals(""))
            return;

        parseItems(reply);
    }

    private String downloadData(String lastJob){ //TODO: get rid of parameter lastJob
        /*  Name: downloadData()
        *   Date created: 21.11.2017
        *   Last modified: 23.11.2017
        *   Description: Contains the method that downloads data from the API and then parses it
        *   Parent methods:
        *       manageTheDownload()
        *   Child methods:
        *       trimPartialByteBuffer()
        */

        StringBuilder stringBuilderBuffer = new StringBuilder();
        byte[] byteBuffer = new byte[this.chunkSize];
        int byteCount;
        String partialNextChangeID = "";
        Pattern pattern = Pattern.compile("\\d*-\\d*-\\d*-\\d*-\\d*");

        // Sleep for 0.5 seconds. Any less and we'll get timed out by the API
        try{Thread.sleep(this.pullDelay);}catch(InterruptedException ex){Thread.currentThread().interrupt();}

        try {
            // Define the request
            URL request = new URL(this.baseAPIURL + lastJob);
            HttpURLConnection connection = (HttpURLConnection)request.openConnection();

            if (connection.getResponseCode() == 429) {
                // We got timed out
                System.out.println("[ERROR] Client was timed out");
                try{Thread.sleep(5000);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
                this.nextChangeID = lastJob;
                return "";
            } else if (connection.getResponseCode() != 200) {
                // When request was bad, put job back into the job pool
                this.nextChangeID = lastJob;
                return "";
            }

            // Define the stream
            InputStream stream = connection.getInputStream();

            while(true){
                // Check if need to quit
                if(!this.flagLocalRun)
                    return "";

                // Stream data, count bytes
                byteCount = stream.read(byteBuffer, 0, this.chunkSize);

                // Transmission has finished
                if (byteCount == -1) break;

                // Run until we get the first 128** bytes in string format
                if(this.nextChangeID.equals("") && partialNextChangeID.length() < this.chunkSize) {
                    partialNextChangeID += new String(this.trimPartialByteBuffer(byteBuffer, byteCount));

                    // Seriously this was a headache
                    Matcher matcher = pattern.matcher(partialNextChangeID);
                    if (matcher.find())
                        this.nextChangeID = matcher.group();
                }

                // Trim the byteBuffer, turn it into a string, add string to buffer
                stringBuilderBuffer.append(new String(trimPartialByteBuffer(byteBuffer, byteCount)));
            }

            // Return the downloaded mess of a JSON string
            return stringBuilderBuffer.toString();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "";
    }

    private byte[] trimPartialByteBuffer(byte[] buffer, int length) {
        /*  Name: trimPartialByteBuffer()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Copies over contents of fixed-size byte array and adds contents to ArrayList, which converts it
        *       into string. Reason: first iteration of get reply returns 26 bytes instead of 128
        *   Parent methods:
        *       downloadData()
        */

        byte[] bufferBuffer = new byte[length];

        for (int i = 0; i < length; i++) {
            bufferBuffer[i] = buffer[i];
        }

        return bufferBuffer;
    }

    private APIReply deSerializeDownloadedJSON(String stringBuffer) {
        /*  Name: deSerializeDownloadedJSON()
        *   Date created: 22.11.2017
        *   Last modified: 23.11.2017
        *   Description: Turns JSON string into java object
        *   Parent methods:
        *       manageTheDownload()
        */

        APIReply reply;

        try {
            ObjectMapper mapper = new ObjectMapper();
            // Since we have no control over the generation of the JSON, this must be allowed
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

            // Turn the string into an object and return it
            reply = mapper.readValue(stringBuffer, APIReply.class);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("exception");
            reply =  new APIReply();
        }

        return reply;

    }

    private void parseItems(APIReply reply){
        /*  Name: parseItems()
        *   Date created: 23.11.2017
        *   Last modified: 23.11.2017
        *   Description: Checks ever item the script has found against preset filters
        *   Parent methods:
        *       run()
        *   Child methods:
        *       downloadData()
        *       deSerializeDownloadedJSON()
        */

        String parameterConstructor;

        // Loop through every single item, checking every single one of them
        for (Stash stash: reply.getStashes()) {
            for (Item item: stash.getItems()) {
                parameterConstructor = item.getLeague() + "|" +  item.getFrameType() + "|" + item.getName();
                for (String parameter: searchParameters) {
                    if (parameter.equalsIgnoreCase(parameterConstructor)){
                        System.out.println("(" + stash.getAccountName() + ") - " + parameterConstructor);
                    }
                }
            }
        }
    }
}
