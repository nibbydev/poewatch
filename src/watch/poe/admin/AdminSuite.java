package watch.poe.admin;

import java.util.ArrayList;

import watch.poe.Config;
import watch.poe.Main;
import watch.poe.Misc;

public class AdminSuite {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final ArrayList<LogMessage> log = new ArrayList<>(Config.admin_logSize);
    private String lastChangeId = null;

    //------------------------------------------------------------------------------------------------------------
    // Logging methods. All of these have slightly different content and need to be formatted.
    //------------------------------------------------------------------------------------------------------------

    /**
     * Logs messages to internal array and file
     *
     * @param msg Message to be logged
     * @param flair Flair to be attached
     */
    public void log_(String msg, int flair) {
        LogMessage logMsg = new LogMessage(msg, flair);
        System.out.println(logMsg.toString());
        log.add(logMsg);

        if (log.size() > Config.admin_logSize) {
            log.subList(0, log.size() - Config.admin_logSize).clear();
        }
    }

    public void _log(Exception ex, int flair) {
        log_(Misc.stackTraceToString(ex), flair);
    }

    //------------------------------------------------------------------------------------------------------------
    // Statistical methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Updates the change id entry in the database
     *
     * @param newChangeId The latest ChangeId string
     */
    public void setChangeID(String newChangeId) {
        if (lastChangeId == null || !lastChangeId.equals(newChangeId)) {
            lastChangeId = newChangeId;
            Main.DATABASE.updateChangeID(newChangeId);
        }
    }
}
