package poe.Managers.Stat;

import java.sql.Timestamp;

public class ValueEntry {
    private Timestamp time = new Timestamp(System.currentTimeMillis());
    private boolean record;
    private Integer value;

    public ValueEntry(Integer value, boolean record) {
        this.record = record;
        this.value = value;
    }

    public ValueEntry(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public Timestamp getTime() {
        return time;
    }

    public boolean isRecord() {
        return record;
    }
}
