package poe.Item.Branches;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Item.ApiDeserializers.ApiItem;
import poe.Item.ApiDeserializers.Property;
import poe.Item.ApiDeserializers.Socket;
import poe.Item.Category.CategoryEnum;
import poe.Item.Item;
import poe.Item.VariantEnum;

public class DefaultBranch extends Item {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Enchantment constructor
     */
    public DefaultBranch(ApiItem apiItem) {
        this.apiItem = apiItem;

        process();
        if (discard) {
            return;
        }

        if (category == null) {
            logger.error("Null category for: " + apiItem.getName() + " " + apiItem.getTypeLine() + " " + apiItem.getFrameType());
        }

        parse();
        buildKey();
    }

    /**
     * Parses item as default
     */
    private void parse() {
        // Maps can be unidentified, corrupted, and any frame type
        if (category.equals(CategoryEnum.map)) {
            parseMaps();
        } else {
            if (!apiItem.isIdentified()) {
                discard = true;
                return;
            }

            // Don't allow any normal/magic/rare items
            if (apiItem.getFrameType() < 3) {
                discard = true;
                return;
            }
        }

        // Remove Synthesised prefix
        if (apiItem.getSynthesised() != null) {
            if (name != null && name.startsWith("Synthesised ")) {
                name = name.replace("Synthesised ", "");
            } else if (typeLine != null && typeLine.startsWith("Synthesised ")) {
                typeLine = typeLine.replace("Synthesised ", "");
            }
        }

        // Some corrupted relics do not turn into rares and retain their relic frametypes
        if (frameType == 9) {
            if (apiItem.getCorrupted() != null && apiItem.getCorrupted()) {
                discard = true;
                return;
            }
        }

        if (category.equals(CategoryEnum.gem)) {
            extractGemData();
        } else if (category.equals(CategoryEnum.currency)) {
            discard = relationManager.isInCurrencyBlacklist(name);
        }

        if (discard) {
            return;
        }

        extractStackSize();
        extractItemLinks();

        // If present, string variation, otherwise null
        variation = VariantEnum.findVariant(this);
    }

    /**
     * Get the current stack and max stack sizes, if present
     */
    private void extractStackSize() {
        if (!apiItem.isStackable() || apiItem.getProperties() == null) {
            return;
        }

        stackSize = apiItem.getStackSize();

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
        Property property = apiItem.getProperties().stream()
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
        if (name.startsWith("Superior ")) {
            name = name.replace("Superior ", "");
        }

        // Find name for unidentified unique maps
        if (frameType >= 3 && !apiItem.isIdentified()) {
            typeLine = name;
            name = relationManager.findUnidUniqueMapName(name, frameType);

            if (name == null) {
                discard = true;
                return;
            }
        }

        // Extract name from magic and rare maps
        if (frameType == 1 || frameType == 2) {
            name = relationManager.extractMapBaseName(name);

            // Map was not in the list
            if (name == null) {
                discard = true;
                return;
            }
        }

        // Attempt to find map tier from properties
        if (apiItem.getProperties() != null) {
            for (Property prop : apiItem.getProperties()) {
                if (prop.name.equals("Map Tier")) {
                    if (!prop.values.isEmpty()) {
                        if (!prop.values.get(0).isEmpty()) {
                            String tmpTier = prop.values.get(0).get(0);
                            mapTier = Integer.parseInt(tmpTier);
                        }
                    }
                    break;
                }
            }
        }

        if (mapTier == null) {
            discard = true;
            return;
        }

        // Set frame to 0 for all non-uniques (and non-relics)
        if (frameType < 3) {
            frameType = 0;
        }
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
        if (apiItem.getSockets() == null) {
            return;
        }

        // Group links together
        Integer[] linkArray = new Integer[]{0, 0, 0, 0, 0, 0};
        for (Socket socket : apiItem.getSockets()) {
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
     * Finds gem-specific data
     */
    private void extractGemData() {
        int level = -1;
        int quality = 0;
        boolean corrupted = false;

        // Attempt to extract lvl and quality from item info
        for (Property prop : apiItem.getProperties()) {
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

        // Begin the long block that filters out gems based on a number of properties
        if (apiItem.getTypeLine().equals("Empower Support") || apiItem.getTypeLine().equals("Enlighten Support") || apiItem.getTypeLine().equals("Enhance Support")) {
            if (quality < 10) quality = 0;
            else quality = 20;

            // Quality doesn't matter for lvl 3 and 4
            if (level > 2) {
                quality = 0;

                if (apiItem.getCorrupted() != null) {
                    corrupted = apiItem.getCorrupted();
                }
            }
        } else {
            if (level < 19) level = 1;          // lvl       1 = 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18
            else if (level < 21) level = 20;    // lvl      20 = 19,20
            // lvl      21 = 21

            if (quality < 17) quality = 0;        // quality   0 = 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16
            else if (quality < 22) quality = 20;  // quality  20 = 17,18,19,20,21
            else quality = 23;                 // quality  23 = 22,23

            // Gets rid of specific gems
            if (level < 20 && quality > 20) quality = 20;  // lvl:1 quality:23-> lvl:1 quality:20

            if (level > 20 || quality > 20) corrupted = true;
            else if (apiItem.getTypeLine().contains("Vaal")) corrupted = true;
        }

        gemLevel = level;
        gemQuality = quality;
        gemCorrupted = corrupted;
    }



}
