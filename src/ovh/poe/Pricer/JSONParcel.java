package ovh.poe.Pricer;

import ovh.poe.Main;
import ovh.poe.RelationManager.IndexedItem;

import java.util.*;

public class JSONParcel {
    private static class HistoryItem {
        public List<Double> mean = new ArrayList<>();
        public List<Double> median = new ArrayList<>();
        public List<Double> mode = new ArrayList<>();
        public List<Integer> quantity = new ArrayList<>();
    }

    public static class JSONItem {
        public double mean, median, mode;
        public int count, quantity;
        public String index, specificKey;
        public String corrupted, lvl, quality, links;
        public HistoryItem history = new HistoryItem();

        public void copy (Entry entry) {
            mean = entry.getMean();
            median = entry.getMedian();
            mode = entry.getMode();
            count = entry.getCount() + entry.getInc_counter();
            quantity = entry.getQuantity();
            index = entry.getItemIndex();
            specificKey = entry.getKey();

            // Copy the history over
            for (Entry.DailyEntry dailyEntry : entry.getDb_weekly()) {
                history.mean.add(dailyEntry.mean);
                history.median.add(dailyEntry.median);
                history.mode.add(dailyEntry.mode);
                history.quantity.add(dailyEntry.quantity);
            }

            /*
            // Check if there's a match for the specific index
            if (Main.RELATIONS.itemIndexToData.containsKey(index)) {
                IndexedItem indexedItem = Main.RELATIONS.itemIndexToData.get(index);
                frame = indexedItem.frame;
                child = indexedItem.child;
                icon = indexedItem.icon;
                name = indexedItem.name;
                type = indexedItem.type;
                var = indexedItem.var;
                tier = indexedItem.tier;
            }
            */

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

    public Map<String, Map<String, List<JSONItem>>> leagues = new HashMap<>();

    public void add(Entry entry) {
        if (entry.getItemIndex().equals("-")) return;

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
