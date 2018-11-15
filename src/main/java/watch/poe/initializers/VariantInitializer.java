package poe.initializers;

import poe.manager.entry.item.variant.ItemVariant;
import poe.manager.entry.item.variant.Variant;

public class VariantInitializer {
    private static ItemVariant splendour = new ItemVariant() {{
        name = "Atziri's Splendour";
        // Note that ordering is important when using .contains() as "increased Armour" is a
        // substring of "increased Armour and Evasion"
        variants = new Variant[]{
                new Variant() {{
                    variation = "ar/ev/es";
                    mods = new String[]{"increased Armour, Evasion and Energy Shield"};
                }},
                new Variant() {{
                    variation = "ar/es/li";
                    mods = new String[]{"increased Armour and Energy Shield", "to maximum Life"};
                }},
                new Variant() {{
                    variation = "ar/es";
                    mods = new String[]{"increased Armour and Energy Shield", "to maximum Energy Shield"};
                }},
                new Variant() {{
                    variation = "ev/es/li";
                    mods = new String[]{"increased Evasion and Energy Shield", "to maximum Life"};
                }},
                new Variant() {{
                    variation = "ev/es";
                    mods = new String[]{"increased Evasion and Energy Shield", "to maximum Energy Shield"};
                }},
                new Variant() {{
                    variation = "ar/ev";
                    mods = new String[]{"increased Armour and Evasion"};
                }},
                new Variant() {{
                    variation = "ar";
                    mods = new String[]{"increased Armour"};
                }}
        };
    }};
    private static ItemVariant vinktar = new ItemVariant() {{
        name = "Vessel of Vinktar";
        variants = new Variant[]{
                new Variant() {{
                    variation = "spells";
                    mods = new String[]{"Lightning Damage to Spells"};
                }},
                new Variant() {{
                    variation = "attacks";
                    mods = new String[]{"Lightning Damage to Attacks"};
                }},
                new Variant() {{
                    variation = "conversion";
                    mods = new String[]{"Converted to Lightning"};
                }},
                new Variant() {{
                    variation = "penetration";
                    mods = new String[]{"Damage Penetrates"};
                }}
        };
    }};
    private static ItemVariant invitation = new ItemVariant() {{
        name = "Doryani's Invitation";
        variants = new Variant[]{
                new Variant() {{
                    variation = "lightning";
                    mods = new String[]{"increased Lightning Damage"};
                }},
                new Variant() {{
                    variation = "fire";
                    mods = new String[]{"increased Fire Damage"};
                }},
                new Variant() {{
                    variation = "cold";
                    mods = new String[]{"increased Cold Damage"};
                }},
                new Variant() {{
                    variation = "physical";
                    mods = new String[]{"increased Global Physical Damage"};
                }}
        };
    }};
    private static ItemVariant fostering = new ItemVariant() {{
        name = "Yriel's Fostering";
        variants = new Variant[]{
                new Variant() {{
                    variation = "snake";
                    mods = new String[]{"Bestial Snake"};
                }},
                new Variant() {{
                    variation = "ursa";
                    mods = new String[]{"Bestial Ursa"};
                }},
                new Variant() {{
                    variation = "rhoa";
                    mods = new String[]{"Bestial Rhoa"};
                }}
        };
    }};
    private static ItemVariant guidance = new ItemVariant() {{
        name = "Volkuur's Guidance";
        variants = new Variant[]{
                new Variant() {{
                    variation = "fire";
                    mods = new String[]{"Fire Damage to Spells"};
                }},
                new Variant() {{
                    variation = "cold";
                    mods = new String[]{"Cold Damage to Spells"};
                }},
                new Variant() {{
                    variation = "lightning";
                    mods = new String[]{"Lightning Damage to Spells"};
                }}
        };
    }};
    private static ItemVariant impresence = new ItemVariant() {{
        name = "Impresence";
        variants = new Variant[]{
                new Variant() {{
                    variation = "lightning";
                    mods = new String[]{"Lightning Damage"};
                }},
                new Variant() {{
                    variation = "fire";
                    mods = new String[]{"Fire Damage"};
                }},
                new Variant() {{
                    variation = "cold";
                    mods = new String[]{"Cold Damage"};
                }},
                new Variant() {{
                    variation = "physical";
                    mods = new String[]{"Physical Damage"};
                }},
                new Variant() {{
                    variation = "chaos";
                    mods = new String[]{"Chaos Damage"};
                }}
        };
    }};

    private static Variant abyssalOneSocket = new Variant() {{
        variation = "1 socket";
        mods = new String[]{"Has 1 Abyssal Socket"};
    }};
    private static Variant abyssalTwoSocket = new Variant() {{
        variation = "2 sockets";
        mods = new String[]{"Has 2 Abyssal Sockets"};
    }};

    // Abyssal variants
    private static ItemVariant poacher = new ItemVariant() {{
        name = "Lightpoacher";
        variants = new Variant[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant shroud = new ItemVariant() {{
        name = "Shroud of the Lightless";
        variants = new Variant[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant bubonic = new ItemVariant() {{
        name = "Bubonic Trail";
        variants = new Variant[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant tomb = new ItemVariant() {{
        name = "Tombfist";
        variants = new Variant[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant hale = new ItemVariant() {{
        name = "Hale Negator";
        variants = new Variant[]{abyssalOneSocket, abyssalTwoSocket};
    }};
    private static ItemVariant command = new ItemVariant() {{
        name = "Command of the Pit";
        variants = new Variant[]{abyssalOneSocket, abyssalTwoSocket};
    }};

    /**
     * Generates array of all current variations
     *
     * @return Array of ItemVariant
     */
    public static ItemVariant[] GetVariants() {
        return new ItemVariant[]{
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
    }
}
