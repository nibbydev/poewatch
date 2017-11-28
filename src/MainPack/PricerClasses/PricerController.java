package MainPack.PricerClasses;

import MainPack.MapperClasses.Item;

import java.util.HashMap;
import java.util.Map;

public class PricerController extends Thread {

    private boolean flagPause = true;
    private static Map<String, String> currencyShorthands;

    private PricerController() {
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


    }

    public void run() {
        /*  Name: run()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Contains the main loop of the pricing service
        *   Child methods:
        */


    }

    /*
     * Methods for parsing items
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
        if(basicChecks(item))
            return;

        parseNote(item);

    }

    private boolean basicChecks(Item item) {
        /*  Name: basicChecks()
        *   Date created: 28.11.2017
        *   Last modified: 28.11.2017
        *   Description: Method that does a few basic checks on items
        *   Parent methods:
        *       checkItem()
        */

        if (item.getNote().equals(""))
            return true;
        else if (item.getFrameType() != 5) // TODO: add more frameTypes
            return true;
        else if (!item.isIdentified())
            return true;
        else if (item.isCorrupted() && item.getFrameType() != 4)
            return true;
        else if (!item.getEnchantMods().equals(""))
            return true;

        // Filter out SSF leagues as trading there is disabled
        return item.getLeague().contains("SSF");
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
            item.setDiscard(true);
            return;
        } else if (!noteList[0].equalsIgnoreCase("~b/o") && !noteList[0].equalsIgnoreCase("~price")) {
            item.setDiscard(true);
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
            item.setDiscard(true);
            return;
        }

        // See if the currency type listed is valid currency type
        if (!currencyShorthands.containsKey(noteList[2])) {
            item.setDiscard(true);
            return;
        }

        // Add currency type to item
        item.setPriceType(currencyShorthands.get(noteList[2])); // TODO: indexes[shorthands[note[2]]]
    }
}
