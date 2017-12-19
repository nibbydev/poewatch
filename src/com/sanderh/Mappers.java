package com.sanderh;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

public class Mappers {
    //  Name: Mapper
    //  Date created: 17.12.2017
    //  Last modified: 17.12.2017
    //  Description: Holds all the mapper classes. These are not used for anything other than to map JSON strings to
    //               objects and therefore will not likely to be changed at all. Each of these classes is similar in
    //               structure - they all have nothing but variables, getters and setters. If additional functionality
    //               is required, it is advised to extend these classes.

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class APIReply {
        //  Name: APIReply
        //  Date created: 23.11.2017
        //  Last modified: 17.12.2017
        //  Description: Class used for deserializing a JSON string

        private String next_change_id;
        private List<Stash> stashes;

        ///////////////////////
        // Getters / Setters //
        ///////////////////////

        public void setStashes(List<Stash> stashes) {
            this.stashes = stashes;
        }

        public void setNext_change_id(String next_change_id) {
            this.next_change_id = next_change_id;
        }

        public String getNext_change_id() {
            return next_change_id;
        }

        public List<Stash> getStashes() {
            return stashes;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeID {
        //  Name: ChangeID()
        //  Date created: 30.11.2017
        //  Last modified: 19.12.2017
        //  Description: Maps http://poe.ninja 's and http://poe-rates.com 's JSON API to an object

        private String next_change_id;
        private String changeId;
        private String change_id;

        ///////////////////////
        // Getters / Setters //
        ///////////////////////

        public String getNext_change_id() {
            return next_change_id;
        }

        public void setNext_change_id(String next_change_id) {
            this.next_change_id = next_change_id;
        }

        public String getChangeId() {
            return next_change_id;
        }

        public void setChangeId(String changeId) {
            this.next_change_id = changeId;
        }

        public void setChange_id(String change_id) {
            this.next_change_id = change_id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        //  Name: Properties
        //  Date created: 28.11.2017
        //  Last modified: 17.12.2017
        //  Description: Class used for deserializing a JSON string

        private String name;
        private List<List<String>> values;

        ///////////////////////
        // Getters / Setters //
        ///////////////////////

        public String getName() {
            return name;
        }

        public List<List<String>> getValues() {
            return values;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValues(List<List<String>> values) {
            this.values = values;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Socket {
        //  Name: Socket
        //  Date created: 28.11.2017
        //  Last modified: 17.12.2017
        //  Description: Class used for deserializing a JSON string

        private int group;
        private String attr;

        ///////////////////////
        // Getters / Setters //
        ///////////////////////

        public int getGroup() {
            return group;
        }

        public String getAttr() {
            return attr;
        }

        public void setAttr(String attr) {
            this.attr = attr;
        }

        public void setGroup(int group) {
            this.group = group;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stash {
        //  Name: Stash
        //  Date created: 23.11.2017
        //  Last modified: 17.12.2017
        //  Description: Class used for deserializing a JSON string

        private String id;
        private String accountName;
        private String stash;
        private String lastCharacterName;
        private List<com.sanderh.Item> items;

        ///////////////////////
        // Getters / Setters //
        ///////////////////////

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setItems(List<com.sanderh.Item> items) {
            this.items = items;
        }

        public void setLastCharacterName(String lastCharacterName) {
            this.lastCharacterName = lastCharacterName;
        }

        public void setStash(String stash) {
            this.stash = stash;
        }

        public List<com.sanderh.Item> getItems() {
            return items;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getId() {
            return id;
        }

        public String getLastCharacterName() {
            return lastCharacterName;
        }

        public String getStash() {
            return stash;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaseItem {
        //  Name: BaseItem
        //  Date created: 23.11.2017
        //  Last modified: 17.12.2017
        //  Description: Class used for deserializing a JSON string

        private int w, h, x, y, ilvl, frameType;
        private boolean identified = true;
        private boolean corrupted = false;
        private String icon, league, id, name, typeLine;
        private String note = "";
        private List<Properties> properties;
        private List<Socket> sockets;
        private List<String> explicitMods;

        // This field varies in the API and cannot be assigned a specific type. A few examples can be seen below:
        // {"category": "jewels"}
        // {"category": {"armour": ["gloves"]}}
        private Object category;

        ///////////////////////
        // Getters / Setters //
        ///////////////////////

        public String getId() {
            return id;
        }

        public void setId(String id) {
            // Save space, use 4x smaller IDs
            this.id = id.substring(0, 16);
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

        public int getFrameType() {
            return frameType;
        }

        public String getIcon() {
            return icon;
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

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean isCorrupted() {
            return corrupted;
        }

        public boolean isIdentified() {
            return identified;
        }

        public List<Properties> getProperties() {
            return properties;
        }

        public List<Socket> getSockets() {
            return sockets;
        }

        public List<String> getExplicitMods() {
            return explicitMods;
        }

        public void setCorrupted(boolean corrupted) {
            this.corrupted = corrupted;
        }

        public void setFrameType(int frameType) {
            this.frameType = frameType;
        }

        public void setH(int h) {
            this.h = h;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public void setIdentified(boolean identified) {
            this.identified = identified;
        }

        public void setIlvl(int ilvl) {
            this.ilvl = ilvl;
        }

        public void setLeague(String league) {
            this.league = league;
        }

        public void setName(String name) {
            this.name = name.substring(name.lastIndexOf(">") + 1);
        }

        public void setNote(String note) {
            this.note = note;
        }

        public void setTypeLine(String typeLine) {
            this.typeLine = typeLine;
        }

        public void setW(int w) {
            this.w = w;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void setProperties(List<Properties> properties) {
            this.properties = properties;
        }

        public void setSockets(List<Socket> sockets) {
            this.sockets = sockets;
        }

        public void setExplicitMods(List<String> explicitMods) {
            this.explicitMods = explicitMods;
        }

        public void setCategory(Object category) {
            this.category = category;
        }

        public Object getCategory() {
            return category;
        }
    }
}
