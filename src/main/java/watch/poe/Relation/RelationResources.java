package poe.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Item.Category.GroupEnum;
import poe.Item.Key;
import poe.Utility.Utility;

import java.util.*;

public class RelationResources {
    private final Logger logger = LoggerFactory.getLogger(RelationResources.class);

    private List<String> currencyBlackList, defaultMaps;
    private List<UniqueMap> uniqueMaps;
    private Map<String, Integer> currencyAliases;
    private Map<GroupEnum, Set<String>> baseItems;

    /**
     * Loads in resource files and creates mappings
     *
     * @return
     */
    public boolean load(Map<Key, Integer> itemData) {
        logger.info("Loading resource files");

        List<CurrencyAlias> aliasList = Utility.loadResource("currency_aliases.json");
        if (aliasList == null) {
            return false;
        }

        currencyBlackList = Utility.loadResource("currency_blacklist.json");
        if (currencyBlackList == null) {
            return false;
        }

        defaultMaps = Utility.loadResource("default_maps.json");
        if (defaultMaps == null) {
            return false;
        }

        uniqueMaps = Utility.loadResource("unique_maps.json");
        if (uniqueMaps == null) {
            return false;
        }

        List<BaseItems> basesList = Utility.loadResource("item_base.json");
        if (basesList == null) {
            return false;
        }


        currencyAliases = buildCurrencyAliasMap(aliasList, itemData);
        baseItems = buildItemBasesMap(basesList);

        logger.info("Finished loading resource files");
        return true;
    }

    /**
     * Maps currency names to ids
     *
     * @param aliasList
     * @param itemData
     * @return
     */
    private Map<String, Integer> buildCurrencyAliasMap(List<CurrencyAlias> aliasList, Map<Key, Integer> itemData) {
        Map<String, Integer> output = new HashMap<>();

        // For every alias, find a matching currency item's id
        for (CurrencyAlias currencyAlias : aliasList) {
            for (Key key : itemData.keySet()) {
                if (key.frame == 5 && key.name.equals(currencyAlias.getName())) {
                    int id = itemData.get(key);

                    for (String alias : currencyAlias.getAliases()) {
                        output.put(alias, id);
                    }
                }
            }
        }

        // Since chaos Chaos Orb doesn't actually exist as an item in the database
        for (CurrencyAlias currencyAlias : aliasList) {
            if (currencyAlias.getName().equals("Chaos Orb")) {
                for (String alias : currencyAlias.getAliases()) {
                    output.put(alias, null);
                }

                break;
            }
        }

        return output;
    }

    /**
     * Maps groups to base names
     *
     * @param basesList
     * @return
     */
    private Map<GroupEnum, Set<String>> buildItemBasesMap(List<BaseItems> basesList) {
        Map<GroupEnum, Set<String>> output = new HashMap<>();

        for (BaseItems baseItems : basesList) {
            GroupEnum group = GroupEnum.valueOf(baseItems.getGroup());

            Set<String> tmpSet = this.baseItems.getOrDefault(group, new HashSet<>());
            tmpSet.addAll(Arrays.asList(baseItems.getBases()));
            this.baseItems.putIfAbsent(group, tmpSet);
        }

        return output;
    }

    public List<String> getCurrencyBlackList() {
        return currencyBlackList;
    }

    public List<String> getDefaultMaps() {
        return defaultMaps;
    }

    public List<UniqueMap> getUniqueMaps() {
        return uniqueMaps;
    }

    public Map<GroupEnum, Set<String>> getBaseItems() {
        return baseItems;
    }

    public boolean hasCurrencyAlias(String alias) {
        return currencyAliases.containsKey(alias);
    }

    public int getCurrencyAlias(String alias) {
        return currencyAliases.get(alias);
    }
}
