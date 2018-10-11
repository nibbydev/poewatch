package poe.manager.entry.item;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

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
        public List<BaseItem> items;
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
     * Property object
     */
    public class Property {
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
     * Base item object
     */
    public static class BaseItem {
        //------------------------------------------------------------------------------------------------------------
        // Base item "primitive" variables
        //------------------------------------------------------------------------------------------------------------

        private boolean identified;
        private int w, h, x, y, ilvl, frameType;
        private Boolean corrupted, shaper, elder;
        private String icon, league, id, name, typeLine, note;

        @SerializedName(value = "raceReward", alternate = {"seaRaceReward", "cisRaceReward", "thRaceReward", "RaceReward"})
        private Object raceReward;

        //------------------------------------------------------------------------------------------------------------
        // Base item objects
        //------------------------------------------------------------------------------------------------------------

        private Map<String, List<String>> category; // TODO: create an object for this monstrosity
        private List<Mappers.Property> properties;
        private List<Mappers.Socket> sockets;
        private List<String> explicitMods;
        private List<String> enchantMods;

        //------------------------------------------------------------------------------------------------------------
        // Getters
        //------------------------------------------------------------------------------------------------------------

        public Boolean getCorrupted() {
            return corrupted;
        }

        public Boolean getElder() {
            return elder;
        }

        public Boolean getShaper() {
            return shaper;
        }

        public int getFrameType() {
            return frameType;
        }

        public int getH() {
            return h;
        }

        public int getIlvl() {
            return ilvl;
        }

        public int getW() {
            return w;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public List<Property> getProperties() {
            return properties;
        }

        public List<Socket> getSockets() {
            return sockets;
        }

        public List<String> getEnchantMods() {
            return enchantMods;
        }

        public Map<String, List<String>> getCategory() {
            return category;
        }

        public List<String> getExplicitMods() {
            return explicitMods;
        }

        public String getIcon() {
            return icon;
        }

        public String getId() {
            return id;
        }

        public String getLeague() {
            return league;
        }

        public String getName() {
            return name;
        }

        public String getNote() {
            return note;
        }

        public String getTypeLine() {
            return typeLine;
        }

        /**
         * Special variable override for when item is chaos
         */
        public void setTypeLine(String typeLine) {
            this.typeLine = typeLine;
        }

        public boolean isIdentified() {
            return identified;
        }

        public Boolean getRaceReward() {
            return raceReward == null ? null : true;
        }
    }
}
