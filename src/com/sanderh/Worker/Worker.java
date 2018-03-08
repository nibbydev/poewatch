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

/**
 * Downloads and processes a batch of data downloaded from the PoE API. Runs in a separate thread.
 */
public class Worker extends Thread {
    private final Object monitor = new Object();
    private volatile boolean flagLocalRun = true;
    private String job;
    private int index;

    /////////////////////////////
    // Actually useful methods //
    /////////////////////////////

    /**
     * Contains main loop. Checks for new jobs and processes them
     */
    public void run() {
        String replyJSONString;
        Mappers.APIReply reply;

        // Run while flag is true
        while (flagLocalRun) {
            // If there's no new job, sleep
            waitOnMonitor();

            // Check if Worker should close after being woken from sleep
            if(!flagLocalRun)
                break;

            // In case the notify came from some other source or there was a timeout, make sure the worker doesn't
            // continue with an empty job
            if (job == null)
                continue;

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
            job = null;
        }
    }

    /**
     * Stops current Worker's process
     */
    public void stopWorker() {
        this.flagLocalRun = false;
        wakeLocalMonitor();
    }

    ////////////////////////////////////
    // Primary download/parse methods //
    ////////////////////////////////////

    /**
     * Downloads data from the API
     * @return Whole stash batch as a string
     */
    private String downloadData() {
        StringBuilder stringBuilderBuffer = new StringBuilder();
        byte[] byteBuffer = new byte[CONFIG.downloadChunkSize];
        boolean regexLock = true;
        InputStream stream = null;
        int byteCount;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - STATISTICS.getLastPullTime() < CONFIG.downloadDelay) {
            justFuckingSleep((int) (CONFIG.downloadDelay - System.currentTimeMillis() + STATISTICS.getLastPullTime()));
        }

        // Run statistics cycle
        STATISTICS.cycle();

        try {
            // Define the request
            URL request = new URL(CONFIG.defaultAPIURL + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(CONFIG.readTimeOut);
            connection.setConnectTimeout(CONFIG.connectTimeOut);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, CONFIG.downloadChunkSize)) != -1) {
                // Check if run flag is lowered
                if (!this.flagLocalRun)
                    return "";

                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != CONFIG.downloadChunkSize) {
                    byte[] trimmedByteBuffer = new byte[byteCount];
                    System.arraycopy(byteBuffer, 0, trimmedByteBuffer, 0, byteCount);

                    // Trim byteBuffer, convert it into string and add to string buffer
                    stringBuilderBuffer.append(new String(trimmedByteBuffer));
                } else {
                    stringBuilderBuffer.append(new String(byteBuffer));
                }

                // Try to find new job number using regex
                if (regexLock) {
                    Pattern pattern = Pattern.compile("\\d*-\\d*-\\d*-\\d*-\\d*");
                    Matcher matcher = pattern.matcher(stringBuilderBuffer.toString());
                    if (matcher.find()) {
                        regexLock = false;

                        // Add new-found job to queue
                        WORKER_CONTROLLER.setNextChangeID(matcher.group());

                        // Add freshest changeID to statistics
                        STATISTICS.setLatestChangeID(matcher.group());

                        // If new changeID is equal to the previous changeID, it has already been downloaded
                        if (matcher.group().equals(job)) {
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
                WORKER_CONTROLLER.setNextChangeID(job);
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

    /**
     * Maps JSON string to an object
     * @param stringBuffer JSON string to be mapped
     * @return Mapped APIReply object
     */
    private Mappers.APIReply deSerializeJSONString(String stringBuffer) {
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

    /**
     * Sleeps for designated amount of time
     * @param timeMS Time in milliseconds to sleep
     */
    private void justFuckingSleep(int timeMS) {
        try {
            Thread.sleep(timeMS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /////////////////////////////
    //     Monitor methods     //
    /////////////////////////////

    /**
     * Sleeps until monitor object is notified
     */
    private void waitOnMonitor() {
        synchronized (monitor) {
            try {
                monitor.wait(500);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Notifies local monitor
     */
    private void wakeLocalMonitor() {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    /////////////////////////////
    //    Getters / Setters    //
    /////////////////////////////

    public void setJob(String job) {
        this.job = job;

        // Wake the worker so it can start working on the job
        wakeLocalMonitor();
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public boolean hasJob() {
        return job != null;
    }

    public String getJob() {
        return job;
    }
}
