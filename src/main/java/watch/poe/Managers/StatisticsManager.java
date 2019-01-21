package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.GroupType;
import poe.Managers.Stat.RecordType;
import poe.Managers.Stat.StatEntry;
import poe.Managers.Stat.StatType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsManager {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);
    private final Map<Thread, Map<StatType, StatEntry>> values;
    private final Database database;

    public StatisticsManager(Database database) {
        this.database = database;
        this.values = new HashMap<>();
    }

    /**
     * Initiates a timer with the specified type.
     *
     * @param statType Timer identifier
     * @param groupType Grouping method
     * @param recordType Grouping time frame
     */
    public void startTimer(StatType statType, GroupType groupType, RecordType recordType) {
        if (statType == null || groupType == null || recordType == null) {
            logger.error("Timer types cannot be null");
            throw new NullPointerException();
        }

        if (groupType.equals(GroupType.NONE) && !(recordType.equals(RecordType.NONE) || recordType.equals(RecordType.SINGULAR))) {
            logger.error("Cannot use aggregating record types without an aggregating GroupType");
            throw new NullPointerException();
        }

        synchronized (values) {
            Map<StatType, StatEntry> timerMap = values.getOrDefault(Thread.currentThread(), new HashMap<>());
            StatEntry statEntry = timerMap.getOrDefault(statType, new StatEntry(groupType, recordType));

            statEntry.startTimer();

            timerMap.putIfAbsent(statType, statEntry);
            values.putIfAbsent(Thread.currentThread(), timerMap);
        }
    }

    /**
     * Stops the timer instance associated with the provided type and notes the delay.
     *
     * @param type Timer identifier
     * @return Timer delay
     */
    public int clkTimer(StatType type) {
        if (type == null) {
            logger.error("Type cannot be null");
            throw new NullPointerException();
        }

        synchronized (values) {
            Map<StatType, StatEntry> timerMap = values.get(Thread.currentThread());

            if (timerMap == null) {
                logger.error("Thread doesn't exist in timer map");
                throw new NullPointerException();
            }

            StatEntry statEntry = timerMap.get(type);

            if (statEntry == null) {
                logger.error("Type doesn't exist in timer map");
                throw new NullPointerException();
            }

            int delay = statEntry.clkTimer();
            statEntry.resetTimer();

            statEntry.addValue(delay);

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
            Map<StatType, StatEntry> entryMap = values.getOrDefault(Thread.currentThread(), new HashMap<>());
            StatEntry statEntry = entryMap.getOrDefault(statType, new StatEntry(groupType, recordType));

            statEntry.addValue(val);

            entryMap.putIfAbsent(statType, statEntry);
            values.putIfAbsent(Thread.currentThread(), entryMap);
        }
    }

    /**
     * Uploads all latest timer delays to database
     */
    public void upload() {
        Map<StatType, Integer> aggregatedValues = new HashMap<>();

        // Combine values from all threads into single list
        synchronized (values) {
            Map<StatType, StatEntry> concatMap = new HashMap<>();

            // Combine all elements from multiple threads under grouped entries
            for (Thread thread : values.keySet()) {
                Map<StatType, StatEntry> entryMap = values.get(thread);

                for (StatType type : entryMap.keySet()) {
                    StatEntry concatEntry = concatMap.get(type);
                    StatEntry entry = entryMap.get(type);

                    // This statistic is not meant to be recorded
                    if (entry.getRecordType().equals(RecordType.NONE)) {
                        continue;
                    }

                    // Entry didn't exist yet
                    if (concatEntry == null) {
                        concatEntry = new StatEntry(
                                entry.getGroupType(),
                                entry.getRecordType()
                        );

                        // Next time it won't be null
                        concatMap.put(type, concatEntry);
                    }

                    // Add all values from the thread-specific group entry to the combined one
                    concatEntry.addValues(entry);
                }
            }

            // Group the concatenated entries using the specified method
            for (StatType type : concatMap.keySet()) {
                StatEntry entry = concatMap.get(type);

                if (entry.getSum() == null) {
                    aggregatedValues.put(type, null);
                } else if (entry.getCount() == 1) {
                    aggregatedValues.put(type, entry.getSumAsInt());
                } else if (entry.getGroupType().equals(GroupType.AVG)) {
                    aggregatedValues.put(type, entry.getAvg());
                } else if (entry.getGroupType().equals(GroupType.ADD)) {
                    aggregatedValues.put(type, entry.getSumAsInt());
                } else {
                    logger.error("You've reached a part of the code that was impossible to reach. May god have mercy on your soul.");
                    throw new RuntimeException();
                }
            }

            values.clear();
        }

        database.upload.uploadStatistics(aggregatedValues);
    }

    /**
     * Gets latest value with the specified type
     *
     * @param type
     * @return The value or 0
     */
    public int getLatest(StatType type) {
        if (type == null) {
            logger.error("StatType cannot be null");
            throw new RuntimeException();
        }

        if (!values.containsKey(Thread.currentThread())) {
            logger.error("Thread not found in thread map");
            throw new RuntimeException();
        }

        Map<StatType, StatEntry> entryMap = values.get(Thread.currentThread());

        if (!entryMap.containsKey(type)) {
            logger.error("StatType not found in entry map");
            throw new RuntimeException();
        }

        StatEntry statEntry = entryMap.get(type);

        if (!statEntry.getGroupType().equals(GroupType.NONE)) {
            logger.error("Cannot get latest value for aggregated statistics!");
            throw new RuntimeException();
        }

        if (statEntry.getSum() == null) {
            logger.error("Cannot get latest value from null");
            throw new RuntimeException();
        }

        if (statEntry.getCount() != 1) {
            logger.error("You've reached a part of the code that was impossible to reach. May god have mercy on your soul.");
            throw new RuntimeException();
        }

        return statEntry.getSumAsInt();
    }
}
