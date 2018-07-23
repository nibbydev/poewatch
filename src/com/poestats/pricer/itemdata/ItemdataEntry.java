package com.poestats.pricer.itemdata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemdataEntry {
    private int id, frame;
    private String name, type, key, parentCategory, childCategory, var, icon;
    private Integer tier, lvl, quality, links;
    private Boolean corrupted;

    public void load(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("id");
        frame = resultSet.getInt("frame");
        name = resultSet.getString("name");
        type = resultSet.getString("type");
        key = resultSet.getString("key");
        parentCategory = resultSet.getString("cpName");
        childCategory = resultSet.getString("ccName");
        icon = resultSet.getString("icon");
        var = resultSet.getString("var");

        corrupted = resultSet.getBoolean("corrupted");
        if (resultSet.wasNull()) corrupted = null;

        tier = resultSet.getInt("tier");
        if (resultSet.wasNull()) tier = null;

        lvl = resultSet.getInt("lvl");
        if (resultSet.wasNull()) lvl = null;

        quality = resultSet.getInt("quality");
        if (resultSet.wasNull()) quality = null;

        links = resultSet.getInt("links");
        if (resultSet.wasNull()) links = null;
    }

    public String getId() {
        return Integer.toString(id);
    }
}
