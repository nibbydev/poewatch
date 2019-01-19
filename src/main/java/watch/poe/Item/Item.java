package poe.Item;

import poe.Item.Variant.ItemVariant;
import poe.Item.Variant.VariantType;
import poe.Item.Variant.Variants;
import poe.Managers.RelationManager;

public class Item {
    private RelationManager relationManager;

    private static final ItemVariant[] variants = Variants.GetVariants();
    private static final String enchantment_icon = "http://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
    private Mappers.BaseItem base;
    private String branch;
    private Key key;

    private boolean discard, doNotIndex;
    private String category, group, variation;
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
        this.relationManager = null;
        this.branch = branch;
    }

    public Item(RelationManager relationManager, String branch) {
        this.relationManager = relationManager;
        this.branch = branch;
    }

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void parse(Mappers.BaseItem base) {
        this.base = base;

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

        // Formats some data for better parsing
        fixBaseData();

        // Find out the item category and group (eg armour/belt/weapon etc)
        extractCategory();
        if (discard) {
            return;
        }

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
        if (name == null || name.equals("") || frameType == 2) {
            name = typeLine;
            typeLine = null;
        }

        // Ignore corrupted state for non-gems and non-relics
        if (frameType != 4 && frameType != 9) {
            corrupted = null;
        }

        // Remove formatting string from name
        name = name.substring(name.lastIndexOf(">") + 1);
    }

    /**
     * Extracts category strings from the object
     */
    private void extractCategory() {
        category = base.getCategory().keySet().toArray()[0].toString();

        // Get first group if present
        if (base.getCategory().get(category).size() > 0) {
            group = base.getCategory().get(category).get(0).toLowerCase();
        }

        // Extract item's category from its icon
        String[] splitItemType = icon.split("/");
        String iconCategory = splitItemType[splitItemType.length - 2].toLowerCase();

        // Divide into specific subcategories
        switch (category) {
            case "currency":
                if (frameType == 8) {
                    category = "prophecy";
                    group = "prophecy";
                } else if (iconCategory.equals("essence")) {
                    group = "essence";
                } else if (iconCategory.equals("piece")) {
                    group = "piece";
                } else if (name.contains("Vial of ")) {
                    group = "vial";
                }

                if (group == null) {
                    group = category;
                }

                break;

            case "gems":
                category = "gem";

                if (group.equals("activegem")) {
                    if (iconCategory.equals("vaalgems")) {
                        group = "vaal";
                    } else {
                        group = "skill";
                    }
                } else {
                    group = "support";
                }
                break;

            case "monsters":
                discard = true;
                return;

            case "maps":
                category = "map";

                if (frameType == 3 || frameType == 9) {
                    group = "unique";
                } else if (iconCategory.equals("breach")) {
                    group = "fragment";
                } else if (iconCategory.equals("scarabs")) {
                    group = "scarab";
                } else if (base.getProperties() == null){
                    group = "fragment";
                } else {
                    group = "map";
                }

                break;

            case "cards":
                category = "card";
                group = category;
                break;

            case "flasks":
                category = "flask";
                group = category;
                break;

            case "jewels":
                category = "jewel";
                group = category;
                break;

            case "weapons":
                category = "weapon";
                break;

            case "accessories":
                category = "accessory";
                break;
        }

        // Override for enchantments
        if (base.getEnchantMods() != null) {
            category = "enchantment";
        }

        // Override for item bases
        if (branch.equals("base")) {
            // Only collect bases for these categories
            if (!category.equals("accessory") &&
                    !category.equals("armour") &&
                    !category.equals("jewel") &&
                    !category.equals("weapon")) {
                discard = true;
                return;
            }

            // Override category
            category = "base";
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

        // Some corrupted relics do not turn into rares and retain their relic frametypes
        if (frameType == 9) {
            if (corrupted != null && corrupted) {
                discard = true;
                return;
            }

            corrupted = null;
        }

        ilvl = null;

        switch (category) {
            case "map":         parseMaps();                break;
            case "gem":         extractGemData();           break;
            case "currency":    checkCurrencyBlacklist();   break;
            default:
                if (frameType < 3) {
                    discard = true;
                    return;
                }
        }

        if (discard) {
            return;
        }

        extractItemLinks();
        checkItemVariant();
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
        if (!category.equals("weapon") && !category.equals("armour")) {
            return;
        } else if (group == null) {
            return;
        }

        // Filter out items that can't have 6 sockets
        switch (group) {
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
    private void checkItemVariant() {
        int matches;

        for (ItemVariant itemVariant : variants) {
            if (!name.equals(itemVariant.name)) {
                continue;
            }

            for (VariantType variantType : itemVariant.variantTypes) {
                // Go though all the item's explicit modifiers and the current variant's mods
                matches = 0;
                for (String variantMod : variantType.mods) {
                    for (String itemMod : base.getExplicitMods()) {
                        if (itemMod.contains(variantMod)) {
                            // If one of the item's mods matches one of the variant's mods, increase the match counter
                            matches++;
                            break;
                        }
                    }
                }

                // If all the variant's mods were present in the item then this item will take this variant's variation
                if (matches == variantType.mods.length) {
                    this.variation = variantType.variation;
                    return;
                }
            }
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
        if (frameType == 2){
            discard = true;
            return;
        }

        // "Superior Item" = "Item"
        if (name.startsWith("Superior ")) {
            name = name.replace("Superior ", "");
        }

        // Ignore talisman bases
        if (group.equals("amulet") && name.contains("Talisman")) {
            discard = true;
            return;
        }

        // Attempt to parse item's base name
        name = relationManager.extractItemBaseName(group, name);

        if (name == null) {
            discard = true;
            return;
        }

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

    public String getCategory() {
        return category;
    }

    public String getGroup() {
        return group;
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

    public String getId() {
        return id;
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
