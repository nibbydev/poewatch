package poe.Managers.Stat;

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
    M_1, M_10, M_30,
    // Aggregate by the hour
    H_1, H_6, H_12, H_24
}
