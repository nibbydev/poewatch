package poe.Managers.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Status.StatusType;

import static java.lang.Math.toIntExact;

public class Collector {
    private static final Logger logger = LoggerFactory.getLogger(Collector.class);
    private final GroupType groupType;
    private final RecordType recordType;
    private final StatType statType;

    private long creationTime;
    private int latestValue = 0;
    private boolean isNull = false;
    private int count = 0;
    private long sum = 0L;

    public Collector(StatType statType, GroupType groupType, RecordType recordType) {
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

        // Set default creationTime
        if (recordType.equals(RecordType.NONE) || recordType.equals(RecordType.SINGULAR)) {
            creationTime = StatusType.M_1.getCurrent();
        } else {
            creationTime = recordType.toStatusType().getCurrent();
        }
    }

    public boolean isCollectingOverTime() {
        return !recordType.equals(RecordType.NONE) && !recordType.equals(RecordType.SINGULAR);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime >= recordType.toStatusType().asMilli();
    }

    public RecordType getRecordType() {
        return recordType;
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
        if (recordType.equals(RecordType.NONE) || recordType.equals(RecordType.SINGULAR)) {
            creationTime = StatusType.M_1.getNext();
        } else {
            creationTime = recordType.toStatusType().getNext();
        }

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
