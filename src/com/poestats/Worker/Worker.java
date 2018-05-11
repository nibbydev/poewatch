package com.poestats.Worker;

import com.poestats.Main;
import com.poestats.Mappers;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads and processes a batch of data downloaded from the PoE API. Runs in a separate thread.
 */
public class Worker extends Thread {
    private final Object monitor = new Object();
    private final Gson gson = Main.getGson();
    private volatile boolean flagLocalRun = true;
    private String job;
    private int index;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

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
                reply = gson.fromJson(replyJSONString, Mappers.APIReply.class);

                // Parse the deserialized JSON if deserialization was successful
                if (!reply.next_change_id.equals(""))
                    Main.ENTRY_CONTROLLER.parseItems(reply);
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

    //------------------------------------------------------------------------------------------------------------
    // Download/parse
    //------------------------------------------------------------------------------------------------------------

    /**
     * Downloads data from the API
     *
     * @return Whole stash batch as a string
     */
    private String downloadData() {
        StringBuilder stringBuilderBuffer = new StringBuilder();
        byte[] byteBuffer = new byte[Main.CONFIG.downloadChunkSize];
        boolean regexLock = true;
        InputStream stream = null;
        int byteCount;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - Main.ADMIN.latestPullTime < Main.CONFIG.downloadDelay) {
            sleepX((int) (Main.CONFIG.downloadDelay - System.currentTimeMillis() + Main.ADMIN.latestPullTime));
        }

        // Run statistics cycle
        Main.ADMIN.pullCountTotal++;
        Main.ADMIN.latestPullTime = System.currentTimeMillis();
        Main.ADMIN.workerCycle();

        try {
            // Define the request
            URL request = new URL("http://www.pathofexile.com/api/public-stash-tabs?id=" + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(Main.CONFIG.readTimeOut);
            connection.setConnectTimeout(Main.CONFIG.connectTimeOut);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, Main.CONFIG.downloadChunkSize)) != -1) {
                // Check if run flag is lowered
                if (!this.flagLocalRun)
                    return "";

                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != Main.CONFIG.downloadChunkSize) {
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
                        Main.WORKER_CONTROLLER.setNextChangeID(matcher.group());

                        // Add freshest changeID to statistics
                        Main.ADMIN.setChangeID(matcher.group());

                        // If new changeID is equal to the previous changeID, it has already been downloaded
                        if (matcher.group().equals(job)) {
                            return "";
                        }
                    }
                }
            }

        } catch (Exception ex) {
            Main.ADMIN.log_("Caught worker download error: " + ex.getMessage(), 3);

            // Add old changeID to the pool only if a new one hasn't been found
            if (regexLock) {
                sleepX(5000);
                Main.WORKER_CONTROLLER.setNextChangeID(job);
            }

            Main.ADMIN.pullCountError++;

            // Clear the buffer so that an empty string will be returned instead
            stringBuilderBuffer.setLength(0);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException ex) {
                Main.ADMIN._log(ex, 3);
            }
        }

        // Return the downloaded mess of a JSON string
        return stringBuilderBuffer.toString();
    }

    /**
     * Sleeps for designated amount of time
     *
     * @param timeMS Time in milliseconds to sleep
     */
    private void sleepX(int timeMS) {
        try {
            Thread.sleep(timeMS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility
    //------------------------------------------------------------------------------------------------------------

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

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

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
