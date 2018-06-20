package com.poestats.pricer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StatusElement {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    public long lastRunTime = System.currentTimeMillis();
    public long twentyFourCounter, sixtyCounter, tenCounter;
    private volatile boolean tenBool, sixtyBool, twentyFourBool;

    //------------------------------------------------------------------------------------------------------------
    // Methods
    //------------------------------------------------------------------------------------------------------------

    public void load(ResultSet resultSet) throws SQLException {
        while (resultSet.next()) {
            switch (resultSet.getString("name")) {
                case "24":
                    twentyFourCounter = resultSet.getLong("value");
                    break;
                case "60":
                    sixtyCounter = resultSet.getLong("value");
                    break;
                case "10":
                    tenCounter = resultSet.getLong("value");
                    break;
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public boolean isTwentyFourBool() {
        return twentyFourBool;
    }

    public boolean isSixtyBool() {
        return sixtyBool;
    }

    public boolean isTenBool() {
        return tenBool;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setTwentyFourBool(boolean twentyFourBool) {
        this.twentyFourBool = twentyFourBool;
    }

    public void setSixtyBool(boolean sixtyBool) {
        this.sixtyBool = sixtyBool;
    }

    public void setTenBool(boolean tenBool) {
        this.tenBool = tenBool;
    }
}
