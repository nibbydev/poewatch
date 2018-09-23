package watch.poe.league;


import watch.poe.Config;
import watch.poe.Main;
import watch.poe.admin.Flair;
import watch.poe.league.derserializer.BaseLeague;
import watch.poe.league.derserializer.Rule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LeagueEntry {
    private String name, display, startAt, endAt;
    private boolean event, active, hardcore;
    private int id;

    public LeagueEntry(BaseLeague baseLeague) {
        name = baseLeague.getName();
        startAt = baseLeague.getStartAt();
        endAt = baseLeague.getEndAt();
        event = baseLeague.isEvent();

        for (Rule rule : baseLeague.getRules()) {
            if (rule.getName().equals("Hardcore")) {
                hardcore = true;
                break;
            }
        }
    }

    public LeagueEntry(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("id");
        name = resultSet.getString("name");
        active = resultSet.getInt("active") == 1;
        event = resultSet.getInt("event") == 1;
        hardcore = resultSet.getInt("hardcore") == 1;
        display = resultSet.getString("display");
        startAt = resultSet.getString("start");
        endAt = resultSet.getString("end");
    }

    /**
     * Finds number of days league has been active for
     *
     * @return Current league length or 0 on error
     */
    public int getElapsedDays() {
        Date startDate = startAt == null ? null : parseDate(startAt);
        Date currentDate = new Date();

        if (startDate == null) {
            return 0;
        } else {
            long startDifference = Math.abs(currentDate.getTime() - startDate.getTime());
            return (int)(startDifference / Config.league_millisecondsInDay);
        }
    }

    /**
     * Finds total days league will elapse
     *
     * @return Total league length or -1 on error
     */
    public int getTotalDays() {
        Date startDate = startAt == null ? null : parseDate(startAt);
        Date endDate = endAt == null ? null : parseDate(endAt);

        if (startDate == null || endDate == null) {
            return  -1;
        } else {
            long totalDifference = Math.abs(endDate.getTime() - startDate.getTime());
            return (int) (totalDifference / Config.league_millisecondsInDay);
        }
    }

    /**
     * Converts string date found in league api to Date object
     *
     * @param date ISO 8601 standard yyyy-MM-dd'T'HH:mm:ss'Z' date
     * @return Created Date object
     */
    private Date parseDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat(Config.league_timeFormat, Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return format.parse(date);
        } catch (ParseException ex) {
            Main.ADMIN.logException(ex, Flair.ERROR);
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getEndAt() {
        return endAt;
    }

    public String getStartAt() {
        return startAt;
    }

    public String getDisplay() {
        return display;
    }

    public Integer getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isEvent() {
        return event;
    }

    public boolean isHardcore() {
        return hardcore;
    }
}
