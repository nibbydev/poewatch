package poe.Managers.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Status.StatusType;

import java.util.Arrays;

/**
 * Defines the aggregating time period, such as that one entry is created per cycle (one minute). Has to be used with
 * the correct GroupType otherwise an exception will be thrown.
 */
public enum RecordType {
    // Do not record entry in database
    NONE,
    // Record all entries separately
    SINGULAR,
    // Aggregate by the minute
    M_10, M_30,
    // Aggregate by the hour
    H_1, H_6, H_12, H_24;

    private static final Logger logger = LoggerFactory.getLogger(RecordType.class);

    public StatusType toStatusType() {
        StatusType status = Arrays.stream(StatusType.values())
                .filter(i -> i.name().equals(this.name()))
                .findFirst()
                .orElse(null);

        if (status == null) {
            logger.error("Non-compatible types");
            throw new RuntimeException();
        }

        return status;
    }
}
