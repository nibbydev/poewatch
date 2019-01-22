package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Status.Status;
import poe.Managers.Status.StatusType;

import java.util.Arrays;

public class StatusManager {
    private static Logger logger = LoggerFactory.getLogger(StatusManager.class);

    private final Status[] statuses = {
            new Status(StatusType.M_1),
            new Status(StatusType.M_10),
            new Status(StatusType.M_30),
            new Status(StatusType.M_60),
            new Status(StatusType.H_6),
            new Status(StatusType.H_12),
            new Status(StatusType.H_24)
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
                .filter(i -> current > i.getCounter() + i.getStatusType().asMilli())
                .forEach(i -> {
                    i.setCounter((current / i.getStatusType().asMilli()) * i.getStatusType().asMilli());
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
     * @param statusType
     * @return
     */
    public boolean isBool(StatusType statusType) {
        Status status = Arrays.stream(statuses)
                .filter(i -> i.getStatusType().equals(statusType))
                .findFirst()
                .orElse(null);

        if (status == null) {
            logger.error("Attempting to access non-existent status");
            throw new RuntimeException();
        }

        return status.isActive();
    }
}
