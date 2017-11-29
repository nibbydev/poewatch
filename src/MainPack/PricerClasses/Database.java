package MainPack.PricerClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Database {
    /*   Name: PriceDatabase
     *   Date created: 28.11.2017
     *   Last modified: 29.11.2017
     *   Description: Class used to store data? I can't do SQL
     */

    // MFW "Harbinger -> BodyArmours|3|Yriel's Fostering|6L|var(speed) -> [[345, 2], [24.234, ], []]"
    private static Map<String, Map<String, Map<String, ArrayList<String[]>>>> rawData = new HashMap<>();
    // Currency-related databases
    private static Map<String, Map<String, ArrayList<Double>>> currencyDatabase = new HashMap<>();
    private static Map<String, Map<String, StatsObject>> currencyStatistics = new HashMap<>();
    // Item-related databases
    private static Map<String, Map<String, Map<String, ArrayList<Double>>>> itemDatabase = new HashMap<>();
    private static Map<String, Map<String, Map<String, StatsObject>>> itemStatistics = new HashMap<>();
    // Gem-related databases
    private static Map<String, Map<String, Map<String, Map<String, ArrayList<Double>>>>> gemDatabase = new HashMap<>();
    private static Map<String, Map<String, Map<String, Map<String, StatsObject>>>> gemStatistics = new HashMap<>();


    private static ArrayList<String> skippedItemDatabaseTypes;
    private static ArrayList<String> gemItemDataBaseItemTypes;
    private static Map<String, String> baseReverseCurrencyIndexes;

    public Database () {
        /*  Name: Database()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method used to set initial default values
        */

        // Has the base indexes, more will be added later
        baseReverseCurrencyIndexes = new HashMap<>(){{
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

        // These itemclasses should be skipped when dealing with item database
        skippedItemDatabaseTypes = new ArrayList<>(){{
            add("Currency");
            add("Gems");
            add("VaalGems");
            add("Support");
        }};

        // To separate out gems from other items
        gemItemDataBaseItemTypes = new ArrayList<>() {{
            add("Gems");
            add("Support");
            add("VaalGems");
        }};
    }

    public void rawDataAddEntry(String league, String itemType, String itemKey, double value, String currencyType) {
        /*  Name: addEntry()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that adds entries to raw data database. Also makes sure entries exist in hashmaps
        */

        // Make sure hashmap has the correct tree
        rawData.putIfAbsent(league, new HashMap<>());
        rawData.get(league).putIfAbsent(itemType, new HashMap<>());
        rawData.get(league).get(itemType).putIfAbsent(itemKey, new ArrayList<>());

        // Add values to database (in string format)
        rawData.get(league).get(itemType).get(itemKey).add(new String[]{Double.toString(value), currencyType});

    }

    /*
     * Methods used to manage currency-related databases
     */

    public void buildCurrencyDatabase() {
        /*  Name: buildCurrencyDatabase()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that adds entries to currency database
        */

        double value;
        int index;

        // Loop through leagues
        for (String league: rawData.keySet()) {
            currencyDatabase.putIfAbsent(league, new HashMap<>());

            // Make sure rawData has next hashMap
            if (!rawData.get(league).containsKey("Currency"))
                continue;

            // Loop through currency names
            for (String name: rawData.get(league).get("Currency").keySet()) {
                if(name.equals("Chaos Orb"))
                    continue;

                currencyDatabase.get(league).putIfAbsent(name, new ArrayList<>());

                // Make sure rawData has next hashMap
                if (!rawData.get(league).get("Currency").containsKey(name))
                    continue;

                // Loop through [value, index] currency entries
                for (String[] entry: rawData.get(league).get("Currency").get(name)) {
                    value = Double.parseDouble(entry[0]);
                    index = Integer.parseInt(entry[1]);

                    // If we have the median price, use that
                    if (index != 1){
                        Double chaosValue = .0;

                        // If there's a value in the statistics database, use that
                        if (currencyStatistics.containsKey(league)){
                            if(currencyStatistics.get(league).containsKey(name)){
                                chaosValue = currencyStatistics.get(league).get(name).getMedian();
                            }
                        }

                        if(chaosValue > .0) {
                            value = value * chaosValue;
                        } else {
                            continue;
                        }
                    }

                    // Add currency to new database
                    currencyDatabase.get(league).get(name).add(value);
                }
                // Make sure the database doesn't get too many values
                if (currencyDatabase.get(league).get(name).size() > 100)
                    currencyDatabase.get(league).get(name).remove(0);
                else if (currencyDatabase.get(league).get(name).size() < 1)
                    currencyDatabase.get(league).remove(name);
            }
            if (currencyDatabase.get(league).size() < 1)
                currencyDatabase.remove(league);
        }
    }

    public void buildCurrencyStatistics() {
        /*  Name: buildCurrencyStatistics()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that adds entries to currency statistics database that holds mean/median values
        */

        int count;

        // Loop through leagues
        for (String league: currencyDatabase.keySet()) {
            currencyStatistics.putIfAbsent(league, new HashMap<>());

            for (String name : currencyDatabase.get(league).keySet()) {
                // Skip entries with a small number of elements as they're hard to base statistics on
                count = currencyDatabase.get(league).get(name).size();
                if(count < 20)
                    continue;

                // Make a copy so the original order persists
                ArrayList<Double> tempCurrencyData = new ArrayList<>();
                tempCurrencyData.addAll(currencyDatabase.get(league).get(name));
                // Sort the entries in growing order
                Collections.sort(tempCurrencyData);

                // Slice sorted copy to get more precision
                if (count < 30)
                    tempCurrencyData.subList(2, count - 10).clear();
                else if (count < 60)
                    tempCurrencyData.subList(3, count - 15).clear();
                else if (count < 80)
                    tempCurrencyData.subList(4, count - 20).clear();
                else if (count < 100)
                    tempCurrencyData.subList(5, count - 30).clear();

                // Set new count value
                count = tempCurrencyData.size();

                Double mean = 0.0;
                Double median;

                // Add up values to calculate mean
                for (Double i: tempCurrencyData) {
                    mean += i;
                }

                // Calculate mean and median values
                mean = mean / count;
                median = count / 2.0;
                median = tempCurrencyData.get(median.intValue());

                // Turn that into a statistics object and put it in the database
                currencyStatistics.get(league).put(name,  new StatsObject(count, mean, median));
            }
            if (currencyStatistics.get(league).size() < 1)
                currencyStatistics.remove(league);
        }
    }

    // TODO: purgeCurrencyDatabase

    /*
     * Methods used to manage item-related databases
     */

    public void buildItemDatabase() {
        /*  Name: buildItemDatabase()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that adds entries to item database
        */

        double value;
        String index;

        // Loop through leagues
        for (String league: rawData.keySet()) {
            itemDatabase.putIfAbsent(league, new HashMap<>());

            for (String itemType : rawData.get(league).keySet()) {
                if (skippedItemDatabaseTypes.contains(itemType))
                    continue;

                itemDatabase.get(league).putIfAbsent(itemType, new HashMap<>());

                for (String name: rawData.get(league).get(itemType).keySet()) {
                    itemDatabase.get(league).get(itemType).putIfAbsent(name, new ArrayList<>());

                    // Loop through [value, index] currency entries
                    for (String[] entry: rawData.get(league).get(itemType).get(name)) {
                        value = Double.parseDouble(entry[0]);
                        index = entry[1];

                        // If we have the median price, use that
                        if (!index.equals("1")){
                            Double chaosValue = .0;

                            // If there's a value in the statistics database, use that
                            if (currencyStatistics.containsKey(league)){
                                if(currencyStatistics.get(league).containsKey(name)){
                                    chaosValue = currencyStatistics.get(league).get(baseReverseCurrencyIndexes.get(index)).getMedian();
                                }
                            }

                            // Replace value with baseCurrency value
                            if(chaosValue > .0) {
                                value = value * chaosValue;
                            } else {
                                continue;
                            }
                        }

                        // Add currency to new database
                        itemDatabase.get(league).get(itemType).get(name).add(value);
                    }

                    // Make sure the database doesn't get too many values
                    if (itemDatabase.get(league).get(itemType).get(name).size() > 100)
                        itemDatabase.get(league).get(itemType).get(name).remove(0);
                    else if (itemDatabase.get(league).get(itemType).get(name).size() < 1)
                        itemDatabase.get(league).get(itemType).remove(name);
                }
                if (itemDatabase.get(league).get(itemType).size() < 1)
                    itemDatabase.get(league).remove(itemType);
            }
            if (itemDatabase.get(league).size() < 1)
                itemDatabase.remove(league);
        }
    }

    public void buildItemStatistics() {
        /*  Name: buildItemStatistics()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that adds entries to item statistics database that holds mean/median values
        */

        int count;

        // Loop through leagues
        for (String league: itemDatabase.keySet()) {
            itemStatistics.putIfAbsent(league, new HashMap<>());

            for (String itemType: itemDatabase.get(league).keySet()) {
                itemStatistics.get(league).putIfAbsent(itemType, new HashMap<>());

                for (String itemName: itemDatabase.get(league).get(itemType).keySet()) {
                    // Skip entries with a small number of elements as they're hard to base statistics on
                    count = itemDatabase.get(league).get(itemType).get(itemName).size();
                    if(count < 30)
                        continue;

                    // Make a copy so the original order persists
                    ArrayList<Double> tempItemData = new ArrayList<>();
                    tempItemData.addAll(itemDatabase.get(league).get(itemType).get(itemName));
                    // Sort the entries in growing order
                    Collections.sort(tempItemData);

                    // Slice sorted copy to get more precision
                    if (count < 40)
                        tempItemData.subList(0, count - 15).clear();
                    else if (count < 50)
                        tempItemData.subList(3, count - 20).clear();
                    else if (count < 70)
                        tempItemData.subList(4, count - 20).clear();
                    else if (count < 80)
                        tempItemData.subList(5, count - 25).clear();
                    else if (count < 100)
                        tempItemData.subList(5, count - 30).clear();

                    // Set new count value
                    count = tempItemData.size();

                    Double mean = 0.0;
                    Double median;

                    // Add up values to calculate mean
                    for (Double i: tempItemData) {
                        mean += i;
                    }

                    // Calculate mean and median values
                    mean = mean / count;
                    median = count / 2.0;
                    median = tempItemData.get(median.intValue());

                    // Turn that into a statistics object and put it in the database
                    itemStatistics.get(league).get(itemType).put(itemName, new StatsObject(count, mean, median));
                }
                if(itemStatistics.get(league).get(itemType).size() < 1)
                    itemStatistics.get(league).remove(itemType);
            }
            if(itemStatistics.get(league).size() < 1)
                itemStatistics.remove(league);
        }
    }


    // TODO: purgeItemDatabase

    /*
     * Methods used to manage gem-related databases
     */

    public void buildGemDatabase() {
        /*  Name: buildGemDatabase()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that adds entries to gem database
        */

        double value;
        String index;
        String gemInfo;
        String gemName;

        // Loop through leagues
        for (String league: rawData.keySet()) {
            gemDatabase.putIfAbsent(league, new HashMap<>());

            for (String gemType : rawData.get(league).keySet()) {
                if (!gemItemDataBaseItemTypes.contains(gemType))
                    continue;

                gemDatabase.get(league).putIfAbsent(gemType, new HashMap<>());

                for (String name : rawData.get(league).get(gemType).keySet()) {
                    gemName = name.split("\\|")[0];
                    gemInfo = name.replace(gemName + "|", "");

                    gemDatabase.get(league).get(gemType).putIfAbsent(gemName, new HashMap<>());
                    gemDatabase.get(league).get(gemType).get(gemName).putIfAbsent(gemInfo, new ArrayList<>());

                    // Loop through [value, index] currency entries
                    for (String[] entry : rawData.get(league).get(gemType).get(name)) {
                        value = Double.parseDouble(entry[0]);
                        index = entry[1];

                        // If we have the median price, use that
                        if (!index.equals("1")){
                            Double chaosValue = .0;

                            // If there's a value in the statistics database, use that
                            if (currencyStatistics.containsKey(league)){
                                if(currencyStatistics.get(league).containsKey(name)){
                                    chaosValue = currencyStatistics.get(league).get(baseReverseCurrencyIndexes.get(index)).getMedian();
                                }
                            }

                            if(chaosValue > .0) {
                                value = value * chaosValue;
                            } else {
                                continue;
                            }
                        }

                        // Add gem to new database
                        gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).add(value);
                    }
                    if(gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).size() > 100)
                        gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).remove(0);
                    else if (gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).size() < 1)
                        gemDatabase.get(league).get(gemType).get(gemName).remove(gemInfo);
                    else if (gemDatabase.get(league).get(gemType).get(gemName).size() < 1)
                        gemDatabase.get(league).get(gemType).remove(gemName);
                }
                if (gemDatabase.get(league).get(gemType).size() < 1)
                    gemDatabase.get(league).remove(gemType);
            }
            if (gemDatabase.get(league).size() < 1)
                gemDatabase.remove(league);
        }
    }

    public void buildGemStatistics() {
        /*  Name: buildItemStatistics()
        *   Date created: 29.11.2017
        *   Last modified: 29.11.2017
        *   Description: Method that adds entries to item statistics database that holds mean/median values
        */

        int count;

        // Loop through leagues
        for (String league: gemDatabase.keySet()) {
            gemStatistics.putIfAbsent(league, new HashMap<>());

            for (String gemType : gemDatabase.get(league).keySet()) {
                gemStatistics.get(league).putIfAbsent(gemType, new HashMap<>());

                for (String gemName : gemDatabase.get(league).get(gemType).keySet()) {
                    gemStatistics.get(league).get(gemType).putIfAbsent(gemName, new HashMap<>());

                    for (String gemInfo : gemDatabase.get(league).get(gemType).get(gemName).keySet()) {
                        // Skip entries with a small number of elements as they're hard to base statistics on
                        count = gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).size();
                        if(count < 20)
                            continue;

                        // Make a copy so the original order persists
                        ArrayList<Double> tempGemData = new ArrayList<>();
                        tempGemData.addAll(gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo));
                        // Sort the entries in growing order
                        Collections.sort(tempGemData);

                        // Slice sorted copy to get more precision
                        if (count < 40)
                            tempGemData.subList(2, count - 10).clear();
                        else if (count < 60)
                            tempGemData.subList(3, count - 15).clear();
                        else if (count < 80)
                            tempGemData.subList(5, count - 20).clear();
                        else if (count < 100)
                            tempGemData.subList(5, count - 30).clear();

                        // Set new count value
                        count = tempGemData.size();

                        Double mean = 0.0;
                        Double median;

                        // Add up values to calculate mean
                        for (Double i: tempGemData) {
                            mean += i;
                        }

                        // Calculate mean and median values
                        mean = mean / count;
                        median = count / 2.0;
                        median = tempGemData.get(median.intValue());

                        // Turn that into a statistics object and put it in the database
                        gemStatistics.get(league).get(gemType).get(gemName).put(gemInfo, new StatsObject(count, mean, median));
                    }
                    if (gemStatistics.get(league).get(gemType).get(gemName).size() < 1)
                        gemStatistics.get(league).get(gemType).remove(gemName);
                }
                if (gemStatistics.get(league).get(gemType).size() < 1)
                    gemStatistics.get(league).remove(gemType);
            }
            if (gemStatistics.get(league).size() < 1)
                gemStatistics.remove(league);
        }
    }

    // TODO: purgeGemDatabase

    /*
     * Methods that are for testing databases
     */

    public void devPrintRawData(){
        // TODO: for development
        for (String league: rawData.keySet()) {
            System.out.println("[LEAGUE] " + league);
            for (String itemType : rawData.get(league).keySet()) {
                System.out.println("  [TYPE] " + itemType);
                for (String name : rawData.get(league).get(itemType).keySet()) {
                    System.out.println("    [KEY] " + name + ": " + rawData.get(league).get(itemType).get(name).size());
                }
            }
        }
    }

    public void devPrintDatabaseData(){
        // TODO: for development
        System.out.println("[CURRENCY]");
        for (String league: currencyDatabase.keySet()) {
            System.out.println("    [" + league + "] ");
            for (String name : currencyDatabase.get(league).keySet()) {
                System.out.println("        [" + name + "] " + currencyDatabase.get(league).get(name).size());
            }
        }

        System.out.println("[ITEMS]");
        for (String league: itemDatabase.keySet()) {
            System.out.println("    [" + league + "]");
            for (String itemType : itemDatabase.get(league).keySet()) {
                System.out.println("        [" + itemType + "] ");
                for (String name : itemDatabase.get(league).get(itemType).keySet()) {
                    System.out.println("            [" + name + "] " + itemDatabase.get(league).get(itemType).get(name).size());
                }
            }
        }

        System.out.println("[GEMS]");
        for (String league: gemDatabase.keySet()) {
            System.out.println("    [" + league + "]");
            for (String itemType : gemDatabase.get(league).keySet()) {
                System.out.println("        [" + itemType + "]");
                for (String name : gemDatabase.get(league).get(itemType).keySet()) {
                    System.out.println("            [" + name + "]");
                    for (String info: gemDatabase.get(league).get(itemType).get(name).keySet()) {
                        System.out.println("                [" + info + "] " + gemDatabase.get(league).get(itemType).get(name).get(info).size());
                    }
                }
            }
        }
    }


}
