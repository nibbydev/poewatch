package com.poestats.pricer;

public class StatusElement {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    public long lastRunTime = System.currentTimeMillis();
    public long twentyFourCounter, sixtyCounter, tenCounter;
    private volatile boolean tenBool, sixtyBool, twentyFourBool;

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
