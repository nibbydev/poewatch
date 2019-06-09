package poe.Item.Branches;

import poe.Item.ApiDeserializers.ApiItem;
import poe.Item.Item;

public class CraftingBaseBranch extends Item {
    /**
     * Base constructor
     */
    public CraftingBaseBranch(ApiItem apiItem) {
        this.apiItem = apiItem;

        process();
        if (discard) {
            return;
        }

        parse();
        buildKey();
    }

    private void parse() {
        // Rares are generally sold for their mods, not bases
        if (apiItem.getFrameType() == 2) {
            discard = true;
            return;
        }

        // "Superior Item" = "Item"
        if (name.startsWith("Superior ")) {
            name = name.replace("Superior ", "");
        }

        // Ignore corrupted bases (including talismans)
        if (apiItem.getCorrupted() != null && apiItem.getCorrupted()) {
            discard = true;
            return;
        }

        // Attempt to extract item's base name
        name = relationManager.extractItemBaseName(group, name);
        if (name == null) {
            discard = true;
            return;
        }

        // Override frame type for all bases
        frameType = 0;
        itemLevel = apiItem.getIlvl();

        // Flatten ilvl rolls
        if (apiItem.getShaper() == null && apiItem.getElder() == null) {
            if (apiItem.getIlvl() < 83) discard = true;
            else itemLevel = 84;
        } else {
            if (apiItem.getIlvl() < 82) discard = true;
            else if (apiItem.getIlvl() > 86) itemLevel = 86;
        }

        // Set influence
        if (apiItem.getShaper() != null) {
            shaper = true;
        } else if (apiItem.getElder() != null) {
            elder = true;
        }
    }
}
