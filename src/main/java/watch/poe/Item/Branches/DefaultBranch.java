package poe.Item.Branches;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Item.Deserializers.ApiItem;
import poe.Item.Deserializers.Property;
import poe.Item.Deserializers.Socket;
import poe.Item.Category.CategoryEnum;
import poe.Item.Item;
import poe.Item.VariantEnum;

public class DefaultBranch extends Item {
    private static final Logger logger = LoggerFactory.getLogger(DefaultBranch.class);

    /**
     * Default constructor
     *
     * @param apiItem Item as it appears in the stash api
     */
    public DefaultBranch(ApiItem apiItem) {
        super(apiItem);
    }

    /**
     * Branch-specific parse method that will be called in superclass constructor
     */
    @Override
    public void parse() {
        // Maps can be unidentified, corrupted, and any frame type
        if (category.equals(CategoryEnum.map)) {
            parseMaps();
        } else {
            if (!originalItem.isIdentified()) {
                discard = true;
                return;
            }

            // Don't allow any normal/magic/rare items
            if (originalItem.getFrameType() < 3 && category != CategoryEnum.currency) {
                discard = true;
                return;
            }
        }

        // Remove Synthesised prefix from item bases
        if (originalItem.getSynthesised() != null) {
            if (key.name != null && key.name.startsWith("Synthesised ")) {
                key.name = key.name.replace("Synthesised ", "");
            } else if (key.type != null && key.type.startsWith("Synthesised ")) {
                key.type = key.type.replace("Synthesised ", "");
            }
        }

        // Some corrupted relics do not turn into rares and retain their relic frametypes
        if (key.frame == 9) {
            if (originalItem.getCorrupted() != null && originalItem.getCorrupted()) {
                discard = true;
                return;
            }
        }

        if (category.equals(CategoryEnum.gem)) {
            extractGemData();
        } else if (category.equals(CategoryEnum.currency)) {
            discard = relationManager.isInCurrencyBlacklist(key.name);
        }

        if (discard) {
            return;
        }

        extractStackSize();
        extractItemLinks();

        // If present, string variation, otherwise null
        key.variation = VariantEnum.findVariant(this);
    }

    /**
     * Get the current stack and max stack sizes, if present
     */
    private void extractStackSize() {
        if (!originalItem.isStackable() || originalItem.getProperties() == null) {
            return;
        }

        stackSize = originalItem.getStackSize();

        /*
        This is what it looks like as JSON:
            "properties": [{
                "name": "Stack Size",
                "values": [["42/1000", 0]],
                "displayMode": 0
              }
            ]
         */

        // Find first stacks size property
        Property property = originalItem.getProperties().stream()
                .filter(i -> i.name.equals("Stack Size"))
                .findFirst()
                .orElse(null);

        if (property == null || property.values.isEmpty() || property.values.get(0).isEmpty()) {
            return;
        }

        String stackSizeString = property.values.get(0).get(0);
        int index = stackSizeString.indexOf("/");

        // Must contain the slash eg "42/1000"
        if (index < 0) {
            return;
        }

        // parse as int
        try {
            maxStackSize = Integer.parseUnsignedInt(stackSizeString.substring(index + 1));
        } catch (NumberFormatException ex) {
            maxStackSize = null;
        }
    }

    /**
     * Extract map-related data from the item
     */
    private void parseMaps() {
        // "Superior Ashen Wood" = "Ashen Wood"
        if (key.name.startsWith("Superior ")) {
            key.name = key.name.replace("Superior ", "");
        }

        // Find name for unidentified unique maps
        if (key.frame >= 3 && !originalItem.isIdentified()) {
            key.type = key.name;
            key.name = relationManager.findUnidUniqueMapName(key.name, key.frame);

            if (key.name == null) {
                discard = true;
                return;
            }
        }

        // Extract name from magic and rare maps
        if (key.frame == 1 || key.frame == 2) {
            key.name = relationManager.extractMapBaseName(key.name);

            // Map was not in the list
            if (key.name == null) {
                discard = true;
                return;
            }
        }

        // Attempt to find map tier from properties
        if (originalItem.getProperties() != null) {
            for (Property prop : originalItem.getProperties()) {
                if (prop.name.equals("Map Tier")) {
                    if (!prop.values.isEmpty()) {
                        if (!prop.values.get(0).isEmpty()) {
                            String tmpTier = prop.values.get(0).get(0);
                            key.mapTier = Integer.parseInt(tmpTier);
                        }
                    }
                    break;
                }
            }
        }

        if (key.mapTier == null) {
            discard = true;
            return;
        }

        extractMapSeries();

        // Set frame to 0 for all non-uniques (and non-relics)
        if (key.frame < 3) key.frame = 0;
    }

    /**
     * Find if item has >5 links
     */
    private void extractItemLinks() {
        // Precaution
        if (!category.equals(CategoryEnum.weapon) && !category.equals(CategoryEnum.armour)) {
            return;
        } else if (group == null) {
            return;
        }

        // Filter out items that can't have 6 sockets
        switch (group) {
            case chest:
            case staff:
            case twosword:
            case twomace:
            case twoaxe:
            case bow:
                break;
            default:
                return;
        }

        // This was an error somehow, somewhere
        if (originalItem.getSockets() == null) {
            return;
        }

        // Group links together
        Integer[] linkArray = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Socket socket : originalItem.getSockets()) {
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
            key.links = largestLink;
        }
    }

    /**
     * Finds gem-specific data
     */
    private void extractGemData() {
        int level = -1;
        int quality = 0;
        boolean corrupted = false;

        // Attempt to extract lvl and quality from item info
        for (Property prop : originalItem.getProperties()) {
            if (prop.name.equals("Level")) {
                level = Integer.parseInt(prop.values.get(0).get(0).split(" ")[0]);
            } else if (prop.name.equals("Quality")) {
                quality = Integer.parseInt(prop.values.get(0).get(0).replace("+", "").replace("%", ""));
            }
        }

        // If quality or lvl was not found, return
        if (level == -1) {
            discard = true;
            return;
        }

        // Accept some quality ranges
        if (quality < 5) {
            quality = 0;
        } else if (quality > 17 && quality < 23) {
            quality = 20;
        } else if (quality != 23) {
            discard = true;
            return;
        }

        // Begin the long block that filters out gems based on a number of properties
        if (originalItem.getTypeLine().equals("Empower Support") || originalItem.getTypeLine().equals("Enlighten Support") || originalItem.getTypeLine().equals("Enhance Support")) {
            // Quality doesn't matter for lvl 3 and 4
            if (level > 2) quality = 0;
        } else if (originalItem.getTypeLine().equals("Brand Recall")) {
            if (level <= 2) {
                level = 1;
            } else if (level < 5) {
                discard = true;
                return;
            }
        } else {
            // Accept some level ranges
            if (level < 5) {
                level = 1;
            } else if (level < 20) {
                discard = true;
                return;
            }
        }

        if (originalItem.getCorrupted() != null) {
            corrupted = originalItem.getCorrupted();
        }

        // Api bug?
        if (!corrupted && (level > 20 || quality > 20)) {
            discard = true;
            return;
        }

        key.gemLevel = level;
        key.gemQuality = quality;
        key.gemCorrupted = corrupted;
    }

    /**
     * Attempt to find the series a map belongs to
     */
    private void extractMapSeries() {
        /* Currently the series are as such:
         http://web.poecdn.com/image/Art/2DItems/Maps/Map45.png?scale=1&w=1&h=1
         http://web.poecdn.com/image/Art/2DItems/Maps/act4maps/Map76.png?scale=1&w=1&h=1
         http://web.poecdn.com/image/Art/2DItems/Maps/AtlasMaps/Chimera.png?scale=1&scaleIndex=0&w=1&h=1
         http://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/VaalTempleBase.png?scale=1&w=1&h=1&mn=1&mt=0
         http://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/VaalTempleBase.png?scale=1&w=1&h=1&mn=2&mt=0
         http://web.poecdn.com/image/Art/2DItems/Maps/Atlas2Maps/New/VaalTempleBase.png?scale=1&w=1&h=1&mn=3&mt=0
        */

        // Ignore unique and relic maps
        if (originalItem.getFrameType() > 2) {
            return;
        }

        String[] splitItemType = originalItem.getIcon().split("/");
        String iconCategory = splitItemType[splitItemType.length - 2].toLowerCase();
        int seriesNumber = 0;

        // Attempt to find series number for newer maps
        try {
            String[] iconParams = originalItem.getIcon().split("\\?", 2)[1].split("&");

            for (String param : iconParams) {
                String[] splitParam = param.split("=");

                if (splitParam[0].equals("mn")) {
                    seriesNumber = Integer.parseInt(splitParam[1]);
                    break;
                }
            }
        } catch (Exception ex) {
            // If it failed, it failed. Doesn't really matter.
        }

        if (iconCategory.equalsIgnoreCase("Maps")) {
            key.mapSeries = 0;
        } else if (iconCategory.equalsIgnoreCase("act4maps")) {
            key.mapSeries = 1;
        } else if (iconCategory.equalsIgnoreCase("AtlasMaps")) {
            key.mapSeries = 2;
        } else if (iconCategory.equalsIgnoreCase("New") && seriesNumber > 0) {
            key.mapSeries = seriesNumber + 2;
        }

        logger.error("Couldn't determine series of map with icon: " + originalItem.getIcon());
        discard = true;
    }
}
