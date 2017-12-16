package com.sanderh.Mappers;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    //  Name: Item
    //  Date created: 23.11.2017
    //  Last modified: 16.12.2017
    //  Description: Class used for deserializing a JSON string

    // Variables used by deserializer
    private int w, h, x, y, ilvl, frameType;
    private boolean identified = true;
    private boolean corrupted = false;
    private String icon, league, id, name, typeLine;
    private String note = "";
    private List<Properties> properties;
    private List<Socket> sockets;
    private List<String> explicitMods;

    // Variables not used by deserializer
    private String priceType, itemType;
    private String key = "";
    private boolean discard = false;
    private double price;

    // Map of potential keyword-value pairs
    private static final Map<String, String> currencyShorthandsMap = new TreeMap<>() {{
        put("exalted", "Exalted Orb");
        put("exalts", "Exalted Orb");
        put("exalt", "Exalted Orb");
        put("exa", "Exalted Orb");
        put("exe", "Exalted Orb");
        put("ex", "Exalted Orb");
        put("chaos", "Chaos Orb");
        put("choas", "Chaos Orb");
        put("chao", "Chaos Orb");
        put("c", "Chaos Orb");
        put("regret", "Orb of Regret");
        put("regrets", "Orb of Regret");
        put("divine", "Divine Orb");
        put("div", "Divine Orb");
        put("chisel", "Cartographer's Chisel");
        put("chis", "Cartographer's Chisel");
        put("cart", "Cartographer's Chisel");
        put("chisels", "Cartographer's Chisel");
        put("alchemy", "Orb of Alchemy");
        put("alch", "Orb of Alchemy");
        put("alc", "Orb of Alchemy");
        put("alts", "Orb of Alteration");
        put("alteration", "Orb of Alteration");
        put("alt", "Orb of Alteration");
        put("fusing", "Orb of Fusing");
        put("fus", "Orb of Fusing");
        put("fusings", "Orb of Fusing");
        put("fuse", "Orb of Fusing");
        put("regal", "Regal Orb");
        put("rega", "Regal Orb");
        put("gcp", "Gemcutter's Prism");
        put("gemc", "Gemcutter's Prism");
        put("jeweller", "Jeweller's Orb");
        put("jewellers", "Jeweller's Orb");
        put("jewel", "Jeweller's Orb");
        put("jew", "Jeweller's Orb");
        put("chromatics", "Chromatic Orb");
        put("chrom", "Chromatic Orb");
        put("chromes", "Chromatic Orb");
        put("chrome", "Chromatic Orb");
        put("chromatic", "Chromatic Orb");
        put("bles", "Blessed Orb");
        put("blessed", "Blessed Orb");
        put("bless", "Blessed Orb");
        put("chance", "Orb of Chance");
        put("chanc", "Orb of Chance");
        put("vaal", "Vaal Orb");
        put("scour", "Orb of Scouring");
        put("scouring", "Orb of Scouring");
        put("silver", "Silver Coin");
        put("aug", "Orb of Augmentation");
        put("mirror", "Mirror of Kalandra");
    }};

    /////////////////////////////////////////////////////////
    // Methods used to convert/calculate/extract item data //
    /////////////////////////////////////////////////////////

    public void parseItem() {
        //  Name: parseItem()
        //  Date created: 08.12.2017
        //  Last modified: 16.12.2017
        //  Description: Calls other Item class related methods.

        // Do a few checks on the league, note and etc
        basicChecks();
        if (discard)
            return;

        // Get price as boolean and currency type as index
        parseNote();
        if (discard)
            return;

        // Make database key and find item type
        formatNameAndItemType();

        switch (frameType) {
            case 0: // Normal
            case 1: // Magic
            case 2: // Rare
                switch (itemType){
                    case "New":
                    case "Maps":
                    case "AtlasMaps":
                    case "Atlas2Maps":
                    case "act4maps":
                        break;
                    default:
                        setDiscard();
                        return;
                }

                if (key.contains("Superior ")) {
                    // "Superior Ashen Wood" = "Ashen Wood"
                    key = key.replace("Superior ", "");
                }

                // Include maps under similar frame type
                frameType = 0;

                break;

            case 4: // Gem
                checkGemInfo();
                break;

            default:
                checkSixLink();
                checkSpecialItemVariant();
        }
    }

    private void basicChecks() {
        //  Name: basicChecks()
        //  Date created: 28.11.2017
        //  Last modified: 16.12.2017
        //  Description: Method that does a few basic checks on items

        if (note.equals("")) {
            // Filter out items without prices
            setDiscard();
        } else if (frameType == 1 || frameType == 2 || frameType == 7) {
            // Filter out unpriceable items
            setDiscard();
        } else if (!identified) {
            // Filter out unidentified items
            setDiscard();
        //} else if (corrupted && frameType != 4) {
        //    // Filter out corrupted items besides gems
        //    setDiscard();
        } else if (league.contains("SSF")) {
            // Filter out SSF leagues as trading there is disabled
            setDiscard();
        } else if (league.equals("false")) {
            // This is a bug in the API
            setDiscard();
        }

        // TODO: add filter for enchanted items
    }

    private void parseNote() {
        //  Name: parseNote()
        //  Date created: 28.11.2017
        //  Last modified: 09.12.2017
        //  Description: Checks and formats notes (user-inputted textfields that usually contain price data)
        //  Parent methods:
        //      parseItem()

        String[] noteList = note.split(" ");
        Double price;

        // Make sure note_list has 3 strings (eg ["~b/o", "5.3", "chaos"])
        if (noteList.length < 3) {
            setDiscard();
            return;
        } else if (!noteList[0].equalsIgnoreCase("~b/o") && !noteList[0].equalsIgnoreCase("~price")) {
            setDiscard();
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
        } catch (Exception ex) {
            setDiscard();
            return;
        }

        // Assign price to item
        this.price = Math.round(price * 1000) / 1000.0;

        // See if the currency type listed is valid currency type
        if (!currencyShorthandsMap.containsKey(noteList[2])) {
            setDiscard();
            return;
        }

        // Add currency type to item
        this.priceType = currencyShorthandsMap.get(noteList[2]);
    }

    private void formatNameAndItemType() {
        //  Name: formatNameAndItemType()
        //  Date created: 28.11.2017
        //  Last modified: 16.12.2017
        //  Description: Format the item's full name and finds the item type

        // Start key with league
        addKey(league);

        // Get the item's type
        String[] splitItemType = icon.split("/");
        String itemType = splitItemType[splitItemType.length - 2];

        // Make sure even the weird items get a correct item type
        if (splitItemType[splitItemType.length - 1].equals("Item.png")) {
            itemType = "Flasks";
        } else if (frameType == 8) {
            // Prophecy items have the same icon category as currency
            itemType = "Prophecy";
        }

        // Set the value in the item object
        this.itemType = itemType;
        addKey("|" + itemType);

        // Format the name that will serve as the database key
        if (name.equals("")) {
            addKey("|" + typeLine);
            name = typeLine;
            typeLine = "";
        } else {
            addKey("|" + name);
            if (!typeLine.equals(""))
                addKey("|" + typeLine);
        }

        // Add frameType to key
        addKey("|" + frameType);
    }

    private void checkGemInfo() {
        //  Name: checkGemInfo()
        //  Date created: 28.11.2017
        //  Last modified: 16.12.2017
        //  Description: Checks gem-specific information

        int lvl = -1;
        int quality = 0;

        // Attempt to extract lvl and quality from item info
        for (Properties prop : properties) {
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
            if (corrupted) {
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
            if (corrupted) {
                if (itemType.equals("VaalGems")) {
                    if (lvl < 10 && quality == 20)
                        lvl = 0;
                    else if (lvl == 10 || quality == 20)
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
        if (corrupted)
            addKey("|1");
        else
            addKey("|0");
    }

    private void checkSixLink() {
        //  Name: checkSixLink()
        //  Date created: 28.11.2017
        //  Last modified: 16.12.2017
        //  Description: Since 6-links are naturally more expensive, assign them a separate key

        // Filter out items that can have 6 links
        switch (itemType) {
            case "Staves":
            case "BodyArmours":
            case "TwoHandSwords":
            case "TwoHandMaces":
            case "TwoHandAxes":
            case "Bows":
                break;
            default:
                return;
        }

        // This was an error somehow, somewhere
        if (sockets == null) {
            setDiscard();
            return;
        }

        // Group links together
        Integer[] links = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Socket socket : sockets) {
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
        //  Last modified: 16.12.2017
        //  Description: Check if the item has a special variant, eg vessel of vinktar
        //  Parent methods:
        //      parseItem()

        String keySuffix = "";

        switch (name) {
            // Try to determine the type of Atziri's Splendour by looking at the item explicit mods
            case "Atziri's Splendour":
                switch (String.join("#", explicitMods.get(0).split("\\d+"))) {
                    case "#% increased Armour and Energy Shield":
                        if (explicitMods.get(1).contains("maximum Life"))
                            keySuffix = "|var(ar/es/li)";
                        else
                            keySuffix = "|var(ar/es)";
                        break;

                    case "#% increased Evasion and Energy Shield":
                        if (explicitMods.get(1).contains("maximum Life"))
                            keySuffix = "|var(ev/es/li)";
                        else
                            keySuffix = "|var(ev/es)";
                        break;

                    case "#% increased Armour and Evasion":
                        keySuffix = "|var(ar/ev)";
                        break;

                    case "#% increased Armour":
                        keySuffix = "|var(ar)";
                        break;

                    case "#% increased Evasion Rating":
                        keySuffix = "|var(ev)";
                        break;

                    case "#% increased Energy Shield":
                        keySuffix = "|var(es)";
                        break;

                    case "#% increased Armour, Evasion and Energy Shield":
                        keySuffix = "|var(ar/ev/es)";
                        break;
                }
                break;

            case "Vessel of Vinktar":
                // Attempt to match preset mod with item mod
                for (String itemMod : explicitMods) {
                    if (itemMod.contains("Lightning Damage to Spells")) {
                        keySuffix = "|var(spells)";
                        break;
                    } else if (itemMod.contains("Lightning Damage to Attacks")) {
                        keySuffix = "|var(attacks)";
                        break;
                    } else if (itemMod.contains("Converted to Lightning")) {
                        keySuffix = "|var(conversion)";
                        break;
                    } else if (itemMod.contains("Damage Penetrates")) {
                        keySuffix = "|var(penetration)";
                        break;
                    }
                }
                break;

            case "Doryani's Invitation":
                // Attempt to match preset mod with item mod
                for (String itemMod : explicitMods) {
                    if (itemMod.contains("increased Lightning Damage")) {
                        keySuffix = "|var(lightning)";
                        break;
                    } else if (itemMod.contains("increased Fire Damage")) {
                        keySuffix = "|var(fire)";
                        break;
                    } else if (itemMod.contains("increased Cold Damage")) {
                        keySuffix = "|var(cold)";
                        break;
                    } else if (itemMod.contains("increased Physical Damage")) {
                        keySuffix = "|var(physical)";
                        break;
                    }
                }
                break;

            case "Yriel's Fostering":
                // Attempt to match preset mod with item mod
                for (String itemMod : explicitMods) {
                    if (itemMod.contains("Chaos Damage to Attacks")) {
                        keySuffix = "|var(chaos)";
                        break;
                    } else if (itemMod.contains("Physical Damage to Attack")) {
                        keySuffix = "|var(physical)";
                        break;
                    } else if (itemMod.contains("increased Attack and Movement Speed")) {
                        keySuffix = "|var(speed)";
                        break;
                    }
                }
                break;

            case "Volkuur's Guidance":
                // Attempt to match preset mod with item mod
                for (String itemMod : explicitMods) {
                    if (itemMod.contains("Fire Damage to Spells")) {
                        keySuffix = "|var(fire)";
                        break;
                    } else if (itemMod.contains("Cold Damage to Spells")) {
                        keySuffix = "|var(cold)";
                        break;
                    } else if (itemMod.contains("Lightning Damage to Spells")) {
                        keySuffix = "|var(lightning)";
                        break;
                    }
                }
                break;

            case "Impresence":
                // Attempt to match preset mod with item mod
                for (String itemMod : explicitMods) {
                    if (itemMod.contains("Lightning Damage")) {
                        keySuffix = "|var(lightning)";
                        break;
                    } else if (itemMod.contains("Fire Damage")) {
                        keySuffix = "|var(fire)";
                        break;
                    } else if (itemMod.contains("Cold Damage")) {
                        keySuffix = "|var(cold)";
                        break;
                    } else if (itemMod.contains("Physical Damage")) {
                        keySuffix = "|var(physical)";
                        break;
                    } else if (itemMod.contains("Chaos Damage")) {
                        keySuffix = "|var(chaos)";
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

    ////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters that do not have anything to do with the deserialization //
    ////////////////////////////////////////////////////////////////////////////////

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

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public String getId() {
        return id;
    }

    public int getH() {
        return h;
    }

    public int getIlvl() {
        return ilvl;
    }

    public int getW() {
        return w;
    }

    public int getFrameType() {
        return frameType;
    }

    public String getIcon() {
        return icon;
    }

    public String getLeague() {
        return league;
    }

    public String getName() {
        return name;
    }

    public String getNote() {
        return note;
    }

    public String getTypeLine() {
        return typeLine;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isCorrupted() {
        return corrupted;
    }

    public boolean isIdentified() {
        return identified;
    }

    public List<Properties> getProperties() {
        return properties;
    }

    public List<Socket> getSockets() {
        return sockets;
    }

    public List<String> getExplicitMods() {
        return explicitMods;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCorrupted(boolean corrupted) {
        this.corrupted = corrupted;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setIdentified(boolean identified) {
        this.identified = identified;
    }

    public void setIlvl(int ilvl) {
        this.ilvl = ilvl;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public void setName(String name) {
        this.name = name.substring(name.lastIndexOf(">") + 1);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setTypeLine(String typeLine) {
        this.typeLine = typeLine;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setProperties(List<Properties> properties) {
        this.properties = properties;
    }

    public void setSockets(List<Socket> sockets) {
        this.sockets = sockets;
    }

    public void setExplicitMods(List<String> explicitMods) {
        this.explicitMods = explicitMods;
    }

}
