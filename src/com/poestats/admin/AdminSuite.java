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
}
