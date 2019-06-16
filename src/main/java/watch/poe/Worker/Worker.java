package poe.Worker;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Deserializers.Reply;
import poe.Item.Parser.ItemParser;
import poe.Managers.Stat.StatType;
import poe.Managers.StatisticsManager;

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
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    private static final Pattern changeIdPattern = Pattern.compile("\\d+(-\\d+){4}");
    private static final Pattern exceptionPattern5xx = Pattern.compile("^.+ 5\\d\\d .+$");
    private static final Pattern exceptionPattern4xx = Pattern.compile("^.+ 4\\d\\d .+$");

    private final StatisticsManager sm;
    private final WorkerManager wm;
    private final ItemParser ip;
    private final Database db;
    private final Config cf;

    private final Object pauseMonitor = new Object();
    private final Object jobMonitor = new Object();
    private final Gson gson = new Gson();

    private String job;
    private int currentJobNr;
    private int workerId;

    // should the worker be running
    private boolean run = true;
    // is the worker currently running
    private boolean isRunning = true;
    // should the worker be paused
    private boolean pause = false;
    // is the worker currently paused
    private boolean isPaused = false;

    /**
     * Default constructor
     *
     * @param id The worker's id
     * @param wm
     * @param sm
     * @param ip
     * @param db
     * @param cf
     */
    public Worker(int id, WorkerManager wm, StatisticsManager sm, ItemParser ip, Database db, Config cf) {
        this.workerId = id;
        this.wm = wm;
        this.sm = sm;
        this.ip = ip;
        this.db = db;
        this.cf = cf;
    }

    /**
     * Main loop of the worker.
     * Checks for new jobs and processes them.
     */
    public void run() {
        while (run) {
            // Wait on monitor until notified and a new job is given
            waitForJob();

            logger.debug("Worker {} starting job {} ({})", workerId, currentJobNr, job);

            // Increment api call counter
            sm.addValue(StatType.COUNT_API_CALLS, null);

            // Download JSON reply from stash API
            Reply reply = download();

            if (reply != null) {
                // Start a timer for total process time
                sm.startTimer(StatType.TIME_PARSE_REPLY);

                // Hand it over to item parser to deal with
                ip.process(reply);

                // End the timer
                sm.clkTimer(StatType.TIME_PARSE_REPLY);
            }

            // If worker should be paused
            if (pause) waitOnPause();

            // Clear current job
            logger.debug("Worker {} finished job {}", workerId, currentJobNr);
            job = null;
        }

        logger.info("Worker {} stopped", workerId);
        isRunning = false;
    }

    /**
     * Requests the worker to stop
     */
    public void requestStop() {
        run = false;

        // Notify job monitor
        synchronized (jobMonitor) {
            jobMonitor.notify();
        }
    }

    /**
     * Beefy method for downloading data from the stash API.
     *
     * @return Valid stash api reply or null
     */
    private Reply download() {
        StringBuilder jsonBuffer = new StringBuilder();
        byte[] byteBuffer = new byte[cf.getInt("worker.bufferSize")];
        boolean regexLock = true;
        boolean gotFirstByte = false;
        InputStream stream = null;
        int byteCount, totalByteCount = 0;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - wm.getLastPullTime() < cf.getInt("worker.downloadDelay")) {
            sleepFor((int) (cf.getInt("worker.downloadDelay") - System.currentTimeMillis() + wm.getLastPullTime()));
        }

        wm.setLastPullTime();

        try {
            sm.startTimer(StatType.TIME_API_REPLY_DOWNLOAD);

            // Define the request
            URL request = new URL("http://www.pathofexile.com/api/public-stash-tabs?id=" + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(cf.getInt("worker.readTimeout"));
            connection.setConnectTimeout(cf.getInt("worker.connectTimeout"));

            sm.startTimer(StatType.TIME_API_TTFB);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, cf.getInt("worker.bufferSize"))) != -1) {
                if (!gotFirstByte) {
                    sm.clkTimer(StatType.TIME_API_TTFB);
                    gotFirstByte = true;
                }

                // Check if run flag is lowered
                if (!run) return null;

                totalByteCount += byteCount;

                // Check if byte has <CHUNK_SIZE> amount of elements (the first request does not)
                if (byteCount != cf.getInt("worker.bufferSize")) {
                    byte[] trimmedByteBuffer = new byte[byteCount];
                    System.arraycopy(byteBuffer, 0, trimmedByteBuffer, 0, byteCount);

                    // Trim byteBuffer, convert it into string and add to string buffer
                    jsonBuffer.append(new String(trimmedByteBuffer));
                } else {
                    jsonBuffer.append(new String(byteBuffer));
                }

                // Try to find new job number using regex
                if (regexLock) {
                    Matcher matcher = changeIdPattern.matcher(jsonBuffer.toString());

                    if (matcher.find()) {
                        regexLock = false;

                        // Add new-found job to queue
                        wm.setNextChangeID(matcher.group());

                        // Update db change id entry
                        db.upload.updateChangeID(matcher.group());

                        // If new changeID is equal to the previous changeID, it has already been downloaded
                        if (matcher.group().equals(job)) {
                            sm.addValue(StatType.COUNT_API_ERRORS_DUPLICATE, 1);
                            return null;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Very professional exception logging
            if (ex.getMessage().contains("Read timed out")) {
                sm.addValue(StatType.COUNT_API_ERRORS_READ_TIMEOUT, null);
            } else if (ex.getMessage().contains("connect timed out")) {
                sm.addValue(StatType.COUNT_API_ERRORS_CONNECT_TIMEOUT, null);
            } else if (ex.getMessage().contains("Connection reset")) {
                sm.addValue(StatType.COUNT_API_ERRORS_CONN_RESET, null);
            } else if (exceptionPattern5xx.matcher(ex.getMessage()).matches()) {
                sm.addValue(StatType.COUNT_API_ERRORS_5XX, null);
            } else if (exceptionPattern4xx.matcher(ex.getMessage()).matches()) {
                sm.addValue(StatType.COUNT_API_ERRORS_4XX, null);
            }

            logger.error("Caught worker download error: " + ex.getMessage());

            // Add old change id back to the pool only if a new one hasn't been found
            if (regexLock) {
                sleepFor(cf.getInt("worker.lockTimeout"));
                wm.setNextChangeID(job);
            }

            return null;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }

            if (!gotFirstByte) {
                sm.clkTimer(StatType.TIME_API_TTFB);
            }

            sm.clkTimer(StatType.TIME_API_REPLY_DOWNLOAD);
            sm.addValue(StatType.COUNT_REPLY_SIZE, totalByteCount);
        }

        // Now at this point we got the whole reply JSON as a ~3MB string
        // and should convert it to an object before returning
        Reply reply = gson.fromJson(jsonBuffer.toString(), Reply.class);

        // In the case the reply's invalid
        if (reply == null || reply.next_change_id == null) {
            return null;
        }

        return reply;
    }

    /**
     * Sleeps for designated amount of time
     *
     * @param timeMS Time in milliseconds to sleep
     */
    private void sleepFor(int timeMS) {
        try {
            Thread.sleep(timeMS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wait on monitor until notified and a new job is given
     */
    private void waitForJob() {
        synchronized (jobMonitor) {
            // While worker should run and no job is given
            while (run && job == null) {
                try {
                    jobMonitor.wait(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                // If worker should pause
                if (pause) waitOnPause();
            }
        }
    }

    /**
     * Wait on pause monitor
     */
    private void waitOnPause() {
        isPaused = true;

        synchronized (pauseMonitor) {
            logger.debug("Worker {} paused", workerId);

            while (pause) {
                try {
                    pauseMonitor.wait(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            logger.debug("Worker {} resumed", workerId);
        }

        isPaused = false;
    }


    public int getWorkerId() {
        return workerId;
    }

    public String getJob() {
        return job;
    }

    public void setJob(int jobNr, String job) {
        this.currentJobNr = jobNr;
        this.job = job;

        // Notify job monitor
        synchronized (jobMonitor) {
            jobMonitor.notify();
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Generates an info string about the worker, including its id, states and current job
     *
     * @return Info string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Worker ");
        sb.append(workerId);

        if (isPaused) sb.append(" (paused)");
        else if (pause) sb.append(" (pausing)");

        if (isRunning) sb.append(" (stopped)");
        else if (!run) sb.append(" (stopping)");

        sb.append(": ");
        sb.append(job);

        return sb.toString();
    }
}
