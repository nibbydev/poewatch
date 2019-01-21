package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.*;

import java.util.*;

import static java.lang.Math.toIntExact;

public class StatisticsManager {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);
    private final Map<Thread, Set<StatTimer>> threadTimers;
    private final Set<StatEntry> values;
    private final Database database;

    public StatisticsManager(Database database) {
        this.database = database;
        this.threadTimers = new HashMap<>();
        this.values = new HashSet<>();

        database.init.getTmpStatistics(this.values);
    }

    /**
     * Initiates a timer with the specified type.
     *
     * @param statType Timer identifier
     */
    public void startTimer(StatType statType) {
        synchronized (threadTimers) {
            Set<StatTimer> statTimerList = threadTimers.getOrDefault(Thread.currentThread(), new HashSet<>());
            threadTimers.putIfAbsent(Thread.currentThread(), statTimerList);

            StatTimer statTimer = statTimerList.stream()
                    .filter(i -> i.getStatType().equals(statType))
                    .findFirst()
                    .orElse(null);

            // If there was no entry, create a new one and add it to the list
            if (statTimer != null) {
                logger.error("Stat timer already exists");
                throw new RuntimeException();
            }

            statTimerList.add(new StatTimer(statType));
        }
    }

    /**
     * Stops the timer instance associated with the provided type and notes the delay.
     *
     * @param statType Timer identifier
     * @return Timer delay
     */
    public int clkTimer(StatType statType, GroupType groupType, RecordType recordType) {
        if (statType == null || groupType == null || recordType == null) {
            logger.error("Timer types cannot be null");
            throw new NullPointerException();
        }

        if (groupType.equals(GroupType.NONE) && !(recordType.equals(RecordType.NONE) || recordType.equals(RecordType.SINGULAR))) {
            logger.error("Cannot use aggregating record types without an aggregating GroupType");
            throw new NullPointerException();
        }

        synchronized (threadTimers) {
            Set<StatTimer> statEntryList = threadTimers.get(Thread.currentThread());

            if (statEntryList == null) {
                logger.error("Thread doesn't exist in map");
                throw new RuntimeException();
            }

            // Find first timer
            StatTimer statTimer = statEntryList.stream()
                    .filter(i -> i.getStatType().equals(statType))
                    .findFirst()
                    .orElse(null);

            // If it didn't exist
            if (statTimer == null) {
                logger.error("Stat type doesn't exist in list");
                throw new RuntimeException();
            }

            // Remove the timer
            statEntryList.remove(statTimer);
            int delay = toIntExact(System.currentTimeMillis() - statTimer.getStartTime());

            // Find first entry
            StatEntry statEntry = values.stream()
                    .filter(i -> i.getStatType().equals(statType))
                    .findFirst()
                    .orElse(null);

            // Create if it didn't exist
            if (statEntry == null) {
                statEntry = new StatEntry(statType, groupType, recordType);
                values.add(statEntry);
            }

            // Add delay to entry
            statEntry.addValue(delay);

            // Return value isn't even used anywhere
            return delay;
        }
    }

    /**
     * Add a value directly
     * @param val Value to save
     * @param statType Timer identifier
     * @param groupType Grouping method
     * @param recordType Grouping time frame
     */
    public void addValue(Integer val, StatType statType, GroupType groupType, RecordType recordType) {
        if (val == null && !groupType.equals(GroupType.NONE)) {
            logger.error("Cannot use null value together with group");
            throw new NullPointerException();
        }

        if (groupType.equals(GroupType.NONE) && !(recordType.equals(RecordType.NONE) || recordType.equals(RecordType.SINGULAR))) {
            logger.error("Cannot use aggregating record types without an aggregating GroupType");
            throw new NullPointerException();
        }

        synchronized (values) {
            StatEntry statEntry = values.stream()
                    .filter(i -> i.getStatType().equals(statType))
                    .findFirst()
                    .orElse(null);

            if (statEntry == null) {
                statEntry = new StatEntry(statType, groupType, recordType);
                values.add(statEntry);
            }

            statEntry.addValue(val);
        }
    }

    /**
     * Uploads all latest timer delays to database
     */
    public void upload() {
        Map<StatType, List<Integer>> concMap = new HashMap<>();
        Set<StatEntry> recordEntries = new HashSet<>();

        synchronized (values) {
            for (StatEntry statEntry : values) {
                List<Integer> concList = concMap.getOrDefault(statEntry.getStatType(), new ArrayList<>());
                concMap.putIfAbsent(statEntry.getStatType(), concList);

                // Should be collected over time
                if (statEntry.isRecord() && !statEntry.isExpired()) {
                    recordEntries.add(statEntry);
                    continue;
                }

                if (statEntry.getSum() == null) {
                    concList.add(null);
                } else if (statEntry.getGroupType().equals(GroupType.ADD)) {
                    concList.add(statEntry.getSumAsInt());
                } else if (statEntry.getGroupType().equals(GroupType.AVG)) {
                    concList.add(statEntry.getAvg());
                } else {
                    concList.add(statEntry.getSumAsInt());
                }
            }

            values.clear();
        }

        // Add back the ongoing collectors
        values.addAll(recordEntries);

        database.upload.uploadStatistics(concMap);
        database.upload.uploadTempStatistics(recordEntries);
    }

    /**
     * Gets latest value with the specified type
     *
     * @param statType
     * @return The value or 0
     */
    public int getLast(StatType statType) {
        if (statType == null) {
            logger.error("StatType cannot be null");
            throw new RuntimeException();
        }

        StatEntry statEntry = values.stream()
                .filter(i -> i.getStatType().equals(statType))
                .findFirst()
                .orElse(null);

        if (statEntry == null) {
            logger.error("StatType not found in entry set");
            throw new RuntimeException();
        }

        return statEntry.getLatestValue();
    }
}
