package com.poestats.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.poestats.Config;
import com.poestats.Main;
import com.poestats.Misc;

import java.io.*;

public class AdminSuite {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private final ArrayList<LogMessage> log = new ArrayList<>(Config.admin_logSize);
    private final ChangeIDElement changeIDElement = new ChangeIDElement();
    private final Gson gson = Main.getGson();

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
     * Updates the latest changeID and latest pull time
     *
     * @param changeID The latest ChangeID string
     */
    public void setChangeID(String changeID) {
        String oldChangeID = changeIDElement.getChangeId();
        changeIDElement.update(changeID);

        if (!changeIDElement.getChangeId().equals(oldChangeID)) {
            Main.DATABASE.updateChangeID(changeID);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Backups
    //------------------------------------------------------------------------------------------------------------

    public void backup(File input, String prefix) {
        String stamp = Misc.dateStamp() + Misc.timeStamp();
        File output = new File(Config.folder_backups, prefix + "_" + stamp + ".zip");

        if (input.isFile()) {
            zipFile(input, output);
        } else {
            List<File> fileList = new ArrayList<>();
            Misc.getAllFiles(input, fileList);
            zipFolder(output, fileList);
        }

        Main.ADMIN.log_("Created backup of: " + input.getPath(), 0);
    }

    private void zipFile(File input, File output) {
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
            FileInputStream fileInputStream = new FileInputStream(input);

            zipOutputStream.putNextEntry(new ZipEntry(input.getName()));

            byte[] bytes = new byte[Config.admin_zipBufferSize];
            int length;
            while ((length = fileInputStream.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }

            zipOutputStream.closeEntry();
            zipOutputStream.close();
            fileInputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void zipFolder(File output, List<File> inputFileList) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(output);
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

            for (File file : inputFileList) {
                if (!file.isDirectory()) {
                    addToZip(file, zipOutputStream);
                }
            }

            zipOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToZip(File input, ZipOutputStream zipOutputStream) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(input);

        String localZipPath = input.getCanonicalPath().substring(Config.folder_data.getCanonicalPath().length() + 1);
        ZipEntry zipEntry = new ZipEntry(localZipPath);
        zipOutputStream.putNextEntry(zipEntry);

        int length;
        byte[] bytes = new byte[Config.admin_zipBufferSize];
        while ((length = fileInputStream.read(bytes)) >= 0) {
            zipOutputStream.write(bytes, 0, length);
        }

        zipOutputStream.closeEntry();
        fileInputStream.close();
    }
}
