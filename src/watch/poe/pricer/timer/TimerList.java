package watch.poe.pricer.timer;

import java.util.ArrayList;
import java.util.List;

class TimerList {
    public List<Long> list = new ArrayList<>();
    public Timer.TimerType type;

    public TimerList(Timer.TimerType type) {
        this.type = type;
    }
}