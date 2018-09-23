package watch.poe.worker;

import watch.poe.Config;
import watch.poe.Main;
import watch.poe.item.Mappers;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.regex.Matcher;

/**
 * Downloads and processes a batch of data downloaded from the PoE API. Runs in a separate thread.
 */
public class Worker extends Thread {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final Object monitor = new Object();
    private final Gson gson = new Gson();
    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private String job;
    private int index;
    private static long lastPullTime;

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

            // Check if worker should close after being woken from sleep
            if(!flagLocalRun) break;

            // In case the notify came from some other source or there was a timeout, make sure the worker doesn't
            // continue with an empty job
            if (job == null) continue;

            // Download and parse data according to the changeID.
            replyJSONString = downloadData();

            // If download was unsuccessful, stop
            if (replyJSONString != null) {
                // Deserialize the JSON string
                reply = gson.fromJson(replyJSONString, Mappers.APIReply.class);

                // Parse the deserialized JSON if deserialization was successful
                if (reply != null && reply.next_change_id != null) {
                    Main.ENTRY_MANAGER.parseItems(reply);
                }
            }

            job = null;
        }

        readyToExit = true;
    }

    /**
     * Stops current worker's process
     */
    public void stopWorker() {
        flagLocalRun = false;
        wakeLocalMonitor();

        while (!readyToExit) try {
            Thread.sleep(50);
            wakeLocalMonitor();
        } catch (InterruptedException ex) { }
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
        byte[] byteBuffer = new byte[Config.worker_downloadBufferSize];
        boolean regexLock = true;
        InputStream stream = null;
        int byteCount;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - Worker.lastPullTime < Config.worker_downloadDelayMS) {
            sleepX((int) (Config.worker_downloadDelayMS - System.currentTimeMillis() + Worker.lastPullTime));
        }

        Worker.lastPullTime = System.currentTimeMillis();

        try {
            // Define the request
            URL request = new URL(Config.worker_APIBaseURL + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(Config.worker_readTimeoutMS);
            connection.setConnectTimeout(Config.worker_connectTimeoutMS);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, Config.worker_downloadBufferSize)) != -1) {
                // Check if run flag is lowered
                if (!flagLocalRun) return null;

                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != Config.worker_downloadBufferSize) {
                    byte[] trimmedByteBuffer = new byte[byteCount];
                    System.arraycopy(byteBuffer, 0, trimmedByteBuffer, 0, byteCount);

                    // Trim byteBuffer, convert it into string and add to string buffer
                    stringBuilderBuffer.append(new String(trimmedByteBuffer));
                } else {
                    stringBuilderBuffer.append(new String(byteBuffer));
                }

                // Try to find new job number using regex
                if (regexLock) {
                    Matcher matcher = Config.worker_changeIDRegexPattern.matcher(stringBuilderBuffer.toString());
                    if (matcher.find()) {
                        regexLock = false;

                        // Add new-found job to queue
                        Main.WORKER_MANAGER.setNextChangeID(matcher.group());

                        // Add freshest changeID to statistics
                        Main.ADMIN.setChangeID(matcher.group());

                        // If new changeID is equal to the previous changeID, it has already been downloaded
                        if (matcher.group().equals(job)) {
                            return null;
                        }
                    }
                }
            }

        } catch (Exception ex) {
            Main.ADMIN.log_("Caught worker download error: " + ex.getMessage(), 3);

            // Add old changeID to the pool only if a new one hasn't been found
            if (regexLock) {
                sleepX(Config.worker_lockTimeoutMS);
                Main.WORKER_MANAGER.setNextChangeID(job);
            }

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
                monitor.wait(Config.monitorTimeoutMS);
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
