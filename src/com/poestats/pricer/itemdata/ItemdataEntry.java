package com.poestats.pricer.itemdata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemdataEntry {
    private int id, frame;
    private String name, type, key, parent_category, child_category;

    public void load(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("id");
        frame = resultSet.getInt("frame");
        name = resultSet.getString("name");
        type = resultSet.getString("type");
        key = resultSet.getString("key");
        parent_category = resultSet.getString("cpName");
        child_category = resultSet.getString("ccName");
    }

    public String getId() {
        return Integer.toString(id);
    }
}
