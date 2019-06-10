package poe.Item;

import poe.Item.Deserializers.ApiItem;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Key {
    public String name, type;
    public Integer links, gemLevel, gemQuality, mapTier, mapSeries, baseItemLevel;
    public Float enchantMin, enchantMax;
    public Boolean gemCorrupted, baseShaper, baseElder;
    public VariantEnum variation;
    public int frame;

    /**
     * Default constructor
     */
    public Key(ApiItem original) {
        name = original.getName();
        type = original.getTypeLine();
        frame = original.getFrameType();

        // Use typeLine as name if name is missing
        if (name == null || name.equals("") || frame == 2) {
            name = type;
            type = null;
        }

        // Remove formatting strings from name
        if (name.contains(">")) {
            name = name.substring(name.lastIndexOf(">") + 1);
        }
    }

    /**
     * Database constructor
     *
     * @param resultSet
     * @throws SQLException
     */
    public Key(ResultSet resultSet) throws SQLException {
        name = resultSet.getString("name");

        type = resultSet.getString("type");
        if (resultSet.wasNull()) type = null;

        variation = VariantEnum.findByVariation(resultSet.getString("var"));
        if (resultSet.wasNull()) variation = null;

        frame = resultSet.getInt("frame");

        baseItemLevel = resultSet.getInt("base_level");
        if (resultSet.wasNull()) baseItemLevel = null;

        links = resultSet.getInt("links");
        if (resultSet.wasNull()) links = null;

        mapTier = resultSet.getInt("map_tier");
        if (resultSet.wasNull()) mapTier = null;

        mapSeries = resultSet.getInt("map_series");
        if (resultSet.wasNull()) mapSeries = null;

        gemLevel = resultSet.getInt("gem_lvl");
        if (resultSet.wasNull()) gemLevel = null;

        gemQuality = resultSet.getInt("gem_quality");
        if (resultSet.wasNull()) gemQuality = null;

        gemCorrupted = resultSet.getBoolean("gem_corrupted");
        if (resultSet.wasNull()) gemCorrupted = null;

        baseShaper = resultSet.getBoolean("base_shaper");
        if (resultSet.wasNull()) baseShaper = null;

        baseElder = resultSet.getBoolean("base_elder");
        if (resultSet.wasNull()) baseElder = null;

        enchantMin = resultSet.getFloat("enchant_min");
        if (resultSet.wasNull()) enchantMin = null;

        enchantMax = resultSet.getFloat("enchant_max");
        if (resultSet.wasNull()) enchantMax = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!Key.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final Key other = (Key) obj;

        if (this.frame != other.frame) {
            return false;
        }

        if (this.name == null ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.type == null ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        if (this.variation == null ? (other.variation != null) : !this.variation.equals(other.variation)) {
            return false;
        }
        if (this.links == null ? (other.links != null) : !this.links.equals(other.links)) {
            return false;
        }
        if (this.gemLevel == null ? (other.gemLevel != null) : !this.gemLevel.equals(other.gemLevel)) {
            return false;
        }
        if (this.gemQuality == null ? (other.gemQuality != null) : !this.gemQuality.equals(other.gemQuality)) {
            return false;
        }
        if (this.mapTier == null ? (other.mapTier != null) : !this.mapTier.equals(other.mapTier)) {
            return false;
        }
        if (this.mapSeries == null ? (other.mapSeries != null) : !this.mapSeries.equals(other.mapSeries)) {
            return false;
        }
        if (this.gemCorrupted == null ? (other.gemCorrupted != null) : !this.gemCorrupted.equals(other.gemCorrupted)) {
            return false;
        }
        if (this.baseShaper == null ? (other.baseShaper != null) : !this.baseShaper.equals(other.baseShaper)) {
            return false;
        }
        if (this.baseElder == null ? (other.baseElder != null) : !this.baseElder.equals(other.baseElder)) {
            return false;
        }
        if (this.enchantMax == null ? (other.enchantMax != null) : !this.enchantMax.equals(other.enchantMax)) {
            return false;
        }
        if (this.enchantMin == null ? (other.enchantMin != null) : !this.enchantMin.equals(other.enchantMin)) {
            return false;
        }
        return this.baseItemLevel == null ? (other.baseItemLevel == null) : this.baseItemLevel.equals(other.baseItemLevel);
    }

    @Override
    public int hashCode() {
        int hash = 3;

        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 53 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 53 * hash + (this.variation != null ? this.variation.hashCode() : 0);
        hash = 53 * hash + (this.links != null ? this.links.hashCode() : 0);
        hash = 53 * hash + (this.gemLevel != null ? this.gemLevel.hashCode() : 0);
        hash = 53 * hash + (this.gemQuality != null ? this.gemQuality.hashCode() : 0);
        hash = 53 * hash + (this.mapTier != null ? this.mapTier.hashCode() : 0);
        hash = 53 * hash + (this.mapSeries != null ? this.mapSeries.hashCode() : 0);
        hash = 53 * hash + (this.gemCorrupted != null ? this.gemCorrupted.hashCode() : 0);
        hash = 53 * hash + (this.baseShaper != null ? this.baseShaper.hashCode() : 0);
        hash = 53 * hash + (this.baseElder != null ? this.baseElder.hashCode() : 0);
        hash = 53 * hash + (this.enchantMin != null ? this.enchantMin.hashCode() : 0);
        hash = 53 * hash + (this.enchantMax != null ? this.enchantMax.hashCode() : 0);
        hash = 53 * hash + (this.baseItemLevel != null ? this.baseItemLevel.hashCode() : 0);
        hash = 53 * hash + this.frame;

        return hash;
    }

    @Override
    public String toString() {
        return "name:" + name +
                "|type:" + type +
                "|frame:" + frame +
                "|ilvl:" + baseItemLevel +
                "|links:" + links +
                "|tier:" + mapTier +
                "|series:" + mapSeries +
                "|var:" + variation +
                "|lvl:" + gemLevel +
                "|qual:" + gemQuality +
                "|corr:" + gemCorrupted +
                "|shaper:" + baseShaper +
                "|elder:" + baseElder +
                "|enchantBottomRange:" + enchantMin +
                "|enchantTopRange:" + enchantMax;
    }
}
