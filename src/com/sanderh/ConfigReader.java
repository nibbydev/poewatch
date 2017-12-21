package com.sanderh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ConfigReader {
    //  Name: ConfigReader
    //  Date created: 20.12.2017
    //  Last modified: 20.12.2017
    //  Description: Object that's used to manage constants

    public int timeZoneOffset = 2;
    public int workerLimit = 5;
    public int downloadChunkSize = 128;
    public int downloadDelay = 800;
    public int readTimeOut = 3000;
    public int connectTimeOut = 10000;
    public int pricerControllerSleepCycle = 60;
    public int dataEntryCycleLimit = 10;
    public int baseDataSize = 100;
    public int hourlyDataSize = 60;
    public int duplicatesSize = 60;
    public String defaultAPIURL = "http://www.pathofexile.com/api/public-stash-tabs?id=";

    public ConfigReader(String file) {
        //  Name: ConfigReader
        //  Date created: 20.12.2017
        //  Last modified: 21.12.2017
        //  Description: Replaces Properties()

        readFile(file);
    }

    private void readFile(String fileName) {
        //  Name: readFile
        //  Date created: 20.12.2017
        //  Last modified: 21.12.2017
        //  Description: Reads config in from file

        String line, key, value;
        File file = new File(fileName);

        if (!file.exists())
            return;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            while ((line = bufferedReader.readLine()) != null) {
                if(line.equals(""))
                    continue;
                else if (line.startsWith("#"))
                    continue;

                key = line.substring(0, line.indexOf("="));
                value = line.substring(line.indexOf("=") + 1);

                switch (key) {
                    case "timeZoneOffset":
                        timeZoneOffset = Integer.parseInt(value);
                        break;
                    case "workerLimit":
                        workerLimit = Integer.parseInt(value);
                        break;
                    case "downloadChunkSize":
                        downloadChunkSize = Integer.parseInt(value);
                        break;
                    case "downloadDelay":
                        downloadDelay = Integer.parseInt(value);
                        break;
                    case "readTimeOut":
                        readTimeOut = Integer.parseInt(value);
                        break;
                    case "connectTimeOut":
                        connectTimeOut = Integer.parseInt(value);
                        break;
                    case "pricerControllerSleepCycle":
                        pricerControllerSleepCycle = Integer.parseInt(value);
                        break;
                    case "dataEntryCycleLimit":
                        dataEntryCycleLimit = Integer.parseInt(value);
                        break;
                    case "baseDataSize":
                        baseDataSize = Integer.parseInt(value);
                        break;
                    case "hourlyDataSize":
                        hourlyDataSize = Integer.parseInt(value);
                        break;
                    case "duplicatesSize":
                        duplicatesSize = Integer.parseInt(value);
                        break;
                    case "defaultAPIURL":
                        defaultAPIURL = value;
                        break;
                    default:
                        System.out.println("[ERROR] Unknown config key: (" + key + ") and value (" + value + ")");
                        break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
