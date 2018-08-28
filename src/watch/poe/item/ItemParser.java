package watch.poe.item;

import watch.poe.Config;
import watch.poe.Main;

import java.util.ArrayList;
import java.util.Map;

public class ItemParser {
    //------------------------------------------------------------------------------------------------------------
    // User-defined variables
    //------------------------------------------------------------------------------------------------------------

    private ArrayList<Item> items = new ArrayList<>();
    private boolean discard, doNotIndex;
    private String priceType;
    private Double price;

    //------------------------------------------------------------------------------------------------------------
    // Constructor
    //------------------------------------------------------------------------------------------------------------

    public ItemParser(Mappers.BaseItem base, Map<String, Double> currencyMap) {
        // Do a few checks on the league, note and etc
        basicChecks(base);
        if (discard) return;

        // Extract price and currency type from item if present
        parseNote(base);
        if (discard) return;

        // Convert item's price to chaos if possible
        convertPrice(currencyMap);
        if (discard) return;

        // Branch item if it meets requirements
        branchItem(base);
    }

    //------------------------------------------------------------------------------------------------------------
    // Generic item methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Check if the item should be discarded immediately.
     */
    private void basicChecks(Mappers.BaseItem base) {
        // No price set on item
        if (base.getNote() == null || base.getNote().equals("")) {
            discard = true;
            return;
        }

        // Filter out items posted on the SSF leagues
        if (base.getLeague().contains("SSF")) {
            discard = true;
            return;
        }

        // Filter out a specific bug in the API
        if (base.getLeague().equals("false")) {
            discard = true;
            return;
        }
    }

    /**
     *  Extract price and currency type from item
     */
    private void parseNote(Mappers.BaseItem base) {
        String[] noteList = base.getNote().split(" ");

        // Make sure note_list has 3 strings (eg ["~b/o", "5.3", "chaos"])
        if (noteList.length < 3 || !noteList[0].equals("~b/o") && !noteList[0].equals("~price")) {
            discard = true;
            return;
        }

        // If the price has a ration then split it (eg ["5, 3"] with or ["24.3"] without a ration)
        String[] priceArray = noteList[1].split("/");

        // Try to figure out if price is numeric
        double tmpPrice;
        try {
            if (priceArray.length == 1) {
                tmpPrice = Double.parseDouble(priceArray[0]);
            } else {
                tmpPrice = Double.parseDouble(priceArray[0]) / Double.parseDouble(priceArray[1]);
            }
        } catch (Exception ex) {
            discard = true;
            return;
        }

        // See if the currency type listed is valid currency type
        if (!Main.RELATIONS.getCurrencyAliasToName().containsKey(noteList[2])) {
            discard = true;
            return;
        }

        // If the seller is selling Chaos Orbs, swap the places of the names
        // (Ie [1 Chaos Orb]+"~b/o 6 fus" ---> [6 Orb of Fusing]+"~b/o 1 chaos")
        if (base.getTypeLine().equals("Chaos Orb")) {
            String typeLineOverride = Main.RELATIONS.getCurrencyAliasToName().get(noteList[2]);
            base.setTypeLine(typeLineOverride);

            priceType = "Chaos Orb";
            price = (Math.round((1 / tmpPrice) * Config.item_pricePrecision) / Config.item_pricePrecision);

            // Prevents other currency items getting Chaos Orb's icon
            doNotIndex = true;
        } else {
            price = Math.round(tmpPrice * Config.item_pricePrecision) / Config.item_pricePrecision;
            priceType = Main.RELATIONS.getCurrencyAliasToName().get(noteList[2]);
        }
    }

    /**
     * Uses provided currencyMap to covert Item's price to chaos if necessary
     *
     * @param currencyMap Map of currency name - chaos value relations
     */
    private void convertPrice(Map<String, Double> currencyMap) {
        // If the Item's price is not in chaos, it needs to be converted to chaos using the currency map
        if (!priceType.equals("Chaos Orb")) {
            // Precaution
            if (currencyMap == null) {
                discard = true;
                return;
            }

            Double chaosValue = currencyMap.get(priceType);

            if (chaosValue == null) {
                discard = true;
                return;
            }

            price = Math.round(price * chaosValue * Config.item_pricePrecision) / Config.item_pricePrecision;
            priceType = "Chaos Orb";
        }

        // User has specified a retarded price
        if (price < 0.0001 || price > 120000) {
            discard = true;
            return;
        }
    }

    /**
     * Check if item should be branched (i.e there could be more than one database entry from that item)
     */
    private void branchItem(Mappers.BaseItem base) {
        // Branch if item is enchanted
        if (base.getEnchantMods() != null) {
            items.add(new Item("enchantment"));
        }

        // Branch if item is a crafting base
        if (base.getFrameType() < 3 || base.getIlvl() >= 68) {
            items.add(new Item("base"));
        }

        // Branch default
        items.add(new Item("default"));
    }

    /**
     * Parses branched items
     */
    public ArrayList<Item> parseItems(Mappers.BaseItem base) {
        for (Item item : items) {
            item.parse(base);
        }

        return items;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public Double getPrice() {
        return price;
    }

    public boolean isDiscard() {
        return discard;
    }

    public boolean isDoNotIndex() {
        return doNotIndex;
    }
}
