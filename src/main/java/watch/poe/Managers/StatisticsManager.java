package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;

public class StatisticsManager {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);
    private final Map<Thread, Set<StatTimer>> threadTimers = new HashMap<>();
    private final Database database;

    private final Collector[] collectors = {
            new Collector(StatType.CYCLE_TOTAL,             GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.CALC_PRICES,             GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.UPDATE_COUNTERS,         GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.CALC_EXALT,              GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.CYCLE_LEAGUES,           GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.ADD_HOURLY,              GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.CALC_DAILY,              GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.RESET_COUNTERS,          GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.REMOVE_OLD_ENTRIES,      GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.ADD_DAILY,               GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.CALC_SPARK,              GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.ACCOUNT_CHANGES,         GroupType.NONE, RecordType.SINGULAR),

            new Collector(StatType.APP_STARTUP,             GroupType.NONE, RecordType.SINGULAR),
            new Collector(StatType.APP_SHUTDOWN,            GroupType.NONE, RecordType.SINGULAR),

            new Collector(StatType.WORKER_DOWNLOAD,         GroupType.AVG,  RecordType.SINGULAR),
            new Collector(StatType.WORKER_PARSE,            GroupType.AVG,  RecordType.SINGULAR),
            new Collector(StatType.WORKER_UPLOAD_ACCOUNTS,  GroupType.AVG,  RecordType.SINGULAR),
            new Collector(StatType.WORKER_RESET_STASHES,    GroupType.AVG,  RecordType.SINGULAR),
            new Collector(StatType.WORKER_UPLOAD_ENTRIES,   GroupType.AVG,  RecordType.SINGULAR),
            new Collector(StatType.WORKER_UPLOAD_USERNAMES, GroupType.AVG,  RecordType.SINGULAR),

            new Collector(StatType.WORKER_DUPLICATE_JOB,    GroupType.ADD,  RecordType.M_10),
            new Collector(StatType.TOTAL_STASHES,           GroupType.ADD,  RecordType.M_10),
            new Collector(StatType.TOTAL_ITEMS,             GroupType.ADD,  RecordType.M_10),
            new Collector(StatType.ACCEPTED_ITEMS,          GroupType.ADD,  RecordType.M_10),

            new Collector(StatType.ACTIVE_ACCOUNTS,         GroupType.NONE, RecordType.SINGULAR),
    };

    public StatisticsManager(Database database) {
        this.database = database;

        // Get ongoing statistics collectors from database
        database.init.getTmpStatistics(collectors);

        // Find if any of the collectors have expired
        Set<Collector> expired = Arrays.stream(collectors)
                .filter(i -> !i.getRecordType().equals(RecordType.NONE))
                .filter(i -> !i.getRecordType().equals(RecordType.SINGULAR))
                .filter(i -> i.isCollectingOverTime())
                .filter(i -> i.isExpired())
                .peek(i -> logger.info(String.format("Expired collector [%s]", i.getStatType().name())))
                .collect(Collectors.toSet());

        if (!expired.isEmpty()) {
            // Delete them from the database
            database.upload.deleteTmpStatistics(expired);
            database.upload.uploadStatistics(expired);

            // Reset the expired collectors
            expired.forEach(Collector::reset);
        }
    }

    /**
     * Initiates a timer with the specified type
     *
     * @param statType Stat identifier
     */
    public void startTimer(StatType statType) {
        synchronized (threadTimers) {
            Set<StatTimer> statTimerList = threadTimers.getOrDefault(Thread.currentThread(), new HashSet<>());
            threadTimers.putIfAbsent(Thread.currentThread(), statTimerList);

            // If there was no entry, create a new one and add it to the list
            if (statTimerList.stream().anyMatch(i -> i.getStatType().equals(statType))) {
                logger.error("Stat timer already exists");
                throw new RuntimeException();
            }

            statTimerList.add(new StatTimer(statType));
        }
    }

    /**
     * Stops the timer instance associated with the provided type and notes the delay
     *
     * @param statType Stat identifier
     * @return Delay in MS
     */
    public int clkTimer(StatType statType) {
        int delay;

        synchronized (threadTimers) {
            Set<StatTimer> statEntryList = threadTimers.get(Thread.currentThread());

            if (statEntryList == null) {
                logger.error("Thread doesn't exist in current context");
                throw new RuntimeException();
            }

            // Find first timer
            StatTimer statTimer = statEntryList.stream()
                    .filter(i -> i.getStatType().equals(statType))
                    .findFirst()
                    .orElse(null);

            // If it didn't exist
            if (statTimer == null) {
                logger.error("Stat type doesn't exist in current context");
                throw new RuntimeException();
            }

            // Remove the timer
            statEntryList.remove(statTimer);

            // Get delay as int MS
            delay = toIntExact(System.currentTimeMillis() - statTimer.getStartTime());
        }

        // Add value to the collector
        addValue(statType, delay);

        // Return value isn't even used anywhere
        return delay;
    }

    /**
     * Add a value directly to a collector
     *
     * @param statType Stat identifier
     * @param val Value to save
     */
    public void addValue(StatType statType, Integer val) {
        synchronized (collectors) {
            // Find first collector
            Collector collector = Arrays.stream(collectors)
                    .filter(i -> i.getStatType().equals(statType))
                    .findFirst()
                    .orElse(null);

            // If it didn't exist
            if (collector == null) {
                logger.error("The specified collector could not be found");
                throw new RuntimeException();
            }

            // Add value to the collector
            collector.addValue(val);
        }
    }

    /**
     * Uploads all latest timer delays to database
     */
    public void upload() {
        // Find collectors that should be uploaded
        Set<Collector> filtered = Arrays.stream(collectors)
                .filter(i -> !i.getRecordType().equals(RecordType.NONE))
                .filter(i -> !i.isCollectingOverTime())
                .filter(i -> i.getCount() > 0)
                .collect(Collectors.toSet());

        // Find collectors that should be uploaded
        Set<Collector> filteredExpired = Arrays.stream(collectors)
                .filter(i -> !i.getRecordType().equals(RecordType.NONE))
                .filter(i -> !i.getRecordType().equals(RecordType.SINGULAR))
                .filter(i -> i.isCollectingOverTime())
                .filter(i -> i.isExpired())
                .collect(Collectors.toSet());

        // Find collectors that should be uploaded to tmp table
        Set<Collector> filteredTmp = Arrays.stream(collectors)
                .filter(i -> !i.getRecordType().equals(RecordType.NONE))
                .filter(i -> !i.getRecordType().equals(RecordType.SINGULAR))
                .filter(i -> i.isCollectingOverTime())
                .filter(i -> !i.isExpired())
                .filter(i -> i.getCount() > 0)
                .collect(Collectors.toSet());

        database.upload.uploadStatistics(filtered);
        database.upload.uploadStatistics(filteredExpired);
        database.upload.deleteTmpStatistics(filteredExpired);
        database.upload.uploadTempStatistics(filteredTmp);

        // Reset all collectors that are not ongoing
        filtered.forEach(Collector::reset);
        filteredExpired.forEach(Collector::reset);
    }

    /**
     * Gets latest value with the specified type
     *
     * @param statType Stat identifier
     * @return The value or 0
     */
    public int getLast(StatType statType) {
        if (statType == null) {
            logger.error("StatType cannot be null");
            throw new RuntimeException();
        }

        // Find first collector
        Collector collector = Arrays.stream(collectors)
                .filter(i -> i.getStatType().equals(statType))
                .findFirst()
                .orElse(null);

        // If it didn't exist
        if (collector == null) {
            logger.error("The specified collector could not be found");
            throw new RuntimeException();
        }

        return collector.getLatestValue();
    }
}
