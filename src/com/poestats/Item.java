package com.poestats;

/**
 * Extends the JSON mapper Item, adding methods that parse, match and calculate Item-related data
 */
public class Item extends Mappers.BaseItem {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private volatile boolean discard = false;
    private String priceType, parentCategory, childCategory, key, variation, tier;
    private double price;
    private int links, level, quality;
    private boolean doNotIndex;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * "Main" controller, calls other methods
     */
    public void parseItem() {
        // Do a few checks on the league, note and etc
        basicChecks();
        if (discard) return;

        // Get price as boolean and currency type as index
        parseNote();
        if (discard) return;

        // Find out the item category (eg armour/belt/weapon etc)
        parseCategory();

        // Make database key and find item type
        formatNameAndItemType();
        if (discard) return;

        // Filter based on frametypes
        switch (frameType) {
            case 0: // Normal
            case 1: // Magic
            case 2: // Rare
                // If it's not a map, discard it
                if (!parentCategory.equals("maps")) {
                    discard = true;
                    return;
                }

                // "Superior Ashen Wood" = "Ashen Wood"
                if (name.contains("Superior ")) name = name.replace("Superior ", "");

                // Include maps under same frame type
                frameType = 0;
                break;

            case 4: // Gem
                checkGemInfo();
                break;

            case 5: // Filter out chaos orbs
                // Discard specific currency items
                checkCurrency();
                break;

            case -1:
                checkEnchant();
                break;

            default: // Everything else will pass through here
                checkSixLink();
                checkSpecialItemVariant();
                break;
        }

        // Attempt to find map tier from properties
        if (parentCategory.equals("maps") && properties != null) {
            for (Mappers.Property prop : properties) {
                if (prop.name.equals("Map Tier")) {
                    try {
                        tier = prop.values.get(0).get(0);
                    } catch (Exception ex) {
                        Main.ADMIN.log_("Couldn't parse tier:", 2);
                        Main.ADMIN._log(ex, 2);
                    }
                    break;
                }
            }
        }

        // Form the database key
        buildKey();
    }

    //------------------------------------------------------------------------------------------------------------
    // Child methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Format the item's database key
     */
    private void buildKey() {
        StringBuilder key = new StringBuilder();

        // Add item's id
        key.append(name);

        // If present, add typeline to database key
        if (typeLine != null) {
            key.append(':');
            key.append(typeLine);
        }

        // Add item's frametype to database key
        key.append('|');
        key.append(frameType);

        // If the item has a 5- or 6-link
        if (links > 4) {
            if (links == 5) key.append("|links:5");
            else if (links == 6) key.append("|links:6");
        }

        // If the item has a variation
        if (variation != null) {
            key.append("|var:");
            key.append(variation);
        }

        // If the item was a gem, add gem info
        if (parentCategory.equals("gems")) {
            key.append("|l:");
            key.append(level);
            key.append("|q:");
            key.append(quality);
            key.append("|c:");
            key.append(corrupted ? 1 : 0);
        }

        // Convert to string
        this.key = key.toString();
    }

    /**
     * Does a few basic checks on items
     */
    private void basicChecks() {
        if (note == null || note.equals("")) {
            discard = true;
        } else if (enchanted) {
            // For pricing items based on their enchants
            frameType = -1;
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
        if (!Main.RELATIONS.getCurrencyAliasToName().containsKey(noteList[2])) {
            discard = true;
            return;
        }

        // Add currency type to item
        // If the seller is selling Chaos Orbs (the default currency), swap the places of the names
        // Ie [1 Chaos Orb]+"~b/o 6 fus" ---> [6 Orb of Fusing]+"~b/o 1 chaos"
        if (typeLine.equals("Chaos Orb")) {
            typeLine = Main.RELATIONS.getCurrencyAliasToName().get(noteList[2]);
            priceType = "Chaos Orb";
            this.price = 1 / (Math.round(price * Config.item_pricePrecision) / Config.item_pricePrecision);
            // Prevents other currency items getting Chaos Orb's icon
            doNotIndex = true;
        } else {
            this.price = Math.round(price * Config.item_pricePrecision) / Config.item_pricePrecision;
            priceType = Main.RELATIONS.getCurrencyAliasToName().get(noteList[2]);
        }
    }

    /**
     * Format the item's full id and finds the item type
     */
    private void formatNameAndItemType() {
        // Format item id and/or typeline
        if (name.equals("")) {
            name = typeLine;
            typeLine = null;
        }

        // Get item's icon entries-category
        String iconCategory;
        if (icon == null) {
            // Misc currency
            iconCategory = "currency";
        } else {
            String[] splitItemType = icon.split("/");
            iconCategory = splitItemType[splitItemType.length - 2].toLowerCase();
        }

        // Divide certain items to different entries-categories
        switch (parentCategory) {
            case "currency":
                // Prophecy items have the same icon category as currency
                if (frameType == 8) parentCategory = "prophecy";
                else if (iconCategory.equals("essence")) parentCategory = "essence";
                else if (iconCategory.equals("piece")) parentCategory = "piece";
                break;
            case "gems":
                // Put vaal gems into separate entries-category
                if (childCategory.equals("activegem") && iconCategory.equals("vaalgems"))
                    childCategory = "vaalgem";
                break;
            case "monsters":
                // Completely ignore monsters
                discard = true;
                break;
            case "maps":
                // Filter all unique maps under "unique" subcategory
                if (frameType == 3) childCategory = "unique";
                else if (iconCategory.equals("breach")) childCategory = "fragment";
                else if (properties == null) childCategory = "fragment";
                else childCategory = "map";
                break;
        }
    }

    /**
     * Checks gem-specific information
     */
    private void checkGemInfo() {
        int lvl = -1;
        int qual = 0;

        // Attempt to extract lvl and quality from item info
        for (Mappers.Property prop : properties) {
            if (prop.name.equals("Level")) {
                lvl = Integer.parseInt(prop.values.get(0).get(0).split(" ")[0]);
            } else if (prop.name.equals("Quality")) {
                qual = Integer.parseInt(prop.values.get(0).get(0).replace("+", "").replace("%", ""));
            }
        }

        // If quality or lvl was not found, return
        if (lvl == -1) {
            discard = true;
            return;
        }

        // Begin the long block that filters out gems based on a number of properties
        if (name.equals("Empower Support") || name.equals("Enlighten Support") || name.equals("Enhance Support")) {
            if (qual < 6) qual = 0;
            else if (qual < 16) qual = 10;
            else qual = 20;

            // Quality doesn't matter for lvl 3 and 4
            if (lvl > 2) qual = 0;
        } else {
            if (lvl < 7) lvl = 1;           // 1  = 1,2,3,4,5,6
            else if (lvl < 19) lvl = 10;    // 10 = 7,8,9,10,11,12,13,14,15,16,17,18
            else if (lvl < 21) lvl = 20;    // 20 = 19,20
            // 21 = 21

            if (qual < 7) qual = 0;           // 0  = 0,1,2,3,4,5,6
            else if (qual < 18) qual = 10;    // 10 = 7,8,9,10,11,12,13,14,15,16,17
            else if (qual < 23) qual = 20;    // 20 = 18,19,20,21,22
            // 23 = 23

            // Gets rid of specific gems
            if (lvl < 20 && qual > 20) qual = 20;         // |4| 1|23|1 and |4|10|23|1
            else if (lvl == 21 && qual < 20) qual = 0;    // |4|21|10|1

            if (lvl < 20 && qual < 20) corrupted = false;
            if (name.contains("Vaal")) corrupted = true;
        }

        this.level = lvl;
        this.quality = qual;
    }

    /**
     * Check if item can have a 6-link, assign them a separate key
     */
    private void checkSixLink() {
        if (childCategory == null) return;

        // Filter out items that can have 6 links
        switch (childCategory) {
            case "chest":
            case "staff":
            case "twosword":
            case "twomace":
            case "twoaxe":
            case "bow":
                break;
            default:
                return;
        }

        // This was an error somehow, somewhere
        if (sockets == null) {
            links = 0;
            return;
        }

        // Group links together
        Integer[] links = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Mappers.Socket socket : sockets) {
            links[socket.group]++;
        }

        // Find largest single link
        for (Integer link : links) {
            if (link > this.links) {
                this.links = link;
            }
        }
    }

    /**
     * Check if item has a variants (e.g. Vessel of Vinktar)
     */
    private void checkSpecialItemVariant() {
        switch (name) {
            // Try to determine the type of Atziri's Splendour by looking at the item explicit mods
            case "Atziri's Splendour":
                switch (String.join("#", explicitMods.get(0).split("\\d+"))) {
                    case "#% increased Armour, Evasion and Energy Shield":
                        variation = "ar/ev/es";
                        break;

                    case "#% increased Armour and Energy Shield":
                        if (explicitMods.get(1).contains("Life"))
                            variation = "ar/es/li";
                        else
                            variation = "ar/es";
                        break;

                    case "#% increased Evasion and Energy Shield":
                        if (explicitMods.get(1).contains("Life"))
                            variation = "ev/es/li";
                        else
                            variation = "ev/es";
                        break;

                    case "#% increased Armour and Evasion":
                        variation = "ar/ev";
                        break;

                    case "#% increased Armour":
                        variation = "ar";
                        break;

                    case "#% increased Evasion Rating":
                        variation = "ev";
                        break;

                    case "+# to maximum Energy Shield":
                        variation = "es";
                        break;
                }
                break;

            case "Vessel of Vinktar":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Lightning Damage to Spells")) {
                        variation = "spells";
                        break;
                    } else if (explicitMod.contains("Lightning Damage to Attacks")) {
                        variation = "attacks";
                        break;
                    } else if (explicitMod.contains("Converted to Lightning")) {
                        variation = "conversion";
                        break;
                    } else if (explicitMod.contains("Damage Penetrates")) {
                        variation = "penetration";
                        break;
                    }
                }
                break;

            case "Doryani's Invitation":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("increased Lightning Damage")) {
                        variation = "lightning";
                        break;
                    } else if (explicitMod.contains("increased Fire Damage")) {
                        variation = "fire";
                        break;
                    } else if (explicitMod.contains("increased Cold Damage")) {
                        variation = "cold";
                        break;
                    } else if (explicitMod.contains("increased Global Physical Damage")) {
                        variation = "physical";
                        break;
                    }
                }
                break;

            case "Yriel's Fostering":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Bestial Snake")) {
                        variation = "snake";
                        break;
                    } else if (explicitMod.contains("Bestial Ursa")) {
                        variation = "ursa";
                        break;
                    } else if (explicitMod.contains("Bestial Rhoa")) {
                        variation = "rhoa";
                        break;
                    }
                }
                break;

            case "Volkuur's Guidance":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Fire Damage to Spells")) {
                        variation = "fire";
                        break;
                    } else if (explicitMod.contains("Cold Damage to Spells")) {
                        variation = "cold";
                        break;
                    } else if (explicitMod.contains("Lightning Damage to Spells")) {
                        variation = "lightning";
                        break;
                    }
                }
                break;

            case "Impresence":
                // Attempt to match preset mod with item mod
                for (String explicitMod : explicitMods) {
                    if (explicitMod.contains("Lightning Damage")) {
                        variation = "lightning";
                        break;
                    } else if (explicitMod.contains("Fire Damage")) {
                        variation = "fire";
                        break;
                    } else if (explicitMod.contains("Cold Damage")) {
                        variation = "cold";
                        break;
                    } else if (explicitMod.contains("Physical Damage")) {
                        variation = "physical";
                        break;
                    } else if (explicitMod.contains("Chaos Damage")) {
                        variation = "chaos";
                        break;
                    }
                }
                break;

            case "Lightpoacher":
            case "Shroud of the Lightless":
            case "Bubonic Trail":
            case "Tombfist":
                if (explicitMods.get(0).equals("Has 1 Abyssal Socket"))
                    variation = "1 socket";
                else if (explicitMods.get(0).equals("Has 2 Abyssal Sockets"))
                    variation = "2 sockets";
                break;

            case "The Beachhead":
                // Attempt to find map tier
                for (Mappers.Property property : properties) {
                    if (property.name.equals("Map Tier")) {
                        if (!property.values.isEmpty()) {
                            if (!property.values.get(0).isEmpty()) {
                                variation = property.values.get(0).get(0);
                            }
                        }
                    }
                }
                break;
        }
    }

    /**
     * Parses item as an enchant
     */
    private void checkEnchant() {
        parentCategory = "enchantments";
        icon = null;
        //childCategory = null;
        typeLine = null;

        if (enchantMods.size() < 1) {
            discard = true;
            return;
        }

        // Match any negative or positive integer or double
        name = enchantMods.get(0).replaceAll("[-]?\\d*\\.?\\d+", "#");

        // "#% chance to Dodge Spell Damage if you've taken Spell Damage Recently" contains a newline in the middle
        if (name.contains("\n")) name = name.replace("\n", " ");

        // Var contains the enchant value (e.g "var:1-160" or "var:120")
        String numberString = enchantMods.get(0).replaceAll("[^-.0-9]+", " ");
        String numbers = String.join("-", numberString.trim().split(" "));

        if (!numbers.equals("")) variation = numbers;
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

        parentCategory = splitString[0].toLowerCase();

        if (!splitString[1].equals("[]")) {
            childCategory =  splitString[1].substring(1, splitString[1].length() - 1).toLowerCase();
        }
    }

    /**
     * Contains some basic rules for currency
     */
    private void checkCurrency() {
        switch (name) {
            case "Chaos Orb":
            case "Imprint":
            case "Scroll Fragment":
            case "Alteration Shard":
            case "Binding Shard":
            case "Horizon Shard":
            case "Engineer's Shard":
            case "Chaos Shard":
            case "Regal Shard":
            case "Alchemy Shard":
            case "Transmutation Shard":
                discard = true;
                break;
            /*case "Portal Scroll":
            case "Scroll of Wisdom":
                if (price > 0.1) discard = true;
                break;
            case "Chromatic Orb":
            case "Cartographer's Chisel":
            case "Armourer's Scrap":
            case "Jeweller's Orb":
            case "Orb of Alteration":
            case "Orb of Augmentation":
            case "Blacksmith's Whetstone":
                if (price > 1) discard = true;
                break;
            case "Mirror of Kalandra":
                if (price < 500) discard = true;
                break;
            case "Orb of Regret":
            case "Vaal Orb":
            case "Regal Orb":
                if (price < 0.2) discard = true;
                break;
            case "Divine Orb":
            case "Ancient Orb":
            case "Exalted Orb":
                if (price < 2) discard = true;
                break;*/
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public String getPriceType() {
        return priceType;
    }

    public double getPrice() {
        return price;
    }

    public int getLevel() {
        return level;
    }

    public int getLinks() {
        return links;
    }

    public int getQuality() {
        return quality;
    }

    public String getChildCategory() {
        return childCategory;
    }

    public String getKey() {
        return key;
    }

    public String getParentCategory() {
        return parentCategory;
    }

    public String getTier() {
        return tier;
    }

    public String getVariation() {
        return variation;
    }

    public boolean isDoNotIndex() {
        return doNotIndex;
    }

    public boolean isDiscard() {
        return discard;
    }
}
