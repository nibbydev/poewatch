package poe.Managers.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class StatEntry {
    private static final Logger logger = LoggerFactory.getLogger(StatEntry.class);
    private final List<Integer> values = new ArrayList<>();
    private final GroupType groupType;
    private final RecordType recordType;
    private Long startTime;

    public StatEntry(GroupType groupType, RecordType recordType) {
        this.groupType = groupType;
        this.recordType = recordType;
    }

    public void addValue(Integer val) {
        values.add(val);
    }

    public List<Integer> getValues() {
        return values;
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
