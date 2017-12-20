package com.sanderh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigReader {
    //  Name: ConfigReader
    //  Date created: 20.12.2017
    //  Last modified: 20.12.2017
    //  Description: Object that's used to manage constants

    private static final Map<String, String> config = new HashMap<>() {{
        put("timeZoneOffset", "2");
        put("workerLimit", "5");
        put("downloadChunkSize", "128");
        put("defaultAPIURL", "http://www.pathofexile.com/api/public-stash-tabs?id=");
        put("downloadDelay", "800");
        put("readTimeOut", "3000");
        put("connectTimeOut", "10000");
        put("PricerControllerSleepCycle", "60");
        put("DataEntryCycleLimit", "10");
        put("baseDataSize", "100");
        put("hourlyDataSize", "60");
        put("duplicatesSize", "60");
    }};

    public ConfigReader() {
        //  Name: ConfigReader
        //  Date created: 20.12.2017
        //  Last modified: 20.12.2017
        //  Description: Replaces Properties()

        readFile("/", "config.cfg");
    }

    private static void readFile(String path, String fileName) {
        //  Name: readFile
        //  Date created: 20.12.2017
        //  Last modified: 20.12.2017
        //  Description: Reads config in from file

        String line, key, value;
        File file = new File(path, fileName);

        if (!file.exists())
            return;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;

                key = line.substring(0, line.indexOf("="));
                value = line.substring(line.indexOf("=") + 1);

                if (!config.containsKey(key)) {
                    System.out.println("[ERROR] Unknown config key: " + key);
                    continue;
                }

                config.put(key, value);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public int getAsInt(String key) {
        //  Name: getAsInt
        //  Date created: 20.12.2017
        //  Last modified: 20.12.2017
        //  Description: Parses config thing as int

        if (!config.containsKey(key)) {
            System.out.println("[ERROR] Unknown config key: " + key);
            return 0;
        }

        return Integer.parseInt(config.get(key));
    }

    public double getAsDouble(String key) {
        //  Name: getAsDouble
        //  Date created: 20.12.2017
        //  Last modified: 20.12.2017
        //  Description: Parses config thing as double

        if (!config.containsKey(key)) {
            System.out.println("[ERROR] Unknown config key: " + key);
            return 0;
        }

        return Double.parseDouble(config.get(key));
    }

    public String getAsStr(String key) {
        //  Name: getAsStr
        //  Date created: 20.12.2017
        //  Last modified: 20.12.2017
        //  Description: Parses config thing as string

        if (!config.containsKey(key)) {
            System.out.println("[ERROR] Unknown config key: " + key);
            return "";
        }

        return config.get(key);
    }
}
