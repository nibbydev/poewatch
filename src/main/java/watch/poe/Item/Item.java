package poe.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Item.Deserializers.ApiItem;
import poe.Item.Branches.CraftingBaseBranch;
import poe.Item.Branches.EnchantBranch;
import poe.Item.Category.CategoryEnum;
import poe.Item.Category.GroupEnum;
import poe.Managers.RelationManager;

import java.util.List;

public abstract class Item {
    private static final Logger logger = LoggerFactory.getLogger(Item.class);
    protected static RelationManager relationManager;

    protected final ApiItem originalItem;
    protected final Key key;

    protected String icon;
    protected Integer stackSize, maxStackSize;
    protected CategoryEnum category;
    protected GroupEnum group;
    protected boolean discard;

    /**
     * Default constructor
     */
    public Item(ApiItem original) {
        this.originalItem = original;
        this.key = new Key(original);

        // Needed for finding group
        icon = formatIcon(originalItem.getIcon());

        // Find the item's category and group (eg armour/belt/weapon etc)
        determineCategoryGroup();

        if (category == null) {
            logger.error("Unknown category for: " + this);
            discard = true;
            return;
        } else if (group == null) {
            logger.error("Unknown group for: " + this);
            discard = true;
            return;
        }

        parse();
    }

    /**
     * Branch-specific parse method that should be overwritten
     */
    public abstract void parse();

    /**
     * Removes any unnecessary fields from the item's icon
     *
     * @param icon Item's icon
     * @return Formatted icon URL
     */
    public static String formatIcon(String icon) {
        if (icon == null) {
            logger.error("Invalid icon passed");
            return null;
        }

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

    /**
     * Gets api category
     *
     * @return api category
     */
    private String findApiCategory() {
        return originalItem.getCategory().keySet().toArray()[0].toString();
    }

    /**
     * Given the category, gets first group
     *
     * @param apiCategory api category
     * @return api group
     */
    private String findApiGroup(String apiCategory) {
        if (originalItem.getCategory().get(apiCategory).size() > 0) {
            return originalItem.getCategory().get(apiCategory).get(0).toLowerCase();
        }

        return null;
    }

    /**
     * Find icon cdn category. For example, if the icon path is
     * "http://web.poecdn.com/image/Art/2DItems/Armours/Helmets/HarbingerShards/Shard1.png"
     * then the icon category is "HarbingerShards"
     *
     * @return Extracted category
     */
    private String findIconCategory() {
        String[] splitItemType = originalItem.getIcon().split("/");
        return splitItemType[splitItemType.length - 2].toLowerCase();
    }

    /**
     * Find item's category and group
     */
    private void determineCategoryGroup() {
        String iconCategory = findIconCategory();
        String apiCategory = findApiCategory();
        String apiGroup = findApiGroup(apiCategory);

        if (apiCategory.equals("monsters") || apiCategory.equals("leaguestones")) {
            discard = true;
            return;
        }

        // Override for enchantments
        if (this.getClass().equals(EnchantBranch.class)) {
            category = CategoryEnum.enchantment;

            // Check all groups and find matching one
            for (GroupEnum groupEnum : category.getGroups()) {
                if (groupEnum.getName().equals(apiGroup)) {
                    group = groupEnum;
                    return;
                }
            }

            discard = true;
            return;
        }

        // Override for item bases
        if (this.getClass().equals(CraftingBaseBranch.class)) {
            category = CategoryEnum.base;

            // Check all groups and find matching one
            for (GroupEnum groupEnum : category.getGroups()) {
                if (groupEnum.getName().equals(apiGroup)) {
                    group = groupEnum;
                    return;
                }
            }

            // There was no matching group which means this base is something we don't collect
            discard = true;
            return;
        }


        // Has to be before currency block as technically prophecies are counted as currency
        if (originalItem.getFrameType() == 8) {
            category = CategoryEnum.prophecy;
            group = GroupEnum.prophecy;
            return;
        }

        if (apiCategory.equals("currency")) {
            category = CategoryEnum.currency;

            if (iconCategory.equals("currency")) {
                group = GroupEnum.currency;
            } else if (iconCategory.equals("essence")) {
                group = GroupEnum.essence;
            } else if ("piece".equals(apiGroup)) { // harbinger pieces
                group = GroupEnum.piece;
            } else if (key.name.startsWith("Vial of ")) { // vials
                group = GroupEnum.vial;
            } else if ("resonator".equals(apiGroup)) {
                group = GroupEnum.resonator;
            } else if ("fossil".equals(apiGroup)) {
                group = GroupEnum.fossil;
            } else if (iconCategory.equals("breach")) { // breach splinters
                group = GroupEnum.splinter;
            } else if (key.name.startsWith("Timeless") && key.name.endsWith("Splinter")) { // legion splinters
                group = GroupEnum.splinter;
            } else if ("incubator".equals(apiGroup)) {
                group = GroupEnum.incubator;
            } else if (iconCategory.equals("divination")) { // stacked deck
                group = GroupEnum.currency;
            }

            return;
        }

        if (apiCategory.equals("gems")) {
            category = CategoryEnum.gem;

            if (iconCategory.equals("vaalgems")) {
                group = GroupEnum.vaal;
            } else if ("activegem".equals(apiGroup)) {
                group = GroupEnum.skill;
            } else if ("supportgem".equals(apiGroup)) {
                group = GroupEnum.support;
            }

            return;
        }

        if (apiCategory.equals("maps")) {
            category = CategoryEnum.map;

            if (originalItem.getFrameType() == 3 || originalItem.getFrameType() == 9) {
                group = GroupEnum.unique;
            } else if (iconCategory.equals("breach")) {
                group = GroupEnum.fragment;
            } else if (iconCategory.equals("scarabs")) {
                group = GroupEnum.scarab;
            } else if (originalItem.getProperties() == null) {
                group = GroupEnum.fragment;
            } else {
                group = GroupEnum.map;
            }

            return;
        }

        if (apiCategory.equals("cards")) {
            category = CategoryEnum.card;
            group = GroupEnum.card;
            return;
        }

        if (apiCategory.equals("flasks")) {
            category = CategoryEnum.flask;
            group = GroupEnum.flask;
            return;
        }

        if (apiCategory.equals("jewels")) {
            category = CategoryEnum.jewel;
            group = GroupEnum.jewel;
            return;
        }

        if (apiCategory.equals("armour")) {
            category = CategoryEnum.armour;

            // Check all groups and find matching one
            for (GroupEnum groupEnum : category.getGroups()) {
                if (groupEnum.getName().equals(apiGroup)) {
                    group = groupEnum;
                    return;
                }
            }

            return;
        }

        if (apiCategory.equals("weapons")) {
            category = CategoryEnum.weapon;

            // Check all groups and find matching one
            for (GroupEnum groupEnum : category.getGroups()) {
                if (groupEnum.getName().equals(apiGroup)) {
                    group = groupEnum;
                    return;
                }
            }

            return;
        }

        if (apiCategory.equals("accessories")) {
            category = CategoryEnum.accessory;

            // Check all groups and find matching one
            for (GroupEnum groupEnum : category.getGroups()) {
                if (groupEnum.getName().equals(apiGroup)) {
                    group = groupEnum;
                    return;
                }
            }
        }
    }




    @Override
    public String toString() {
        return key.toString();
    }


    public static void setRelationManager(RelationManager relationManager) {
        Item.relationManager = relationManager;
    }

    public boolean isDiscard() {
        return discard;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public GroupEnum getGroup() {
        return group;
    }

    public List<String> getExplicitMods() {
        return originalItem.getExplicitMods();
    }

    public Integer getStackSize() {
        return stackSize;
    }

    public Key getKey() {
        return key;
    }

    public Integer getMaxStackSize() {
        return maxStackSize;
    }

    public String getIcon() {
        return icon;
    }
}
