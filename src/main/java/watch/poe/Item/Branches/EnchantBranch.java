package poe.Item.Branches;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Item.Deserializers.ApiItem;
import poe.Item.Item;

public class EnchantBranch extends Item {
    private static final Logger logger = LoggerFactory.getLogger(EnchantBranch.class);

    /**
     * Default constructor
     *
     * @param apiItem Item as it appears in the stash api
     */
    public EnchantBranch(ApiItem apiItem) {
        super(apiItem);
    }

    /**
     * Branch-specific parse method that will be called in superclass constructor
     */
    @Override
    public void parse() {
        // Precaution (enchant without mods)
        if (originalItem.getEnchantMods().size() < 1) {
            discard = true;
            return;
        }

        // Override some values
        icon = "http://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
        key.type = null;
        key.frame = 0;

        // Match any negative or positive integer or double
        key.name = originalItem.getEnchantMods().get(0).replaceAll("[-]?\\d*\\.?\\d+", "#");

        // "#% chance to Dodge Spell Damage if you've taken Spell Damage Recently" contains a newline in the middle
        if (key.name.contains("\n")) {
            key.name = key.name.replace("\n", " ");
        }

        String numString = originalItem.getEnchantMods().get(0).replaceAll("[^-.0-9]+", " ").trim();
        if ("".equals(numString)) return;
        String[] numArray = numString.split(" ");

        // Some enchants have free to vary rolls, flatten them
        flattenEnchantRolls(numArray);

        // If there's at least 1 roll
        if (numArray.length == 1) {
            key.enchantMin = key.enchantMax = Float.parseFloat(numArray[0]);
        }

        // If there are two rolls
        if (numArray.length == 2) {
            key.enchantMin = Float.parseFloat(numArray[0]);
            key.enchantMax = Float.parseFloat(numArray[1]);
        }
    }

    /**
     * Determines the tier/roll of an enchant if it has mod tiers
     *
     * @param numArray List of numbers found in enchant
     */
    private void flattenEnchantRolls(String[] numArray) {
        // Assume name variable has the enchant name with numbers replaced by pound signs
        switch (key.name) {
            case "Lacerate deals # to # added Physical Damage against Bleeding Enemies":
                int num1 = Integer.parseInt(numArray[0]);
                int num2 = Integer.parseInt(numArray[1]);

                // Merc: (4-8) to (10-15)
                if (num1 <= 8 && num2 <= 15) {
                    numArray[0] = "8";
                    numArray[1] = "15";
                }
                // Uber: (14-18) to (20-25)
                else if (num1 >= 14 && num1 <= 18 && num2 <= 25 && num2 >= 20) {
                    numArray[0] = "18";
                    numArray[1] = "25";
                }

                break;
        }
    }
}
