package com.sanderh;

import java.io.*;

public class Converter {
    private static BufferedReader defineReader(File inputFile) {
        //  Name: defineReader()
        //  Date created: 25.12.2017
        //  Last modified: 25.12.2017
        //  Description: Assigns reader buffer

        if (!inputFile.exists())
            return null;

        // Open up the reader (it's fine if the file is missing)
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static BufferedWriter defineWriter(File outputFile) {
        //  Name: defineWriter()
        //  Date created: 25.12.2017
        //  Last modified: 25.12.2017
        //  Description: Assigns writer buffer

        // Open up the writer (if this throws an exception holy fuck something went massively wrong)
        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static double round(double value, int places) {
        String temp = Double.toString(value);
        return Double.parseDouble(temp.substring(0, temp.indexOf(".") + places));
    }

    private static void convert() {
        File inputFile = new File("./database.txt");
        File outputFile = new File("./database.txt.newconverted");

        BufferedReader reader = defineReader(inputFile);
        BufferedWriter writer = defineWriter(outputFile);

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split("::");

                String key = splitLine[0];
                String stats = splitLine[1];
                String[] prices = splitLine[2].split(",");
                String[] ids = splitLine[3].split(",");
                String[] means = splitLine[4].split(",");
                String[] meds = splitLine[5].split(",");
                String[] accounts = splitLine[6].split(",");

                writer.write(key + "::" + stats + "::");

                if(prices[0].equals("-")) {
                    writer.write("-");
                } else {
                    for (int i = 0; i < prices.length; i++) {
                        String id, account;

                        if(accounts.length > i) account = accounts[i];
                        else account = "-";

                        if(ids.length > i) id = ids[i];
                        else id = "-";

                        if (i < prices.length - 1)
                            writer.write(prices[i] + "," + account + "," + id + "|");
                        else
                            writer.write(prices[i] + "," + account + "," + id);
                    }
                }

                writer.write("::");

                if(means[0].equals("-") || meds[0].equals("-")) {
                    writer.write("-");
                } else {
                    for (int i = 0; i < means.length; i++) {
                        if (i < means.length - 1)
                            writer.write(means[i] + "," + meds[i] + "|");
                        else
                            writer.write(means[i] + "," + meds[i]);
                    }
                }

                writer.write("\n");
                writer.flush();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println(5 / 3);
        System.out.println(2 / 3);
        System.out.println(47 / 3);
    }
}
