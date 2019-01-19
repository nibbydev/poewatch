package poe.Managers.Relation;

public class CurrencyAliases {
    private static final AliasEntry[] aliases = new AliasEntry[]{
            new AliasEntry() {{
                name = "Chaos Orb";
                aliases = new String[]{"chaos", "choas", "c"};
            }},
            new AliasEntry() {{
                name = "Exalted Orb";
                aliases = new String[]{"exalted", "exalt", "exa", "ex"};
            }},
            new AliasEntry() {{
                name = "Divine Orb";
                aliases = new String[]{"divine", "div"};
            }},
            new AliasEntry() {{
                name = "Orb of Alchemy";
                aliases = new String[]{"alchemy", "alch", "alc"};
            }},
            new AliasEntry() {{
                name = "Orb of Fusing";
                aliases = new String[]{"fusings", "fusing", "fuse", "fus"};
            }},
            new AliasEntry() {{
                name = "Orb of Alteration";
                aliases = new String[]{"alteration", "alts", "alt"};
            }},
            new AliasEntry() {{
                name = "Regal Orb";
                aliases = new String[]{"regal'", "rega"};
            }},
            new AliasEntry() {{
                name = "Vaal Orb";
                aliases = new String[]{"vaal"};
            }},
            new AliasEntry() {{
                name = "Orb of Regret";
                aliases = new String[]{"regrets", "regret", "regr"};
            }},
            new AliasEntry() {{
                name = "Cartographer's Chisel";
                aliases = new String[]{"chisel", "chis", "cart"};
            }},
            new AliasEntry() {{
                name = "Jeweller's Orb";
                aliases = new String[]{"jewellers", "jeweller", "jew"};
            }},
            new AliasEntry() {{
                name = "Silver Coin";
                aliases = new String[]{"silver"};
            }},
            new AliasEntry() {{
                name = "Perandus Coin";
                aliases = new String[]{"coins", "coin", "perandus"};
            }},
            new AliasEntry() {{
                name = "Orb of Scouring";
                aliases = new String[]{"scouring", "scour"};
            }},
            new AliasEntry() {{
                name = "Gemcutter's Prism";
                aliases = new String[]{"gcp", "gemc"};
            }},
            new AliasEntry() {{
                name = "Orb of Chance";
                aliases = new String[]{"chance", "chanc"};
            }},
            new AliasEntry() {{
                name = "Chromatic Orb";
                aliases = new String[]{"chrome", "chrom"};
            }},
            new AliasEntry() {{
                name = "Blessed Orb";
                aliases = new String[]{"blessed", "bless", "bles"};
            }},
            new AliasEntry() {{
                name = "Glassblower's Bauble";
                aliases = new String[]{"bauble", "glass"};
            }},
            new AliasEntry() {{
                name = "Orb of Augmentation";
                aliases = new String[]{"aug"};
            }},
            new AliasEntry() {{
                name = "Orb of Transmutation";
                aliases = new String[]{"trans", "tra"};
            }},
            new AliasEntry() {{
                name = "Apprentice Cartographer's Sextant";
                aliases = new String[]{"apprentice-sextant", "apprentice"};
            }},
            new AliasEntry() {{
                name = "Journeyman Cartographer's Sextant";
                aliases = new String[]{"journeyman-sextant", "journeyman"};
            }}
    };

    public static AliasEntry[] getAliases() {
        return aliases;
    }

    public static class AliasEntry {
        String name;
        String[] aliases;

        public String getName() {
            return name;
        }

        public String[] getAliases() {
            return aliases;
        }
    }
}
