package MainPack;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    /*  Name: Item
    *   Date created: 23.11.2017
    *   Last modified: 23.11.2017
    *   Description: Class used for deserializing a JSON string
    */

    // Predefined Variables
    private int w;
    private int h;
    private int ilvl;
    private String icon;
    private String league;
    private String id;
    private String name;
    private String typeLine;
    private boolean identified;
    private boolean corrupted;
    private String note;
    private int frameType;
    private int x;
    private int y;

    // Get Methods
    public String getId() { return id; }
    public int getH() { return h; }
    public int getIlvl() { return ilvl; }
    public int getW() { return w; }
    public int getFrameType() { return frameType; }
    public String getIcon() { return icon; }
    public String getLeague() { return league; }
    public String getName() { return name; }
    public String getNote() { return note; }
    public String getTypeLine() { return typeLine; }
    public int getX() { return x; }
    public int getY() { return y; }

    // Set Methods
    public void setId(String id) { this.id = id; }
    public void setCorrupted(boolean corrupted) { this.corrupted = corrupted; }
    public void setFrameType(int frameType) { this.frameType = frameType; }
    public void setH(int h) { this.h = h; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setIdentified(boolean identified) { this.identified = identified; }
    public void setIlvl(int ilvl) { this.ilvl = ilvl; }
    public void setLeague(String league) { this.league = league; }
    public void setName(String name) {
        if(name.contains("<<set:MS>><<set:M>><<set:S>>"))
            this.name = name.replace("<<set:MS>><<set:M>><<set:S>>", "");
        else
            this.name = name;
    }
    public void setNote(String note) { this.note = note; }
    public void setTypeLine(String typeLine) { this.typeLine = typeLine; }
    public void setW(int w) { this.w = w; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

}
