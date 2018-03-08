package com.sanderh;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps indexes and shorthands to currency names and vice versa
 */
public class RelationManager {
    public Map<String, String> indexToName = new HashMap<>();
    public Map<String, String> nameToIndex = new HashMap<>();
    public Map<String, String> shortHandToIndex = new HashMap<>();
    public Map<String, String> shortHandToName = new HashMap<>();

    /**
     * Reads currency relation data from file on object init
     */
    public RelationManager() {
        readCurrencyFromFile();
    }

    /**
     * Reads currency relation data from file and adds it to maps
     */
    private void readCurrencyFromFile() {
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
