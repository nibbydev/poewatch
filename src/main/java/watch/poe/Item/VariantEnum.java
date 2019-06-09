package poe.Item;

public enum VariantEnum {
    splendour_ar_ev_es("Atziri's Splendour", "ar/ev/es", "increased Armour, Evasion and Energy Shield"),
    splendour_ar_es_li("Atziri's Splendour", "ar/es/li", "increased Armour and Energy Shield", "to maximum Life"),
    splendour_ev_es_li("Atziri's Splendour", "ev/es/li", "increased Evasion and Energy Shield", "to maximum Life"),
    splendour_ar_es("Atziri's Splendour", "ar/es", "increased Armour and Energy Shield", "to maximum Energy Shield"),
    splendour_ar_ev("Atziri's Splendour", "ar/ev", "increased Armour and Evasion"),
    splendour_ev_es("Atziri's Splendour", "ev/es", "increased Evasion and Energy Shield", "to maximum Energy Shield"),
    splendour_ar("Atziri's Splendour", "ar", "increased Armour"),
    splendour_ev("Atziri's Splendour", "ev", "increased Evasion"),
    splendour_es("Atziri's Splendour", "es", "increased Energy Shield"),

    vinktar_spells("Vessel of Vinktar", "spells", "Lightning Damage to Spells"),
    vinktar_attacks("Vessel of Vinktar", "attacks", "Lightning Damage to Attacks"),
    vinktar_pen("Vessel of Vinktar", "conversion", "Converted to Lightning"),
    vinktar_conv("Vessel of Vinktar", "penetration", "Damage Penetrates"),

    doryanis_lightning("Doryani's Invitation", "lightning", "increased Lightning Damage"),
    doryanis_fire("Doryani's Invitation", "fire", "increased Fire Damage"),
    doryanis_cold("Doryani's Invitation", "cold", "increased Cold Damage"),
    doryanis_phys("Doryani's Invitation", "physical", "increased Global Physical Damage"),

    fostering_snake("Yriel's Fostering", "snake", "Bestial Snake"),
    fostering_ursa("Yriel's Fostering", "ursa", "Bestial Ursa"),
    fostering_rhoa("Yriel's Fostering", "rhoa", "Bestial Rhoa"),

    volkuurs_fire("Volkuur's Guidance", "fire", "Fire Damage to Spells"),
    volkuurs_cold("Volkuur's Guidance", "cold", "Cold Damage to Spells"),
    volkuurs_lightning("Volkuur's Guidance", "lightning", "Lightning Damage to Spells"),

    impresence_lightning("Impresence", "lightning", "Lightning Damage"),
    impresence_fire("Impresence", "fire", "Fire Damage"),
    impresence_cold("Impresence", "cold", "Cold Damage"),
    impresence_phys("Impresence", "physical", "Physical Damage"),
    impresence_chaos("Impresence", "chaos", "Chaos Damage"),

    lightpoacher_1s("Lightpoacher", "1 socket", "Has 1 Abyssal Socket"),
    lightpoacher_2s("Lightpoacher", "2 sockets", "Has 2 Abyssal Sockets"),

    shroud_1s("Shroud of the Lightless", "1 socket", "Has 1 Abyssal Socket"),
    shroud_2s("Shroud of the Lightless", "2 sockets", "Has 2 Abyssal Sockets"),

    bubonic_1s("Bubonic Trail", "1 socket", "Has 1 Abyssal Socket"),
    bubonic_2s("Bubonic Trail", "2 sockets", "Has 2 Abyssal Sockets"),

    tombfist_1s("Tombfist", "1 socket", "Has 1 Abyssal Socket"),
    tombfist_2s("Tombfist", "2 sockets", "Has 2 Abyssal Sockets"),

    negator_1s("Hale Negator", "1 socket", "Has 1 Abyssal Socket"),
    negator_2s("Hale Negator", "2 sockets", "Has 2 Abyssal Sockets"),

    command_1s("Command of the Pit", "1 socket", "Has 1 Abyssal Socket"),
    command_2s("Command of the Pit", "2 sockets", "Has 2 Abyssal Sockets"),

    prophecy_master_zana("A Master Seeks Help", "zana", "You will find Zana"),
    prophecy_master_einar("A Master Seeks Help", "einhar", "You will find Einhar"),
    prophecy_master_jun("A Master Seeks Help", "jun", "You will find Jun"),
    prophecy_master_niko("A Master Seeks Help", "niko", "You will find Niko"),
    prophecy_master_alva("A Master Seeks Help", "alva", "You will find Alva");

    private String itemName;
    private String variation;
    private String[] itemMods;

    VariantEnum(String itemName, String variation, String... itemMods) {
        this.itemName = itemName;
        this.variation = variation;
        this.itemMods = itemMods;
    }

    public static VariantEnum findVariant(Item item) {
        // Go through all variations
        for (VariantEnum variation : VariantEnum.values()) {
            // Check if the item name matches
            if (!item.key.name.equals(variation.itemName)) {
                continue;
            }

            // Bit of spaghetti for prophecies
            if (item.key.frame == 8) {
                if (item.originalItem.getProphecyText().contains(variation.itemMods[0])) {
                    return variation;
                } else {
                    continue;
                }
            }

            int matches = 0;

            // Go though all the item's explicit modifiers and the current variant's mods
            for (String variantMod : variation.itemMods) {
                for (String itemMod : item.getExplicitMods()) {
                    if (itemMod.contains(variantMod)) {
                        // If one of the item's mods matches one of the variant's mods, increase the match counter
                        matches++;
                        break;
                    }
                }
            }

            // If all the variant's mods were present in the item then this item will take this variant's variation
            if (matches == variation.itemMods.length) {
                return variation;
            }
        }

        return null;
    }

    public String getVariation() {
        return variation;
    }
}
