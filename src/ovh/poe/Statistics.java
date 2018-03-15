package ovh.poe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Collects various statistics
 */
public class Statistics {
    private final long startTime = System.currentTimeMillis();
    private String latestChangeID;

    private int pullCountTotal = 0;
    private int pullCountFailed = 0;
    private long lastPullTime = 0;

    private int changeIDCycleCounter = 0;
    private long lastSuccessfulPullTime = 0;
    private String status = "UP";
    private String oldStatus = "";

    //////////////////
    // Main methods //
    //////////////////

    /**
     * Called from Worker's download method, enables Statistics to run at <x> download cycles
     */
    public void cycle() {
        // Runs ever <x> cycles
        if (pullCountTotal >= 5) {
            // If there have not been any successful pulls, set status to "unreachable"
            if (pullCountTotal <= pullCountFailed)
                this.status = "UNREACHABLE";
                // Else if there are a few failed pulls, set status to "questionable"
            else if (pullCountFailed > 2)
                this.status = "QUESTIONABLE";
                // Else set status to "up"
            else
                this.status = "UP";

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

    /**
     * Writes latest ChangeID to file
     */
    public void writeChangeID() {
        OutputStream fOut = null;

        // Writes values from statistics to file
        try {
            File fFile = new File("./http/api/data/ChangeID");
            fOut = new FileOutputStream(fFile);
            fOut.write(("{\"changeId\":\"" + latestChangeID + "\",\"status\":\"" + status + "\",\"lastUpdate\":\"" +
                    lastSuccessfulPullTime + "\"}").getBytes());

        } catch (IOException ex) {
            System.out.println("[ERROR] Could not write ./http/api/data/ChangeID");
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

    /**
     * Updates the latest changeID and time
     *
     * @param latestChangeID The newest ChangeID string
     */
    public void setLatestChangeID(String latestChangeID) {
        this.latestChangeID = latestChangeID;
        lastSuccessfulPullTime = System.currentTimeMillis();
        changeIDCycleCounter++;

        // Write to file every <x> cycles
        if (changeIDCycleCounter > 2) {
            changeIDCycleCounter = 0;
            writeChangeID();
        }
    }

    /**
     * Updates status code and status timer based on situation
     *
     * @param status Up/Down/Questionable/Throttled/Unreachable
     */
    public void setStatus(String status) {
        this.status = status;
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void incPullCountFailed() {
        this.pullCountFailed++;
    }

    public long getLastPullTime() {
        return lastPullTime;
    }

    public long getLastSuccessfulPullTime() {
        return lastSuccessfulPullTime;
    }
}
