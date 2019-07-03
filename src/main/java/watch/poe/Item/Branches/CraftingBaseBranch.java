package poe.Item.Branches;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Item.Category.GroupEnum;
import poe.Item.Deserializers.ApiItem;
import poe.Item.Item;

public class CraftingBaseBranch extends Item {
    private static final Logger logger = LoggerFactory.getLogger(CraftingBaseBranch.class);

    /**
     * Default constructor
     *
     * @param apiItem Item as it appears in the stash api
     */
    public CraftingBaseBranch(ApiItem apiItem) {
        super(apiItem);
    }

    /**
     * Branch-specific parse method that will be called in superclass constructor
     */
    @Override
    public void parse() {
        // Rares are generally sold for their mods, not bases
        if (originalItem.isIdentified() && originalItem.getFrameType() == 2) {
            discard = true;
            return;
        }

        // Incubators get branched here since the have an ilvl
        if (super.group.equals(GroupEnum.incubator)) {
            discard = true;
            return;
        }

        // "Superior Item" = "Item"
        if (key.name.startsWith("Superior ")) {
            key.name = key.name.replace("Superior ", "");
        }

        // Ignore corrupted bases (including talismans)
        if (originalItem.getCorrupted() != null && originalItem.getCorrupted()) {
            discard = true;
            return;
        }

        // Attempt to extract item's base name
        key.name = relationManager.extractItemBaseName(group, key.name);
        if (key.name == null) {
            discard = true;
            return;
        }

        // Override frame type for all bases
        key.frame = 0;
        key.baseItemLevel = originalItem.getIlvl();

        // Flatten ilvl rolls
        if (originalItem.getShaper() == null && originalItem.getElder() == null) {
            if (originalItem.getIlvl() < 83) discard = true;
            else key.baseItemLevel = 84;
        } else {
            if (originalItem.getIlvl() < 82) discard = true;
            else if (originalItem.getIlvl() > 86) key.baseItemLevel = 86;
        }

        // Set influence
        if (originalItem.getShaper() != null) {
            key.baseShaper = true;
        } else if (originalItem.getElder() != null) {
            key.baseElder = true;
        }
    }
}
