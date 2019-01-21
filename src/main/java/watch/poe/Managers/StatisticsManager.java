package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Stat.GroupType;
import poe.Managers.Stat.ValueEntry;
import poe.Managers.Stat.StatType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsManager {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);
    private final Database database;

    private final Map<Thread, Map<StatType, Long>> timers = new HashMap<>();
    private final Map<Thread, Map<StatType, ValueEntry>> values = new HashMap<>();

    public StatisticsManager(Database database) {
        this.database = database;
    }

    /**
     * Initiates a timer with the specified type.
     *
     * @param type String type for timer
     */
    public void startTimer(StatType type) {
        if (type == null) {
            logger.error("Type cannot be null");
            throw new NullPointerException();
        }

        Map<StatType, Long> timerMap = timers.getOrDefault(Thread.currentThread(), new HashMap<>());

        if (timerMap.containsKey(type)) {
            logger.error("Type already exists in timer map");
            throw new NullPointerException();
        }

        timerMap.put(type, System.currentTimeMillis());
        timers.putIfAbsent(Thread.currentThread(), timerMap);
    }

    /**
     * Stops the timer instance associated with the provided type and notes the delay.
     *
     * @param type Enum identifier
     * @param group Group all entries with same type up as 1 entry
     * @param record Save entry in database
     * @return Timer delay
     */
    public int clkTimer(StatType type, GroupType group, boolean record) {
        if (type == null) {
            logger.error("Type cannot be null");
            throw new NullPointerException();
        }

        Map<StatType, Long> timerMap = timers.getOrDefault(Thread.currentThread(), new HashMap<>());

        if (!timerMap.containsKey(type)) {
            logger.error("Type doesn't exist in timer map");
            throw new NullPointerException();
        }

        int delay = (int) (System.currentTimeMillis() - timerMap.remove(type));
        addValue(type, delay, group, record);

        return delay;
    }

    /**
     * Add a value directly
     * @param type Enum identifier
     * @param val Value to save
     * @param group Group all entries with same type up as 1 entry
     * @param record Save entry in database
     */
    public void addValue(StatType type, Integer val, GroupType group, boolean record) {
        if (val == null && !group.equals(GroupType.NONE)) {
            logger.error("Cannot use null value together with group");
            throw new NullPointerException();
        }

        synchronized (values) {
            Map<StatType, ValueEntry> entryMap = values.getOrDefault(Thread.currentThread(), new HashMap<>());
            ValueEntry valueEntry = entryMap.getOrDefault(type, new ValueEntry(group, record));

            valueEntry.addValue(val);

            entryMap.putIfAbsent(type, valueEntry);
            values.putIfAbsent(Thread.currentThread(), entryMap);
        }
    }

    /**
     * Uploads all latest timer delays to database
     */
    public void upload() {
        Map<StatType, List<Integer>> combinedValues = new HashMap<>();

        // Combine values from all threads into single list
        synchronized (values) {
            Map<StatType, ValueEntry> concatMap = new HashMap<>();

            // Combine all elements from multiple threads under grouped entries
            for (Thread thread : values.keySet()) {
                Map<StatType, ValueEntry> entryMap = values.get(thread);

                for (StatType type : entryMap.keySet()) {
                    ValueEntry concatEntry = concatMap.get(type);
                    ValueEntry entry = entryMap.get(type);

                    // Entry didn't exist yet
                    if (concatEntry == null) {
                        concatEntry = new ValueEntry(
                                entry.getGroupType(),
                                entry.isRecord()
                        );

                        // Next time it won't be null
                        concatMap.put(type, concatEntry);
                    }

                    // Add all values from the thread-specific group entry to the combined one
                    concatEntry.getValues().addAll(entry.getValues());
                }
            }

            // Group the concatenated entries using the specified method
            for (StatType type : concatMap.keySet()) {
                ValueEntry entry = concatMap.get(type);

                // Get the combined list
                List<Integer> combinedList = combinedValues.getOrDefault(type, new ArrayList<>());
                combinedValues.putIfAbsent(type, combinedList);

                if (entry.getGroupType().equals(GroupType.NONE)) {
                    // If list is empty then have value as null, otherwise with GroupType NONE the list will contain
                    // only 1 value
                    if (entry.getValues().isEmpty()) {
                        combinedList.add(null);
                    } else {
                        combinedList.addAll(entry.getValues());
                    }
                } else if (entry.getGroupType().equals(GroupType.AVG)) {
                    long sum = 0;

                    // Not to worry, the values are almost never > smallint
                    for (Integer val : entry.getValues()) {
                        sum += val;
                    }

                    combinedList.add((int) (sum / entry.getValues().size()));
                } else if (entry.getGroupType().equals(GroupType.ADD)) {
                    int value = 0;

                    for (Integer val : entry.getValues()) {
                        value += val;
                    }

                    combinedList.add(value);
                } else {
                    logger.error("You've reached a part of the code that was impossible to reach. May god have mercy on your soul.");
                    throw new NullPointerException();
                }
            }

            values.clear();
        }

        database.upload.uploadStatistics(combinedValues);
    }

    /**
     * Gets latest value with the specified type
     *
     * @param type
     * @return The value or 0
     */
    public int getLatest(StatType type) {
        if (type == null) {
            return 0;
        }

        if (!values.containsKey(Thread.currentThread())) {
            return 0;
        }

        Map<StatType, ValueEntry> timerMap = values.get(Thread.currentThread());

        if (!timerMap.containsKey(type)) {
            return 0;
        }

        ValueEntry valueEntry = timerMap.get(type);

        if (valueEntry.getValues().isEmpty()) {
            return 0;
        }

        return valueEntry.getValues().get(valueEntry.getValues().size() - 1);
    }
}
