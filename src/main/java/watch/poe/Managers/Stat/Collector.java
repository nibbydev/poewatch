package poe.Managers.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Status.TimeFrame;

import static java.lang.Math.toIntExact;

public class Collector {
    private static final Logger logger = LoggerFactory.getLogger(Collector.class);
    private final GroupType groupType;
    private final TimeFrame collectionPeriod;
    private final StatType statType;

    private long creationTime, insertTime;
    private Integer historySize;
    private int latestValue = 0;
    private boolean isValueNull = false;
    private int count = 0;
    private long sum = 0L;

    public Collector(StatType statType, GroupType groupType, TimeFrame collectionPeriod, Integer historySize) {
        this.statType = statType;
        this.groupType = groupType;

        // Either can be null
        this.collectionPeriod = collectionPeriod;
        this.historySize = historySize;

        if (statType == null || groupType == null) {
            logger.error("Timer types cannot be null");
            throw new RuntimeException();
        }

        // Set defaults
        reset();
    }

    public boolean isRecorded() {
        return collectionPeriod != null;
    }

    public boolean hasValues() {
        return count > 0;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime >= collectionPeriod.asMilli();
    }

    public boolean isValueNull() {
        return isValueNull;
    }

    public void addValue(Integer val) {
        if (val == null) {
            isValueNull = true;
        } else {
            latestValue = val;
            sum += val;
        }

        count++;
    }

    public Integer getValue() {
        if (groupType.equals(GroupType.COUNT)) {
            return toIntExact(count);
        }

        if (isValueNull) {
            return null;
        }

        if (groupType.equals(GroupType.SUM)) {
            return toIntExact(sum);
        }

        if (groupType.equals(GroupType.AVG)) {
            return toIntExact(sum / count);
        }

        return toIntExact(sum);
    }

    public void reset() {
        if (collectionPeriod == null) {
            creationTime = TimeFrame.M_1.getCurrent();
            insertTime = TimeFrame.M_1.getNext();
        } else {
            creationTime = collectionPeriod.getCurrent();
            insertTime = collectionPeriod.getNext();
        }

        latestValue = 0;
        isValueNull = false;
        count = 0;
        sum = 0L;
    }

    public long getSum() {
        return sum;
    }

    public int getCount() {
        return count;
    }

    public StatType getStatType() {
        return statType;
    }

    public int getLatestValue() {
        return latestValue;
    }

    public void setSum(Long sum) {
        if (sum == null) {
            isValueNull = true;
        } else {
            this.sum = sum;
        }
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Integer getHistorySize() {
        return historySize;
    }



    public long getCreationTime() {
        return creationTime;
    }

    public long getInsertTime() {
        return insertTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
        insertTime = creationTime + collectionPeriod.asMilli();
    }
}
