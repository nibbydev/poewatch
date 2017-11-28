package MainPack.PricerClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Database {
    /*   Name: PriceDatabase
     *   Date created: 28.11.2017
     *   Last modified: 28.11.2017
     *   Description: Class used to store data? I can't do SQL
     */

    // MFW "Harbinger -> BodyArmours|3|Yriel's Fostering|6L|var(speed) -> [[345, 2], [24.234, ], []]"
    private static Map<String, Map<String, Map<String, ArrayList<String[]>>>> rawData = new HashMap<>();
    private static Map<String, Map<String, Map<String, ArrayList<Double>>>> itemDatabase = new HashMap<>();
    private static Map<String, Map<String, ArrayList<Double>>> currencyDatabase = new HashMap<>();

    private ArrayList<String> skippedItemDatabaseTypes; // TODO: add values ["Currency", "Gems", "VaalGems", "Support"]


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

    /*
     * Methods used to manage gem-related databases
     */


}
