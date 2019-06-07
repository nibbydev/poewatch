package poe.Worker;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.ApiDeserializers.Reply;
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

    private static long lastPullTime;
    private final WorkerManager workerManager;
    private final StatisticsManager statisticsManager;
    private final Database database;
    private final Config config;
    private final Gson gson;
    private final Object jobMonitor = new Object();
    private final Object pauseMonitor = new Object();
    private ItemParser itemParser;
    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private String job;
    private int workerId;
    private boolean pauseFlag = false;
    private boolean isPaused = false;

    public Worker(WorkerManager wm, StatisticsManager sm, Database db, Config cnf, int id, ItemParser ip) {
        this.itemParser = ip;
        this.workerManager = wm;
        this.statisticsManager = sm;
        this.database = db;
        this.config = cnf;
        this.workerId = id;

        gson = new Gson();
    }


    /**
     * Contains main loop. Checks for new jobs and processes them
     */
    public void run() {
        while (flagLocalRun) {
            waitForJob();

            String replyString = download();
            statisticsManager.addValue(StatType.COUNT_API_CALLS, null);

            if (replyString != null) {
                Reply reply = gson.fromJson(replyString, Reply.class);

                if (pauseFlag) {
                    pauseWorker();
                }

                if (reply != null && reply.next_change_id != null) {
                    statisticsManager.startTimer(StatType.TIME_PARSE_REPLY);
                    itemParser.processApiReply(reply);
                    statisticsManager.clkTimer(StatType.TIME_PARSE_REPLY);
                }
            }

            job = null;
        }

        logger.info("Worker ({}) stopped", workerId);
        readyToExit = true;
    }

    /**
     * Requests the worker to stop
     */
    public void requestStop() {
        flagLocalRun = false;
        wakeLocalMonitor();
    }

    /**
     * Downloads data from the API
     *
     * @return Whole stash batch as a string
     */
    private String download() {
        StringBuilder stringBuilderBuffer = new StringBuilder();
        byte[] byteBuffer = new byte[config.getInt("worker.bufferSize")];
        boolean regexLock = true;
        boolean gotFirstByte = false;
        InputStream stream = null;
        int byteCount, totalByteCount = 0;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - Worker.lastPullTime < config.getInt("worker.downloadDelay")) {
            sleepFor((int) (config.getInt("worker.downloadDelay") - System.currentTimeMillis() + Worker.lastPullTime));
        }

        Worker.lastPullTime = System.currentTimeMillis();

        try {
            statisticsManager.startTimer(StatType.TIME_API_REPLY_DOWNLOAD);

            // Define the request
            URL request = new URL("http://www.pathofexile.com/api/public-stash-tabs?id=" + this.job);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            // Define timeouts: 3 sec for connecting, 10 sec for ongoing connection
            connection.setReadTimeout(config.getInt("worker.readTimeout"));
            connection.setConnectTimeout(config.getInt("worker.connectTimeout"));

            statisticsManager.startTimer(StatType.TIME_API_TTFB);

            // Define the streamer (used for reading in chunks)
            stream = connection.getInputStream();

            // Stream data and count bytes
            while ((byteCount = stream.read(byteBuffer, 0, config.getInt("worker.bufferSize"))) != -1) {
                if (!gotFirstByte) {
                    statisticsManager.clkTimer(StatType.TIME_API_TTFB);
                    gotFirstByte = true;
                }

                // Check if run flag is lowered
                if (!flagLocalRun) return null;

                totalByteCount += byteCount;

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
                    Matcher matcher = changeIdPattern.matcher(stringBuilderBuffer.toString());

                    if (matcher.find()) {
                        regexLock = false;

                        // Add new-found job to queue
                        workerManager.setNextChangeID(matcher.group());

                        // Update db change id entry
                        database.upload.updateChangeID(matcher.group());

                        // If new changeID is equal to the previous changeID, it has already been downloaded
                        if (matcher.group().equals(job)) {
                            statisticsManager.addValue(StatType.COUNT_API_ERRORS_DUPLICATE, 1);
                            return null;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Very professional exception logging
            if (ex.getMessage().contains("Read timed out")) {
                statisticsManager.addValue(StatType.COUNT_API_ERRORS_READ_TIMEOUT, null);
            } else if (ex.getMessage().contains("connect timed out")) {
                statisticsManager.addValue(StatType.COUNT_API_ERRORS_CONNECT_TIMEOUT, null);
            } else if (ex.getMessage().contains("Connection reset")) {
                statisticsManager.addValue(StatType.COUNT_API_ERRORS_CONN_RESET, null);
            } else if (exceptionPattern5xx.matcher(ex.getMessage()).matches()) {
                statisticsManager.addValue(StatType.COUNT_API_ERRORS_5XX, null);
            } else if (exceptionPattern4xx.matcher(ex.getMessage()).matches()) {
                statisticsManager.addValue(StatType.COUNT_API_ERRORS_4XX, null);
            }

            logger.error("Caught worker download error: " + ex.getMessage());

            // Add old changeID to the pool only if a new one hasn't been found
            if (regexLock) {
                sleepFor(config.getInt("worker.lockTimeout"));
                workerManager.setNextChangeID(job);
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
                statisticsManager.clkTimer(StatType.TIME_API_TTFB);
            }

            statisticsManager.clkTimer(StatType.TIME_API_REPLY_DOWNLOAD);
            statisticsManager.addValue(StatType.COUNT_REPLY_SIZE, totalByteCount);
        }

        // Return the downloaded mess of a JSON string
        return stringBuilderBuffer.toString();
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

    private void waitForJob() {
        synchronized (jobMonitor) {
            while (flagLocalRun && job == null) {
                try {
                    jobMonitor.wait(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                if (pauseFlag) {
                    pauseWorker();
                }
            }
        }
    }

    private void pauseWorker() {
        isPaused = true;

        synchronized (pauseMonitor) {
            logger.debug("Worker {} paused", workerId);

            while (pauseFlag) {
                try {
                    pauseMonitor.wait(200);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            logger.debug("Worker {} resumed", workerId);
        }

        isPaused = false;
    }

    private void wakeLocalMonitor() {
        synchronized (jobMonitor) {
            jobMonitor.notify();
        }
    }

    public int getWorkerId() {
        return workerId;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
        wakeLocalMonitor();
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPauseFlag(boolean pauseFlag) {
        this.pauseFlag = pauseFlag;
    }

    public boolean isReadyToExit() {
        return readyToExit;
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
        else if (pauseFlag) sb.append(" (pausing)");

        if (readyToExit) sb.append(" (stopped)");
        else if (!flagLocalRun) sb.append(" (stopping)");

        sb.append(": ");
        sb.append(job);

        return sb.toString();
    }
}
