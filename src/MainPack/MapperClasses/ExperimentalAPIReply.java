package MainPack.MapperClasses;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentalAPIReply {
    /*   Name: ExperimentalAPIReply
     *   Date created: 29.11.2017
     *   Last modified: 29.11.2017
     *   Description: Currently not in use
     */

    private String next_change_id;
    private List<Stash> stashes;

    /*
     * Deserializer methods: APIReply SET
     */

    public void setStashes(List<Stash> stashes) {
        this.stashes = stashes;
    }

    public void setNext_change_id(String next_change_id) {
        this.next_change_id = next_change_id;
    }

    /*
     * Deserializer methods: APPIReply GET
     */

    public String getNext_change_id() {
        return next_change_id;
    }

    public List<Stash> getStashes() {
        return stashes;
    }

    /*
     * Deserializer class: Stash
     */

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Stash {
        /*   Name: Stash
         *   Date created: 23.11.2017
         *   Last modified: 29.11.2017
         *   Description: Class used for deserializing a JSON string
         */

        private String id;
        private String accountName;
        private String stash;
        private String lastCharacterName;
        private List<Item> items;

        /*
         * Deserializer methods: Stash SET
         */

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }

        public void setLastCharacterName(String lastCharacterName) {
            this.lastCharacterName = lastCharacterName;
        }

        public void setStash(String stash) {
            this.stash = stash;
        }

        /*
         * Deserializer methods: Stash GET
         */

        public List<Item> getItems() {
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

        /*
         * Deserializer class: Item
         */

        @JsonIgnoreProperties(ignoreUnknown = true)
        class Item {
            /*   Name: Item
             *   Date created: 23.11.2017
             *   Last modified: 29.11.2017
             *   Description: Class used for deserializing a JSON string
             */

            private int w;
            private int h;
            private int ilvl;
            private String icon;
            private String league;
            private String id;
            private String name;
            private String typeLine;
            private boolean identified = true;
            private boolean corrupted = false;
            private String note = "";
            private int frameType;
            private int x;
            private int y;
            private List<Properties> properties;
            private List<Socket> sockets;
            private List<String> explicitMods;

            // Not for the deserializer
            private boolean discard = false;
            private double price;
            private String priceType;
            private String itemType;
            private String key = "";

            /*
             * Deserializer methods: Item GET
             */

            public String getId() {
                return id;
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

            /*
             * Deserializer methods: Item SET
             */

            public void setId(String id) {
                this.id = id;
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
                if (name.contains("<<set:MS>><<set:M>><<set:S>>"))
                    this.name = name.replace("<<set:MS>><<set:M>><<set:S>>", "");
                else
                    this.name = name;
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

            /*
             * User methods: Item
             */

            public boolean isDiscard() {
                return discard;
            }

            public void setDiscard() {
                this.discard = true;
            }

            public double getPrice() {
                return price;
            }

            public String getPriceType() {
                return priceType;
            }

            public void setPrice(double price) {
                this.price = price;
            }

            public void setPriceType(String priceType) {
                this.priceType = priceType;
            }

            public String getItemType() {
                return itemType;
            }

            public void setItemType(String itemType) {
                this.itemType = itemType;
            }

            public String getKey() {
                return key;
            }

            public void addKey(String buffer) {
                this.key += buffer;
            }

            /*
             * Deserializer class: Properties
             */

            @JsonIgnoreProperties(ignoreUnknown = true)
            class Properties {
                /*   Name: Properties
                 *   Date created: 28.11.2017
                 *   Last modified: 29.11.2017
                 *   Description: Class used for deserializing a JSON string
                 */

                private String name;
                private List<List<String>> values;

                /*
                 * Deserializer methods: Properties GET
                 */

                public String getName() {
                    return name;
                }

                public List<List<String>> getValues() {
                    return values;
                }

                /*
                 * Deserializer methods: Properties SET
                 */

                public void setName(String name) {
                    this.name = name;
                }

                public void setValues(List<List<String>> values) {
                    this.values = values;
                }
            }

            /*
             * Deserializer class: Sockets
             */

            @JsonIgnoreProperties(ignoreUnknown = true)
            class Sockets {
                /*   Name: Sockets
                 *   Date created: 28.11.2017
                 *   Last modified: 29.11.2017
                 *   Description: Class used for deserializing a JSON string
                 */

                private int group;
                private String attr;

                /*
                 * Deserializer methods: Sockets GET
                 */

                public int getGroup() {
                    return group;
                }

                public String getAttr() {
                    return attr;
                }

                /*
                 * Deserializer methods: Sockets SET
                 */

                public void setAttr(String attr) {
                    this.attr = attr;
                }

                public void setGroup(int group) {
                    this.group = group;
                }
            }
        }
    }
}
