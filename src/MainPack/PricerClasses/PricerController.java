package MainPack.PricerClasses;

import MainPack.MapperClasses.Item;
import MainPack.MapperClasses.Properties;
import MainPack.MapperClasses.Socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PricerController extends Thread {

    private boolean flagLocalRun = true;
    private boolean flagPause = false; // TODO: add controller methods
    private static Map<String, String> currencyShorthands;
    private static ArrayList<String> specialGems;
    private static ArrayList<String> potentialSixLinkItems;
    private static Database database;

    public PricerController() {
        /*  Name: PricerController()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that sets default values to the class's static variables
        */

        // Suggested solution for putting a lot of values in at once
        // https://stackoverflow.com/questions/8261075/adding-multiple-entries-to-a-hashmap-at-once-in-one-statement
        currencyShorthands = new HashMap<>(){{
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

        // These items have special values. They need to be analyzed differently
        specialGems = new ArrayList<>(){{
            add("Empower Support");
            add("Enlighten Support");
            add("Enhance Support");
        }};

        // These items can have up to six links. This could mean a lot higher price. Hence, we give them a separate
        // database key
        potentialSixLinkItems = new ArrayList<>(){{
            add("Staves");
            add("BodyArmours");
            add("TwoHandSwords");
            add("TwoHandMaces");
            add("TwoHandAxes");
            add("Bows");
        }};

        // Base database that holds all found price data
        database = new Database();
    }

    public void run() {
        /*  Name: run()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Contains the main loop of the pricing service
        *   Child methods:
        */

        while(true) {
            sleepWhile(10 * 60);

            // Prepare for database building
            flagPause = true;
            database.buildCurrencyDatabase();
            database.buildItemDatabase();
            flagPause = false;

            // Break if run flag has been lowered
            if(!flagLocalRun)
                break;
        }

    }

    private void sleepWhile(int howLongInSeconds){
        /*  Name: sleepWhile()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Sleeps for <howLongInSeconds> seconds
        *   Parent methods:
        *       run()
        */

        for (int i = 0; i < howLongInSeconds; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Break if run flag has been lowered
            if(!flagLocalRun)
                break;
        }
    }

    /*
     * Get/set values outside method
     */

    public void setFlagLocalRun(boolean flagLocalRun) {
        this.flagLocalRun = flagLocalRun;
    }

    /*
     * Check and parse items
     */

    public void checkItem(Item item){
        /*  Name: checkItem()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that's used to add entries to the databases
        *   Child methods:
        */

        // Pause during I/O operations
        while(flagPause){
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
        if (item.getFrameType() == 0 && item.getItemType().contains("maps"))
            return; // TODO: verify this works as intended

        // Check gem info
        if(item.getFrameType() == 4){
            // Check gem info (checks gem info)
            checkGemInfo(item);
            if(item.isDiscard())
                return;
        } else {
            checkSixLink(item);
            checkSpecialItemVariant(item);
        }

        // Add item to database
        database.rawDataAddEntry(
                item.getLeague(),
                item.getItemType(),
                item.getKey(),
                item.getPrice(),
                item.getPriceType()
        );
    }

    private void basicChecks(Item item) {
        /*  Name: basicChecks()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that does a few basic checks on items
        *   Parent methods:
        *       checkItem()
        */

        if (item.getNote().equals("")) {
            // Filter out items without prices
            item.setDiscard();
        } else if (item.getFrameType() != 5) { // TODO: add more frameTypes
            // Filter out unpriceable items
            item.setDiscard();
        } else if (!item.isIdentified()) {
            // Filter out unidentified items
            item.setDiscard();
        } else if (item.isCorrupted() && item.getFrameType() != 4) {
            // Filter out corrupted items besides gems
            item.setDiscard();
        } else if (item.getLeague().contains("SSF")){
            // Filter out SSF leagues as trading there is disabled
            item.setDiscard();
        }
        // TODO: add filter for enchanted items
    }

    private void parseNote(Item item) {
        /*  Name: parseNote()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that checks and formats items notes
        *   Parent methods:
        *       checkItem()
        */

        String[] noteList = item.getNote().split(" ");

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
                item.setPrice(Integer.getInteger(priceArray[0]));
            else
                item.setPrice(Integer.getInteger(priceArray[0]) / Integer.getInteger(priceArray[1]));
        } catch (Exception ex){ // TODO more specific exceptions
            item.setDiscard();
            return;
        }

        // See if the currency type listed is valid currency type
        if (!currencyShorthands.containsKey(noteList[2])) {
            item.setDiscard();
            return;
        }

        // Add currency type to item
        item.setPriceType(currencyShorthands.get(noteList[2])); // TODO: indexes[shorthands[note[2]]]
    }

    private void formatNameAndItemType(Item item) {
        /*  Name: formatNameAndItemType()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Format the item's full name and find the item type
        *   Parent methods:
        *       checkItem()
        */

        // Format the name that will serve as the database key
        if (item.getName().equals("")) {
            item.addKey(item.getTypeLine());
        } else {
            // Get rid of unnecessary formatting (this might've gotten removed in JSON deserialization)
            item.addKey(item.getName().replace("<<set:MS>><<set:M>><<set:S>>", ""));
            if(!item.getTypeLine().equals(""))
                item.addKey(item.getName() + "|" + item.getTypeLine());
        }

        // Add frameType to key if it isn't currency or gem-related
        if (item.getFrameType() != 4 && item.getFrameType() != 5) // TODO: add more frameTypes
            item.addKey(item.getName() + "|" + item.getTypeLine());

        // Get the item's type
        String[] splitItemType = item.getIcon().split("/");
        String itemType = splitItemType[splitItemType.length - 2]; // TODO: verify this is correct (ie not -3)

        // Make sure even the weird items get a correct item type
        if (splitItemType[splitItemType.length - 1].equals("Item.png")) {
            itemType = "Flasks";
        } else if (item.getFrameType() == 8) {
            // Prophecy items have the same icon category as currency
            itemType = "Prophecy";
        }

        // Set the value in the item object
        item.setItemType(itemType);
    }

    private void checkGemInfo(Item item){
        /*  Name: checkGemInfo()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Checks gem-specific information
        *   Parent methods:
        *       checkItem()
        */

        int lvl = -1;
        int quality = -1;

        // Attempt to extract lvl and quality from item info
        for (Properties prop: item.getProperties()) {
            if (prop.getName().equals("Level"))
                lvl = Integer.getInteger(prop.getValues().get(0).get(0).split(" ")[0]);
            else if (prop.getName().equals("Quality"))
                quality = Integer.getInteger(prop.getValues().get(0).get(0).replace("+", "").replace("%", ""));
        }

        // If quality or lvl was not found, return
        if(lvl + quality == -2) {
            item.setDiscard();
            return;
        }

        // Begin the long block that filters out gems based on a number of properties
        if(specialGems.contains(item.getName())){
            if(item.isCorrupted()){
                if(lvl == 4 || lvl == 3)
                    quality = 0;
                else {
                    item.setDiscard();
                    return;
                }
            } else {
                if(quality < 10)
                    quality = 0;
                else if (quality > 17)
                    quality = 20;
                else {
                    item.setDiscard();
                    return;
                }
            }
        } else {
            if(item.isCorrupted()){
                if(item.getItemType().equals("VaalGems")) {
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
        /*  Name: checkSixLink()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Since 6-links are naturally more expensive, assign them a separate database key
        *   Parent methods:
        *       checkItem()
        */

        // Filter out items that can't have 6 links
        if(!potentialSixLinkItems.contains(item.getItemType())) {
            item.setDiscard();
            return;
        }

        // Group links together
        Integer[] links = new Integer[]{0,0,0,0,0,0};
        for (Socket socket: item.getSockets()) {
            //links.set(socket.getGroup(), links.get(socket.getGroup()) + 1);
            links[socket.getGroup()]++;
        }

        // Find largest single link
        int maxLinks = 0;
        for (Integer link: links) {
            if(link > maxLinks)
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

    // TODO: add this
    private void checkSpecialItemVariant(Item item) {
        /*  Name: checkSpecialItemVariant()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Placeholder method. Will be used to differentiate between different item types
        *   Parent methods:
        *       checkItem()
        */


    }





}
