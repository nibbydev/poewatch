package ovh.poe;

import com.google.gson.Gson;
import ovh.poe.Pricer.Entry;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

public class HistoryController {
    // Index map. Has mappings of: [index - HistoryItem]
    private static class IndexMap extends HashMap<String, HistoryItem> { }

    private static class HistoryItem {
        private double[] mean;
        private double[] median;
        private double[] mode;
        private int[] quantity;
        private int[] count;
    }

    private IndexMap indexMap;
    private File inputFile;
    private File outputFile;
    private Gson gson = Main.getGson();
    private String league, category;
    private int currentLeagueDay;
    private int totalLeagueLength;
    private int daysInStandard = 90;

    public void configure(String league, String category) {
        this.category = category;
        this.league = league;

        inputFile = new File("./data/history/"+league+"/"+category+".json");
        outputFile = new File("./data/history/"+league+"/"+category+".tmp");

        getLeagueLengths();

        indexMap = null;
    }

    private void getLeagueLengths() {
        List<RelationManager.LeagueLengthElement> lengthElements = Main.RELATIONS.getLeagueLengthMap();
        if (lengthElements != null) {
            for (RelationManager.LeagueLengthElement lengthElement : lengthElements) {
                if (lengthElement.id.equals(league)) {
                    currentLeagueDay = lengthElement.elapse;
                    totalLeagueLength = lengthElement.total;
                    return;
                }
            }
        }

        currentLeagueDay = -1;
        totalLeagueLength = -1;
    }

    public void readFile() {
        if (inputFile == null) {
            Main.ADMIN.log_("No variables defined for HistoryController ('"+league+"','"+category+"')", 3);
            return;
        }

        // Open up the reader
        try (Reader reader = Misc.defineReader(inputFile)) {
            if (reader == null) throw new IOException();
            indexMap = gson.fromJson(reader, IndexMap.class);
        } catch (IOException ex) {
            Main.ADMIN.log_("Couldn't load '"+inputFile.getName()+"' ('"+league+"')", 3);
            indexMap = new IndexMap();
        }
    }

    public void add(String index, Entry entry) {
        if (indexMap == null) {
            return;
        } else if (entry.getDb_daily().isEmpty()) {
            return;
        }

        HistoryItem historyItem = indexMap.getOrDefault(index, new HistoryItem());
        Entry.DailyEntry dailyEntry = entry.getDb_daily().get(entry.getDb_daily().size() - 1);

        if (league.equals("Standard") || league.equals("Hardcore")) {
            if (historyItem.mean == null) {
                historyItem.mean        = new double[daysInStandard];
                historyItem.median      = new double[daysInStandard];
                historyItem.mode        = new double[daysInStandard];
                historyItem.quantity    = new int[daysInStandard];
                historyItem.count       = new int[daysInStandard];
            }

            System.arraycopy(historyItem.mean,      1, historyItem.mean,        0, historyItem.mean.length - 1);
            System.arraycopy(historyItem.median,    1, historyItem.median,      0, historyItem.mean.length - 1);
            System.arraycopy(historyItem.mode,      1, historyItem.mode,        0, historyItem.mean.length - 1);
            System.arraycopy(historyItem.quantity,  1, historyItem.quantity,    0, historyItem.mean.length - 1);
            System.arraycopy(historyItem.count,     1, historyItem.count,       0, historyItem.mean.length - 1);

            int lastIndex = daysInStandard - 1;

            historyItem.mean[lastIndex]      = dailyEntry.getMean();
            historyItem.median[lastIndex]    = dailyEntry.getMedian();
            historyItem.mode[lastIndex]      = dailyEntry.getMode();
            historyItem.quantity[lastIndex]  = dailyEntry.getQuantity();
            historyItem.count[lastIndex]     = entry.getCount();
        } else {
            if (historyItem.mean == null) {
                historyItem.mean        = new double[totalLeagueLength];
                historyItem.median      = new double[totalLeagueLength];
                historyItem.mode        = new double[totalLeagueLength];
                historyItem.quantity    = new int[totalLeagueLength];
                historyItem.count       = new int[totalLeagueLength];
            }

            historyItem.mean[currentLeagueDay]      = dailyEntry.getMean();
            historyItem.median[currentLeagueDay]    = dailyEntry.getMedian();
            historyItem.mode[currentLeagueDay]      = dailyEntry.getMode();
            historyItem.quantity[currentLeagueDay]  = dailyEntry.getQuantity();
            historyItem.count[currentLeagueDay]     = entry.getCount();
        }

        indexMap.putIfAbsent(index, historyItem);
    }

    public void writeFile() {
        if (outputFile == null) {
            Main.ADMIN.log_("No variables defined for HistoryController ('"+league+"','"+category+"')", 3);
            return;
        } else if (indexMap == null) {
            Main.ADMIN.log_("No indexMap defined for HistoryController ('"+league+"','"+category+"')", 3);
            return;
        }

        new File("./data/history/"+league+"/").mkdirs();

        try (Writer writer = Misc.defineWriter(outputFile)) {
            if (writer == null) throw new IOException();
            gson.toJson(indexMap, writer);
        } catch (IOException ex) {
            Main.ADMIN.log_("Could not write to '"+outputFile.getName()+"' ('"+league+"')", 3);
            Main.ADMIN._log(ex, 3);
        }

        // Remove original file
        if (inputFile.exists() && !inputFile.delete()) {
            String errorMsg = "Unable to remove '"+league+"/"+category+"/"+inputFile.getName()+"'";
            Main.ADMIN.log_(errorMsg, 4);
        }
        // Rename temp file to original file
        if (outputFile.exists() && !outputFile.renameTo(inputFile)) {
            String errorMsg = "Unable to rename '"+league+"/"+category+"/"+outputFile.getName()+"'";
            Main.ADMIN.log_(errorMsg, 4);
        }

        // Clean up
        inputFile =  null;
        outputFile = null;
        indexMap.clear();
        league =  null;
        category =  null;
    }
}
