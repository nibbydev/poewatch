package watch.poe.pricer.timer;

class TimerEntry {
    public Long time;
    public Timer.TimerType type;

    public TimerEntry(Timer.TimerType type) {
        this.time = System.currentTimeMillis();
        this.type = type;
    }
}