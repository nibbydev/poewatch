package com.sanderh;

import static com.sanderh.Main.RELATIONS;

public class Item extends Mappers.BaseItem {
    //  Name: NewItem
    //  Date created: 23.11.2017
    //  Last modified: 25.12.2017
    //  Description: Extends the JSON mapper Item, adding methods that parse, match and calculate Item-related data

    private String priceType, itemType;
    private String key = "";
    private boolean discard = false;
    private double price;

    /////////////////////////////////////////////////////////
    // Methods used to convert/calculate/extract item data //
    /////////////////////////////////////////////////////////

    public void parseItem() {
        //  Name: parseItem()
        //  Date created: 08.12.2017
        //  Last modified: 19.12.2017
        //  Description: Calls other Item class related methods.

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

        switch (getFrameType()) {
            case 0: // Normal
            case 1: // Magic
            case 2: // Rare
                if (!itemType.contains("maps")) {
                    setDiscard();
                    return;
                }

                // "Superior Ashen Wood" = "Ashen Wood"
                if (key.contains("Superior ")) {
                    key = key.replace("Superior ", "");
                }

                // Include maps under same frame type
                setFrameType(0);
                break;

            case 4: // Gem
                checkGemInfo();
                break;

            // Filter out chaos orbs
            case 5:
                if(getName().equals("Chaos Orb"))
                    setDiscard();

            default:
                checkSixLink();
                checkSpecialItemVariant();
                break;
        }
    }

    private void basicChecks() {
        //  Name: basicChecks()
        //  Date created: 28.11.2017
        //  Last modified: 17.12.2017
        //  Description: Method that does a few basic checks on items

        if (getNote().equals("")) {
            // Filter out items without prices
            setDiscard();
        } else if (getFrameType() == 1 || getFrameType() == 2 || getFrameType() == 7) {
            // Filter out unpriceable items
            setDiscard();
        } else if (!isIdentified()) {
            // Filter out unidentified items
            setDiscard();
        } else if (getLeague().contains("SSF")) {
            // Filter out SSF leagues as trading there is disabled
            setDiscard();
        } else if (getLeague().equals("false")) {
            // This is a bug in the API
            setDiscard();
        }

        // TODO: add filter for enchanted items
    }

    private void parseNote() {
        //  Name: parseNote()
        //  Date created: 28.11.2017
        //  Last modified: 25.12.2017
        //  Description: Checks and formats notes (user-inputted textfields that usually contain price data)

        String[] noteList = getNote().split(" ");
        Double price;

        // Make sure note_list has 3 strings (eg ["~b/o", "5.3", "chaos"])
        if (noteList.length < 3) {
            setDiscard();
            return;
        } else if (!noteList[0].equalsIgnoreCase("~b/o")) {
            if (!noteList[0].equalsIgnoreCase("~price")) {
                setDiscard();
                return;
            }
        }

        // If the price has a ration then split it (eg ["5, 3"] with or ["24.3"] without a ration)
        String[] priceArray = noteList[1].split("/");

        // Try to figure out if price is numeric
        try {
            if (priceArray.length == 1)
                price = Double.parseDouble(priceArray[0]);
            else
                price = Double.parseDouble(priceArray[0]) / Double.parseDouble(priceArray[1]);
        } catch (Exception ex) {
            setDiscard();
            return;
        }

        // See if the currency type listed is valid currency type
        if (!RELATIONS.shortHandToIndex.containsKey(noteList[2])) {
            setDiscard();
            return;
        }

        // Add currency type to item
        // If the seller is selling Chaos Orbs (the default currency), swap the places of the names
        // Ie [1 Chaos Orb]+"~b/o 6 fus" ---> [6 Orb of Fusing]+"~b/o 1 chaos"
        if (getTypeLine().equals("Chaos Orb")){
            setTypeLine(RELATIONS.shortHandToName.get(noteList[2]));
            priceType = "1";
            this.price = 1 / (Math.round(price * 1000) / 1000.0);
        } else {
            this.price = Math.round(price * 1000) / 1000.0;
            priceType = RELATIONS.shortHandToIndex.get(noteList[2]);
        }
    }

    private void formatNameAndItemType() {
        //  Name: formatNameAndItemType()
        //  Date created: 28.11.2017
        //  Last modified: 17.12.2017
        //  Description: Format the item's full name and finds the item type

        // Start key with league
        addKey(getLeague());

        // Divide large categories into smaller ones
        String[] splitItemType = getIcon().split("/");
        String iconType = splitItemType[splitItemType.length - 2].toLowerCase();

        // The API is pretty horrible, format it better
        switch (itemType) {
            case "currency":
                // Prophecy items have the same icon category as currency
                if(getFrameType() == 8)
                    itemType += ":prophecy";
                else if (iconType.equals("divination") || iconType.equals("currency"))
                    itemType += ":orbs";
                else
                    itemType += ":" + iconType;
                break;

            case "gems":
                switch (iconType){
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
                    if (getFrameType() == 3) {
                        // Unique IF: itemType="maps", iconType="maps", frameType=3
                        itemType += ":unique";
                    } else if (getProperties() == null){
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
        }

        // Set the value in the item object
        addKey("|" + itemType);

        // Format the name that will serve as the database key
        if (getName().equals("")) {
            addKey("|" + getTypeLine());
            setName(getTypeLine());
            setTypeLine("");
        } else {
            addKey("|" + getName());
            if (!getTypeLine().equals(""))
                addKey("|" + getTypeLine());
        }

        // Add frameType to key
        addKey("|" + getFrameType());
    }

    private void checkGemInfo() {
        //  Name: checkGemInfo()
        //  Date created: 28.11.2017
        //  Last modified: 17.12.2017
        //  Description: Checks gem-specific information

        int lvl = -1;
        int quality = 0;

        // Attempt to extract lvl and quality from item info
        for (Mappers.Properties prop : getProperties()) {
            if (prop.getName().equals("Level")) {
                lvl = Integer.parseInt(prop.getValues().get(0).get(0).split(" ")[0]);
            } else if (prop.getName().equals("Quality")) {
                quality = Integer.parseInt(prop.getValues().get(0).get(0).replace("+", "").replace("%", ""));
            }
        }

        // If quality or lvl was not found, return
        if (lvl == -1) {
            setDiscard();
            return;
        }

        // Begin the long block that filters out gems based on a number of properties
        if (key.contains("Empower Support") || key.contains("Enlighten Support") || key.contains("Enhance Support")) {
            if (isCorrupted()) {
                if (lvl == 4 || lvl == 3)
                    quality = 0;
                else {
                    setDiscard();
                    return;
                }
            } else {
                if (quality < 10)
                    quality = 0;
                else if (quality > 17)
                    quality = 20;
                else {
                    setDiscard();
                    return;
                }
            }
        } else {
            if (isCorrupted()) {
                if (key.contains("Vaal ")) {
                    if (lvl < 10 && quality == 20)
                        lvl = 0;
                    else if (lvl == 20 && quality == 20)
                        ;
                    else if (lvl == 20 && quality < 10)
                        quality = 0;
                    else {
                        setDiscard();
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
                        setDiscard();
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
                    setDiscard();
                    return;
                }
            }
        }

        // Add the lvl and key to database key
        addKey("|" + lvl + "|" + quality);

        // Add corruption notifier
        if (isCorrupted())
            addKey("|1");
        else
            addKey("|0");
    }

    private void checkSixLink() {
        //  Name: checkSixLink()
        //  Date created: 28.11.2017
        //  Last modified: 17.12.2017
        //  Description: Since 6-links are naturally more expensive, assign them a separate key

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
        if (getSockets() == null) {
            setDiscard();
            return;
        }

        // Group links together
        Integer[] links = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Mappers.Socket socket : getSockets()) {
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
            addKey("|6L");
        else if (maxLinks == 5)
            addKey("|5L");
        else
            addKey("|0L");
    }

    private void checkSpecialItemVariant() {
        //  Name: checkSpecialItemVariant()
        //  Date created: 28.11.2017
        //  Last modified: 17.12.2017
        //  Description: Check if the item has a special variant, eg vessel of vinktar

        String keySuffix = "";

        switch (getName()) {
            // Try to determine the type of Atziri's Splendour by looking at the item explicit mods
            case "Atziri's Splendour":
                switch (String.join("#", getExplicitMods().get(0).split("\\d+"))) {
                    case "#% increased Armour and Energy Shield":
                        if (getExplicitMods().get(1).contains("maximum Life"))
                            keySuffix = "|var:ar/es/li";
                        else
                            keySuffix = "|var:ar/es";
                        break;

                    case "#% increased Evasion and Energy Shield":
                        if (getExplicitMods().get(1).contains("maximum Life"))
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

                    case "#% increased Energy Shield":
                        keySuffix = "|var:es";
                        break;

                    case "#% increased Armour, Evasion and Energy Shield":
                        keySuffix = "|var:ar/ev/es";
                        break;
                }
                break;

            case "Vessel of Vinktar":
                // Attempt to match preset mod with item mod
                for (String explicitMod : getExplicitMods()) {
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
                for (String explicitMod : getExplicitMods()) {
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
                for (String explicitMod : getExplicitMods()) {
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
                for (String explicitMod : getExplicitMods()) {
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
                for (String explicitMod : getExplicitMods()) {
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

            default:
                return;
        }

        // Add new key suffix to existing key
        addKey(keySuffix);
    }

    private void parseCategory() {
        //  Name: parseCategory()
        //  Date created: 17.12.2017
        //  Last modified: 17.12.2017
        //  Description: Gets text-value(s) from category Object

        // A rough outline of what it is meant to do:
        // {"category": "jewels"}                  -> "jewels"                -> {"jewels"}             -> "jewels"
        // {"category": {"armour": ["gloves"]}}    -> "{armour=[gloves]}"     -> {"armour", "gloves"}   -> "armour:gloves"
        // {"category": {"weapons": ["bow"]}}      -> "{weapons=[bow]}"       -> {"weapons", "bow"}     -> "weapons:bow"

        String asString = getCategory().toString();

        if (asString.contains("=")) {
            // Removes brackets: "{armour=[gloves]}" -> "armour=[gloves]"
            asString = asString.substring(1, asString.length() - 1);
            // Split: "armour=[gloves]" -> {"armour", "[gloves]"}
            String[] splitString = asString.split("=");
            // Add "armour" to final string, remove brackets around string (eg: "[gloves]" -> "gloves") and add to final string
            itemType = splitString[0] + ":" + splitString[1].substring(1, splitString[1].length() - 1);
        } else {
            itemType = asString;
        }
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public boolean isDiscard() {
        return discard;
    }

    public void setDiscard() {
        this.discard = true;
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
