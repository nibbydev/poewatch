package MainPack.PricerClasses;

import MainPack.MapperClasses.Item;

import java.util.*;

public class Database {
    /*   Name: PriceDatabase
     *   Date created: 28.11.2017
     *   Last modified: 29.11.2017
     *   Description: Class used to store data? I can't do SQL
     */

    private static Map<String, ArrayList<String[]>> rawData = new HashMap<>();
    private static Map<String, ArrayList<Double>> baseDatabase = new HashMap<>();
    private static Map<String, StatsObject> statistics = new HashMap<>();

    private static Map<String, String> baseReverseCurrencyIndexes;

    public Database() {
        /*  Name: Database()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method used to set initial default values
        */

        // Has the base indexes, more will be added later
        baseReverseCurrencyIndexes = new HashMap<>() {{
            put("1", "Chaos Orb");
            put("2", "Exalted Orb");
            put("3", "Divine Orb");
            put("4", "Orb of Alchemy");
            put("5", "Orb of Fusing");
            put("6", "Orb of Alteration");
            put("7", "Regal Orb");
            put("8", "Vaal Orb");
            put("9", "Orb of Regret");
            put("10", "Cartographer's Chisel");
            put("11", "Jeweller's Orb");
            put("12", "Silver Coin");
            put("13", "Perandus Coin");
            put("14", "Orb of Scouring");
            put("15", "Gemcutter's Prism");
            put("16", "Orb of Chance");
            put("17", "Chromatic Orb");
            put("18", "Blessed Orb");
            put("19", "Glassblower's Bauble");
            put("20", "Orb of Augmentation");
            put("21", "Orb of Transmutation");
            put("22", "Mirror of Kalandra");
            put("23", "Scroll of Wisdom");
            put("24", "Portal Scroll");
            put("25", "Blacksmith's Whetstone");
            put("26", "Armourer's Scrap");
            put("27", "Apprentice Cartographer's Sextant");
            put("28", "Journeyman Cartographer's Sextant");
            put("29", "Master Cartographer's Sextant");
        }};

    }

    public void rawDataAddEntry(Item item) {
        /*  Name: addEntry()
        *   Date created: 28.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that adds entries to raw data database. Also makes sure entries exist in hashmaps
        */

        // Add value to database (in string format)
        if (rawData.containsKey(item.getKey())) {
            rawData.get(item.getKey()).add(new String[]{Double.toString(item.getPrice()), item.getPriceType()});
        } else {
            rawData.put(item.getKey(), new ArrayList<>() {{
                add(new String[]{Double.toString(item.getPrice()), item.getPriceType()});
            }});
        }
    }

    public void clearRawData() {
        /*  Name: clearRawData()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method used for clearing rawData database
        */

        rawData.clear();
    }

    /*
     * Methods used to manage databases
     */

    public void buildDatabases() {
        /*  Name: buildDatabases()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that generates a table of "key: [chaos value]" entries
        */

        Double value;
        String index;
        Double chaosValue;

        // Loop through entries
        for (String key : rawData.keySet()) {
            for (String[] entry : rawData.get(key)) {
                value = Double.parseDouble(entry[0]);
                index = entry[1];

                // If we have the median price, use that
                if (!index.equals("1")) {
                    // Set initial value
                    chaosValue = 0.0;

                    // If there's a value in the statistics database, use that
                    if (statistics.containsKey(key))
                        chaosValue = statistics.get(key).getMedian();

                    if (chaosValue > 0.0)
                        value = value * chaosValue;
                    else
                        continue;
                }

                baseDatabase.putIfAbsent(key, new ArrayList<>());
                baseDatabase.get(key).add(value);
            }
        }
    }

    public void buildStatistics() {
        /*  Name: buildStatistics()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that adds entries to statistics database
        */

        Double mean;
        Double median;
        int count;

        // Loop through currency entries
        for (String key : baseDatabase.keySet()) {
            // Skip entries with a small number of elements as they're hard to base statistics on
            count = baseDatabase.get(key).size();
            if (count < 20)
                continue;

            // Make a copy so the original order persists
            ArrayList<Double> tempValueList = new ArrayList<>();
            tempValueList.addAll(baseDatabase.get(key));
            // Sort the entries in growing order
            Collections.sort(tempValueList);

            // Slice sorted copy for more precision
            if (count < 30)
                tempValueList.subList(2, count - 10).clear();
            else if (count < 60)
                tempValueList.subList(3, count - 15).clear();
            else if (count < 80)
                tempValueList.subList(4, count - 20).clear();
            else if (count < 100)
                tempValueList.subList(5, count - 30).clear();

            // Reassign count
            count = tempValueList.size();

            mean = 0.0;
            // Add up values to calculate mean
            for (Double i : tempValueList) { // TODO: replace with lambda
                mean += i;
            }

            // Calculate mean and median values
            mean = mean / count;
            median = count / 2.0;
            median = tempValueList.get(median.intValue());

            // Turn that into a statistics object and put it in the database
            statistics.put(key, new StatsObject(count, mean, median));
        }
    }

    public void purgeDatabases() {
        /*  Name: purgeDatabases()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that removes entries like "50 000" from items that would otherwise interfere with statistics
        */

        double median;

        for (String key : statistics.keySet()) {
            if (!baseDatabase.containsKey(key))
                continue;

            // Get the median
            median = statistics.get(key).getMedian();

            // Remove values that are 200% larger/smaller than the median
            for (Double value : new ArrayList<>(baseDatabase.get(key))) {
                if (value > median * 2.0 || value < median / 2.0) {
                    baseDatabase.get(key).remove(value);
                }
            }
        }
    }

    /*
     * Methods used for development, they print data
     */

    public void devPrintData() {
        /*  Name: devPrintRawData()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Prints out database. Used for development
        */

        System.out.println("\n[=================================================== RAW DATA ===================================================]");

        for (String key : rawData.keySet()) {
            System.out.print("[" + key + "]: ");

            for(String[] info : rawData.get(key)){
                System.out.print(Arrays.toString(info) + ",");
            }

            System.out.println();
        }

        System.out.println("\n[=================================================== DATABASES ===================================================]");

        for (String key : baseDatabase.keySet()) {
            System.out.println("[" + key + "]: " + Arrays.toString(baseDatabase.get(key).toArray()));
        }

        System.out.println("\n[=================================================== STATISTICS ===================================================]");

        for (String key : statistics.keySet()) {
            System.out.println("[" + key + "] Median: " + statistics.get(key).getMedian());
            System.out.println("[" + key + "] Mean: " + statistics.get(key).getMean());
            System.out.println("[" + key + "] Count: " + statistics.get(key).getCount());
        }
    }

}
