package watch.poe.manager.admin;

import java.util.Calendar;

public class LogMessage {
    private String timeStamp = timeStamp();
    private String dateStamp = dateStamp();
    private String msg;
    private Flair flair;

    LogMessage (String msg, Flair flair) {
        this.msg = msg;
        this.flair = flair;
    }

    private static String timeStamp() {
        StringBuilder stringBuilder = new StringBuilder();

        // Refresh calendar
        Calendar calendar = Calendar.getInstance();

        // Form [HH:MM:SS]
        stringBuilder.append("[");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.MINUTE)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.SECOND)));
        stringBuilder.append("]");

        // Return [HH:MM:SS]
        return stringBuilder.toString();
    }

    private static String dateStamp() {
        StringBuilder stringBuilder = new StringBuilder();

        // Refresh calendar
        Calendar calendar = Calendar.getInstance();

        // Form [DD.MM.YYYY]
        stringBuilder.append("[");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.MONTH)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%04d", calendar.get(Calendar.YEAR)));
        stringBuilder.append("]");

        // Return [DD.MM.YYYY]
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return dateStamp + timeStamp + genFlair(flair) + " " + msg;
    }

    public static String genFlair(Flair flair) {
        switch (flair) {
            case STATUS:    return "[STATUS]";
            case DEBUG:     return "[DEBUG]";
            case INFO:      return "[INFO]";
            case WARN:      return "[WARN]";
            case ERROR:     return "[ERROR]";
            case CRITICAL:  return "[CRITICAL]";
            case FATAL:     return "[FATAL]";
            default:        return "";
        }
    }
}
