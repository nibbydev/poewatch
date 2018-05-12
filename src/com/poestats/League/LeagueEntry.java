package com.poestats.League;


import com.poestats.Main;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LeagueEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    // For deserializer
    private String id, startAt, endAt;
    // User-defined variables
    private int elapse, remain, total;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void parse() {
        findDaysSince();
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Calculates how many days a league has been active for, how many days until the end of a league and how many days
     * the league will run;
     */
    private void findDaysSince() {
        Date startDate = startAt == null ? null : parseDate(startAt);
        Date endDate = endAt == null ? null : parseDate(endAt);
        Date currentDate = new Date();

        if (startDate == null || endDate == null) {
            total = -1;
        } else {
            long totalDifference = Math.abs(endDate.getTime() - startDate.getTime());
            total = (int) (totalDifference / (24 * 60 * 60 * 1000));
        }

        if (startDate == null) {
            elapse = 0;
        } else {
            long startDifference = Math.abs(currentDate.getTime() - startDate.getTime());
            elapse = (int)(startDifference / (24 * 60 * 60 * 1000));
        }

        if (endDate == null) {
            remain = -1;
        } else {
            long endDifference = Math.abs(endDate.getTime() - currentDate.getTime());
            remain = (int) (endDifference / (24 * 60 * 60 * 1000));
        }
    }

    /**
     * Converts string date found in league api to Date object
     *
     * @param date ISO 8601 standard yyyy-MM-dd'T'HH:mm:ss'Z' date
     * @return Created Date object
     */
    private Date parseDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return format.parse(date);
        } catch (ParseException ex) {
            Main.ADMIN._log(ex, 3);
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public int getElapse() {
        return elapse;
    }

    public int getTotal() {
        return total;
    }
}
