package poe.Item;

import poe.Managers.RelationManager;

import java.util.ArrayList;

public class ItemParser {
    private static RelationManager relationManager;

    private ArrayList<Item> items = new ArrayList<>();
    private Mappers.BaseItem base;
    private boolean discard, doNotIndex;
    private Integer idPrice;
    private Double price;

    //------------------------------------------------------------------------------------------------------------
    // Constructor
    //------------------------------------------------------------------------------------------------------------

    public ItemParser(Mappers.BaseItem base) {
        this.base = base;

        // Do a few checks on the league, note and etc
        basicChecks(base);
        if (discard) return;

        // Extract price and currency type from item if present
        parseNote(base);
        if (discard) return;

        // Branch item if necessary
        branchItem();
        if (discard) return;

        // Parse the items
        for (Item item : items) {
            item.parse(base);
        }
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

        // Race rewards usually cost tens of times more than the average for their sweet, succulent altArt
        if (base.getRaceReward() != null) {
            discard = true;
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
        try {
            if (priceArray.length == 1) {
                price = Double.parseDouble(priceArray[0]);
            } else {
                price = Double.parseDouble(priceArray[0]) / Double.parseDouble(priceArray[1]);
            }
        } catch (Exception ex) {
            discard = true;
            return;
        }

        // If the currency type listed is not valid
        if (!relationManager.getCurrencyAliases().containsKey(noteList[2])) {
            discard = true;
            return;
        }

        // If listed price was something retarded
        if (price < 0.0001 || price > 90000) {
            discard = true;
            return;
        }

        // Get id of the currency the item was listed for
        idPrice = relationManager.getCurrencyAliases().get(noteList[2]);
    }

    /**
     * Check if item should be branched (i.e there could be more than one database entry from that item)
     */
    private void branchItem() {
        // Branch if item is enchanted
        if (base.getEnchantMods() != null) {
            items.add(new Item(Branch.enchantment));
        }

        // Branch if item is a crafting base
        if (base.getFrameType() < 3 && base.getIlvl() >= 68) {
            items.add(new Item(relationManager, Branch.base));
        }

        // Branch default
        items.add(new Item(Branch.none));
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters & setters
    //------------------------------------------------------------------------------------------------------------

    public Double getPrice() {
        return price;
    }

    public Integer getIdPrice() {
        return idPrice;
    }

    public boolean isDiscard() {
        return discard;
    }

    public boolean isDoNotIndex() {
        return doNotIndex;
    }

    public ArrayList<Item> getBranchedItems() {
        return items;
    }

    public static void setRelationManager(RelationManager relationManager) {
        ItemParser.relationManager = relationManager;
    }
}
