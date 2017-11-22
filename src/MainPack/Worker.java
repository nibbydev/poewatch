package MainPack;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker extends Thread {
    /*  Name: Worker()
    *   Date created: 21.11.2017
    *   Last modified: 22.11.2017
    *   Description: Contains a worker used to download and parse a batch from the PoE API. Runs in a separate loop.
    *   Example usage: *to be added*
    */

    private boolean flagLocalRun;
    private boolean flagLocalStop;
    private String baseAPIURL;
    private int chunkSize;

    public String nextChangeID;
    public long pullDelay;
    public int workerIndex;
    public String job;

    public void inizilize(int workerIndex){
        /*  Name: inizilize()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Method used to pass arguments to the thread before execution
        */

        this.flagLocalRun = true;
        this.flagLocalStop = false;
        this.pullDelay = 1000;
        this.workerIndex = workerIndex;
        this.chunkSize = 128;
        this.baseAPIURL = "http://www.pathofexile.com/api/public-stash-tabs?id=";
        this.nextChangeID = "";
        this.job = "";
    }

    public void run() {
        /*  Name: run()
        *   Date created: 21.11.2017
        *   Last modified: 21.11.2017
        *   Description: Contains the main loop of the thread.
        *   Child methods:
        *       downloadData()
        */

        // Run until stop flag is raised
        while(this.flagLocalRun){
            // Check for new jobs
            if(!this.job.equals("")) {
                // Download and parse data according to the changeID.
                downloadData(this.job);

                // Empty the job string, indicating this worker is ready for another job
                this.job = "";
            }
            // Somehow sleep for 0.1 seconds
            try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
        }
        this.flagLocalStop = true;
    }

    private void downloadData(String lastJob){
        /*  Name: downloadData()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Contains the method that downloads data from the API and then parses it
        *   Parent methods:
        *       run()
        */

        byte[] buffer = new byte[this.chunkSize];
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
                return;
            } else if (connection.getResponseCode() != 200) {
                // When request was bad, put job back into the job pool
                this.nextChangeID = lastJob;
                return;
            }

            // Define the stream
            InputStream stream = connection.getInputStream();

            while(true){
                // Stream data, count bytes
                byteCount = stream.read(buffer, 0, this.chunkSize);

                // Transmission has finished
                if (byteCount == -1) break;

                //Run until we get the first 128** bytes in string format
                if(this.nextChangeID.equals("") && partialNextChangeID.length() < this.chunkSize) {
                    partialNextChangeID += this.convertPartialBufferToString(buffer, byteCount);

                    // Seriously this was a headache
                    Matcher matcher = pattern.matcher(partialNextChangeID);
                    if (matcher.find())
                        this.nextChangeID = matcher.group();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    private String convertPartialBufferToString(byte[] buffer, int length) {
        /*  Name: convertPartialBufferToString()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Copies over contents of fixed-size byte array and adds contents to ArrayList, which converts it
        *   into string. Reason: first iteration of get reply returns 26 bytes instead of 128
        */

        byte[] bufferBuffer = new byte[length];

        for (int i = 0; i < length; i++) {
            bufferBuffer[i] = buffer[i];
        }

        return new String(bufferBuffer);
    }
}
