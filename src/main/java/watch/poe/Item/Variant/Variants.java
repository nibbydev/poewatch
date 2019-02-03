package poe.Item.Variant;

public class Variants {
    private static ItemVariant splendour = new ItemVariant() {{
        name = "Atziri's Splendour";
        // Note that ordering is important when using .contains() as "increased Armour" is a
        // substring of "increased Armour and Evasion"
        variantTypes = new VariantType[]{
                new VariantType() {{
                    variation = "ar/ev/es";
                    mods = new String[]{"increased Armour, Evasion and Energy Shield"};
                }},
                new VariantType() {{
                    variation = "ar/es/li";
                    mods = new String[]{"increased Armour and Energy Shield", "to maximum Life"};
                }},
                new VariantType() {{
                    variation = "ar/es";
                    mods = new String[]{"increased Armour and Energy Shield", "to maximum Energy Shield"};
                }},
                new VariantType() {{
                    variation = "ev/es/li";
                    mods = new String[]{"increased Evasion and Energy Shield", "to maximum Life"};
                }},
                new VariantType() {{
                    variation = "ev/es";
                    mods = new String[]{"increased Evasion and Energy Shield", "to maximum Energy Shield"};
                }},
                new VariantType() {{
                    variation = "ar/ev";
                    mods = new String[]{"increased Armour and Evasion"};
                }},
                new VariantType() {{
                    variation = "ar";
                    mods = new String[]{"increased Armour"};
                }},
                new VariantType() {{
                    variation = "ev";
                    mods = new String[]{"increased Evasion"};
                }},
                new VariantType() {{
                    variation = "es";
                    mods = new String[]{"increased Energy Shield"};
                }}
        };
    }};
    private static ItemVariant vinktar = new ItemVariant() {{
        name = "Vessel of Vinktar";
        variantTypes = new VariantType[]{
                new VariantType() {{
                    variation = "spells";
                    mods = new String[]{"Lightning Damage to Spells"};
                }},
                new VariantType() {{
                    variation = "attacks";
                    mods = new String[]{"Lightning Damage to Attacks"};
                }},
                new VariantType() {{
                    variation = "conversion";
                    mods = new String[]{"Converted to Lightning"};
                }},
                new VariantType() {{
                    variation = "penetration";
                    mods = new String[]{"Damage Penetrates"};
                }}
        };
    }};
    private static ItemVariant invitation = new ItemVariant() {{
        name = "Doryani's Invitation";
        variantTypes = new VariantType[]{
                new VariantType() {{
                    variation = "lightning";
                    mods = new String[]{"increased Lightning Damage"};
                }},
                new VariantType() {{
                    variation = "fire";
                    mods = new String[]{"increased Fire Damage"};
                }},
                new VariantType() {{
                    variation = "cold";
                    mods = new String[]{"increased Cold Damage"};
                }},
                new VariantType() {{
                    variation = "physical";
                    mods = new String[]{"increased Global Physical Damage"};
                }}
        };
    }};
    private static ItemVariant fostering = new ItemVariant() {{
        name = "Yriel's Fostering";
        variantTypes = new VariantType[]{
                new VariantType() {{
                    variation = "snake";
                    mods = new String[]{"Bestial Snake"};
                }},
                new VariantType() {{
                    variation = "ursa";
                    mods = new String[]{"Bestial Ursa"};
                }},
                new VariantType() {{
                    variation = "rhoa";
                    mods = new String[]{"Bestial Rhoa"};
                }}
        };
    }};
    private static ItemVariant guidance = new ItemVariant() {{
        name = "Volkuur's Guidance";
        variantTypes = new VariantType[]{
                new VariantType() {{
                    variation = "fire";
                    mods = new String[]{"Fire Damage to Spells"};
                }},
                new VariantType() {{
                    variation = "cold";
                    mods = new String[]{"Cold Damage to Spells"};
                }},
                new VariantType() {{
                    variation = "lightning";
                    mods = new String[]{"Lightning Damage to Spells"};
                }}
        };
    }};
    private static ItemVariant impresence = new ItemVariant() {{
        name = "Impresence";
        variantTypes = new VariantType[]{
                new VariantType() {{
                    variation = "lightning";
                    mods = new String[]{"Lightning Damage"};
                }},
                new VariantType() {{
                    variation = "fire";
                    mods = new String[]{"Fire Damage"};
                }},
                new VariantType() {{
                    variation = "cold";
                    mods = new String[]{"Cold Damage"};
                }},
                new VariantType() {{
                    variation = "physical";
                    mods = new String[]{"Physical Damage"};
                }},
                new VariantType() {{
                    variation = "chaos";
                    mods = new String[]{"Chaos Damage"};
                }}
        };
    }};

    private static VariantType abyssalOneSocket = new VariantType() {{
        variation = "1 socket";
        mods = new String[]{"Has 1 Abyssal Socket"};
    }};
    private static VariantType abyssalTwoSocket = new VariantType() {{
        variation = "2 sockets";
        mods = new String[]{"Has 2 Abyssal Sockets"};
    }};

    // Abyssal variants
    private static ItemVariant poacher = new ItemVariant() {{
        name = "Lightpoacher";
        variantTypes = new VariantType[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant shroud = new ItemVariant() {{
        name = "Shroud of the Lightless";
        variantTypes = new VariantType[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant bubonic = new ItemVariant() {{
        name = "Bubonic Trail";
        variantTypes = new VariantType[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant tomb = new ItemVariant() {{
        name = "Tombfist";
        variantTypes = new VariantType[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant hale = new ItemVariant() {{
        name = "Hale Negator";
        variantTypes = new VariantType[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant command = new ItemVariant() {{
        name = "Command of the Pit";
        variantTypes = new VariantType[]{abyssalOneSocket, abyssalTwoSocket};
    }};

    private static ItemVariant[] variants = new ItemVariant[]{
            splendour,
            vinktar,
            invitation,
            fostering,
            guidance,
            impresence,
            poacher,
            shroud,
            bubonic,
            tomb,
            hale,
            command
    };

    public static ItemVariant[] getVariants() {
        return variants;
    }
}
