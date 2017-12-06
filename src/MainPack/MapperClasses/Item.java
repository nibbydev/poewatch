package MainPack.MapperClasses;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    //  Name: Item
    //  Date created: 23.11.2017
    //  Last modified: 29.11.2017
    //  Description: Class used for deserializing a JSON string

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

    private boolean discard = false;
    private double price;
    private String priceType;
    private String itemType;
    private String key = "";

    ////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters that do not have anything to do with the deserialization //
    ////////////////////////////////////////////////////////////////////////////////

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

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

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

}
