package com.poestats.pricer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class ParcelEntry {
    private class HistoryData {
        public double[] mean = new double[7];
        public double[] median = new double[7];
        public double[] mode = new double[7];
        public double[] exalted = new double[7];
        public int[] count = new int[7];
        public int[] quantity = new int[7];
        public double[] spark = new double[7];
    }

    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    // Item
    private String sup, sub;
    private double mean, median, mode, exalted;
    private int count, quantity;

    // Sup
    private String parent, child, name, type, supKey;
    private int frame;

    // Sub
    private String tier, lvl, quality, corrupted, links, var, subKey, icon;

    // History
    private HistoryData historyData = new HistoryData();

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void loadItem(ResultSet result) throws SQLException {
        sup = result.getString("sup");
        sub = result.getString("sub");

        mean = result.getDouble("mean");
        median = result.getDouble("median");
        mode = result.getDouble("mode");
        exalted = result.getDouble("exalted");
        count = result.getInt("count");
        quantity = result.getInt("quantity");

        parent = result.getString("parent =");
        child = result.getString("child");
        name = result.getString("name");
        type = result.getString("type");
        supKey = result.getString("supKey");
        frame = result.getInt("frame");

        tier = result.getString("tier");
        lvl = result.getString("lvl");
        quality = result.getString("quality");
        corrupted = result.getString("corrupted");
        links = result.getString("links");
        var = result.getString("var");
        subKey = result.getString("subKey");
        icon = result.getString("icon");
    }

    public void loadHistory(ResultSet result) throws SQLException {
        int count = 6;
        while (result.next()) {
            historyData.mean[count] = result.getDouble("mean");
            historyData.median[count] = result.getDouble("median");
            historyData.mode[count] = result.getDouble("mode");
            historyData.exalted[count] = result.getDouble("exalted");

            historyData.count[count] = result.getInt("count");
            historyData.quantity[count] = result.getInt("quantity");
            count--;
        }
    }

    private void calcSpark() {

    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

}
