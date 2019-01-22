package poe.Managers.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.toIntExact;

public class Collector {
    private static final Logger logger = LoggerFactory.getLogger(Collector.class);
    private final GroupType groupType;
    private final RecordType recordType;
    private final StatType statType;

    private long creationTime;
    private int latestValue;
    private boolean isNull;
    private int count;
    private long sum;

    public Collector(StatType statType, GroupType groupType, RecordType recordType) {
        reset();

        this.statType = statType;
        this.groupType = groupType;
        this.recordType = recordType;

        if (statType == null || groupType == null || recordType == null) {
            logger.error("Timer types cannot be null");
            throw new RuntimeException();
        }

        if (groupType.equals(GroupType.NONE)) {
            if (!recordType.equals(RecordType.NONE) && !recordType.equals(RecordType.SINGULAR)) {
                logger.error("Cannot use aggregating record types without an aggregating GroupType");
                throw new RuntimeException();
            }
        }
    }

    public boolean canUpload() {
        // Specifically marked as not to be uploaded
        if (recordType.equals(RecordType.NONE)) {
            return false;
        }

        // No values have been added
        if (count == 0) {
            return false;
        }

        // Collector is supposed to run over a period of time and has not finished yet
        if (!recordType.equals(RecordType.SINGULAR) && !isExpired()) {
            return false;
        }

        return true;
    }

    public boolean canUploadTmp() {
        // Not to be uploaded to tmp table
        if (recordType.equals(RecordType.NONE) || recordType.equals(RecordType.SINGULAR)) {
            return false;
        }

        // No values have been added
        if (count == 0) {
            return false;
        }

        if (isExpired()) {
            return false;
        }

        return true;
    }


    public void addValue(Integer val) {
        if (val == null) {
            if (!groupType.equals(GroupType.NONE)) {
                logger.error("Cannot use null value together with group");
                throw new RuntimeException();
            }

            isNull = true;
        } else {
            latestValue = val;
            sum += val;
        }

        count++;
    }

    public Integer getValue() {
        if (isNull) {
            return null;
        }

        if (groupType.equals(GroupType.ADD)) {
            return toIntExact(sum);
        }

        if (groupType.equals(GroupType.AVG)) {
            return toIntExact(sum / count);
        }

        return toIntExact(sum);
    }

    public void reset() {
        creationTime = System.currentTimeMillis();
        latestValue = 0;
        isNull = false;
        count = 0;
        sum = 0L;
    }

    public Long getSum() {
        return isNull ? null : sum;
    }

    public int getCount() {
        return count;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isExpired() {
        if (recordType.equals(RecordType.NONE) || recordType.equals(RecordType.SINGULAR)) {
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
        if (sum == null) {
            isNull = true;
        } else {
            this.sum = sum;
        }
    }

    public void setCount(int count) {
        this.count = count;
    }
}
