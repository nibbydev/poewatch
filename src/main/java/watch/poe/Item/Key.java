package poe.Item;

import poe.Item.Deserializers.ApiItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class Key {
    public String name, type;
    public Integer links, gemLevel, gemQuality, mapTier, mapSeries, baseItemLevel;
    public Float enchantMin, enchantMax;
    public Boolean gemCorrupted, shaper, elder, crusader, redeemer, hunter, warlord;
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

        variation = VariantEnum.findByVariation(name, resultSet.getString("var"));
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

        shaper = resultSet.getBoolean("shaper");
        if (resultSet.wasNull()) shaper = null;

        elder = resultSet.getBoolean("elder");
        if (resultSet.wasNull()) elder = null;

        crusader = resultSet.getBoolean("crusader");
        if (resultSet.wasNull()) crusader = null;

        redeemer = resultSet.getBoolean("redeemer");
        if (resultSet.wasNull()) redeemer = null;

        hunter = resultSet.getBoolean("hunter");
        if (resultSet.wasNull()) hunter = null;

        warlord = resultSet.getBoolean("warlord");
        if (resultSet.wasNull()) warlord = null;

        enchantMin = resultSet.getFloat("enchant_min");
        if (resultSet.wasNull()) enchantMin = null;

        enchantMax = resultSet.getFloat("enchant_max");
        if (resultSet.wasNull()) enchantMax = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return frame == key.frame &&
                name.equals(key.name) &&
                Objects.equals(type, key.type) &&
                Objects.equals(links, key.links) &&
                Objects.equals(gemLevel, key.gemLevel) &&
                Objects.equals(gemQuality, key.gemQuality) &&
                Objects.equals(mapTier, key.mapTier) &&
                Objects.equals(mapSeries, key.mapSeries) &&
                Objects.equals(baseItemLevel, key.baseItemLevel) &&
                Objects.equals(enchantMin, key.enchantMin) &&
                Objects.equals(enchantMax, key.enchantMax) &&
                Objects.equals(gemCorrupted, key.gemCorrupted) &&
                Objects.equals(shaper, key.shaper) &&
                Objects.equals(elder, key.elder) &&
                Objects.equals(crusader, key.crusader) &&
                Objects.equals(redeemer, key.redeemer) &&
                Objects.equals(hunter, key.hunter) &&
                Objects.equals(warlord, key.warlord) &&
                variation == key.variation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, links, gemLevel, gemQuality, mapTier, mapSeries, baseItemLevel, enchantMin, enchantMax, gemCorrupted, shaper, elder, crusader, redeemer, hunter, warlord, variation, frame);
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
                "|shaper:" + shaper +
                "|elder:" + elder +
                "|crusader:" + crusader +
                "|redeemer:" + redeemer +
                "|hunter:" + hunter +
                "|warlord:" + warlord +
                "|enchantBottomRange:" + enchantMin +
                "|enchantTopRange:" + enchantMax;
    }
}
