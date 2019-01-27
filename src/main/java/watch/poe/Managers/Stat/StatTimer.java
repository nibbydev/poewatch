package poe.Managers.Stat;

public class StatTimer {
    private long startTime = System.currentTimeMillis();
    private StatType statType;

    public StatTimer(StatType statType) {
        this.statType = statType;
    }

    public StatType getStatType() {
        return statType;
    }

    public long getStartTime() {
        return startTime;
    }
}
