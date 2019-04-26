package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Interval.Interval;
import poe.Managers.Interval.TimeFrame;

import java.util.Arrays;

/**
 * Deals with delays, intervals and precise timings.
 */
public class IntervalManager {
    private static Logger logger = LoggerFactory.getLogger(IntervalManager.class);

    /**
     * Currently available intervals
     */
    private final Interval[] intervals = {
            new Interval(TimeFrame.M_1),
            new Interval(TimeFrame.M_10),
            new Interval(TimeFrame.M_30),
            new Interval(TimeFrame.M_60),
            new Interval(TimeFrame.H_6),
            new Interval(TimeFrame.H_12),
            new Interval(TimeFrame.H_24)
    };

    public IntervalManager() {
        // Set counters to their default state upon program start
        checkFlagStates();
        // Reset flags in case any were raised
        resetFlags();
    }

    /**
     * Raises certain flags after certain intervals
     */
    public void checkFlagStates() {
        long current = System.currentTimeMillis();

        // Find all interval entries that should be activated
        Arrays.stream(intervals)
                .filter(i -> current > i.getCounter() + i.getTimeFrame().asMilli())
                .forEach(i -> {
                    i.setCounter((current / i.getTimeFrame().asMilli()) * i.getTimeFrame().asMilli());
                    i.setActive(true);
                });
    }

    /**
     * Sets all flags as false
     */
    public void resetFlags() {
        Arrays.stream(intervals).forEach(i -> i.setActive(false));
    }

    /**
     * Returns the flag related to the provided time frame
     *
     * @param timeFrame
     * @return
     */
    public boolean isBool(TimeFrame timeFrame) {
        Interval interval = Arrays.stream(intervals)
                .filter(i -> i.getTimeFrame().equals(timeFrame))
                .findFirst()
                .orElse(null);

        if (interval == null) {
            logger.error("Attempting to access non-existent time frame");
            throw new RuntimeException();
        }

        return interval.isActive();
    }
}
