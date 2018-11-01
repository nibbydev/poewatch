package poe.manager.entry.item;


public class Item {
    public static final String enchantment_icon = "http://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
    private Mappers.BaseItem base;
    private String branch;
    private Key key;

    private boolean discard, doNotIndex;
    private String parentCategory, childCategory, variation;
    private Integer links, level, quality, tier;

    // Overrides
    private boolean identified;
    private String icon, name, typeLine, id;
    private Integer frameType, ilvl;
    private Boolean corrupted, shaper, elder;

    //------------------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------------------

    public Item(String branch) {
        this.branch = branch;
    }

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void parse(Mappers.BaseItem base) {
        // Get item data from the base
        identified = base.isIdentified();
        ilvl = base.getIlvl();
        frameType = base.getFrameType();
        corrupted = base.getCorrupted();
        shaper = base.getShaper();
        elder = base.getElder();
        icon = base.getIcon();
        id = base.getId();
        name = base.getName();
        typeLine = base.getTypeLine();

        // Fixes some wrongly formatted data
        fixBaseData();

        this.base = base;

        // Find out the item category (eg armour/belt/weapon etc)
        extractCategory();

        switch (branch) {
            case "enchantment": parseEnchant(); break;
            case "base":        parseBase();    break;
            case "default":     parseDefault(); break;
        }

        // Form the unique database key
        key = new Key(this);
    }

    /**
     * Fixes some wrongly formatted data
     */
    private void fixBaseData() {
        // Use typeLine as name if name missing
        if (name == null || name.equals("")) {
            name = typeLine;
            typeLine = null;
        }

        // Ignore corrupted state for non-gems
        if (frameType != 4) {
            corrupted = null;
        }

        // Remove formatting string from name
        name = name.substring(name.lastIndexOf(">") + 1);
    }

    /**
     * Extracts category strings from the object
     */
    private void extractCategory() {
        // Get parent category
        parentCategory = base.getCategory().keySet().toArray()[0].toString();

        // Get first child category if present
        if (base.getCategory().get(parentCategory).size() > 0) {
            childCategory = base.getCategory().get(parentCategory).get(0).toLowerCase();
        }

        // Extract item's category from its icon
        String[] splitItemType = icon.split("/");
        String iconCategory = splitItemType[splitItemType.length - 2].toLowerCase();

        // Divide into specific subcategories
        switch (parentCategory) {
            case "currency":
                if (frameType == 8) {
                    parentCategory = "prophecy";
                    childCategory = "prophecy";
                } else if (iconCategory.equals("essence")) {
                    childCategory = "essence";
                } else if (iconCategory.equals("piece")) {
                    childCategory = "piece";
                }
                break;

            case "gems":
                parentCategory = "gem";

                if (childCategory.equals("activegem")) {
                    if (iconCategory.equals("vaalgems")) {
                        childCategory = "vaal";
                    } else {
                        childCategory = "skill";
                    }
                } else {
                    childCategory = "support";
                }
                break;

            case "monsters":
                discard = true;
                break;

            case "maps":
                parentCategory = "map";

                if (frameType == 3 || frameType == 9) {
                    childCategory = "unique";
                } else if (iconCategory.equals("breach")) {
                    childCategory = "fragment";
                } else if (base.getProperties() == null){
                    childCategory = "fragment";
                } else {
                    childCategory = "map";
                }
                break;

            case "cards":
                parentCategory = "card";
                break;

            case "flasks":
                parentCategory = "flask";
                break;

            case "jewels":
                parentCategory = "jewel";
                break;

            case "weapons":
                parentCategory = "weapon";
                break;

            case "accessories":
                parentCategory = "accessory";
                break;
        }

        // Override for enchantments
        if (base.getEnchantMods() != null) {
            parentCategory = "enchantment";
        }

        // Override for item bases
        if (branch.equals("base")) {
            switch (parentCategory) {
                case "accessory": break;
                case "armour":    break;
                case "jewel":     break;
                case "weapon":    break;
                default:
                    discard = true;
                    return;
            }

            if (parentCategory.equals("jewels")) {
                childCategory = "jewel";
            }

            parentCategory = "base";
        }
    }

    /**
     * Removes any unnecessary fields from the item's icon
     *
     * @param icon An item's bloated URL
     * @return Formatted icon URL
     */
    public static String formatIconURL(String icon) {
        String[] splitURL = icon.split("\\?", 2);
        String fullIcon = splitURL[0];

        if (splitURL.length > 1) {
            StringBuilder paramBuilder = new StringBuilder();

            for (String param : splitURL[1].split("&")) {
                String[] splitParam = param.split("=");

                switch (splitParam[0]) {
                    case "scale":
                    case "w":
                    case "h":
                    case "mr": // shaped
                    case "mn": // background
                    case "mt": // tier
                    case "relic":
                        paramBuilder.append("&");
                        paramBuilder.append(splitParam[0]);
                        paramBuilder.append("=");
                        paramBuilder.append(splitParam[1]);
                        break;
                    default:
                        break;
                }
            }

            // If there are parameters that should be kept, add them to fullIcon
            if (paramBuilder.length() > 0) {
                // Replace the first "&" symbol with "?"
                paramBuilder.setCharAt(0, '?');
                fullIcon += paramBuilder.toString();
            }
        }

        return fullIcon;
    }

    //------------------------------------------------------------------------------------------------------------
    // Default parsing
    //------------------------------------------------------------------------------------------------------------

    /**
     * Parses item as default
     */
    private void parseDefault() {
        if (!identified) {
            discard = true;
            return;
        }

        // TODO: allow magic/rare/unidentified maps
        if (frameType == 1 || frameType == 2) {
            discard = true;
            return;
        }

        ilvl = null;

        switch (parentCategory) {
            case "map":         parseMaps();                break;
            case "gem":         extractGemData();           break;
            case "currency":    checkCurrencyBlacklist();   break;
            default:
                if (frameType < 3) {
                    discard = true;
                    return;
                }
        }

        extractItemLinks();
        checkSpecialItemVariant();
    }

    /**
     * Extract map-related data from the item
     */
    private void parseMaps() {
        // "Superior Ashen Wood" = "Ashen Wood"
        if (name.contains("Superior ")) {
            name = name.replace("Superior ", "");
        }

        // Attempt to find map tier from properties
        if (base.getProperties() != null) {
            for (Mappers.Property prop : base.getProperties()) {
                if (prop.name.equals("Map Tier")) {
                    if (!prop.values.isEmpty()) {
                        if (!prop.values.get(0).isEmpty()) {
                            String tmpTier = prop.values.get(0).get(0);
                            tier = Integer.parseInt(tmpTier);
                        }
                    }
                    break;
                }
            }
        }



        // Set frame to 0 for all non-unique
        if (frameType < 3) frameType = 0;
    }

    /**
     * Contains some basic blacklist entries for currency items
     */
    private void checkCurrencyBlacklist() {
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
            case "Bestiary Orb":
            case "Necromancy Net":
            case "Thaumaturgical Net":
            case "Reinforced Steel Net":
            case "Strong Steel Net":
            case "Simple Steel Net":
            case "Reinforced Iron Net":
            case "Strong Iron Net":
            case "Simple Iron Net":
            case "Reinforced Rope Net":
            case "Strong Rope Net":
            case "Simple Rope Net":
            case "Unshaping Orb":
            case "Master Cartographer's Seal":
            case "Journeyman Cartographer's Seal":
            case "Apprentice Cartographer's Seal":
                discard = true;
        }
    }

    /**
     * Find if item has >5 links
     */
    private void extractItemLinks() {
        // Precaution
        if (!parentCategory.equals("weapon") && !parentCategory.equals("armour")) {
            return;
        } else if (childCategory == null) {
            return;
        }

        // Filter out items that can't have 6 sockets
        switch (childCategory) {
            case "chest":       break;
            case "staff":       break;
            case "twosword":    break;
            case "twomace":     break;
            case "twoaxe":      break;
            case "bow":         break;
            default:            return;
        }

        // This was an error somehow, somewhere
        if (base.getSockets() == null) {
            return;
        }

        // Group links together
        Integer[] linkArray = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Mappers.Socket socket : base.getSockets()) {
            linkArray[socket.group]++;
        }

        // Find largest single link
        int largestLink = 0;
        for (Integer link : linkArray) {
            if (link > largestLink) {
                largestLink = link;
            }
        }

        if (largestLink > 4) {
            links = largestLink;
        }
    }

    /**
     * Check if item has a variant
     */
    private void checkSpecialItemVariant() {
        switch (name) {
            // Try to determine the type of Atziri's Splendour by looking at the item explicit mods
            case "Atziri's Splendour":
                switch (String.join("#", base.getExplicitMods().get(0).split("\\d+"))) {
                    case "#% increased Armour, Evasion and Energy Shield":
                        variation = "ar/ev/es";
                        break;

                    case "#% increased Armour and Energy Shield":
                        if (base.getExplicitMods().get(1).contains("Life"))
                            variation = "ar/es/li";
                        else
                            variation = "ar/es";
                        break;

                    case "#% increased Evasion and Energy Shield":
                        if (base.getExplicitMods().get(1).contains("Life"))
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
                for (String explicitMod : base.getExplicitMods()) {
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
                for (String explicitMod : base.getExplicitMods()) {
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
                for (String explicitMod : base.getExplicitMods()) {
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
                for (String explicitMod : base.getExplicitMods()) {
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
                for (String explicitMod : base.getExplicitMods()) {
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
            case "Command of the Pit":
                if (base.getExplicitMods().get(0).equals("Has 1 Abyssal Socket"))
                    variation = "1 socket";
                else if (base.getExplicitMods().get(0).equals("Has 2 Abyssal Sockets"))
                    variation = "2 sockets";
                break;
        }
    }

    /**
     * Finds gem-specific data
     */
    private void extractGemData() {
        int lvl = -1;
        int qual = 0;
        boolean corrupted = false;

        // Attempt to extract lvl and quality from item info
        for (Mappers.Property prop : base.getProperties()) {
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
            if (qual < 10) qual = 0;
            else qual = 20;

            // Quality doesn't matter for lvl 3 and 4
            if (lvl > 2) {
                qual = 0;

                if (this.corrupted != null) {
                    corrupted = this.corrupted;
                }
            }
        } else {
            if (lvl < 19) lvl = 1;          // lvl       1 = 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18
            else if (lvl < 21) lvl = 20;    // lvl      20 = 19,20
            // lvl      21 = 21

            if (qual < 17) qual = 0;        // quality   0 = 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16
            else if (qual < 22) qual = 20;  // quality  20 = 17,18,19,20,21
            else qual = 23;                 // quality  23 = 22,23

            // Gets rid of specific gems
            if (lvl < 20 && qual > 20) qual = 20;  // lvl:1 quality:23-> lvl:1 quality:20

            if (lvl > 20 || qual > 20) corrupted = true;
            else if (name.contains("Vaal")) corrupted = true;
        }

        this.level = lvl;
        this.quality = qual;
        this.corrupted = corrupted;
    }

    //------------------------------------------------------------------------------------------------------------
    // Enchantment parsing
    //------------------------------------------------------------------------------------------------------------

    /**
     * Parses item as an enchant
     */
    private void parseEnchant() {
        // Precaution
        if (base.getEnchantMods().size() < 1) {
            discard = true;
            return;
        }

        // Override some values
        ilvl = null;
        icon = enchantment_icon;
        typeLine = null;
        frameType = 0;

        // Attempt to extract the enchantment's name from its modifiers
        extractEnchantName();
    }

    /**
     * Attempt to extract the enchantment's name from its modifiers
     */
    private void extractEnchantName() {
        // Match any negative or positive integer or double
        name = base.getEnchantMods().get(0).replaceAll("[-]?\\d*\\.?\\d+", "#");

        // "#% chance to Dodge Spell Damage if you've taken Spell Damage Recently" contains a newline in the middle
        if (name.contains("\n")) {
            name = name.replace("\n", " ");
        }

        // Var contains the enchant value (e.g "var:1-160" or "var:120")
        String numString = base.getEnchantMods().get(0).replaceAll("[^-.0-9]+", " ").trim();
        String[] numArray = numString.split(" ");
        flattenEnchantRolls(numArray);
        String numbers = String.join("-", numArray);

        if (!numbers.equals("")) {
            variation = numbers;
        }
    }

    /**
     * Determines the tier/roll of an enchant if it has mod tiers
     *
     * @param numArray List of numbers found in enchant
     */
    private void flattenEnchantRolls(String[] numArray) {
        // Assume name variable has the enchant name with numbers replaced by pound signs
        switch (name) {
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

    //------------------------------------------------------------------------------------------------------------
    // Base parsing
    //------------------------------------------------------------------------------------------------------------

    private void parseBase() {
        // "Superior Item" = "Item"
        if (name.startsWith("Superior ")) {
            name = name.replace("Superior ", "");
        }

        // Get item's base name
        extractBaseName();
        if (discard) {
            return;
        }

        // Set frame to base value
        frameType = 0;

        // Flatten ilvl rolls
        flattenItemLevel();

        // Set influence
        if (shaper != null) {
            variation = "shaper";
        } else if (elder != null) {
            variation = "elder";
        }
    }

    /**
     * Extracts item's base name, functions as a whitelist for bases
     */
    private void extractBaseName() {
        // Precaution / shouldn't run
        if (childCategory == null) {
            discard = true;
            return;
        }

        name = BaseNameExtractor.extract(childCategory, name);

        if (name == null) {
            discard = true;
        }
    }

    /**
     * Flatten ilvl rolls
     */
    private void flattenItemLevel() {
        if (shaper == null && elder == null) {
            if         (ilvl  < 83) discard = true;
            else                    ilvl = 84;
        } else {
            if         (ilvl  < 82) discard = true;
            else if    (ilvl  > 86) ilvl = 86;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public Integer getCorrupted() {
        return corrupted == null ? null : (corrupted ? 1 : 0);
    }

    public boolean isDiscard() {
        return discard;
    }

    public boolean isDoNotIndex() {
        return doNotIndex;
    }

    public String getParentCategory() {
        return parentCategory;
    }

    public String getChildCategory() {
        return childCategory;
    }

    public String getVariation() {
        return variation;
    }

    public Key getKey() {
        return key;
    }

    public Integer getLinks() {
        return links;
    }

    public Integer getLevel() {
        return level;
    }

    public Integer getQuality() {
        return quality;
    }

    public Integer getTier() {
        return tier;
    }

    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getTypeLine() {
        return typeLine;
    }

    public long getId() {
        return Long.parseLong(id.substring(56), 16);
    }

    public Integer getFrameType() {
        return frameType;
    }

    public Integer getIlvl() {
        return ilvl;
    }

    public Boolean getShaper() {
        return shaper;
    }

    public Boolean getElder() {
        return elder;
    }
}
