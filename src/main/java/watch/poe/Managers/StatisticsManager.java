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
     * @param record Save entry in database
     * @param group Group all entries with same type up as 1 entry
     * @return Timer delay
     */
    public long clkTimer(StatType type, boolean record, boolean group) {
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
        addValue(type, delay, record, group);

        return delay;
    }

    /**
     * Add a value directly
     *
     * @param type Enum identifier
     * @param val Value to save
     * @param record Save entry in database
     * @param group Group all entries with same type up as 1 entry
     */
    public void addValue(StatType type, Long val, boolean record, boolean group) {
        if (group) {
            if (val == null) {
                logger.error("Cannot use null value together with group");
                throw new NullPointerException();
            }

            synchronized (groupValues) {
                Map<StatType, GroupValueEntry> entryMap = groupValues.getOrDefault(Thread.currentThread(), new HashMap<>());
                GroupValueEntry groupValueEntry = entryMap.getOrDefault(type, new GroupValueEntry(record));

                groupValueEntry.addValue(val);

                entryMap.putIfAbsent(type, groupValueEntry);
                groupValues.putIfAbsent(Thread.currentThread(), entryMap);
            }
        } else {
            synchronized (values) {
                Map<StatType, List<ValueEntry>> entryMap = values.getOrDefault(Thread.currentThread(), new HashMap<>());
                List<ValueEntry> entryList = entryMap.getOrDefault(type, new ArrayList<>());

                entryList.add(new ValueEntry(val, record));

                entryMap.putIfAbsent(type, entryList);
                values.putIfAbsent(Thread.currentThread(), entryMap);
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
            Map<StatType, List<GroupValueEntry>> threadConcatMap = new HashMap<>();

            // Combine all elements from threads under grouped list
            for (Thread thread : groupValues.keySet()) {
                Map<StatType, GroupValueEntry> entryMap = groupValues.get(thread);

                for (StatType type : entryMap.keySet()) {
                    List<GroupValueEntry> threadConcatList = threadConcatMap.getOrDefault(type, new ArrayList<>());
                    GroupValueEntry groupValueEntry = entryMap.get(type);

                    if (!groupValueEntry.record) {
                        continue;
                    }

                    threadConcatList.add(groupValueEntry);
                    threadConcatMap.putIfAbsent(type, threadConcatList);
                }
            }

            // Get averages of the values and store them under the same type
            for (StatType type : threadConcatMap.keySet()) {
                List<GroupValueEntry> threadConcatList = threadConcatMap.get(type);

                List<Long> means = new ArrayList<>();
                for (GroupValueEntry groupValueEntry : threadConcatList) {
                    means.add(findMean(groupValueEntry.values));
                }

                List<ValueEntry> combinedList = combinedValues.getOrDefault(type, new ArrayList<>());
                combinedList.add(new ValueEntry(findMean(means)));
                combinedValues.putIfAbsent(type, combinedList);
            }

            groupValues.clear();
        }

        database.upload.uploadStatistics(combinedValues);
    }

    /**
     * Finds the mean value from a group of entries
     *
     * @param values
     * @return 0 on failure, mean otherwise
     */
    public static long findMean(List<Long> values) {
        if (values.isEmpty()) {
            return 0;
        }

        long sum = 0;

        // Not to worry, the values are almost never > smallint
        for (Long entry : values) {
            sum += entry;
        }

        return sum / values.size();
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
        Timestamp time = new Timestamp(System.currentTimeMillis());
        List<Long> values = new ArrayList<>();
        boolean record;

        GroupValueEntry(boolean record) {
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
        cycle_total,
        cycle_0_calcPrices,
        cycle_0_updateCounters,
        cycle_0_calculateExalted,
        cycle_10_leagueCycle,
        cycle_60_addHourly,
        cycle_60_calcDaily,
        cycle_60_resetCounters,
        cycle_24_removeOldItemEntries,
        cycle_24_addDaily,
        cycle_24_calcSpark,
        cycle_24_accountNameChanges,

        app_startup,
        app_shutdown,

        worker_duplicateJob,
        worker_group_dl,
        worker_group_parse,
        worker_group_ulAccounts,
        worker_group_resetStashes,
        worker_group_ulEntries,
        worker_group_ulUsernames
    }
}
