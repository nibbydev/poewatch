package com.poestats.pricer.itemdata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemdataEntry {
    private int id, frame;
    private String name, type, key, parent_category, child_category;
    private List<ItemdataEntryMember> members = new ArrayList<>();

    public void load(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("idp-id");
        frame = resultSet.getInt("frame");
        name = resultSet.getString("name");
        type = resultSet.getString("type");
        key = resultSet.getString("parent-key");
        parent_category = resultSet.getString("cp-name");
        child_category = resultSet.getString("cc-name");
    }

    public void loadMember(ResultSet resultSet) throws SQLException {
        ItemdataEntryMember member = new ItemdataEntryMember();
        member.load(resultSet);
        members.add(member);
    }

    public String getId() {
        return Integer.toString(id);
    }
}
