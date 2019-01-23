package poe.Managers.Status;

public class Status {
    private TimeFrame timeFrame;
    private volatile boolean active;
    private long counter;

    public Status(TimeFrame timeFrame) {
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
