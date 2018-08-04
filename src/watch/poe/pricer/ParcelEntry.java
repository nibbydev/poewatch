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
    private Integer tier, lvl, quality, corrupted, links;
    private String var, icon;
    private HistoryData history = new HistoryData();

    /**
     * Parses all data on initialization
     *
     * @param resultSet ResultSet of output data query
     * @throws SQLException
     */
    public ParcelEntry(ResultSet resultSet) throws SQLException {
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

        // Parse history presented as CSV
        calcSpark(resultSet.getString("history"));
    }

    /**
     * Converts provided values to a format readable by the JS sparkline plugin
     *
     * @param historyEntries Doubles as CSV
     */
    private void calcSpark(String historyEntries) {
        Double[] historyMean = new Double[7];
        int historyCounter = 6;

        // Set latest/newest price to current price
        historyMean[6] = mean;

        // Parse history presented as CSV
        if (historyEntries != null) {
            for (String historyEntry : historyEntries.split(",")) {
                if (--historyCounter < 0) break;

                try {
                    historyMean[historyCounter] = Double.parseDouble(historyEntry);
                } catch (NumberFormatException ex) { }
            }
        }

        // Find the first price from the left that is not null
        Double firstPrice = null;
        for (Double mean : historyMean) {
            if (mean != null) {
                firstPrice = mean;
                break;
            }
        }

        if (firstPrice != null) {
            // Find each value's percentage in relation to the first price
            for (int i = 0; i < 7; i++) {
                if (historyMean[i] != null && historyMean[i] > 0) {
                    historyMean[i] = (historyMean[i] / firstPrice - 1) * 100.0;
                    historyMean[i] = Math.round(historyMean[i] * 100.0) / 100.0;
                }
            }

            // Copy values over
            System.arraycopy(historyMean, 0, history.spark, 0, 7);

            // Find % difference between first price and last price
            history.change = (1 - firstPrice / mean) * 1000.0;
            history.change = Math.round(history.change * 100.0) / 1000.0;
        }
    }

    /**
     * Inner class for binding variable history to an associative array in output
     */
    private class HistoryData {
        public Double[] spark = new Double[7];
        public double change;
    }
}
