package poe.Managers.Stat;

public class StatTimer {
    private long startTime = System.currentTimeMillis();
    private StatType type;

    public StatTimer(StatType type) {
        this.type = type;
    }

    public StatType getType() {
        return type;
    }

    public long getStartTime() {
        return startTime;
    }
}
