package poe.manager.entry;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusElement {
    private static Logger logger = LoggerFactory.getLogger(StatusElement.class);
    private Config config;

    public long lastRunTime = System.currentTimeMillis();
    public long twentyFourCounter, sixtyCounter, tenCounter;
    private volatile boolean tenBool, sixtyBool, twentyFourBool;

    public StatusElement (Config config) {
        this.config = config;
    }

    /**
     * Rounds counters on program start
     */
    public void fixCounters() {
        long current = System.currentTimeMillis();

        if (current - tenCounter > 600000) {
            tenCounter += current - tenCounter;
        }

        if (current - sixtyCounter > 3600000) {
            sixtyCounter += current - sixtyCounter;
        }

        if (current - twentyFourCounter > 86400000) {
            if (twentyFourCounter == 0) twentyFourCounter -= config.getInt("entry.counterOffset");
            twentyFourCounter += current - twentyFourCounter;
        }
    }

    /**
     * Raises certain flags after certain intervals
     */
    public void checkFlagStates() {
        long current = System.currentTimeMillis();

        // Run once every 10min
        if (current - tenCounter > 600000) {
            tenCounter += current - tenCounter;
            setTenBool(true);
            logger.info("10 activated");
        }

        // Run once every 60min
        if (current - sixtyCounter > 3600000) {
            sixtyCounter += current - sixtyCounter;
            setSixtyBool(true);
            logger.info("60 activated");
        }

        // Run once every 24h
        if (current - twentyFourCounter > 86400000) {
            if (twentyFourCounter == 0) twentyFourCounter -= config.getInt("entry.counterOffset");
            twentyFourCounter += current - twentyFourCounter;
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
