package watch.poe.pricer;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class ParcelEntry {
    private int id;
    private double mean, exalted;
    private int quantity;
    private String child, name, type;
    private int frame;
    private Integer tier, lvl, quality, corrupted, links, ilvl;
    private String var, icon;
    private HistoryData history = new HistoryData();

    public int id_l, id_cp;

    /**
     * Parses all data on initialization
     *
     * @param resultSet ResultSet of output data query
     * @throws SQLException
     */
    public ParcelEntry(ResultSet resultSet) throws SQLException {
        id_l = resultSet.getInt("id_l");
        id_cp = resultSet.getInt("id_cp");

        // Get data that is never null
        id = resultSet.getInt("id_d");
        mean = resultSet.getDouble("mean");
        exalted = resultSet.getDouble("exalted");
        quantity = resultSet.getInt("quantity");
        child = resultSet.getString("ccName");
        name = resultSet.getString("name");
        type = resultSet.getString("type");
        frame = resultSet.getInt("frame");
        var = resultSet.getString("var");
        icon = resultSet.getString("icon");

        exalted = Math.round(exalted * 1000.0) / 1000.0;

        // Get data that might be null
        try {
            tier = Integer.parseUnsignedInt(resultSet.getString("tier"));
        } catch (NumberFormatException ex) {
        }
        try {
            lvl = Integer.parseUnsignedInt(resultSet.getString("lvl"));
        } catch (NumberFormatException ex) {
        }
        try {
            quality = Integer.parseUnsignedInt(resultSet.getString("quality"));
        } catch (NumberFormatException ex) {
        }
        try {
            corrupted = Integer.parseUnsignedInt(resultSet.getString("corrupted"));
        } catch (NumberFormatException ex) {
        }
        try {
            links = Integer.parseUnsignedInt(resultSet.getString("links"));
        } catch (NumberFormatException ex) {
        }
        try {
            ilvl = Integer.parseUnsignedInt(resultSet.getString("ilvl"));
        } catch (NumberFormatException ex) {
        }

        // Parse history presented as CSV
        calcSpark(resultSet.getString("history"));
    }

    /**
     * Converts provided values to a format readable by the JS sparkline plugin
     *
     * @param historyEntries Doubles as CSV
     */
    private void calcSpark(String historyEntries) {
        int historyCounter = 6;

        // Set latest/newest price to current price
        history.values[6] = mean;

        // Parse history presented as CSV
        if (historyEntries != null) {
            String[] splitEntries = historyEntries.split(",");

            for (String historyEntry : splitEntries) {
                if (--historyCounter < 0) break;

                try {
                    history.values[historyCounter] = Double.parseDouble(historyEntry);
                } catch (NumberFormatException ex) { }
            }
        }

        // Find the first and last spark percentages that are not null
        Double firstVal = null, lastVal = null;
        for (Double val : history.values) {
            if (val != null) {
                if (firstVal == null) firstVal = val;
                lastVal = val;
            }
        }

        // Find % difference between first price and last price
        if (firstVal != null) {
            history.change =  Math.round((lastVal / firstVal * 100.0 - 100) * 100.0) / 100.0;
        }
    }

    /**
     * Inner class for binding variable history to an associative array in output
     */
    private class HistoryData {
        public Double[] values = new Double[7];
        public double change;
    }
}
