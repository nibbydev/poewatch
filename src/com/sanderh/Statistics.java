package com.sanderh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Statistics {
    //  Name: Statistics
    //  Date created: 16.12.2017
    //  Last modified: 18.12.2017
    //  Description: Object meant to be used as a static statistics collector

    private final long startTime = System.currentTimeMillis();
    private String latestChangeID;

    private int pullCountTotal = 0;
    private int pullCountFailed = 0;
    private int pullCountDuplicate = 0;

    private int itemCountCorrupted = 0;
    private int itemCountUnidentified = 0;
    private int[] itemCountFrameType = {0,0,0,0,0,0,0,0,0,0};

    private int cycleCounter = 0;
    private String status = "up";

    //////////////////
    // Main methods //
    //////////////////

    public void parseItem(Item item) {
        //  Name: parseItem
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Checks some item values, adds result to counters

        if(item.isCorrupted()) itemCountCorrupted++;
        if(!item.isIdentified()) itemCountUnidentified++;
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
            fOut.write(("{\"change_id\":\"" + latestChangeID + "\",\"status\":\"" + status + "\"}").getBytes());

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

    ///////////////////////
    // Special    Setter //
    ///////////////////////

    public void setLatestChangeID(String latestChangeID) {
        //  Name: setLatestChangeID
        //  Date created: 16.12.2017
        //  Last modified: 18.12.2017
        //  Description: Updates the latest changeID but also runs the code every x seconds

        this.latestChangeID = latestChangeID;

        if (cycleCounter >= 1) {
            writeChangeID();
            cycleCounter = 0;
        } else {
            cycleCounter++;
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void incPullCountTotal() {
        this.pullCountTotal++;
    }

    public void incPullCountFailed() {
        this.pullCountFailed++;
    }

    public void incPullCountDuplicate() {
        this.pullCountDuplicate++;
    }

    public void setStatus(int status) {
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
        }
    }
}
