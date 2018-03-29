package ovh.poe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;

public class AdminSuite {
    private static class LogMessage {
        String timeStamp = timeStamp();
        String dateStamp = dateStamp();
        String msg;
        int flair;

        LogMessage (String msg, int flair) {
            this.msg = msg;
            this.flair = flair;
        }

        @Override
        public String toString() {
            return dateStamp + timeStamp + getFlair(flair) + " " + msg;
        }
    }

    public static class ChangeIDElement {
        public String changeId, status;
        public long lastUpdate;

        public void setStatus (int status) {
            switch (status) {
                default:
                case 0:
                    this.status = "up";
                    break;
                case 1:
                    this.status = "questionable";
                    break;
                case 2:
                    this.status = "unreachable";
                    break;
                case 3:
                    this.status = "down";
                    break;
            }
        }
    }

    private ArrayList<LogMessage> log = new ArrayList<>(2048);
    private final long startTime = System.currentTimeMillis();
    private final Gson gson = Main.getGson();

    public ChangeIDElement changeIDElement = new ChangeIDElement();
    private String oldStatus;

    public int pullCountTotal;
    public int pullCountError;
    public long latestPullTime;


    //------------------------------------------------------------------------------------------------------------
    // Logging methods. All of these have slightly different content and need to be formatted.
    //------------------------------------------------------------------------------------------------------------

    public void log_(String msg, int flair) {
        LogMessage logMsg = new LogMessage(msg, flair);
        System.out.println(logMsg.toString());
        log.add(logMsg);

        // Limit log messages
        if (log.size() > 2048) log.subList(0, log.size() - 2048).clear();
    }

    //------------------------------------------------------------------------------------------------------------
    // Statistical methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Called from Worker's download method, enables statistics to run every <x> download cycles
     */
    public void workerCycle() {
        // Runs ever <x> cycles
        if (pullCountTotal > 10) {
            if (pullCountError > pullCountTotal) changeIDElement.setStatus(2);
            else if (pullCountError > 2) changeIDElement.setStatus(1);
            else changeIDElement.setStatus(0);

            // Write info to file if there has been a change
            if (oldStatus == null || !oldStatus.equals(changeIDElement.status)) {
                oldStatus = changeIDElement.status;
                saveChangeID();
            }

            // (Re)set data
            pullCountTotal = pullCountError = 0;
        }
    }

    /**
     * Saves data to file on program exit
     */
    public void saveChangeID() {
        // Saves change id to file
        File file = new File("./data/changeID.json");
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            gson.toJson(changeIDElement, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to changeID.json", 3);
            ex.printStackTrace();
        }
    }

    /**
     * Updates the latest changeID and latest pull time
     *
     * @param changeID The latest ChangeID string
     */
    public void setChangeID(String changeID) {
        changeIDElement.lastUpdate = System.currentTimeMillis();

        if (changeIDElement.changeId == null || !changeIDElement.changeId.equals(changeID)) {
            changeIDElement.changeId = changeID;
            saveChangeID();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Backups. Original code by Deron Eriksson taken on 29 March, 2018 from
    // http://www.avajava.com/tutorials/lessons/how-do-i-zip-a-directory-and-all-its-contents.html
    //------------------------------------------------------------------------------------------------------------

    public void backupOutput() {
        File directoryToZip = new File("./data/output");
        String outputFileName = "./backups/output " + dateStamp() + timeStamp().replaceAll(":", ".") + ".zip";

        List<File> fileList = new ArrayList<>();
        getAllFiles(directoryToZip, fileList);
        writeZipFile(outputFileName, directoryToZip, fileList);
    }

    private void getAllFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles();

        for (File file : files) {
            fileList.add(file);

            if (file.isDirectory()) {
                getAllFiles(file, fileList);
            }
        }
    }

    private void writeZipFile(String fileName, File directoryToZip, List<File> fileList) {

        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }

            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws IOException {
        FileInputStream fis = new FileInputStream(file);

        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                file.getCanonicalPath().length());
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    //------------------------------------------------------------------------------------------------------------
    // Utility methods
    //------------------------------------------------------------------------------------------------------------

    static String timeStamp() {
        StringBuilder stringBuilder = new StringBuilder();

        // Refresh calendar
        Calendar calendar = Calendar.getInstance();

        // Form [HH:MM:SS]
        stringBuilder.append("[");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        stringBuilder.append(":");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.MINUTE)));
        stringBuilder.append(":");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.SECOND)));
        stringBuilder.append("]");

        // Return [HH:MM:SS]
        return stringBuilder.toString();
    }

    static String dateStamp() {
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

    static String getFlair(int flair) {
        switch (flair) {
            case -1:
                return "[STATUS]";
            case 0:
                return "[DEBUG]";
            case 1:
                return "[INFO]";
            case 2:
                return "[WARN]";
            case 3:
                return "[ERROR]";
            case 4:
                return "[CRITICAL]";
            case 5:
                return "[FATAL]";
            default:
                return "";
        }
    }
}
