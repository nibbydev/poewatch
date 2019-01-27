package poe.Managers.Interval;

public class Interval {
    private TimeFrame timeFrame;
    private volatile boolean active;
    private long counter;

    public Interval(TimeFrame timeFrame) {
        this.timeFrame = timeFrame;
    }

    public boolean isActive() {
        return active;
    }

    public long getCounter() {
        return counter;
    }

    public TimeFrame getTimeFrame() {
        return timeFrame;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }
}
