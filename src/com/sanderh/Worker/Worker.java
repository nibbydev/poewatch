package com.sanderh.Worker;

import com.sanderh.Mappers;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sanderh.Main.*;

public class Worker extends Thread {
    //  Name: Worker
    //  Date created: 21.11.2017
    //  Last modified: 18.12.2017
    //  Description: Contains a worker used to download and parse a batch from the PoE API. Runs in a separate loop.

    private static final int DOWNLOAD_DELAY = Integer.parseInt(PROPERTIES.getProperty("downloadDelay"));
    private static final int CHUNK_SIZE = Integer.parseInt(PROPERTIES.getProperty("downloadChunkSize"));
    private static final int READ_TIMEOUT = Integer.parseInt(PROPERTIES.getProperty("readTimeOut"));
    private static final int CONNECT_TIMEOUT = Integer.parseInt(PROPERTIES.getProperty("connectTimeOut"));
    private final Object monitor = new Object();
    private boolean flagLocalRun = true;
    private String job = "";
    private int index;

    /////////////////////////////
    // Actually useful methods //
    /////////////////////////////

    public void run() {
        //  Name: run()
        //  Date created: 21.11.2017
        //  Last modified: 17.12.2017
        //  Description: Contains the main loop of the worker

        String replyJSONString;
        Mappers.APIReply reply;

        // Run while flag is true
        while (flagLocalRun) {
            // Snooze
            checkMonitor();

            // Check if new job has been added after interrupt
            if (!job.equals("") && flagLocalRun) {
                // Download and parse data according to the changeID.
                replyJSONString = downloadData();

                // If download was unsuccessful, stop
                if (!replyJSONString.equals("")) {
                    // Deserialize the JSON string
                    reply = deSerializeJSONString(replyJSONString);

                    // Parse the deserialized JSON if deserialization was successful
                    if (!reply.getNext_change_id().equals(""))
                        PRICER_CONTROLLER.parseItems(reply);
                }

                // Clear the job
                setJob("");
            }
        }
    }

    private void checkMonitor() {
        //  Name: checkMonitor()
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Sleeps until monitor object is notified?

        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private String downloadData() {
        //  Name: downloadData()
        //  Date created: 21.11.2017
        //  Last modified: 18.12.2017
        //  Description: Method that downloads data from the API

        StringBuilder stringBuilderBuffer = new StringBuilder();
        byte[] byteBuffer = new byte[CHUNK_SIZE];
        boolean regexLock = true;
        InputStream stream = null;
        int byteCount;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - STATISTICS.getLastPullTime() < DOWNLOAD_DELAY) {
            justFuckingSleep((int) (DOWNLOAD_DELAY - System.currentTimeMillis() + STATISTICS.getLastPullTime()));
        }

        // Run statistics cycle
        STATISTICS.cycle();

        try {
            // Define the request
            URL request = new URL(PROPERTIES.getProperty("defaultAPIURL") + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECT_TIMEOUT);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, CHUNK_SIZE)) != -1) {
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
                        regexLock = false;

                        // Add new-found job to queue
                        WorkerController.setNextChangeID(matcher.group());
                        wakeWorkerControllerMonitor();

                        // Add freshest changeID to statistics
                        STATISTICS.setLatestChangeID(matcher.group());

                        // If new changeID is equal to the previous changeID, it has already been downloaded
                        if (matcher.group().equals(this.job)) {
                            STATISTICS.incPullCountDuplicate();
                            return "";
                        }
                    }
                }
            }

        } catch (Exception ex) {
            System.out.println(timeStamp() + " Caught worker download error: " + ex.getMessage());

            // Add old changeID to the pool only if a new one hasn't been found
            if (regexLock) {
                justFuckingSleep(5000);
                WorkerController.setNextChangeID(this.job);
                wakeWorkerControllerMonitor();
            }

            STATISTICS.incPullCountFailed();

            // Clear the buffer so that an empty string will be returned instead
            stringBuilderBuffer.setLength(0);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // Return the downloaded mess of a JSON string
        return stringBuilderBuffer.toString();
    }

    private void wakeWorkerControllerMonitor() {
        //  Name: wakeWorkerControllerMonitor()
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Wakes WorkerController

        synchronized (WorkerController.getMonitor()) {
            WorkerController.getMonitor().notifyAll();
        }
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

    private Mappers.APIReply deSerializeJSONString(String stringBuffer) {
        //  Name: deSerializeJSONString()
        //  Date created: 22.11.2017
        //  Last modified: 17.12.2017
        //  Description: Maps a JSON string to an object

        Mappers.APIReply reply;

        try {
            // Define the mapper
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

            // Map the JSON string to an object
            reply = mapper.readValue(stringBuffer, Mappers.APIReply.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Mappers.APIReply();
        }

        return reply;
    }

    private void justFuckingSleep(int timeMS) {
        //  Name: justFuckingSleep()
        //  Date created: 02.12.2017
        //  Last modified: 17.12.2017
        //  Description: Sleeps for <timeMS> ms

        try {
            Thread.sleep(timeMS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void setJob(String job) {
        this.job = job;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public boolean hasJob() {
        return !job.equals("");
    }

    public String getJob() {
        return job;
    }

    public Object getMonitor() {
        return monitor;
    }

    public void stopWorker() {
        this.flagLocalRun = false;
    }
}
