package ovh.poe;

import static ovh.poe.Main.CONFIG;
import static ovh.poe.Main.RELATIONS;

/**
 * Extends the JSON mapper Item, adding methods that parse, match and calculate Item-related data
 */
public class Item extends Mappers.BaseItem {
    private volatile boolean discard = false;
    private String priceType, itemType;
    private String key = "";
    private double price;

    /////////////////////////////////////////////////////////
    // Methods used to convert/calculate/extract item data //
    /////////////////////////////////////////////////////////

    /**
     * "Main" controller, calls other methods
     */
    public void parseItem() {
        // Do a few checks on the league, note and etc
        basicChecks();
        if (discard)
            return;

        // Get price as boolean and currency type as index
        parseNote();
        if (discard)
            return;

        // Find out the item category (eg armour/belt/weapon etc)
        parseCategory();

        // Make database key and find item type
        formatNameAndItemType();
        if (discard)
            return;

        switch (frameType) {
            case 0: // Normal
            case 1: // Magic
            case 2: // Rare
                if (!itemType.contains("maps")) {
                    discard = true;
                    return;
                }

                // "Superior Ashen Wood" = "Ashen Wood"
                if (key.contains("Superior ")) {
                    key = key.replace("Superior ", "");
                }

                // Include maps under same frame type
                frameType = 0;
                break;

            case 4: // Gem
                checkGemInfo();
                break;

            // Filter out chaos orbs
            case 5:
                if (name.equals("Chaos Orb")) {
                    discard = true;
                    return;
                }

            default:
                checkSixLink();
                checkSpecialItemVariant();
                break;
        }
    }

    /**
     * Does a few basic checks on items
     */
    private void basicChecks() {
        if (note.equals("")) {
            // Filter out items without prices
            discard = true;
        } else if (frameType == 1 || frameType == 2 || frameType == 7) {
            // Filter out unpriceable items
            discard = true;
        } else if (!identified) {
            // Filter out unidentified items
            discard = true;
        } else if (league.contains("SSF")) {
            // Filter out SSF leagues as trading there is disabled
            discard = true;
        } else if (league.equals("false")) {
            // This is a bug in the API
            discard = true;
        } else if (enchanted) {
            // Enchanted items usually have a much, much higher price
            discard = true;
        }
    }

    /**
     * Check and format item note (user-inputted text field that usually contain item price)
     */
    private void parseNote() {
        String[] noteList = note.split(" ");

        // Make sure note_list has 3 strings (eg ["~b/o", "5.3", "chaos"])
        if (noteList.length < 3 || !noteList[0].equals("~b/o") && !noteList[0].equals("~price")) {
            discard = true;
            return;
        }

        // If the price has a ration then split it (eg ["5, 3"] with or ["24.3"] without a ration)
        String[] priceArray = noteList[1].split("/");

        // Try to figure out if price is numeric
        Double price;
        try {
            if (priceArray.length == 1)
                price = Double.parseDouble(priceArray[0]);
            else
                price = Double.parseDouble(priceArray[0]) / Double.parseDouble(priceArray[1]);
        } catch (Exception ex) {
            discard = true;
            return;
        }

        // See if the currency type listed is valid currency type
        if (!RELATIONS.shortHandToIndex.containsKey(noteList[2])) {
            discard = true;
            return;
        }

        // Add currency type to item
        // If the seller is selling Chaos Orbs (the default currency), swap the places of the names
        // Ie [1 Chaos Orb]+"~b/o 6 fus" ---> [6 Orb of Fusing]+"~b/o 1 chaos"
        if (typeLine.equals("Chaos Orb")) {
            typeLine = RELATIONS.shortHandToName.get(noteList[2]);
            priceType = "1";
            this.price = 1 / (Math.round(price * CONFIG.pricePrecision) / CONFIG.pricePrecision);
        } else {
            this.price = Math.round(price * CONFIG.pricePrecision) / CONFIG.pricePrecision;
            priceType = RELATIONS.shortHandToIndex.get(noteList[2]);
        }
    }

    /**
     * Format the item's full name and finds the item type
     */
    private void formatNameAndItemType() {
        // Start key with league
        addKey(league);

        // Divide large categories into smaller ones
        String[] splitItemType = icon.split("/");
        String iconType = splitItemType[splitItemType.length - 2].toLowerCase();

        // The API is pretty horrible, format it better
        switch (itemType) {
            case "currency":
                // Prophecy items have the same icon category as currency
                if (frameType == 8)
                    itemType += ":prophecy";
                else if (iconType.equals("divination") || iconType.equals("currency"))
                    itemType += ":orbs";
                else
                    itemType += ":" + iconType;
                break;

            case "gems":
                switch (iconType) {
                    case "vaalgems":
                        itemType += ":vaal";
                        break;
                    case "gems":
                        itemType += ":skill";
                        break;
                    default:
                        itemType += ":" + iconType;
                        break;
                }
                break;

            case "maps":
                if (iconType.equals("maps")) {
                    if (frameType == 3) {
                        // Unique IF: itemType="maps", iconType="maps", frameType=3
                        itemType += ":unique";
                    } else if (properties == null) {
                        // Fragment IF: itemType="maps", iconType="maps", frameType=0, properties=null
                        itemType += ":fragment";
                    } else {
                        // Legacy map IF: itemType="maps", iconType="maps", frameType=0, properties!=null
                        itemType += ":" + iconType;
                    }
                } else if (iconType.equals("atlasmaps")) {
                    // Fragment IF: itemType="maps", iconType="atlasmaps", frameType=0
                    itemType += ":fragment";
                } else {
                    itemType += ":" + iconType;
                }
                break;

            case "cards":
                itemType = "divinationcards";
                break;

            case "monsters":
                discard = true;
                break;
        }

        // Set the value in the item object
        addKey("|" + itemType);

        // Format the name that will serve as the database key
        if (name.equals("")) {
            addKey("|" + typeLine);
            setName(typeLine);
            typeLine = "";
        } else {
            addKey("|" + name);
            if (!typeLine.equals(""))
                addKey("|" + typeLine);
        }

        // Add frameType to key
        addKey("|" + frameType);
    }

    /**
     * Checks gem-specific information
     */
    private void checkGemInfo() {
        int lvl = -1;
        int quality = 0;

        // Attempt to extract lvl and quality from item info
        for (Mappers.Properties prop : properties) {
            if (prop.name.equals("Level")) {
                lvl = Integer.parseInt(prop.values.get(0).get(0).split(" ")[0]);
            } else if (prop.name.equals("Quality")) {
                quality = Integer.parseInt(prop.values.get(0).get(0).replace("+", "").replace("%", ""));
            }
        }

        // If quality or lvl was not found, return
        if (lvl == -1) {
            discard = true;
            return;
        }

        boolean tempCorruptionMarker = corrupted;
        // Begin the long block that filters out gems based on a number of properties
        if (key.contains("Empower") || key.contains("Enlighten") || key.contains("Enhance")) {
            if (quality < 6) quality = 0;
            else if (quality < 16) quality = 10;
            else if (quality >= 16) quality = 20;

            // Quality doesn't matter for lvl 3 and 4
            if (lvl > 2) quality = 0;
        } else {
            if (lvl < 7) lvl = 1;           // 1  = 1,2,3,4,5,6
            else if (lvl < 19) lvl = 10;    // 10 = 7,8,9,10,11,12,13,14,15,16,17,18
            else if (lvl < 21) lvl = 20;    // 20 = 19,20
            // 21 = 21

            if (quality < 7) quality = 0;           // 0  = 0,1,2,3,4,5,6
            else if (quality < 18) quality = 10;    // 10 = 7,8,9,10,11,12,13,14,15,16,17
            else if (quality < 23) quality = 20;    // 20 = 18,19,20,21,22
            // 23 = 23

            // Gets rid of specific gems
            if (lvl < 20 && quality > 20) quality = 20;         // |4| 1|23|1 and |4|10|23|1
            else if (lvl == 21 && quality < 20) quality = 0;    // |4|21|10|1

            if (lvl < 20 && quality < 20) tempCorruptionMarker = false;
            if (key.contains("Vaal")) tempCorruptionMarker = true;
        }

        // Add the lvl and key to database key
        addKey("|" + lvl + "|" + quality);

        // Add corruption notifier
        if (tempCorruptionMarker) addKey("|1");
        else addKey("|0");
    }

    /**
     * Check if item can have a 6-link, assign them a separate key
     */
    private void checkSixLink() {
        // Filter out items that can have 6 links
        switch (itemType) {
            case "armour:chest":
            case "weapons:staff":
            case "weapons:twosword":
            case "weapons:twomace":
            case "weapons:twoaxe":
            case "weapons:bow":
                break;
            default:
                return;
        }

        // This was an error somehow, somewhere
        if (sockets == null) {
            discard = true;
            return;
        }

        // Group links together
        Integer[] links = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Mappers.Socket socket : sockets) {
            links[socket.group]++;
        }

        // Find largest single link
        int maxLinks = 0;
        for (Integer link : links) {
            if (link > maxLinks)
                maxLinks = link;
        }

        // Update database key accordingly
        if (maxLinks == 6)
            addKey("|6L");
        else if (maxLinks == 5)
            addKey("|5L");
    }

    /**
     * Check if item has a variants (e.g. Vessel of Vinktar)
     */
    private void checkSpecialItemVariant() {
        String keySuffix = "";

        switch (name) {
            // Try to determine the type of Atziri's Splendour by looking at the item explicit mods
            case "Atziri's Splendour":
                switch (String.join("#", explicitMods.get(0).split("\\d+"))) {
                    case "#% increased Armour, Evasion and Energy Shield":
                        keySuffix = "|var:ar/ev/es";
                        break;

                    case "#% increased Armour and Energy Shield":
                        if (explicitMods.get(1).contains("Life"))
                            keySuffix = "|var:ar/es/li";
                        else
                            keySuffix = "|var:ar/es";
                        break;

                    case "#% increased Evasion and Energy Shield":
                        if (explicitMods.get(1).contains("Life"))
                            keySuffix = "|var:ev/es/li";
                        else
                            keySuffix = "|var:ev/es";
                        break;

                    case "#% increased Armour and Evasion":
                        keySuffix = "|var:ar/ev";
                        break;

                    case "#% increased Armour":
                        keySuffix = "|var:ar";
                        break;

                    case "#% increased Evasion Rating":
                        keySuffix = "|var:ev";
                        break;

                    case "+# to maximum Energy Shield":
                        keySuffix = "|var:es";
                        break;
                }
                break;

            case "Vessel of Vinktar":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Lightning Damage to Spells")) {
                        keySuffix = "|var:spells";
                        break;
                    } else if (explicitMod.contains("Lightning Damage to Attacks")) {
                        keySuffix = "|var:attacks";
                        break;
                    } else if (explicitMod.contains("Converted to Lightning")) {
                        keySuffix = "|var:conversion";
                        break;
                    } else if (explicitMod.contains("Damage Penetrates")) {
                        keySuffix = "|var:penetration";
                        break;
                    }
                }
                break;

            case "Doryani's Invitation":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("increased Lightning Damage")) {
                        keySuffix = "|var:lightning";
                        break;
                    } else if (explicitMod.contains("increased Fire Damage")) {
                        keySuffix = "|var:fire";
                        break;
                    } else if (explicitMod.contains("increased Cold Damage")) {
                        keySuffix = "|var:cold";
                        break;
                    } else if (explicitMod.contains("increased Physical Damage")) {
                        keySuffix = "|var:physical";
                        break;
                    }
                }
                break;

            case "Yriel's Fostering":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Chaos Damage to Attacks")) {
                        keySuffix = "|var:chaos";
                        break;
                    } else if (explicitMod.contains("Physical Damage to Attack")) {
                        keySuffix = "|var:physical";
                        break;
                    } else if (explicitMod.contains("increased Attack and Movement Speed")) {
                        keySuffix = "|var:speed";
                        break;
                    }
                }
                break;

            case "Volkuur's Guidance":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Fire Damage to Spells")) {
                        keySuffix = "|var:fire";
                        break;
                    } else if (explicitMod.contains("Cold Damage to Spells")) {
                        keySuffix = "|var:cold";
                        break;
                    } else if (explicitMod.contains("Lightning Damage to Spells")) {
                        keySuffix = "|var:lightning";
                        break;
                    }
                }
                break;

            case "Impresence":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Lightning Damage")) {
                        keySuffix = "|var:lightning";
                        break;
                    } else if (explicitMod.contains("Fire Damage")) {
                        keySuffix = "|var:fire";
                        break;
                    } else if (explicitMod.contains("Cold Damage")) {
                        keySuffix = "|var:cold";
                        break;
                    } else if (explicitMod.contains("Physical Damage")) {
                        keySuffix = "|var:physical";
                        break;
                    } else if (explicitMod.contains("Chaos Damage")) {
                        keySuffix = "|var:chaos";
                        break;
                    }
                }
                break;

            case "Lightpoacher":
            case "Shroud of the Lightless":
            case "Bubonic Trail":
            case "Tombfist":
                if (explicitMods.get(0).equals("Has 1 Abyssal Socket")) {
                    keySuffix = "|var:1";
                    break;
                } else if (explicitMods.get(0).equals("Has 2 Abyssal Sockets")) {
                    keySuffix = "|var:2";
                    break;
                }

            default:
                return;
        }

        // Add new key suffix to existing key
        addKey(keySuffix);
    }

    /**
     * Gets text-value(s) from category object
     */
    private void parseCategory() {
        // A rough outline of what it is meant to do:
        // {"category": "jewels"}                  -> "jewels"                -> {"jewels"}             -> "jewels"
        // {"category": {"armour": ["gloves"]}}    -> "{armour=[gloves]}"     -> {"armour", "gloves"}   -> "armour:gloves"
        // {"category": {"weapons": ["bow"]}}      -> "{weapons=[bow]}"       -> {"weapons", "bow"}     -> "weapons:bow"

        String asString = category.toString();

        // "{armour=[gloves]}" -> "armour=[gloves]"
        asString = asString.substring(1, asString.length() - 1);

        // "armour=[gloves]" -> {"armour", "[gloves]"}
        String[] splitString = asString.split("=");

        if (splitString[1].equals("[]")) {
            itemType = splitString[0];
        } else {
            itemType = splitString[0] + ":" + splitString[1].substring(1, splitString[1].length() - 1);
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public boolean isDiscard() {
        return discard;
    }

    public double getPrice() {
        return price;
    }

    public String getPriceType() {
        return priceType;
    }

    public String getKey() {
        return key;
    }

    public void addKey(String partialKey) {
        this.key += partialKey;
    }
}
