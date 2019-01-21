package poe.Managers.Stat;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ValueEntry {
    private Timestamp time = new Timestamp(System.currentTimeMillis());
    private List<Integer> values = new ArrayList<>();
    private GroupType groupType;
    private boolean record;

    public ValueEntry(GroupType groupType, boolean record) {
        this.groupType = groupType;
        this.record = record;
    }

    public void addValue(Integer val) {
        values.add(val);
    }

    public List<Integer> getValues() {
        return values;
    }

    public Timestamp getTime() {
        return time;
    }

    public GroupType getGroupType() {
        return groupType;
    }

    public boolean isRecord() {
        return record;
    }
}
