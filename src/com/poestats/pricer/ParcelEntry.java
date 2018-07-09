package com.poestats.pricer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ParcelEntry {
    public class HistoryData {
        public transient Double[] mean = new Double[7];
        public Double[] spark = new Double[7];
        public double change;
    }

    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    // Item
    private int id;
    private double mean, exalted;
    private int quantity;

    // Sup
    private String child, name, type;
    private int frame;

    // Sub
    private Integer tier, lvl, quality, corrupted, links;
    private String var, icon;

    private HistoryData history = new HistoryData();
    private transient int historyCounter = 6;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    public void loadItem(ResultSet result) throws SQLException {
        id = result.getInt("id_d");

        mean = result.getDouble("mean");
        exalted = result.getDouble("exalted");
        quantity = result.getInt("quantity");

        child = result.getString("ccName");
        name = result.getString("name");
        type = result.getString("type");
        frame = result.getInt("frame");

        var = result.getString("var");
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
    }

    public void calcSpark() {
        history.mean[6] = mean;

        double lowestSpark = 0;
        double highestSpark = 0;

        // Find lowest and highest mean price
        for (Double mean : history.mean) {
            if (mean != null) {
                if (lowestSpark == 0 || mean < lowestSpark) {
                    lowestSpark = mean;
                }

                if (mean > highestSpark) {
                    highestSpark = mean;
                }
            }
        }

        // Calculate sparkline percentage based on the highest and lowest prices
        for (int i = 7; --i > 0;) {
            Double newSpark = null;

            if (history.mean[i] != null && lowestSpark > 0) {
                newSpark = history.mean[i] / lowestSpark - 1;
                newSpark = Math.round(newSpark * 10000.0) / 100.0;
            }

            history.spark[i] = newSpark;
        }

        double firstMean = 0;
        double lastMean = 0;

        // Find the first price that is not null
        for (int i = 7; --i > 0;) {
            if (history.mean[i] != null) {
                firstMean = history.mean[i];
            }
        }

        // Find the last price that is not null
        for (int i = 0; i < 7; i++) {
            if (history.mean[i] != null) {
                lastMean = history.mean[i];
            }
        }

        // Find % difference between first price and last price
        history.change = (1 - firstMean / lastMean) * 100;
        history.change = Math.round(history.change * 1000.0) / 1000.0;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------


    public int getId() {
        return id;
    }
}
