package poe.Relation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Database.Database;
import poe.Item.Category.GroupEnum;
import poe.Item.Key;
import poe.Utility.Utility;

import java.lang.reflect.Type;
import java.util.*;

public class RelationResources {
    private final Logger logger = LoggerFactory.getLogger(RelationResources.class);
    private final Gson gson = new Gson();
    private Database database;
    private final Indexer indexer;

    private List<String> currencyBlackList, defaultMaps;
    private List<UniqueMap> uniqueMaps;
    private Map<String, Integer> currencyAliases;
    private Map<GroupEnum, Set<String>> baseItems;

    public RelationResources(Database database, Indexer indexer) {
        this.database = database;
        this.indexer = indexer;
    }

    /**
     * Loads in resource files and creates mappings
     *
     * @return
     */
    public boolean init() {
        boolean success = loadResourceFiles();
        if (!success) {
            return false;
        }

        success = verifyDatabase();
        if (!success) {
            return false;
        }

        return true;
    }

    private boolean loadResourceFiles() {
        logger.info("Loading resource files");

        String json = Utility.loadFile("currency_aliases.json");
        Type type = new TypeToken<List<CurrencyAlias>>() {}.getType();
        List<CurrencyAlias> aliasList = gson.fromJson(json, type);
        if (aliasList == null) {
            return false;
        }

        json = Utility.loadFile("currency_blacklist.json");
        type = new TypeToken<List<String>>() {}.getType();
        currencyBlackList = gson.fromJson(json, type);
        if (currencyBlackList == null) {
            return false;
        }

        json = Utility.loadFile("default_maps.json");
        type = new TypeToken<List<String>>() {}.getType();
        defaultMaps = gson.fromJson(json, type);
        if (defaultMaps == null) {
            return false;
        }

        json = Utility.loadFile("unique_maps.json");
        type = new TypeToken<List<UniqueMap>>() {}.getType();
        uniqueMaps = gson.fromJson(json, type);
        if (uniqueMaps == null) {
            return false;
        }

        json = Utility.loadFile("item_bases.json");
        type = new TypeToken<List<BaseItems>>() {}.getType();
        List<BaseItems> basesList = gson.fromJson(json, type);
        if (basesList == null) {
            return false;
        }

        buildCurrencyAliasMap(aliasList, indexer.getItemData());
        buildItemBasesMap(basesList);

        logger.info("Finished loading resource files");
        return true;
    }

    private boolean verifyDatabase() {
        logger.info("Verifying categories");

        boolean success = database.setup.verifyCategories();
        if (!success) {
            return false;
        }

        success = database.setup.verifyGroups();
        if (!success) {
            return false;
        }

        logger.info("Finished verifying categories");
        return true;
    }

    /**
     * Maps currency names to ids
     *
     * @param aliasList
     * @param itemData
     */
    private void buildCurrencyAliasMap(List<CurrencyAlias> aliasList, Map<Key, Integer> itemData) {
        currencyAliases = new HashMap<>();

        // For every alias, find a matching currency item's id
        for (CurrencyAlias currencyAlias : aliasList) {
            for (Key key : itemData.keySet()) {
                if (key.frame == 5 && key.name.equals(currencyAlias.getName())) {
                    int id = itemData.get(key);

                    for (String alias : currencyAlias.getAliases()) {
                        currencyAliases.put(alias, id);
                    }
                }
            }
        }

        // Since chaos Chaos Orb doesn't actually exist as an item in the database
        for (CurrencyAlias currencyAlias : aliasList) {
            if (currencyAlias.getName().equals("Chaos Orb")) {
                for (String alias : currencyAlias.getAliases()) {
                    currencyAliases.put(alias, null);
                }

                break;
            }
        }
    }

    /**
     * Maps groups to base names
     *
     * @param basesList
     * @return
     */
    private void buildItemBasesMap(List<BaseItems> basesList) {
        baseItems = new HashMap<>();

        for (BaseItems tmp : basesList) {
            GroupEnum group = GroupEnum.valueOf(tmp.getGroup());

            Set<String> tmpSet = baseItems.getOrDefault(group, new HashSet<>());
            tmpSet.addAll(tmp.getBases());
            baseItems.putIfAbsent(group, tmpSet);
        }
    }

    public boolean hasCurrencyAlias(String alias) {
        return currencyAliases.containsKey(alias);
    }

    public Integer getCurrencyAlias(String alias) {
        return currencyAliases.get(alias);
    }

    /**
     * Extracts item's base class from its name
     * Eg 'Blasting Corsair Sword of Needling' -> 'Corsair Sword'
     *
     * @param group Group the item belongs to
     * @param name  Item name
     * @return Extracted name or null on failure
     */
    public String extractItemBaseName(GroupEnum group, String name) {
        if (name == null) {
            return null;
        }

        Set<String> baseSet = baseItems.get(group);

        if (baseSet == null) {
            return null;
        }

        for (String base : baseSet) {
            if (name.contains(base)) {
                return base;
            }
        }

        return null;
    }

    public String extractMapBaseName(String name) {
        if (name == null) {
            return null;
        }

        return defaultMaps.stream()
                .filter(name::contains)
                .findFirst()
                .orElse(null);
    }

    public boolean isInCurrencyBlacklist(String name) {
        if (name == null) {
            return false;
        }

        return currencyBlackList.stream().anyMatch(name::equalsIgnoreCase);
    }

    public String findUnidentifiedUniqueMapName(String type, int frame) {
        UniqueMap uniqueMap = uniqueMaps.stream()
                .filter(i -> i.getFrame() == frame && i.getType().equals(type))
                .findFirst()
                .orElse(null);

        if (uniqueMap == null) {
            return null;
        }

        return uniqueMap.getName();
    }
}
