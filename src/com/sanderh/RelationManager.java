package com.sanderh;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class RelationManager {
    //  Name: RelationManager()
    //  Date created: 25.12.2017
    //  Last modified: 25.12.2017
    //  Description: Contains maps that connect shortened indexed versions of currency item names

    public Map<String, String> indexToName = new HashMap<>();
    public Map<String, String> nameToIndex = new HashMap<>();
    public Map<String, String> shortHandToIndex = new HashMap<>();
    public Map<String, String> shortHandToName = new HashMap<>();

    public RelationManager () {
        readCurrencyFromFile();
    }

    public void readCurrencyFromFile() {
        //  Name: readCurrencyFromFile()
        //  Date created: 25.12.2017
        //  Last modified: 25.12.2017
        //  Description: Reads currency relation data in from file and adds it to maps

        File file = new File("./currencyRelations.txt");

        // Open up the reader
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String[] splitLine;
            String line;

            while ((line = reader.readLine()) != null) {
                splitLine = line.split("\\|");
                indexToName.put(splitLine[0], splitLine[1]);
                nameToIndex.put(splitLine[1], splitLine[0]);

                for (String shorthand : splitLine[2].split(",")) {
                    shortHandToIndex.put(shorthand, splitLine[0]);
                    shortHandToName.put(shorthand, splitLine[1]);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
