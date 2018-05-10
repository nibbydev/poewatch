package ovh.poe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Loads in values from file. Values can be accessed from anywhere in the script
 */
public class ConfigReader {
    public int timeZoneOffset = 2;
    public int workerLimit = 5;
    public int downloadChunkSize = 128;
    public int downloadDelay = 800;
    public int readTimeOut = 7000;
    public int connectTimeOut = 10000;
    public int pricerControllerSleepCycle = 60;
    public int dataEntryCycleLimit = 10;
    public int baseDataSize = 100;
    public int hourlyDataSize = 64;
    //public int medianLeftShift = 3;
    public double pricePrecision = 1000.0;

    public int dbTempSize = 16;
    public int calcShiftPercent = 80;
    public int calcNewShiftPercent = 60;

    /**
     * Calls method that loads in config values on class init
     *
     * @param fileName Config file id in relation to local path
     */
    ConfigReader(String fileName) {
        readFile(fileName);
    }

    /**
     * Reads values from CSV config file, overwrites static class values
     *
     * @param fileName Config file id in relation to local path
     */
    private void readFile(String fileName) {
        String line, key, value;
        File file = new File(fileName);

        if (!file.exists())
            return;

        try (BufferedReader bufferedReader = Misc.defineReader(file)) {
            if (bufferedReader == null) return;

            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals(""))
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
                    case "medianLeftShift":
                        //medianLeftShift = Integer.parseInt(value);
                        break;
                    case "pricePrecision":
                        pricePrecision = Math.pow(10, Integer.parseInt(value));
                        break;
                    default:
                        Main.ADMIN.log_("Unknown config key '"+key+"' with value '"+value+"'", 3);
                        break;
                }
            }
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 3);
        }
    }
}
