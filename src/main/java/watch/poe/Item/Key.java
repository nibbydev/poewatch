package poe.Item;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Key {
    private String name, typeLine, variation;
    private Integer links, level, quality, tier, iLvl;
    private Float enchantBottomRange, enchantTopRange;
    private Boolean corrupted, shaper, elder;
    private int frameType;

    //------------------------------------------------------------------------------------------------------------
    // Loaders
    //------------------------------------------------------------------------------------------------------------

    public Key(Item item) {
        name = item.name;
        typeLine = item.typeLine;
        frameType = item.frameType;
        iLvl = item.itemLevel;
        links = item.links;
        tier = item.mapTier;
        shaper = item.shaper;
        elder = item.elder;
        enchantBottomRange = item.enchantBottomRange;
        enchantTopRange = item.enchantTopRange;
        variation = item.variation == null ? null : item.variation.getVariation();

        level = item.gemLevel;
        quality = item.gemQuality;
        corrupted = item.gemCorrupted;
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

        corrupted = resultSet.getBoolean("corrupted");
        if (resultSet.wasNull()) corrupted = null;

        shaper = resultSet.getBoolean("shaper");
        if (resultSet.wasNull()) shaper = null;

        elder = resultSet.getBoolean("elder");
        if (resultSet.wasNull()) elder = null;

        enchantBottomRange = resultSet.getFloat("enchantBottomRange");
        if (resultSet.wasNull()) enchantBottomRange = null;

        enchantTopRange = resultSet.getFloat("enchantTopRange");
        if (resultSet.wasNull()) enchantTopRange = null;
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
        if (this.shaper == null ? (other.shaper != null) : !this.shaper.equals(other.shaper)) {
            return false;
        }
        if (this.elder == null ? (other.elder != null) : !this.elder.equals(other.elder)) {
            return false;
        }
        if (this.enchantTopRange == null ? (other.enchantTopRange != null) : !this.enchantTopRange.equals(other.enchantTopRange)) {
            return false;
        }
        if (this.enchantBottomRange == null ? (other.enchantBottomRange != null) : !this.enchantBottomRange.equals(other.enchantBottomRange)) {
            return false;
        }
        return this.iLvl == null ? (other.iLvl == null) : this.iLvl.equals(other.iLvl);
    }

    @Override
    public int hashCode() {
        int hash = 3;

        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 53 * hash + (this.typeLine != null ? this.typeLine.hashCode() : 0);
        hash = 53 * hash + (this.variation != null ? this.variation.hashCode() : 0);
        hash = 53 * hash + (this.links != null ? this.links.hashCode() : 0);
        hash = 53 * hash + (this.level != null ? this.level.hashCode() : 0);
        hash = 53 * hash + (this.quality != null ? this.quality.hashCode() : 0);
        hash = 53 * hash + (this.tier != null ? this.tier.hashCode() : 0);
        hash = 53 * hash + (this.corrupted != null ? this.corrupted.hashCode() : 0);
        hash = 53 * hash + (this.shaper != null ? this.shaper.hashCode() : 0);
        hash = 53 * hash + (this.elder != null ? this.elder.hashCode() : 0);
        hash = 53 * hash + (this.enchantBottomRange != null ? this.enchantBottomRange.hashCode() : 0);
        hash = 53 * hash + (this.enchantTopRange != null ? this.enchantTopRange.hashCode() : 0);
        hash = 53 * hash + (this.iLvl != null ? this.iLvl.hashCode() : 0);
        hash = 53 * hash + this.frameType;

        return hash;
    }

    @Override
    public String toString() {
        return "name:" + name + "|type:" + typeLine + "|frame:" + frameType + "|ilvl:" + iLvl + "|links:" + links
                + "|tier:" + tier + "|var:" + variation + "|lvl:" + level + "|qual:" + quality + "|corr:" + corrupted
                + "|shaper:" + shaper + "|elder:" + elder + "|enchantBottomRange:" + enchantBottomRange
                + "|enchantTopRange:" + enchantTopRange;
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

    public Boolean getCorrupted() {
        return corrupted;
    }

    public Integer getiLvl() {
        return iLvl;
    }

    public Float getEnchantTopRange() {
        return enchantTopRange;
    }

    public Boolean getShaper() {
        return shaper;
    }

    public Boolean getElder() {
        return elder;
    }

    public Float getEnchantBottomRange() {
        return enchantBottomRange;
    }
}
