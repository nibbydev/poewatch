package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Status.Status;
import poe.Managers.Status.TimeFrame;

import java.util.Arrays;

public class StatusManager {
    private static Logger logger = LoggerFactory.getLogger(StatusManager.class);

    private final Status[] statuses = {
            new Status(TimeFrame.M_1),
            new Status(TimeFrame.M_10),
            new Status(TimeFrame.M_30),
            new Status(TimeFrame.M_60),
            new Status(TimeFrame.H_6),
            new Status(TimeFrame.H_12),
            new Status(TimeFrame.H_24)
    };

    public StatusManager() {
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

        // Find all status entries that should be activated
        Arrays.stream(statuses)
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
        Arrays.stream(statuses).forEach(i -> i.setActive(false));
    }

    /**
     * Returns the flag related to the provided Status
     *
     * @param timeFrame
     * @return
     */
    public boolean isBool(TimeFrame timeFrame) {
        Status status = Arrays.stream(statuses)
                .filter(i -> i.getTimeFrame().equals(timeFrame))
                .findFirst()
                .orElse(null);

        if (status == null) {
            logger.error("Attempting to access non-existent status");
            throw new RuntimeException();
        }

        return status.isActive();
    }
}
