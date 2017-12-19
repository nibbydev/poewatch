package com.sanderh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Statistics {
    //  Name: Statistics
    //  Date created: 16.12.2017
    //  Last modified: 19.12.2017
    //  Description: Object meant to be used as a static statistics collector

    private final long startTime = System.currentTimeMillis();
    private String latestChangeID;

    private int pullCountTotal = 0;
    private int pullCountFailed = 0;
    private int pullCountDuplicate = 0;
    private long lastPullTime = 0;

    private int itemCountCorrupted = 0;
    private int itemCountUnidentified = 0;
    private int[] itemCountFrameType = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private int changeIDCycleCounter = 0;
    private long lastStatusChangeWhen = 0;
    private String status = "up";
    private String oldStatus = "";

    //////////////////
    // Main methods //
    //////////////////

    public void cycle() {
        //  Name: cycle
        //  Date created: 18.12.2017
        //  Last modified: 19.12.2017
        //  Description: Called from Worker's download method, enables Statistics to run at <x> download cycles

        // Runs ever <x> cycles
        if (pullCountTotal >= 5) {
            // If there have not been any successful pulls, set status to "unreachable"
            if (pullCountTotal <= pullCountFailed)
                setStatus(4);
            // Else if there are a few failed pulls, set status to "questionable"
            else if (pullCountFailed > 2)
                setStatus(2);
            // Else set status to "up"
            else
                setStatus(0);

            // Write info to file if there has been a change
            if (!oldStatus.equals(status))
                writeChangeID();

            // (Re)set data
            oldStatus = status;
            pullCountTotal = 0;
            pullCountFailed = 0;
        }

        // Increment pull counter and update last pull time
        lastPullTime = System.currentTimeMillis();
        this.pullCountTotal++;
    }

    public void parseItem(Item item) {
        //  Name: parseItem
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Checks some item values, adds result to counters

        if (item.isCorrupted()) itemCountCorrupted++;
        if (!item.isIdentified()) itemCountUnidentified++;
        itemCountFrameType[item.getFrameType()]++;
    }

    public void writeChangeID() {
        //  Name: writeChangeID
        //  Date created: 18.12.2017
        //  Last modified: 18.12.2017
        //  Description: Writes latest ChangeID to file

        OutputStream fOut = null;

        // Writes values from statistics to file
        try {
            File fFile = new File("./http/data/ChangeID");
            fOut = new FileOutputStream(fFile);
            fOut.write(("{\"change_id\":\"" + latestChangeID + "\",\"status\":\"" + status + "\",\"lastUpdate\":\"" +
                    lastStatusChangeWhen + "\"}").getBytes());

        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write ./http/data/ChangeID");
            ex.printStackTrace();
        } finally {
            try {
                if (fOut != null) {
                    fOut.flush();
                    fOut.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    ////////////////////////
    // Special    Setters //
    ////////////////////////

    public void setLatestChangeID(String latestChangeID) {
        //  Name: setLatestChangeID
        //  Date created: 16.12.2017
        //  Last modified: 19.12.2017
        //  Description: Updates the latest changeID and time

        this.latestChangeID = latestChangeID;
        lastStatusChangeWhen = System.currentTimeMillis();
        changeIDCycleCounter++;

        // Write to file every <x> cycles
        if (changeIDCycleCounter > 2) {
            changeIDCycleCounter = 0;
            writeChangeID();
        }
    }

    public void setStatus(int status) {
        //  Name: setStatus
        //  Date created: 18.12.2017
        //  Last modified: 18.12.2017
        //  Description: Updates status code and status timer

        switch (status) {
            case 0:
                this.status = "up";
                break;
            case 1:
                this.status = "down";
                break;
            case 2:
                this.status = "questionable";
                break;
            case 3:
                this.status = "throttled";
                break;
            case 4:
                this.status = "unreachable";
                break;
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void incPullCountFailed() {
        this.pullCountFailed++;
    }

    public void incPullCountDuplicate() {
        this.pullCountDuplicate++;
    }

    public long getLastPullTime() {
        return lastPullTime;
    }
}
