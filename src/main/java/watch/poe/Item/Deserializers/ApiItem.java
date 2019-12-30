package poe.Item.Deserializers;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Base item object
 */
public class ApiItem {
    private boolean identified;
    private int ilvl, frameType;
    private Boolean corrupted, synthesised;
    private String icon, league, id, name, typeLine, note;
    private Integer stackSize;
    private String prophecyText;

    @SerializedName(value = "raceReward", alternate = {"seaRaceReward", "cisRaceReward", "thRaceReward", "RaceReward"})
    private Object raceReward;

    private Influences influences;
    private Extended extended;
    private List<Property> properties;
    private List<Socket> sockets;
    private List<String> explicitMods;
    private List<String> enchantMods;

    public Influences getInfluences() {
        return influences;
    }

    public Boolean getCorrupted() {
        return corrupted;
    }

    public int getFrameType() {
        return frameType;
    }

    public int getIlvl() {
        return ilvl;
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

    public boolean isStackable() {
        return stackSize != null;
    }

    public Integer getStackSize() {
        return stackSize;
    }

    public Boolean getSynthesised() {
        return synthesised;
    }

    public boolean isIdentified() {
        return identified;
    }

    public Boolean isRaceReward() {
        return raceReward == null ? null : true;
    }

    public String getProphecyText() {
        return prophecyText;
    }

    public Extended getExtended() {
        return extended;
    }

    public boolean isEnchantBranch() {
        return enchantMods != null;
    }

    public boolean isCraftingBranch() {
        return influences != null
                && (frameType == 0 || frameType == 1 || frameType == 2)
                && (influences.shaper != null && influences.shaper
                || influences.elder != null && influences.elder
                || influences.crusader != null && influences.crusader
                || influences.redeemer != null && influences.redeemer
                || influences.hunter != null && influences.hunter
                || influences.warlord != null && influences.warlord);
    }
}
