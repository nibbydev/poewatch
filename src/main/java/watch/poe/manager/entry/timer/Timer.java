package poe.manager.entry.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.entry.StatusElement;

import java.util.HashMap;
import java.util.Map;

public class Timer {
    public enum TimerType {NONE, DEFAULT, TEN, SIXTY, TWENTY}

    private Map<String, TimerList> timeLog = new HashMap<>();
    private Map<String, TimerEntry> timers = new HashMap<>();
    private Map<String, Long> delays = new HashMap<>();
    private final Object monitor = new Object();
    private volatile boolean flagStop = false;
    private Database database;

    private static Logger logger = LoggerFactory.getLogger(Timer.class);

    public Timer(Database database) {
        this.database = database;
    }

    /**
     * Computes delays for all marked timers that match the active status in the provided StatusElement and
     * stores those delays in the delays map
     *
     * @param statusElement A valid StatusElement instance
     */
    public void computeCycleDelays(StatusElement statusElement) {
        logger.info("Calculating cycle delays...");

        Map<String, Long> averages = new HashMap<>();
        Map<String, Long> delays = new HashMap<>();
        long total = 0;

        TimerType type = translate(statusElement);

        for (String key : timeLog.keySet()) {
            TimerList timerList = timeLog.get(key);

            // This is a bit retarded
            if (timerList.type.equals(TimerType.NONE)) {
                continue;
            } else if (type.equals(TimerType.SIXTY)) {
                if (timerList.type.equals(TimerType.TWENTY)) continue;
            } else if (type.equals(TimerType.TEN)) {
                if (timerList.type.equals(TimerType.SIXTY)) continue;
                if (timerList.type.equals(TimerType.TWENTY)) continue;
            } else if (type.equals(TimerType.DEFAULT)) {
                if (timerList.type.equals(TimerType.TEN)) continue;
                if (timerList.type.equals(TimerType.SIXTY)) continue;
                if (timerList.type.equals(TimerType.TWENTY)) continue;
            }

            if (timerList.list.size() > 1) {
                long avg = 0;
                for (Long time : timerList.list) avg += time;
                avg /= timerList.list.size();

                if (avg > 0) {
                    averages.put(key, avg);
                    total += avg;
                }
            }
        }

        if (total > 0) {
            logger.info(String.format("Total delay span was %5d milliseconds", total));

            // Span delays over a 50 second time frame
            long multiplier = total < 50 * 1000 ? 50 * 1000 / total - 1 : 0;

            // Compute delays for each valid entry
            for (String key : averages.keySet()) {
                delays.put(key, averages.get(key) * multiplier);
            }

            // Assign delays map
            this.delays = delays;
        }
    }

    /**
     * Converts StatusElement status type to TimerType enum member.
     *
     * @param statusElement A valid StatusElement instance
     * @return The matching TimerType if state is active or TimerType.DEFAULT if not.
     */
    private TimerType translate(StatusElement statusElement) {
        if (statusElement.isTwentyFourBool()) return TimerType.TWENTY;
        if (statusElement.isSixtyBool()) return TimerType.SIXTY;
        if (statusElement.isTenBool()) return TimerType.TEN;

        return TimerType.DEFAULT;
    }

    /**
     * Converts int status type to TimerType enum member.
     *
     * @param type A valid integer
     * @return The matching TimerType if state is active or TimerType.DEFAULT if not.
     */
    public static TimerType translate(Integer type) {
        if (type == null) return TimerType.DEFAULT;
        if (type == 10) return TimerType.TEN;
        if (type == 60) return TimerType.SIXTY;
        if (type == 24) return TimerType.TWENTY;

        return TimerType.DEFAULT;
    }

    /**
     * Converts TimerType status type to Integer
     *
     * @param type A valid TimerType
     * @return The matching Integer if state is active or null if not.
     */
    public static Integer translate(TimerType type) {
        switch (type) {
            case DEFAULT:
                return null;
            case TEN:
                return 10;
            case SIXTY:
                return 60;
            case TWENTY:
                return 24;
        }

        return 0;
    }

    /**
     * Calls _start with provided parameters
     *
     * @param key  String identifier for timer
     * @param type Type of timer, works hierarchically
     */
    public void start(String key, TimerType type) {
        _start(key, type);
    }

    /**
     * Calls _start with provided parameters.
     * Type will be DEFAULT
     *
     * @param key String identifier for timer
     */
    public void start(String key) {
        _start(key, TimerType.DEFAULT);
    }

    /**
     * Timer start method, initiates a timer instance for the specific key.
     * If there exists a delay associated with the key, it will wait for that amount before proceeding.
     *
     * @param key  String identifier for timer
     * @param type Type of timer, works hierarchically
     */
    private void _start(String key, TimerType type) {
        // Get delay or null if none
        Long delay = delays.get(key);

        if (!type.equals(TimerType.NONE)) {
            if (delay != null) {
                logger.info(String.format("Delay %5d (actual %5d) found for key %8s",
                        delay,
                        getLatest(key),
                        key));

                long startTime = System.currentTimeMillis();

                // Wait for specified time before proceeding
                while (!flagStop && System.currentTimeMillis() - startTime < delay) {
                    synchronized (monitor) {
                        try {
                            monitor.wait(10);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } else {
                logger.info(String.format("Delay  null found for key %8s", key));
            }
        }

        // Create a new TimerEntry instance and add to timer map
        TimerEntry timerEntry = new TimerEntry(type);
        timers.put(key, timerEntry);
    }

    /**
     * Stops the timer instance associated with the provided key and logs the delay.
     *
     * @param key String identifier for timer
     */
    public void clk(String key) {
        // Remove and get TimerEntry from timer list
        TimerEntry timerEntry = timers.remove(key);
        // Find delay between right now and timer's start point
        long delay = System.currentTimeMillis() - timerEntry.time;

        TimerList timerList = timeLog.getOrDefault(key, new TimerList(timerEntry.type));

        // Truncate list if entry count exceeds limit
        if (timerList.list.size() >= database.config.getInt("misc.timerLogHistoryLength")) {
            timerList.list.remove(0);
        }

        timerList.list.add(delay);

        timeLog.putIfAbsent(key, timerList);
    }

    /**
     * Returns the latest delay for the provided timer key from the logs.
     *
     * @param key String identifier for timer
     * @return Delay as milliseconds or -1 if not found
     */
    public long getLatest(String key) {
        TimerList timerList = timeLog.get(key);

        if (timerList == null || timerList.list.isEmpty()) {
            return -1;
        }

        return timerList.list.get(timerList.list.size() - 1);
    }

    /**
     * Removes all timers, allowing cycle to finish instantly and the program to exit
     */
    public void stop() {
        delays.clear();
        flagStop = true;
    }

    /**
     * Uploads all latest timer delays to database
     */
    public void uploadDelays(StatusElement statusElement) {
        database.upload.uploadTimers(timeLog, statusElement);
    }

    /**
     * Gets delays from database on program start
     */
    public void getDelays() {
        Map<String, TimerList> timeLog = new HashMap<>();

        database.init.getTimers(timeLog);

        this.timeLog = timeLog;
    }
}
