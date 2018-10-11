package poe.manager.entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Config;

public class StatusElement {
    public long lastRunTime = System.currentTimeMillis();
    public long twentyFourCounter, sixtyCounter, tenCounter;
    private volatile boolean tenBool, sixtyBool, twentyFourBool;

    private static Logger logger = LoggerFactory.getLogger(StatusElement.class);

    /**
     * Rounds counters on program start
     */
    public void fixCounters() {
        long current = System.currentTimeMillis();

        if (current - tenCounter > Config.entryController_tenMS) {
            long gap = (current - tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            tenCounter += gap;
        }

        if (current - sixtyCounter > Config.entryController_sixtyMS) {
            long gap = (current - sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            sixtyCounter += gap;
        }

        if (current - twentyFourCounter > Config.entryController_twentyFourMS) {
            if (twentyFourCounter == 0) twentyFourCounter -= Config.entryController_counterOffset;
            long gap = (current - twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS;
            twentyFourCounter += gap;
        }
    }

    /**
     * Raises certain flags after certain intervals
     */
    public void checkFlagStates() {
        long current = System.currentTimeMillis();

        // Run once every 10min
        if (current - tenCounter > Config.entryController_tenMS) {
            tenCounter += (current - tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            setTenBool(true);
            logger.info("10 activated");
        }

        // Run once every 60min
        if (current - sixtyCounter > Config.entryController_sixtyMS) {
            sixtyCounter += (current - sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            setSixtyBool(true);
            logger.info("60 activated");
        }

        // Run once every 24h
        if (current - twentyFourCounter > Config.entryController_twentyFourMS) {
            if (twentyFourCounter == 0) {
                twentyFourCounter -= Config.entryController_counterOffset;
            }

            twentyFourCounter += (current - twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS;
            setTwentyFourBool(true);
            logger.info("24 activated");
        }
    }


    //------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    //------------------------------------------------------------------------------------------------------------

    public long getTenRemainMin() {
        return 10 - (System.currentTimeMillis() - tenCounter) / 60000;
    }

    public long getSixtyRemainMin() {
        return 60 - (System.currentTimeMillis() - sixtyCounter) / 60000;
    }

    public long getTwentyFourRemainMin() {
        return 1440 - (System.currentTimeMillis() - twentyFourCounter) / 60000;
    }

    public boolean isTwentyFourBool() {
        return twentyFourBool;
    }

    public boolean isSixtyBool() {
        return sixtyBool;
    }

    public boolean isTenBool() {
        return tenBool;
    }

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
