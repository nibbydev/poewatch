package poe.Managers.League;

import java.util.Arrays;

public class BaseLeague {
    private String id;
    private boolean event;
    private String startAt, endAt;
    private Rule[] rules;

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

    public Rule[] getRules() {
        return rules;
    }

    public boolean isHardcore() {
        return Arrays.stream(rules).anyMatch(i -> i.getName().equals("Hardcore"));
    }

    public static class Rule {
        private String id;
        private String name;

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }
    }
}
