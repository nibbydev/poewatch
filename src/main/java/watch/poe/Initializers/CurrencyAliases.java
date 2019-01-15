package poe.Initializers;

import java.util.HashMap;
import java.util.Map;

public class CurrencyAliases {
    private static Currency[] aliases = new Currency[]{
            new Currency() {{
                name = "Chaos Orb";
                aliases = new String[]{"chaos", "choas", "c"};
            }},
            new Currency() {{
                name = "Exalted Orb";
                aliases = new String[]{"exalted", "exalt", "exa", "ex"};
            }},
            new Currency() {{
                name = "Divine Orb";
                aliases = new String[]{"divine", "div"};
            }},
            new Currency() {{
                name = "Orb of Alchemy";
                aliases = new String[]{"alchemy", "alch", "alc"};
            }},
            new Currency() {{
                name = "Orb of Fusing";
                aliases = new String[]{"fusings", "fusing", "fuse", "fus"};
            }},
            new Currency() {{
                name = "Orb of Alteration";
                aliases = new String[]{"alteration", "alts", "alt"};
            }},
            new Currency() {{
                name = "Regal Orb";
                aliases = new String[]{"regal'", "rega"};
            }},
            new Currency() {{
                name = "Vaal Orb";
                aliases = new String[]{"vaal"};
            }},
            new Currency() {{
                name = "Orb of Regret";
                aliases = new String[]{"regrets", "regret", "regr"};
            }},
            new Currency() {{
                name = "Cartographer's Chisel";
                aliases = new String[]{"chisel", "chis", "cart"};
            }},
            new Currency() {{
                name = "Jeweller's Orb";
                aliases = new String[]{"jewellers", "jeweller", "jew"};
            }},
            new Currency() {{
                name = "Silver Coin";
                aliases = new String[]{"silver"};
            }},
            new Currency() {{
                name = "Perandus Coin";
                aliases = new String[]{"coins", "coin", "perandus"};
            }},
            new Currency() {{
                name = "Orb of Scouring";
                aliases = new String[]{"scouring", "scour"};
            }},
            new Currency() {{
                name = "Gemcutter's Prism";
                aliases = new String[]{"gcp", "gemc"};
            }},
            new Currency() {{
                name = "Orb of Chance";
                aliases = new String[]{"chance", "chanc"};
            }},
            new Currency() {{
                name = "Chromatic Orb";
                aliases = new String[]{"chrome", "chrom"};
            }},
            new Currency() {{
                name = "Blessed Orb";
                aliases = new String[]{"blessed", "bless", "bles"};
            }},
            new Currency() {{
                name = "Glassblower's Bauble";
                aliases = new String[]{"bauble", "glass"};
            }},
            new Currency() {{
                name = "Orb of Augmentation";
                aliases = new String[]{"aug"};
            }},
            new Currency() {{
                name = "Orb of Transmutation";
                aliases = new String[]{"trans", "tra"};
            }},
            new Currency() {{
                name = "Apprentice Cartographer's Sextant";
                aliases = new String[]{"apprentice-sextant", "apprentice"};
            }},
            new Currency() {{
                name = "Journeyman Cartographer's Sextant";
                aliases = new String[]{"journeyman-sextant", "journeyman"};
            }}
    };

    public static Map<String, String> GetAliasMap() {
        HashMap<String, String> tmp = new HashMap<>();

        for (Currency currency : aliases) {
            for (String alias : currency.aliases) {
                tmp.put(alias, currency.name);
            }
        }

        return tmp;
    }

    private static class Currency {
        String name;
        String[] aliases;
    }
}
