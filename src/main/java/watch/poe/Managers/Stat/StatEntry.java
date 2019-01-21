package poe.Managers.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.toIntExact;

public class StatEntry {
    private static final Logger logger = LoggerFactory.getLogger(StatEntry.class);
    private final GroupType groupType;
    private final RecordType recordType;

    private Long startTime;
    private Long sum = 0L;
    private int count = 0;

    public StatEntry(GroupType groupType, RecordType recordType) {
        this.groupType = groupType;
        this.recordType = recordType;
    }

    public void addValue(Integer val) {
        sum = val == null ? null : sum + val;
        count++;
    }

    public void addValues(StatEntry otherEntry) {
        if (!otherEntry.groupType.equals(groupType)) {
            logger.error("StatEntry aggregation types do not match");
            throw new RuntimeException();
        }

        if (otherEntry.sum == null) {
            sum = null;
        } else {
            sum += otherEntry.sum;
        }

        count += otherEntry.count;
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

    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public void resetTimer() {
        this.startTime = null;
    }

    public int clkTimer() {
        if (startTime == null) {
            logger.error("Cannot subtract delay when timer has not been started!");
            throw new NullPointerException();
        }

        return (int) (System.currentTimeMillis() - startTime);
    }
}
