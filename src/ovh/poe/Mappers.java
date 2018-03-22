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

    public static class CurrencyRelation {
        String name, index;
        String[] aliases;
    }

    public static class IndexedItem {
        public String name, type, parent, child, icon, var, tier, key;
        public int frame;

        public void add(Item item, String key) {
            name = item.name;
            parent = item.parentCategory;
            frame = item.frameType;
            this.key = key;

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
