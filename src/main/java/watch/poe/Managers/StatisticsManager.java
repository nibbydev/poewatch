package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsManager {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);
    private final Database database;

    private final Map<Thread, Map<StatType, Long>> timers = new HashMap<>();
    private final Map<Thread, Map<StatType, GroupValueEntry>> groupValues = new HashMap<>();
    private final Map<Thread, Map<StatType, List<ValueEntry>>> values = new HashMap<>();

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
    public long clkTimer(StatType type, GroupType group, boolean record) {
        if (type == null) {
            logger.error("Type cannot be null");
            throw new NullPointerException();
        }

        Map<StatType, Long> timerMap = timers.getOrDefault(Thread.currentThread(), new HashMap<>());

        if (!timerMap.containsKey(type)) {
            logger.error("Type doesn't exist in timer map");
            throw new NullPointerException();
        }

        long delay = System.currentTimeMillis() - timerMap.remove(type);
        addValue(type, delay, group, record);

        return delay;
    }

    /**
     * Add a value directly
     *  @param type Enum identifier
     * @param val Value to save
     * @param group Group all entries with same type up as 1 entry
     * @param record Save entry in database
     */
    public void addValue(StatType type, Long val, GroupType group, boolean record) {
        if (group.equals(GroupType.NONE)) {
            synchronized (values) {
                Map<StatType, List<ValueEntry>> entryMap = values.getOrDefault(Thread.currentThread(), new HashMap<>());
                List<ValueEntry> entryList = entryMap.getOrDefault(type, new ArrayList<>());

                entryList.add(new ValueEntry(val, record));

                entryMap.putIfAbsent(type, entryList);
                values.putIfAbsent(Thread.currentThread(), entryMap);
            }
        } else {
            if (val == null) {
                logger.error("Cannot use null value together with group");
                throw new NullPointerException();
            }

            synchronized (groupValues) {
                Map<StatType, GroupValueEntry> entryMap = groupValues.getOrDefault(Thread.currentThread(), new HashMap<>());
                GroupValueEntry groupValueEntry = entryMap.getOrDefault(type, new GroupValueEntry(group, record));

                groupValueEntry.addValue(val);

                entryMap.putIfAbsent(type, groupValueEntry);
                groupValues.putIfAbsent(Thread.currentThread(), entryMap);
            }
        }
    }

    /**
     * Uploads all latest timer delays to database
     */
    public void upload() {
        Map<StatType, List<ValueEntry>> combinedValues = new HashMap<>();

        // Combine values from all threads into single list
        synchronized (values) {
            for (Thread thread : values.keySet()) {
                Map<StatType, List<ValueEntry>> entryMap = values.get(thread);

                for (StatType type : entryMap.keySet()) {
                    List<ValueEntry> entryList = entryMap.get(type);
                    List<ValueEntry> combinedList = combinedValues.getOrDefault(type, new ArrayList<>());

                    // Add only if entry was mark to be recorded
                    for (ValueEntry valueEntry : entryList) {
                        if (valueEntry.record) {
                            combinedList.add(valueEntry);
                        }
                    }

                    combinedValues.putIfAbsent(type, combinedList);
                }
            }

            values.clear();
        }

        // Combine grouped values from all threads into single list
        synchronized (groupValues) {
            Map<StatType, GroupValueEntry> concatMap = new HashMap<>();

            // Combine all elements from multiple threads under grouped entries
            for (Thread thread : groupValues.keySet()) {
                Map<StatType, GroupValueEntry> entryMap = groupValues.get(thread);

                for (StatType type : entryMap.keySet()) {
                    GroupValueEntry concatEntry = concatMap.get(type);
                    GroupValueEntry entry = entryMap.get(type);

                    // Entry didn't exist yet
                    if (concatEntry == null) {
                        concatEntry = new GroupValueEntry(
                                entry.groupType,
                                entry.record
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
                GroupValueEntry entry = concatMap.get(type);
                Long value;

                if (entry.getValues().isEmpty()) {
                    value = (long) 0;
                } else if (entry.groupType.equals(GroupType.AVG)) {
                    long sum = 0;

                    // Not to worry, the values are almost never > smallint
                    for (Long val : entry.getValues()) {
                        sum += val;
                    }

                    value = sum / entry.getValues().size();
                } else if (entry.groupType.equals(GroupType.ADD)) {
                    value = (long) 0;

                    for (Long val : entry.getValues()) {
                        value += val;
                    }
                } else {
                    logger.error("You've reached a part of the code that was impossible to reach. May god have mercy on your soul.");
                    throw new NullPointerException();
                }

                List<ValueEntry> combinedList = combinedValues.getOrDefault(type, new ArrayList<>());
                combinedList.add(new ValueEntry(value));
                combinedValues.putIfAbsent(type, combinedList);
            }

            groupValues.clear();
        }

        database.upload.uploadStatistics(combinedValues);
    }

    /**
     * Gets latest value with the specified type
     *
     * @param type
     * @return The value or 0
     */
    public long getLatest(StatType type) {
        if (type == null) {
            return 0;
        }

        if (!values.containsKey(Thread.currentThread())) {
            return 0;
        }

        Map<StatType, List<ValueEntry>> timerMap = values.get(Thread.currentThread());

        if (!timerMap.containsKey(type)) {
            return 0;
        }

        List<ValueEntry> timerList = timerMap.get(type);

        if (timerList.isEmpty()) {
            return 0;
        }

        return timerList.get(timerList.size() - 1).value;
    }

    public static class ValueEntry {
        Timestamp time = new Timestamp(System.currentTimeMillis());
        boolean record;
        Long value;

        ValueEntry(Long value, boolean record) {
            this.record = record;
            this.value = value;
        }

        ValueEntry(Long value) {
            this.value = value;
        }

        public Long getValue() {
            return value;
        }

        public Timestamp getTime() {
            return time;
        }
    }

    public static class GroupValueEntry {
        private Timestamp time = new Timestamp(System.currentTimeMillis());
        private List<Long> values = new ArrayList<>();
        private GroupType groupType;
        private boolean record;

        GroupValueEntry(GroupType groupType, boolean record) {
            this.groupType = groupType;
            this.record = record;
        }

        public void addValue(Long val) {
            values.add(val);
        }

        public List<Long> getValues() {
            return values;
        }

        public Timestamp getTime() {
            return time;
        }
    }

    public enum StatType {
        CYCLE_TOTAL,
        CYCLE_CALC_PRICES,
        CYCLE_UPDATE_COUNTERS,
        CYCLE_CALC_EXALTED,
        CYCLE_LEAGUE_CYCLE,
        CYCLE_ADD_HOURLY,
        CYCLE_CALC_DAILY,
        CYCLE_RESET_COUNTERS,
        CYCLE_REMOVE_OLD_ENTRIES,
        CYCLE_ADD_DAILY,
        CYCLE_CALC_SPARK,
        CYCLE_ACCOUNT_CHANGES,

        APP_STARTUP,
        APP_SHUTDOWN,

        WORKER_DUPLICATE_JOB,
        WORKER_GROUP_DL,
        WORKER_GROUP_PARSE,
        WORKER_GROUP_UL_ACCOUNTS,
        WORKER_GROUP_RESET_STASHES,
        WORKER_GROUP_UL_ENTRIES,
        WORKER_GROUP_UL_USERNAMES,

        TOTAL_ITEMS,
        ACCEPTED_ITEMS,
        ACTIVE_ACCOUNTS
    }

    public enum GroupType {
        NONE,
        AVG,
        ADD
    }
}
