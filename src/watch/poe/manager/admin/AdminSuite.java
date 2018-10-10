package watch.poe.manager.admin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import watch.poe.Config;
import watch.poe.Main;

public class AdminSuite {
    private final ArrayList<LogMessage> log = new ArrayList<>(Config.admin_logSize);
    private String lastChangeId = null;

    /**
     * Logs messages
     *
     * @param msg Message to be logged
     * @param flair Flair to be attached
     */
    public void log(String msg, Flair flair) {
        LogMessage logMsg = new LogMessage(msg, flair);

        System.out.println(logMsg.toString());

        log.add(logMsg);

        if (log.size() > Config.admin_logSize) {
            log.subList(0, log.size() - Config.admin_logSize).clear();
        }
    }

    /**
     * Logs exception
     *
     * @param ex Exception to be logged
     * @param flair Flair to be attached
     */
    public void logException(Exception ex, Flair flair) {
        log(stackTraceToString(ex), flair);
    }

    /**
     * Converts exception stack trace to string
     *
     * @param ex Exception to convert to string
     * @return Exception as string
     */
    private static String stackTraceToString(Exception ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

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
