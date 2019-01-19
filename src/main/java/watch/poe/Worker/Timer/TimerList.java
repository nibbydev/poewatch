package poe.Worker.Timer;

import java.util.ArrayList;
import java.util.List;

public class TimerList {
    public List<Long> list = new ArrayList<>();
    public Timer.TimerType type;

    public TimerList(Timer.TimerType type) {
        this.type = type;
    }
}