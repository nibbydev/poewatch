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

    private final Map<Thread, Map<KeyType, Long>> timers = new HashMap<>();
    private final Map<Thread, Map<KeyType, GroupValueEntry>> groupValues = new HashMap<>();
    private final Map<Thread, Map<KeyType, List<ValueEntry>>> values = new HashMap<>();

    public StatisticsManager(Database database) {
        this.database = database;
    }

    /**
     * Initiates a timer with the specified key.
     *
     * @param key String key for timer
     */
    public void startTimer(KeyType key) {
        if (key == null) {
            logger.error("Key cannot be null");
            throw new NullPointerException();
        }

        Map<KeyType, Long> timerMap = timers.getOrDefault(Thread.currentThread(), new HashMap<>());

        if (timerMap.containsKey(key)) {
            logger.error("Key already exists in timer map");
            throw new NullPointerException();
        }

        timerMap.put(key, System.currentTimeMillis());
        timers.putIfAbsent(Thread.currentThread(), timerMap);
    }

    /**
     * Stops the timer instance associated with the provided key and notes the delay.
     *
     * @param key String identifier for timer
     */
    public long clkTimer(KeyType key, boolean record, boolean group) {
        if (key == null) {
            logger.error("Key cannot be null");
            throw new NullPointerException();
        }

        Map<KeyType, Long> timerMap = timers.getOrDefault(Thread.currentThread(), new HashMap<>());

        if (!timerMap.containsKey(key)) {
            logger.error("Key doesn't exist in timer map");
            throw new NullPointerException();
        }

        long delay = System.currentTimeMillis() - timerMap.remove(key);
        addValue(key, delay, record, group);

        return delay;
    }

    public void addValue(KeyType key, long val, boolean record, boolean group) {
        if (group) {
            synchronized (groupValues) {
                Map<KeyType, GroupValueEntry> entryMap = groupValues.getOrDefault(Thread.currentThread(), new HashMap<>());
                GroupValueEntry groupValueEntry = entryMap.getOrDefault(key, new GroupValueEntry(record));

                groupValueEntry.addValue(val);

                entryMap.putIfAbsent(key, groupValueEntry);
                groupValues.putIfAbsent(Thread.currentThread(), entryMap);
            }
        } else {
            synchronized (values) {
                Map<KeyType, List<ValueEntry>> entryMap = values.getOrDefault(Thread.currentThread(), new HashMap<>());
                List<ValueEntry> entryList = entryMap.getOrDefault(key, new ArrayList<>());

                entryList.add(new ValueEntry(val, record));

                entryMap.putIfAbsent(key, entryList);
                values.putIfAbsent(Thread.currentThread(), entryMap);
            }
        }
    }

    /**
     * Uploads all latest timer delays to database
     */
    public void upload() {
        Map<KeyType, List<ValueEntry>> combinedValues = new HashMap<>();

        // Combine values from all threads into single list
        synchronized (values) {
            for (Thread thread : values.keySet()) {
                Map<KeyType, List<ValueEntry>> entryMap = values.get(thread);

                for (KeyType key : entryMap.keySet()) {
                    List<ValueEntry> entryList = entryMap.get(key);
                    List<ValueEntry> combinedList = combinedValues.getOrDefault(key, new ArrayList<>());

                    // Add only if entry was mark to be recorded
                    for (ValueEntry valueEntry : entryList) {
                        if (valueEntry.record) {
                            combinedList.add(valueEntry);
                        }
                    }

                    combinedValues.putIfAbsent(key, combinedList);
                }
            }

            values.clear();
        }

        // Combine grouped values from all threads into single list
        synchronized (groupValues) {
            Map<KeyType, List<GroupValueEntry>> threadConcatMap = new HashMap<>();

            // Combine all elements from threads under grouped list
            for (Thread thread : groupValues.keySet()) {
                Map<KeyType, GroupValueEntry> entryMap = groupValues.get(thread);

                for (KeyType key : entryMap.keySet()) {
                    List<GroupValueEntry> threadConcatList = threadConcatMap.getOrDefault(key, new ArrayList<>());
                    GroupValueEntry groupValueEntry = entryMap.get(key);

                    if (!groupValueEntry.record) {
                        continue;
                    }

                    threadConcatList.add(groupValueEntry);
                    threadConcatMap.putIfAbsent(key, threadConcatList);
                }
            }

            // Get averages of the values and store them under the same key
            for (KeyType key : threadConcatMap.keySet()) {
                List<GroupValueEntry> threadConcatList = threadConcatMap.get(key);

                List<Long> means = new ArrayList<>();
                for (GroupValueEntry groupValueEntry : threadConcatList) {
                    means.add(findMean(groupValueEntry.values));
                }

                List<ValueEntry> combinedList = combinedValues.getOrDefault(key, new ArrayList<>());
                combinedList.add(new ValueEntry(findMean(means)));
                combinedValues.putIfAbsent(key, combinedList);
            }

            groupValues.clear();
        }

        database.upload.uploadStatistics(combinedValues);
    }

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
     * Gets latest value with the specified key
     *
     * @param key
     * @return The value or 0
     */
    public long getLatest(KeyType key) {
        if (key == null) {
            return 0;
        }

        if (!values.containsKey(Thread.currentThread())) {
            return 0;
        }

        Map<KeyType, List<ValueEntry>> timerMap = values.get(Thread.currentThread());

        if (!timerMap.containsKey(key)) {
            return 0;
        }

        List<ValueEntry> timerList = timerMap.get(key);

        if (timerList.isEmpty()) {
            return 0;
        }

        return timerList.get(timerList.size() - 1).value;
    }

    public static class ValueEntry {
        Timestamp time = new Timestamp(System.currentTimeMillis());
        boolean record;
        long value;

        ValueEntry(long value, boolean record) {
            this.record = record;
            this.value = value;
        }

        ValueEntry(long value) {
            this.value = value;
        }

        public long getValue() {
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

        public void addValue(long val) {
            values.add(val);
        }

        public List<Long> getValues() {
            return values;
        }

        public Timestamp getTime() {
            return time;
        }
    }



    public enum KeyType {
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
