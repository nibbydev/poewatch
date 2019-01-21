package poe.Managers.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

import static java.lang.Math.toIntExact;

public class StatEntry {
    private static final Logger logger = LoggerFactory.getLogger(StatEntry.class);
    private final GroupType groupType;
    private final RecordType recordType;
    private final StatType statType;

    private long creationTime = System.currentTimeMillis();
    private Long sum = 0L;
    private int count = 0;
    private int latestValue;

    public StatEntry(StatType statType, GroupType groupType, RecordType recordType) {
        this.statType = statType;
        this.groupType = groupType;
        this.recordType = recordType;
    }

    public void addValue(Integer val) {
        if (val == null || sum == null) {
            sum = null;
        } else {
            latestValue = val;
            sum += val;
        }

        count++;
    }

    public Long getSum() {
        return sum;
    }

    public int getAvg() {
        return toIntExact(sum / count);
    }

    public int getSumAsInt() {
        return toIntExact(sum);
    }

    public int getCount() {
        return count;
    }

    public GroupType getGroupType() {
        return groupType;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isRecord() {
        return !recordType.equals(RecordType.NONE) && !recordType.equals(RecordType.SINGULAR);
    }

    public boolean isExpired() {
        if (!isRecord()) {
            return false;
        }

        long delay;

        switch (recordType) {
            case M_10:
                delay = 10 * 60 * 1000;
                break;
            case M_30:
                delay = 30 * 60 * 1000;
                break;
            case H_1:
                delay = 60 * 60 * 1000;
                break;
            case H_6:
                delay = 6 * 60 * 60 * 1000;
                break;
            case H_12:
                delay = 12 * 60 * 60 * 1000;
                break;
            case H_24:
                delay = 24 * 60 * 60 * 1000;
                break;
            default:
                logger.error("Unhandled switch branch");
                throw new RuntimeException();
        }

        return creationTime - System.currentTimeMillis() > delay;
    }

    public StatType getStatType() {
        return statType;
    }

    public int getLatestValue() {
        return latestValue;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setSum(Long sum) {
        this.sum = sum;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
