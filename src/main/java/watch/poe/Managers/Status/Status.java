package poe.Managers.Status;

public class Status {
    private StatusType statusType;
    private volatile boolean active;
    private long counter;

    public Status(StatusType statusType) {
        this.statusType = statusType;
    }

    public boolean isActive() {
        return active;
    }

    public long getCounter() {
        return counter;
    }

    public StatusType getStatusType() {
        return statusType;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }
}
