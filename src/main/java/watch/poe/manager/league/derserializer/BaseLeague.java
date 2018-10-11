package poe.manager.league.derserializer;

import java.util.List;

public class BaseLeague {
    private String id;
    private boolean event;
    private String startAt, endAt;
    private List<Rule> rules;

    public String getName() {
        return id;
    }

    public String getEndAt() {
        return endAt;
    }

    public String getStartAt() {
        return startAt;
    }

    public boolean isEvent() {
        return event;
    }

    public List<Rule> getRules() {
        return rules;
    }
}
