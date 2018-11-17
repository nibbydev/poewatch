package poe.manager.worker;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.entry.EntryManager;
import poe.manager.entry.item.Mappers;

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
    private static Logger logger = LoggerFactory.getLogger(Worker.class);
    private Config config;

    private static long lastPullTime;
    private final Object monitor = new Object();
    private final Gson gson = new Gson();
    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private String job;
    private int index;
    private EntryManager entryManager;
    private WorkerManager workerManager;
    private Database database;

    private static Pattern pattern = Pattern.compile("\\d*-\\d*-\\d*-\\d*-\\d*");

    public Worker(EntryManager entryManager, WorkerManager workerManager, Database database, Config config) {
        this.entryManager = entryManager;
        this.workerManager = workerManager;
        this.database = database;
        this.config = config;
    }

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
            if (!flagLocalRun) break;

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
                    entryManager.parseItems(reply);
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
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Downloads data from the API
     *
     * @return Whole stash batch as a string
     */
    private String downloadData() {
        StringBuilder stringBuilderBuffer = new StringBuilder();
        byte[] byteBuffer = new byte[config.getInt("worker.bufferSize")];
        boolean regexLock = true;
        InputStream stream = null;
        int byteCount;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - Worker.lastPullTime < config.getInt("worker.downloadDelay")) {
            sleepX((int) (config.getInt("worker.downloadDelay") - System.currentTimeMillis() + Worker.lastPullTime));
        }

        Worker.lastPullTime = System.currentTimeMillis();

        try {
            // Define the request
            URL request = new URL("http://www.pathofexile.com/api/public-stash-tabs?id=" + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(config.getInt("worker.readTimeout"));
            connection.setConnectTimeout(config.getInt("worker.connectTimeout"));

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, config.getInt("worker.bufferSize"))) != -1) {
                // Check if run flag is lowered
                if (!flagLocalRun) return null;

                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != config.getInt("worker.bufferSize")) {
                    byte[] trimmedByteBuffer = new byte[byteCount];
                    System.arraycopy(byteBuffer, 0, trimmedByteBuffer, 0, byteCount);

                    // Trim byteBuffer, convert it into string and add to string buffer
                    stringBuilderBuffer.append(new String(trimmedByteBuffer));
                } else {
                    stringBuilderBuffer.append(new String(byteBuffer));
                }

                // Try to find new job number using regex
                if (regexLock) {
                    Matcher matcher = pattern.matcher(stringBuilderBuffer.toString());

                    if (matcher.find()) {
                        regexLock = false;

                        // Add new-found job to queue
                        workerManager.setNextChangeID(matcher.group());

                        // Update db change id entry
                        database.upload.updateChangeID(matcher.group());

                        // If new changeID is equal to the previous changeID, it has already been downloaded
                        if (matcher.group().equals(job)) {
                            return null;
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Caught worker download error: " + ex.getMessage());

            // Add old changeID to the pool only if a new one hasn't been found
            if (regexLock) {
                sleepX(config.getInt("worker.lockTimeout"));
                workerManager.setNextChangeID(job);
            }

            // Clear the buffer so that an empty string will be returned instead
            stringBuilderBuffer.setLength(0);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
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

    /**
     * Sleeps until monitor object is notified
     */
    private void waitOnMonitor() {
        synchronized (monitor) {
            try {
                monitor.wait(config.getInt("worker.monitorTimeout"));
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean hasJob() {
        return job != null;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;

        // Wake the worker so it can start working on the job
        wakeLocalMonitor();
    }
}
