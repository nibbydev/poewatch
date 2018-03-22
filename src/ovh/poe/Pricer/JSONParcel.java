package ovh.poe.Pricer;

import ovh.poe.Main;
import ovh.poe.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JSONParcel {
    public static class JSONItem {
        public double mean, median, mode;
        public int count, inc, frame;
        public String child, icon, name, type, var, index;
        public String corrupted, lvl, quality, links, tier;

        public void copy (DataEntry entry) {
            mean = entry.getMean();
            median = entry.getMedian();
            mode = entry.getMode();
            count = entry.getCount() + entry.getInc_counter();
            inc = entry.getInc_counter();

            index = entry.getItemIndex();

            // Check if there's a match for the specific index
            if (Main.RELATIONS.itemIndexToData.containsKey(index)) {
                Mappers.IndexedItem indexedItem = Main.RELATIONS.itemIndexToData.get(index);
                frame = indexedItem.frame;
                child = indexedItem.child;
                icon = indexedItem.icon;
                name = indexedItem.name;
                type = indexedItem.type;
                var = indexedItem.var;
                tier = indexedItem.tier;
            }

            // Get some data (eg links/lvl/quality/corrupted) from item key
            // "Standard|weapons:twosword|Starforge:Infernal Sword|3|links:6"
            String[] splitKey = entry.getKey().split("\\|");
            for (int i = 3; i < splitKey.length; i++) {
                if (splitKey[i].contains("links:")) links = splitKey[i].split(":")[1];
                else if (splitKey[i].contains("l:")) lvl = splitKey[i].split(":")[1];
                else if (splitKey[i].contains("q:")) quality = splitKey[i].split(":")[1];
                else if (splitKey[i].contains("c:")) corrupted = splitKey[i].split(":")[1];
            }
        }
    }

    public Map<String, Map<String, List<JSONItem>>> leagues = new TreeMap<>();

    // TODO: maps need tier info
    public void add(DataEntry entry) {
        if (entry.getItemIndex().equals("-1")) return;

        // "Hardcore Bestiary|currency:orbs|Orb of Transmutation|5"
        String[] splitKey = entry.getKey().split("\\|");
        String parentCategoryName = splitKey[1].split(":")[0];
        String leagueName = splitKey[0];

        if (!leagues.containsKey(leagueName)) leagues.put(leagueName, new TreeMap<>());
        Map<String, List<JSONItem>> league = leagues.get(leagueName);

        if (!league.containsKey(parentCategoryName)) league.put(parentCategoryName, new ArrayList<>());
        List<JSONItem> category = league.get(parentCategoryName);

        JSONItem item = new JSONItem();
        item.copy(entry);
        category.add(item);
    }

    public void sort() {
        for (String leagueKey : leagues.keySet()) {
            Map<String, List<JSONItem>> league = leagues.get(leagueKey);

            for (String categoryKey : league.keySet()) {
                List<JSONItem> category = league.get(categoryKey);
                List<JSONItem> sortedCategory = new ArrayList<>();

                while (!category.isEmpty()) {
                    JSONItem mostExpensiveItem = null;

                    for (JSONItem item : category) {
                        if (mostExpensiveItem == null)
                            mostExpensiveItem = item;
                        else if (item.mean > mostExpensiveItem.mean)
                            mostExpensiveItem = item;
                    }

                    category.remove(mostExpensiveItem);
                    sortedCategory.add(mostExpensiveItem);
                }

                // Write sortedCategory to league map
                league.put(categoryKey, sortedCategory);
            }
        }
    }

    public void clear () {
        leagues.clear();
    }
}
