package com.poestats.pricer.itemdata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemdataEntryMember {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private int id;
    private Integer tier, lvl, quality, corrupted, links;
    private String var, key, icon;

    public void load(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("idc-id");
        var = resultSet.getString("var");
        key = resultSet.getString("child-key");
        icon = resultSet.getString("icon");

        try {
            tier = Integer.parseUnsignedInt(resultSet.getString("tier"));
        } catch (NumberFormatException ex) {}

        try {
            lvl = Integer.parseUnsignedInt(resultSet.getString("lvl"));
        } catch (NumberFormatException ex) {}

        try {
            quality = Integer.parseUnsignedInt(resultSet.getString("quality"));
        } catch (NumberFormatException ex) {}

        try {
            corrupted = Integer.parseUnsignedInt(resultSet.getString("corrupted"));
        } catch (NumberFormatException ex) {}

        try {
            links = Integer.parseUnsignedInt(resultSet.getString("links"));
        } catch (NumberFormatException ex) {}
    }
}
