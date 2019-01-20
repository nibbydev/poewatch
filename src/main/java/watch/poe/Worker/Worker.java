package poe.Worker;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Item;
import poe.Item.ItemParser;
import poe.Item.Mappers;
import poe.Managers.LeagueManager;
import poe.Managers.RelationManager;
import poe.Managers.StatisticsManager;
import poe.Managers.StatisticsManager.StatType;
import poe.Managers.WorkerManager;
import poe.Worker.Entry.RawItemEntry;
import poe.Worker.Entry.RawUsernameEntry;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Downloads and processes a batch of data downloaded from the PoE API. Runs in a separate thread.
 */
public class Worker extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    private static final Set<Long> DbStashes = new HashSet<>(100000);
    private static final Pattern pattern = Pattern.compile("\\d*-\\d*-\\d*-\\d*-\\d*");
    private static final CRC32 crc = new CRC32();
    private static long lastPullTime;
    private final WorkerManager workerManager;
    private final LeagueManager leagueManager;
    private final RelationManager relationManager;
    private final StatisticsManager statisticsManager;
    private final Database database;
    private final Config config;
    private final Gson gson;
    private final Object jobMonitor = new Object();
    private final Object pauseMonitor = new Object();
    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private String job;
    private int workerId;
    private boolean pauseFlag = false;
    private boolean isPaused = false;

    public Worker(WorkerManager wm, StatisticsManager sm, LeagueManager lm, RelationManager rm, Database db, Config cnf, int id) {
        this.workerManager = wm;
        this.statisticsManager = sm;
        this.leagueManager = lm;
        this.relationManager = rm;
        this.database = db;
        this.config = cnf;
        this.workerId = id;

        gson = new Gson();
    }

    private static long calcCrc(String str) {
        if (str == null) {
            return 0;
        } else {
            crc.reset();
            crc.update(str.getBytes());
            return crc.getValue();
        }
    }

    public static Set<Long> getDbStashes() {
        return DbStashes;
    }

    /**
     * Contains main loop. Checks for new jobs and processes them
     */
    public void run() {
        while (flagLocalRun) {
            waitForJob();

            statisticsManager.startTimer(StatType.worker_group_dl);
            String replyString = download();
            statisticsManager.clkTimer(StatType.worker_group_dl, true, true);

            if (replyString != null) {
                Mappers.APIReply reply = gson.fromJson(replyString, Mappers.APIReply.class);

                if (pauseFlag) {
                    pauseWorker();
                }

                if (reply != null && reply.next_change_id != null) {
                    statisticsManager.startTimer(StatType.worker_group_parse);
                    parseItems(reply);
                    statisticsManager.clkTimer(StatType.worker_group_parse, true, true);

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

        while (!readyToExit) {
            wakeLocalMonitor();
            sleepFor(50);
        }
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
        InputStream stream = null;
        int byteCount;

        // Sleep for x milliseconds
        while (System.currentTimeMillis() - Worker.lastPullTime < config.getInt("worker.downloadDelay")) {
            sleepFor((int) (config.getInt("worker.downloadDelay") - System.currentTimeMillis() + Worker.lastPullTime));
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
                            statisticsManager.addValue(StatType.worker_duplicateJob, null, true, true);
                            return null;
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Caught worker download error: " + ex.getMessage());

            // Add old changeID to the pool only if a new one hasn't been found
            if (regexLock) {
                sleepFor(config.getInt("worker.lockTimeout"));
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
     * Adds entries to the databases
     *
     * @param reply APIReply object that a worker has downloaded and deserialized
     */
    private void parseItems(Mappers.APIReply reply) {
        // Set of account names and items extracted from the API call
        Set<Long> accounts = new HashSet<>();
        Set<Long> nullStashes = new HashSet<>();
        Set<RawItemEntry> items = new HashSet<>();
        // Separate set for collecting account and character names
        Set<RawUsernameEntry> usernames = new HashSet<>();

        for (Mappers.Stash stash : reply.stashes) {
            // Get league ID. If it's an unknown ID, skip this stash
            Integer id_l = leagueManager.getLeagueId(stash.league);
            if (id_l == null) {
                continue;
            }

            // Calculate CRCs
            long account_crc = calcCrc(stash.accountName);
            long stash_crc = calcCrc(stash.id);

            // If the stash is in use somewhere in the database
            synchronized (DbStashes) {
                if (DbStashes.contains(stash_crc)) {
                    nullStashes.add(stash_crc);
                }
            }

            if (stash.accountName == null || !stash.isPublic) {
                continue;
            }

            boolean hasValidItems = false;

            for (Mappers.BaseItem baseItem : stash.items) {
                long item_crc = calcCrc(baseItem.getId());

                // Create an ItemParser instance for every item in the stash, as one item
                // may branch into multiple db entries. For examples, a Devoto's Devotion with
                // a Tornado Shot enchantment creates 2 entries.
                ItemParser itemParser = new ItemParser(baseItem);

                // There was something off with the base item, discard it and don'tt create branched items
                if (itemParser.isDiscard()) {
                    continue;
                }

                // Parse branched items and create objects for db upload
                for (Item item : itemParser.getBranchedItems()) {
                    // Check if this specific branched item should be discarded
                    if (item.isDiscard()) {
                        continue;
                    }

                    // Get item's ID (if missing, index it)
                    Integer id_d = relationManager.index(item, id_l);
                    if (id_d == null) {
                        continue;
                    }

                    RawItemEntry entry = new RawItemEntry(id_l, id_d, account_crc, stash_crc, item_crc,
                            itemParser.getIdPrice(), itemParser.getPrice());
                    items.add(entry);

                    // Set flag to indicate stash contained at least 1 valid item
                    if (!hasValidItems) {
                        hasValidItems = true;
                    }
                }
            }

            // If stash contained at least 1 valid item, save the account
            if (hasValidItems) {
                DbStashes.add(stash_crc);
                accounts.add(account_crc);
            }

            // As this is a completely separate service, collect all character and account names separately
            if (stash.lastCharacterName != null) {
                usernames.add(new RawUsernameEntry(stash.accountName, stash.lastCharacterName, id_l));
            }
        }

        // Shovel everything to db
        statisticsManager.startTimer(StatType.worker_group_ulAccounts);
        database.upload.uploadAccounts(accounts);
        statisticsManager.clkTimer(StatType.worker_group_ulAccounts, true, true);

        statisticsManager.startTimer(StatType.worker_group_resetStashes);
        database.flag.resetStashReferences(nullStashes);
        statisticsManager.clkTimer(StatType.worker_group_resetStashes, true, true);

        statisticsManager.startTimer(StatType.worker_group_ulEntries);
        database.upload.uploadEntries(items);
        statisticsManager.clkTimer(StatType.worker_group_ulEntries, true, true);

        statisticsManager.startTimer(StatType.worker_group_ulUsernames);
        database.upload.uploadUsernames(usernames);
        statisticsManager.clkTimer(StatType.worker_group_ulUsernames, true, true);
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
            System.out.printf("- worker %d paused\n", workerId);

            while (pauseFlag) {
                try {
                    pauseMonitor.wait(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.printf("- worker %d resumed\n", workerId);
        }

        isPaused = false;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    /**
     * Notifies local monitor
     */
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
}
