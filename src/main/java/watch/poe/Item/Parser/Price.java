package poe.Item.Parser;

import poe.Managers.RelationManager;

public class Price {
    private static RelationManager relationManager;
    private boolean hasPrice;
    private Integer currencyId;
    private double price;

    public Price(String itemNote, String stashNote) {
        parseBuyoutNote(itemNote);

        if (!hasPrice) {
            parseBuyoutNote(stashNote);
        }
    }

    public static void setRelationManager(RelationManager relationManager) {
        Price.relationManager = relationManager;
    }

    public double getPrice() {
        return price;
    }

    public boolean hasPrice() {
        return hasPrice;
    }

    /**
     * Attempt to parse the buyout note the user has set
     *
     * @param buyoutNote User-set buyout note
     */
    private void parseBuyoutNote(String buyoutNote) {
        // No price set on item
        if (buyoutNote == null || buyoutNote.equals("")) {
            return;
        }

        String[] noteList = buyoutNote.split("\\s+");

        // Make sure note_list has 3 strings (eg ["~b/o", "5.3", "chaos"])
        if (noteList.length < 3 || !noteList[0].equals("~b/o") && !noteList[0].equals("~price")) {
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
            return;
        }

        // An error somewhere
        if (Double.isNaN(price)) {
            return;
        }

        // Is the listed currency valid?
        if (!relationManager.getCurrencyAliases().containsKey(noteList[2])) {
            return;
        }

        // If listed price was something retarded
        if (price < 0.0001 || price > 90000) {
            return;
        }

        // Get id of the currency the item was listed for
        currencyId = relationManager.getCurrencyAliases().get(noteList[2]);

        // Mark that we were able to get a valid price from the buyout note
        hasPrice = true;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }
}
