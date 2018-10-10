package watch.poe.manager.entry.item;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Key {
    private String name, typeLine, variation;
    private Integer links, level, quality, tier, corrupted, iLvl;
    private int frameType;

    //------------------------------------------------------------------------------------------------------------
    // Loaders
    //------------------------------------------------------------------------------------------------------------

    public Key(Item item) {
        name = item.getName();
        typeLine = item.getTypeLine();
        frameType = item.getFrameType();
        iLvl = item.getIlvl();
        links = item.getLinks();
        tier = item.getTier();
        variation = item.getVariation();

        level = item.getLevel();
        quality = item.getQuality();
        corrupted = item.getCorrupted();
    }

    public Key(ResultSet resultSet) throws SQLException {
        name = resultSet.getString("name");

        typeLine = resultSet.getString("type");
        if (resultSet.wasNull()) typeLine = null;

        variation = resultSet.getString("var");
        if (resultSet.wasNull()) variation = null;

        frameType = resultSet.getInt("frame");

        iLvl = resultSet.getInt("ilvl");
        if (resultSet.wasNull()) iLvl = null;

        links = resultSet.getInt("links");
        if (resultSet.wasNull()) links = null;

        tier = resultSet.getInt("tier");
        if (resultSet.wasNull()) tier = null;

        level = resultSet.getInt("lvl");
        if (resultSet.wasNull()) level = null;

        quality = resultSet.getInt("quality");
        if (resultSet.wasNull()) quality = null;

        corrupted = resultSet.getInt("corrupted");
        if (resultSet.wasNull()) corrupted = null;
    }

    //------------------------------------------------------------------------------------------------------------
    // Equality
    //------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!Key.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final Key other = (Key) obj;

        if (this.frameType != other.frameType) {
            return false;
        }

        if (this.name == null ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.typeLine == null ? (other.typeLine != null) : !this.typeLine.equals(other.typeLine)) {
            return false;
        }
        if (this.variation == null ? (other.variation != null) : !this.variation.equals(other.variation)) {
            return false;
        }
        if (this.links == null ? (other.links != null) : !this.links.equals(other.links)) {
            return false;
        }
        if (this.level == null ? (other.level != null) : !this.level.equals(other.level)) {
            return false;
        }
        if (this.quality == null ? (other.quality != null) : !this.quality.equals(other.quality)) {
            return false;
        }
        if (this.tier == null ? (other.tier != null) : !this.tier.equals(other.tier)) {
            return false;
        }
        if (this.corrupted == null ? (other.corrupted != null) : !this.corrupted.equals(other.corrupted)) {
            return false;
        }
        if (this.iLvl == null ? (other.iLvl != null) : !this.iLvl.equals(other.iLvl)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;

        hash = 53 * hash + (this.name       != null ? this.name.hashCode()      : 0);
        hash = 53 * hash + (this.typeLine   != null ? this.typeLine.hashCode()  : 0);
        hash = 53 * hash + (this.variation  != null ? this.variation.hashCode() : 0);
        hash = 53 * hash + (this.links      != null ? this.links.hashCode()     : 0);
        hash = 53 * hash + (this.level      != null ? this.level.hashCode()     : 0);
        hash = 53 * hash + (this.quality    != null ? this.quality.hashCode()   : 0);
        hash = 53 * hash + (this.tier       != null ? this.tier.hashCode()      : 0);
        hash = 53 * hash + (this.corrupted  != null ? this.corrupted.hashCode() : 0);
        hash = 53 * hash + (this.iLvl       != null ? this.iLvl.hashCode()      : 0);
        hash = 53 * hash + this.frameType;

        return hash;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    //------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getTypeLine() {
        return typeLine;
    }

    public Integer getTier() {
        return tier;
    }

    public Integer getQuality() {
        return quality;
    }

    public Integer getLevel() {
        return level;
    }

    public Integer getLinks() {
        return links;
    }

    public String getVariation() {
        return variation;
    }

    public int getFrameType() {
        return frameType;
    }

    public Integer getCorrupted() {
        return corrupted;
    }

    public Integer getiLvl() {
        return iLvl;
    }
}
