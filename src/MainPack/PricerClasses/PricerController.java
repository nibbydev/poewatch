package MainPack.PricerClasses;

import MainPack.MapperClasses.Item;
import MainPack.MapperClasses.Properties;
import MainPack.MapperClasses.Socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PricerController extends Thread {
    //  Name: PricerController
    //  Date created: 28.11.2017
    //  Last modified: 01.12.2017
    //  Description: A threaded object that manages databases

    private int sleepLength = 10;
    private boolean flagLocalRun = true;
    private boolean flagPause = false;
    private static Map<String, String> currencyShorthands;
    private static Map<String, String> baseCurrencyIndexes;
    private static ArrayList<String> potentialSixLinkItems;
    private static Map<String, Map<String, String>> itemVariants;
    private static Database database = new Database();

    public PricerController() {
        //  Name: PricerController()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Method that sets default values to the class's static variables

        // Suggested solution for putting a lot of values in at once
        // https://stackoverflow.com/questions/8261075/adding-multiple-entries-to-a-hashmap-at-once-in-one-statement
        currencyShorthands = new HashMap<>() {{
            put("exalt", "Exalted Orb");
            put("regret", "Orb of Regret");
            put("divine", "Divine Orb");
            put("chis", "Cartographer's Chisel");
            put("chao", "Chaos Orb");
            put("alchemy", "Orb of Alchemy");
            put("alts", "Orb of Alteration");
            put("fusing", "Orb of Fusing");
            put("fus", "Orb of Fusing");
            put("alteration", "Orb of Alteration");
            put("choas", "Chaos Orb");
            put("rega", "Regal Orb");
            put("gcp", "Gemcutter's Prism");
            put("regrets", "Orb of Regret");
            put("jeweller", "Jeweller's Orb");
            put("regal", "Regal Orb");
            put("chromatics", "Chromatic Orb");
            put("bles", "Blessed Orb");
            put("jewellers", "Jeweller's Orb");
            put("chance", "Orb of Chance");
            put("ex", "Exalted Orb");
            put("chromes", "Chromatic Orb");
            put("chanc", "Orb of Chance");
            put("chrom", "Chromatic Orb");
            put("exalted", "Exalted Orb");
            put("blessed", "Blessed Orb");
            put("c", "Chaos Orb");
            put("chaos", "Chaos Orb");
            put("chisel", "Cartographer's Chisel");
            put("alch", "Orb of Alchemy");
            put("exa", "Exalted Orb");
            put("vaal", "Vaal Orb");
            put("chrome", "Chromatic Orb");
            put("jew", "Jeweller's Orb");
            put("exalts", "Exalted Orb");
            put("scour", "Orb of Scouring");
            put("cart", "Cartographer's Chisel");
            put("alc", "Orb of Alchemy");
            put("fuse", "Orb of Fusing");
            put("exe", "Exalted Orb");
            put("jewel", "Jeweller's Orb");
            put("div", "Divine Orb");
            put("alt", "Orb of Alteration");
            put("fusings", "Orb of Fusing");
            put("chisels", "Cartographer's Chisel");
            put("chromatic", "Chromatic Orb");
            put("scouring", "Orb of Scouring");
            put("gemc", "Gemcutter's Prism");
            put("silver", "Silver Coin");
            put("aug", "Orb of Augmentation");
            put("mirror", "Mirror of Kalandra");
        }};

        // Index currency, will take up less space overall
        baseCurrencyIndexes = new HashMap<>() {{
            put("Chaos Orb", "1");
            put("Exalted Orb", "2");
            put("Divine Orb", "3");
            put("Orb of Alchemy", "4");
            put("Orb of Fusing", "5");
            put("Orb of Alteration", "6");
            put("Regal Orb", "7");
            put("Vaal Orb", "8");
            put("Orb of Regret", "9");
            put("Cartographer's Chisel", "10");
            put("Jeweller's Orb", "11");
            put("Silver Coin", "12");
            put("Perandus Coin", "13");
            put("Orb of Scouring", "14");
            put("Gemcutter's Prism", "15");
            put("Orb of Chance", "16");
            put("Chromatic Orb", "17");
            put("Blessed Orb", "18");
            put("Glassblower's Bauble", "19");
            put("Orb of Augmentation", "20");
            put("Orb of Transmutation", "21");
            put("Mirror of Kalandra", "22");
            put("Scroll of Wisdom", "23");
            put("Portal Scroll", "24");
            put("Blacksmith's Whetstone", "25");
            put("Armourer's Scrap", "26");
            put("Apprentice Cartographer's Sextant", "27");
            put("Journeyman Cartographer's Sextant", "28");
            put("Master Cartographer's Sextant", "29");
        }};

        // These items can have up to six links. This could mean a lot higher price. Hence, we give them a separate
        // database key
        potentialSixLinkItems = new ArrayList<>() {{
            add("Staves");
            add("BodyArmours");
            add("TwoHandSwords");
            add("TwoHandMaces");
            add("TwoHandAxes");
            add("Bows");
        }};

        // Since some itmes have special variants with the same name, they need to be split up
        itemVariants = new HashMap<>() {{
            put("Atziri's Splendour", new HashMap<>());
            put("Vessel of Vinktar", new HashMap<>() {{
                put("spells", "Lightning Damage to Spells");
                put("attacks", "Lightning Damage to Attacks");
                put("conversion", "Converted to Lightning");
                put("penetration", "Damage Penetrates");
            }});
            put("Doryani's Invitation", new HashMap<>() {{
                put("lightning", "increased Lightning Damage");
                put("fire", "increased Fire Damage");
                put("cold", "increased Cold Damage");
                put("physical", "increased Physical Damage");
            }});
            put("Yriel's Fostering", new HashMap<>() {{
                put("chaos", "Chaos Damage to Attacks");
                put("physical", "Physical Damage to Attack");
                put("speed", "increased Attack and Movement Speed");
            }});
            put("Volkuur's Guidance", new HashMap<>() {{
                put("fire", "Fire Damage to Spells");
                put("cold", "Cold Damage to Spells");
                put("lightning", "Lightning Damage to Spells");
            }});

        }};
    }

    public void run() {
        //  Name: run()
        //  Date created: 28.11.2017
        //  Last modified: 30.11.2017
        //  Description: Contains the main loop of the pricing service

        while (true) {
            sleepWhile(sleepLength * 60);

            // Break if run flag has been lowered
            if (!flagLocalRun)
                break;

            // Prepare for database building
            flagPause = true;

            // Build databases
            database.buildDatabases();
            database.purgeDatabases();
            database.buildStatistics();

            // Clear raw data
            database.clearRawData();
            flagPause = false;
        }
    }

    private void sleepWhile(int howLongInSeconds) {
        //  Name: sleepWhile()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Sleeps for <howLongInSeconds> seconds
        //  Parent methods:
        //      run()

        for (int i = 0; i < howLongInSeconds; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Break if run flag has been lowered
            if (!flagLocalRun)
                break;
        }
    }

    public void devPrintData() {
        //  Name: devPrintData()
        //  Date created: 30.11.2017
        //  Last modified: 30.11.2017
        //  Description: Roundabout method for printing out whole database

        database.devPrintData();
    }

    ///////////////////////////
    // Check and parse items //
    ///////////////////////////

    public void checkItem(Item item) {
        //  Name: checkItem()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Method that's used to add entries to the databases

        // Pause during I/O operations
        while (flagPause) {
            // Sleep for 100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        // Do a few checks on the league, note and etc
        basicChecks(item);
        if (item.isDiscard())
            return;

        // Get price as boolean and currency type as index
        parseNote(item);
        if (item.isDiscard())
            return;

        // Make database key and find item type
        formatNameAndItemType(item);

        // Filter out white base types (but allow maps)
        if (item.getFrameType() == 0 && !item.getItemType().contains("maps"))
            return;

        // Check gem info or check links and variants
        if (item.getFrameType() == 4) {
            checkGemInfo(item);

            if (item.isDiscard())
                return;
        } else {
            checkSixLink(item);
            checkSpecialItemVariant(item);
        }

        // Add item to database
        database.rawDataAddEntry(item);
    }

    private void basicChecks(Item item) {
        //  Name: basicChecks()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Method that does a few basic checks on items
        //  Parent methods:
        //      checkItem()

        if (item.getNote().equals("")) {
            // Filter out items without prices
            item.setDiscard();
        } else if (item.getFrameType() == 1 || item.getFrameType() == 2 || item.getFrameType() == 7) {
            // Filter out unpriceable items
            item.setDiscard();
        } else if (!item.isIdentified()) {
            // Filter out unidentified items
            item.setDiscard();
        } else if (item.isCorrupted() && item.getFrameType() != 4) {
            // Filter out corrupted items besides gems
            item.setDiscard();
        } else if (item.getLeague().contains("SSF")) {
            // Filter out SSF leagues as trading there is disabled
            item.setDiscard();
        }
        // TODO: add filter for enchanted items
    }

    private void parseNote(Item item) {
        //  Name: parseNote()
        //  Date created: 28.11.2017
        //  Last modified: 01.12.2017
        //  Description: Method that checks and formats items notes
        //  Parent methods:
        //      checkItem()

        String[] noteList = item.getNote().split(" ");
        Double price;

        // Make sure note_list has 3 strings (eg ["~b/o", "5.3", "chaos"])
        if (noteList.length < 3) {
            item.setDiscard();
            return;
        } else if (!noteList[0].equalsIgnoreCase("~b/o") && !noteList[0].equalsIgnoreCase("~price")) {
            item.setDiscard();
            return;
        }

        // If the price has a ration then split it (eg ["5, 3"] with or ["24.3"] without a ration)
        String[] priceArray = noteList[1].split("/");

        // Try to figure out if price is numeric
        try {
            if (priceArray.length == 1)
                price = Double.parseDouble(priceArray[0]);
            else
                price = Double.parseDouble(priceArray[0]) / Double.parseDouble(priceArray[1]);
        } catch (Exception ex) { // TODO more specific exceptions
            item.setDiscard();
            return;
        }

        // Assign price to item
        item.setPrice(Math.round(price * 1000) / 1000.0);

        // See if the currency type listed is valid currency type
        if (!currencyShorthands.containsKey(noteList[2])) {
            item.setDiscard();
            return;
        }

        // Add currency type to item
        item.setPriceType(baseCurrencyIndexes.get(currencyShorthands.get(noteList[2])));
    }

    private void formatNameAndItemType(Item item) {
        //  Name: formatNameAndItemType()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Format the item's full name and find the item type
        //  Parent methods:
        //      checkItem()

        // Start key with league
        item.addKey(item.getLeague());

        // Get the item's type
        String[] splitItemType = item.getIcon().split("/");
        String itemType = splitItemType[splitItemType.length - 2];

        // Make sure even the weird items get a correct item type
        if (splitItemType[splitItemType.length - 1].equals("Item.png")) {
            itemType = "Flasks";
        } else if (item.getFrameType() == 8) {
            // Prophecy items have the same icon category as currency
            itemType = "Prophecy";
        }

        // Set the value in the item object
        item.setItemType(itemType);
        item.addKey("|" + itemType);

        // Format the name that will serve as the database key
        if (item.getName().equals("")) {
            item.addKey("|" + item.getTypeLine());
        } else {
            // Get rid of unnecessary formatting (this might've gotten removed in JSON deserialization)
            item.addKey("|" + item.getName().replace("<<set:MS>><<set:M>><<set:S>>", ""));
            if (!item.getTypeLine().equals(""))
                item.addKey("|" + item.getTypeLine());
        }

        // Add frameType to key if it isn't currency or gem-related
        if (item.getFrameType() != 4 && item.getFrameType() != 5) // TODO: add more frameTypes
            item.addKey("|" + item.getFrameType());

    }

    private void checkGemInfo(Item item) {
        //  Name: checkGemInfo()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Checks gem-specific information
        //  Parent methods:
        //      checkItem()

        int lvl = -1;
        int quality = 0;

        // Attempt to extract lvl and quality from item info
        for (Properties prop : item.getProperties()) {
            if (prop.getName().equals("Level")) {
                lvl = Integer.parseInt(prop.getValues().get(0).get(0).split(" ")[0]);
            } else if (prop.getName().equals("Quality")) {
                quality = Integer.parseInt(prop.getValues().get(0).get(0).replace("+", "").replace("%", ""));
            }
        }

        // If quality or lvl was not found, return
        if (lvl == -1) {
            item.setDiscard();
            return;
        }

        // Begin the long block that filters out gems based on a number of properties
        if (item.getName().equals("Empower Support") || item.getName().equals("Enlighten Support") || item.getName().equals("Enhance Support")) {
            if (item.isCorrupted()) {
                if (lvl == 4 || lvl == 3)
                    quality = 0;
                else {
                    item.setDiscard();
                    return;
                }
            } else {
                if (quality < 10)
                    quality = 0;
                else if (quality > 17)
                    quality = 20;
                else {
                    item.setDiscard();
                    return;
                }
            }
        } else {
            if (item.isCorrupted()) {
                if (item.getItemType().equals("VaalGems")) {
                    if (lvl < 10 && quality == 20)
                        lvl = 0;
                    else if (lvl == 20 && quality == 20)
                        ; // TODO: improve this
                    else {
                        item.setDiscard();
                        return;
                    }
                } else {
                    if (lvl == 21 && quality == 20)
                        ;
                    else if (lvl == 20 && quality == 23)
                        ;
                    else if (lvl == 20 && quality == 20)
                        ;
                    else {
                        item.setDiscard();
                        return;
                    }
                }
            } else {
                if (lvl < 10 && quality == 20)
                    lvl = 0;
                else if (lvl == 20 && quality == 20)
                    ;
                else if (lvl == 20 && quality < 10)
                    quality = 0;
                else {
                    item.setDiscard();
                    return;
                }
            }
        }

        // Add the lvl and key to database key
        item.addKey("|" + lvl + "|" + quality);

        // Add corruption notifier
        if (item.isCorrupted())
            item.addKey("|1");
        else
            item.addKey("|0");


    }

    private void checkSixLink(Item item) {
        //  Name: checkSixLink()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Since 6-links are naturally more expensive, assign them a separate database key
        //  Parent methods:
        //      checkItem()

        // Filter out items that can't have 6 links
        if (!potentialSixLinkItems.contains(item.getItemType())) {
            item.setDiscard();
            return;
        }

        // Group links together
        Integer[] links = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Socket socket : item.getSockets()) {
            //links.set(socket.getGroup(), links.get(socket.getGroup()) + 1);
            links[socket.getGroup()]++;
        }

        // Find largest single link
        int maxLinks = 0;
        for (Integer link : links) {
            if (link > maxLinks)
                maxLinks = link;
        }

        // Update database key accordingly
        if (maxLinks == 6)
            item.addKey("|6L");
        else if (maxLinks == 5)
            item.addKey("|5L");
        else
            item.addKey("|0L");
    }

    private void checkSpecialItemVariant(Item item) {
        //  Name: checkSpecialItemVariant()
        //  Date created: 28.11.2017
        //  Last modified: 29.11.2017
        //  Description: Check if the item has a special variant, eg vessel of vinktar
        //  Parent methods:
        //      checkItem()

        // Skip non-special items
        if (!itemVariants.containsKey(item.getName()))
            return;

        String keySuffix = "";

        // Atziri's Splendour is the base of my existence. Try to determine the type of Atziri's Splendour by looking
        // at the item properties
        if (item.getName().equals("Atziri's Splendour")) {
            int armour = 0;
            int evasion = 0;
            int energy = 0;

            // Find each property's amount
            for (Properties prop : item.getProperties()) {
                switch (prop.getName()) {
                    case "Armour":
                        armour = Integer.parseInt(prop.getValues().get(0).get(0));
                        break;
                    case "Evasion Rating":
                        evasion = Integer.parseInt(prop.getValues().get(0).get(0));
                        break;
                    case "Energy Shield":
                        energy = Integer.parseInt(prop.getValues().get(0).get(0));
                        break;
                }
            }

            // Run them through this massive IF block to determine the variant
            // Values taken from https://pathofexile.gamepedia.com/Atziri%27s_Splendour (at 29.11.2017)
            if (1052 <= armour && armour <= 1118) {
                if (energy == 76) {
                    keySuffix = "|var(ar/ev/li)";
                } else if (204 <= energy && energy <= 217) {
                    keySuffix = "|var(ar/es/li)";
                } else if (428 <= energy && energy <= 489) {
                    keySuffix = "|var(ar/es)";
                }
            } else if (armour > 1600) {
                keySuffix = "|var(ar)";
            } else if (evasion > 1600) {
                keySuffix = "|var(ev)";
            } else if (energy > 500) {
                keySuffix = "|var(es)";
            } else if (1283 <= armour && armour <= 1513) {
                keySuffix = "|var(ar/ev/es)";
            } else if (1052 <= evasion && evasion <= 1118) {
                if (energy > 400) {
                    keySuffix = "|var(ev/es)";
                } else {
                    keySuffix = "|var(ev/es/li)";
                }
            }
        } else {
            String mod;
            // Go through preset item mods
            for (String key : itemVariants.get(item.getName()).keySet()) {
                mod = itemVariants.get(item.getName()).get(key);
                // Go through item mods
                for (String itemMod : item.getExplicitMods()) {
                    // Attempt to match preset mod with item mod
                    if (itemMod.contains(mod)) {
                        keySuffix = "|var(" + key + ")";
                        break; // TODO: exit upper-level for loop
                    }
                }
            }
        }

        // Add new key suffix to existing key
        item.addKey(keySuffix);
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void setFlagLocalRun(boolean flagLocalRun) {
        this.flagLocalRun = flagLocalRun;
    }

    public void setSleepLength(int sleepLength) {
        this.sleepLength = sleepLength;
    }

    public void setFlagPause(boolean flagPause) {
        this.flagPause = flagPause;
    }

    public boolean isFlagPause() {
        return flagPause;
    }
}
