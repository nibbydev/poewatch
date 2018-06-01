package com.poestats.pricer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ParcelEntry {
    public class HistoryData {
        public Double[] mean = new Double[7];
        public Double[] median = new Double[7];
        public Double[] mode = new Double[7];
        public Double[] exalted = new Double[7];
        public Integer[] count = new Integer[7];
        public Integer[] quantity = new Integer[7];
        public Double[] spark = new Double[7];
        public double change;
    }

    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    // Item
    private String sup, sub;
    private double mean, median, mode, exalted;
    private int count, quantity;

    // Sup
    private String child, name, type, supKey;
    private int frame;

    // Sub
    private Integer tier, lvl, quality, corrupted, links;
    private String var, subKey, icon;


    private HistoryData history = new HistoryData();
    private transient int historyCounter = 7;

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

        child = result.getString("child");
        name = result.getString("name");
        type = result.getString("type");
        supKey = result.getString("supKey");
        frame = result.getInt("frame");

        var = result.getString("var");
        subKey = result.getString("subKey");
        icon = result.getString("icon");

        try {
            tier = Integer.parseUnsignedInt(result.getString("tier"));
        } catch (NumberFormatException ex) {}

        try {
            lvl = Integer.parseUnsignedInt(result.getString("lvl"));
        } catch (NumberFormatException ex) {}

        try {
            quality = Integer.parseUnsignedInt(result.getString("quality"));
        } catch (NumberFormatException ex) {}

        try {
            corrupted = Integer.parseUnsignedInt(result.getString("corrupted"));
        } catch (NumberFormatException ex) {}

        try {
            links = Integer.parseUnsignedInt(result.getString("links"));
        } catch (NumberFormatException ex) {}
    }

    public void loadHistory(ResultSet result) throws SQLException {
        if (--historyCounter < 0) return;

        history.mean[historyCounter] = result.getDouble("mean");
        history.median[historyCounter] = result.getDouble("median");
        history.mode[historyCounter] = result.getDouble("mode");
        history.exalted[historyCounter] = result.getDouble("exalted");

        history.count[historyCounter] = result.getInt("count");
        history.quantity[historyCounter] = result.getInt("quantity");
    }

    public void calcSpark() {
        double lowestSpark = 0;
        double highestSpark = 0;
        double firstSpark = 0;
        double lastSpark = 0;

        for (Double mean : history.mean) {
            if (mean != null) {
                if (lowestSpark == 0) lowestSpark = mean;
                else if (mean < lowestSpark) lowestSpark = mean;

                if (mean > highestSpark) highestSpark = mean;
            }
        }

        for (int i = 7; --i > 0;) {
            Double newSpark = null;

            if (history.mean[i] != null && lowestSpark > 0) {
                newSpark = history.mean[i] / lowestSpark - 1;
                newSpark = Math.round(newSpark * 10000.0) / 100.0;
            }

            history.spark[i] = newSpark;
        }

        for (int i = 7; --i > 0;) {
            if (history.spark[i] != null) {
                firstSpark = history.spark[i];
            }
        }

        for (int i = 0; i < 7; i++) {
            if (history.spark[i] != null) {
                lastSpark = history.spark[i];
            }
        }

        if (firstSpark == 0 || lastSpark == 0) {
            history.change = 0;
        } else {
            history.change = (1 - firstSpark / lastSpark) * 100;
            history.change = Math.round(history.change * 1000.0) / 1000.0;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getIndex() {
        return sup + sub;
    }
}
