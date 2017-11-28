package MainPack.PricerClasses;

import java.util.ArrayList;
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
    private static Map<String, Map<String, Map<String, ArrayList<Double>>>> itemDatabase = new HashMap<>();
    private static Map<String, Map<String, ArrayList<Double>>> currencyDatabase = new HashMap<>();
    private static Map<String, Map<String, Map<String, Map<String, ArrayList<Double>>>>> gemDatabase = new HashMap<>();

    private static ArrayList<String> skippedItemDatabaseTypes;
    private static Map<String, String> baseReverseCurrencyIndexes;
    private static ArrayList<String> gemItemDataBaseItemTypes;

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
                currencyDatabase.get(league).putIfAbsent(name, new ArrayList<>());

                // Make sure rawData has next hashMap
                if (!rawData.get(league).get("Currency").containsKey(name))
                    continue;

                // Loop through [value, index] currency entries
                for (String[] entry: rawData.get(league).get("Currency").get(name)) {
                    value = Double.parseDouble(entry[0]);
                    index = Integer.parseInt(entry[1]);

                    // Skip everything that isn't base value
                    if (index != 1){
                        // TODO: Get price from currency statistics
                        continue;
                    }

                    // Add currency to new database
                    currencyDatabase.get(league).get(name).add(value);
                }
                // Make sure the database doesn't get too many values
                if (currencyDatabase.get(league).get(name).size() > 100)
                    currencyDatabase.get(league).get(name).remove(0);
                else if (currencyDatabase.get(league).get(name).size() < 1)
                    currencyDatabase.get(league).get(name).clear();
            }
            if (currencyDatabase.get(league).size() < 1)
                currencyDatabase.get(league).clear();
        }
        if (currencyDatabase.size() < 1)
            currencyDatabase.clear();
    }

    // TODO: buildCurrencyStatistics
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
        int index;

        // Loop through leagues
        for (String league: rawData.keySet()) {
            itemDatabase.putIfAbsent(league, new HashMap<>());

            for (String itemType : rawData.get(league).keySet()) {
                itemDatabase.get(league).putIfAbsent(itemType, new HashMap<>());

                if (skippedItemDatabaseTypes.contains(itemType))
                    continue;

                for (String name: rawData.get(league).get(itemType).keySet()) {
                    itemDatabase.get(league).get(itemType).putIfAbsent(name, new ArrayList<>());

                    // Loop through [value, index] currency entries
                    for (String[] entry: rawData.get(league).get(itemType).get(name)) {
                        value = Double.parseDouble(entry[0]);
                        index = Integer.parseInt(entry[1]);

                        // Skip everything that isn't base value
                        if (index != 1){
                            // TODO: Get price from currency statistics
                            continue;
                        }

                        // Add currency to new database
                        itemDatabase.get(league).get(itemType).get(name).add(value);
                    }

                    // Make sure the database doesn't get too many values
                    if (itemDatabase.get(league).get(itemType).get(name).size() > 100)
                        itemDatabase.get(league).get(itemType).get(name).remove(0);
                    else if (itemDatabase.get(league).get(itemType).get(name).size() < 1)
                        itemDatabase.get(league).get(itemType).get(name).clear();
                }
                if (itemDatabase.get(league).get(itemType).size() < 1)
                    itemDatabase.get(league).get(itemType).clear();
            }
            if (itemDatabase.get(league).size() < 1)
                itemDatabase.get(league).clear();
        }
        if (itemDatabase.size() < 1)
            itemDatabase.clear();
    }

    // TODO: buildItemStatistics
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
        int index;
        String gemInfo;
        String gemName;

        // Loop through leagues
        for (String league: rawData.keySet()) {
            gemDatabase.putIfAbsent(league, new HashMap<>());

            for (String gemType : rawData.get(league).keySet()) {
                gemDatabase.get(league).putIfAbsent(gemType, new HashMap<>());

                if (!gemItemDataBaseItemTypes.contains(gemType))
                    continue;

                for (String name : rawData.get(league).get(gemType).keySet()) {
                    gemName = name.split("\\|")[0];
                    gemInfo = name.replace(gemName + "|", "");

                    gemDatabase.get(league).get(gemType).putIfAbsent(gemName, new HashMap<>());
                    gemDatabase.get(league).get(gemType).get(gemName).putIfAbsent(gemInfo, new ArrayList<>());

                    // Loop through [value, index] currency entries
                    for (String[] entry : rawData.get(league).get(gemType).get(name)) {
                        value = Double.parseDouble(entry[0]);
                        index = Integer.parseInt(entry[1]);

                        // Skip everything that isn't base value
                        if (index != 1) {
                            continue; // TODO: Get price from gem statistics
                        }

                        // Add gem to new database
                        gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).add(value);
                    }
                    if(gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).size() > 100)
                        gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).remove(0);
                    else if (gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).size() < 1)
                        gemDatabase.get(league).get(gemType).get(gemName).get(gemInfo).clear();
                    else if (gemDatabase.get(league).get(gemType).get(gemName).size() < 1)
                        gemDatabase.get(league).get(gemType).get(gemName).clear();
                }
                if (gemDatabase.get(league).get(gemType).size() < 1)
                    gemDatabase.get(league).get(gemType).clear();
            }
            if (gemDatabase.get(league).size() < 1)
                gemDatabase.get(league).clear();
        }
        if (gemDatabase.size() < 1)
            gemDatabase.clear();
    }


    // TODO: buildGemStatistics
    // TODO: purgeGemDatabase

    public void devPrintData(){
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


}
