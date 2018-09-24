package watch.poe.pricer;

import watch.poe.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timer {
    private Map<String, List<Long>> timeLog = new HashMap<>();
    private Map<String, Long> timers = new HashMap<>();

    public void start(String type) {
        timers.put(type, System.nanoTime());
    }

    public void clk(String type) {
        long time = System.nanoTime() - timers.remove(type);

        List<Long> timerList = timeLog.getOrDefault(type, new ArrayList<>());

        if (timerList.size() >= Config.timerLogHistoryLength) {
            timerList.remove(0);
        }

        timerList.add(time);

        timeLog.putIfAbsent(type, timerList);
    }

    public long getLatestMS(String type) {
        List<Long> timerList = timeLog.getOrDefault(type, null);

        if (timerList == null || timerList.isEmpty()) {
            return -1;
        }

        return timerList.get(timerList.size() - 1) / 1000000;
    }

    public long getLatestNS(String type) {
        List<Long> timerList = timeLog.getOrDefault(type, null);

        if (timerList == null || timerList.isEmpty()) {
            return -1;
        }

        return timerList.get(timerList.size() - 1);
    }
}
