package ovh.poe;

import ovh.poe.Pricer.DataEntry;

import java.util.*;

/**
 * Holds all the mapper classes. These are not used for anything other than to map JSON strings to objects and
 * therefore will not likely to be changed at all. Each of these classes is similar in  structure - they all have
 * nothing but variables, getters and setters. If additional functionality is required, it is advised to extend
 * these classes.
 */
public class Mappers {
    /**
     * Complete API reply
     */
    public class APIReply {
        public String next_change_id;
        public List<Stash> stashes;
    }

    /**
     * Stash object
     */
    public class Stash {
        public String id;
        public String accountName;
        public String stash;
        public String lastCharacterName;
        public List<Item> items;

        public void fix() {
            if (accountName != null)
                accountName = accountName.replaceAll("[^A-Za-z0-9]", "_");
            if (lastCharacterName != null)
                lastCharacterName = lastCharacterName.replaceAll("[^A-Za-z0-9]", "_");
        }
    }


    /**
     * Universal deserializer for poe.ninja, poe.rates and pathofexile.com/api
     */
    public class ChangeID {
        public String next_change_id;
        public String changeId;
        public String psapi;

        public String get() {
            if (next_change_id != null) return next_change_id;
            else if (changeId != null) return changeId;
            else return psapi;
        }
    }

    /**
     * Properties object
     */
    public class Properties {
        public String name;
        public List<List<String>> values;
    }

    /**
     * Socket object
     */
    public class Socket {
        public int group;
        public String attr;
    }

    /**
     * Item object
     */
    public static class BaseItem {
        public int w, h, x, y, ilvl, frameType;
        public boolean identified, corrupted, enchanted;
        public String icon, league, id, name, typeLine;
        public String note;
        public List<Properties> properties;
        public List<Socket> sockets;
        public List<String> explicitMods;

        // This field varies in the API and cannot be assigned a specific type. A few examples can be seen below:
        // "category": {"jewels": []}
        // "category": {"armour": ["gloves"]}
        public Object category;
        public Object enchantMods;

        public void fix() {
            id = id.substring(0, 16);
            name = name.substring(name.lastIndexOf(">") + 1);
            enchanted = enchantMods != null;
        }
    }

    /**
     * Serializable output
     */
    public static class JSONParcel {
        public static class Item {
            public double mean, median, mode;
            public int count, inc, index, frame;
            public String child, icon, name, type, var;
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
                    IndexedItem indexedItem = Main.RELATIONS.itemIndexToData.get(index);
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

        public Map<String, Map<String, List<Item>>> leagues = new TreeMap<>();

        // TODO: maps need tier info
        public void add(DataEntry entry) {
            if (entry.getItemIndex() < 0) return;

            // "Hardcore Bestiary|currency:orbs|Orb of Transmutation|5"
            String[] splitKey = entry.getKey().split("\\|");
            String parentCategoryName = splitKey[1].split(":")[0];
            String leagueName = splitKey[0];

            if (!leagues.containsKey(leagueName)) leagues.put(leagueName, new TreeMap<>());
            Map<String, List<Item>> league = leagues.get(leagueName);

            if (!league.containsKey(parentCategoryName)) league.put(parentCategoryName, new ArrayList<>());
            List<Item> category = league.get(parentCategoryName);

            Item item = new Item();
            item.copy(entry);
            category.add(item);
        }

        public void sort() {
            for (String leagueKey : leagues.keySet()) {
                Map<String, List<Item>> league = leagues.get(leagueKey);

                for (String categoryKey : league.keySet()) {
                    List<Item> category = league.get(categoryKey);
                    List<Item> sortedCategory = new ArrayList<>();

                    while (!category.isEmpty()) {
                        Item mostExpensiveItem = null;

                        for (Item item : category) {
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

    public static class CurrencyRelation {
        String name, index;
        String[] aliases;
    }

    public static class IndexedItem {
        public String name, type, parent, child, icon, var, tier;
        public int frame;

        public void add(Item item) {
            name = item.name;
            parent = item.parentCategory;
            frame = item.frameType;

            if (item.icon != null) icon = Item.formatIconURL(item.icon);
            if (item.typeLine != null) type = item.typeLine;
            if (item.childCategory != null) child = item.childCategory;
            if (item.variation != null) var = item.variation;
            if (item.tier != null) tier = item.tier;
        }
    }

    public static class HourlyEntry {
        public double mean, median, mode;

        public HourlyEntry (double mean, double median, double mode) {
            this.mean = mean;
            this.median = median;
            this.mode = mode;
        }
    }

    public static class ItemEntry {
        public double price;
        public String accountName, id;

        public ItemEntry (double price, String accountName, String id) {
            this.price = price;
            this.accountName = accountName;
            this.id = id;
        }
    }

    public static class LeagueListElement {
        public String id, startAt, endAt;
    }
}
