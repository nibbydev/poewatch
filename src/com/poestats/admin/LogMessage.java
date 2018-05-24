package com.poestats.admin;

import com.poestats.Misc;

public class LogMessage {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String timeStamp = Misc.timeStamp();
    private String dateStamp = Misc.dateStamp();
    private String msg;
    private int flair;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    LogMessage (String msg, int flair) {
        this.msg = msg;
        this.flair = flair;
    }

    //------------------------------------------------------------------------------------------------------------
    // Methods
    //------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return dateStamp + timeStamp + getFlair(flair) + " " + msg;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public static String getFlair(int flair) {
        switch (flair) {
            case -1:
                return "[STATUS]";
            case 0:
                return "[DEBUG]";
            case 1:
                return "[INFO]";
            case 2:
                return "[WARN]";
            case 3:
                return "[ERROR]";
            case 4:
                return "[CRITICAL]";
            case 5:
                return "[FATAL]";
            default:
                return "";
        }
    }

    public int getFlair() {
        return flair;
    }

    public String getDateStamp() {
        return dateStamp;
    }

    public String getMsg() {
        return msg;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setDateStamp(String dateStamp) {
        this.dateStamp = dateStamp;
    }

    public void setFlair(int flair) {
        this.flair = flair;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
