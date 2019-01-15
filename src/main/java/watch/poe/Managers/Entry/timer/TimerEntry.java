package poe.Managers.Entry.timer;

public class TimerEntry {
    public Long time;
    public Timer.TimerType type;

    public TimerEntry(Timer.TimerType type) {
        this.time = System.currentTimeMillis();
        this.type = type;
    }
}