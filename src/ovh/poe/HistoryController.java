package ovh.poe;

import com.google.gson.Gson;
import ovh.poe.Pricer.Entry;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryController {
    // Index map. Has mappings of: [index - HistoryItem]
    private static class IndexMap extends HashMap<String, HistoryItem> { }

    private static class HistoryItem {
        private ArrayList<Double> mean;
        private ArrayList<Double> median;
        private ArrayList<Double> mode;
        private ArrayList<Integer> quantity;
        private ArrayList<Integer> count;
    }

    private File inputFile;
    private File outputFile;
    private IndexMap indexMap;
    private Gson gson = Main.getGson();
    private String league, category;
    private static final int daysInALeague = 3 * 30;
    private int daysSinceLeagueStart;

    public void configure(String league, String category) {
        this.category = category;
        this.league = league;

        inputFile = new File("./data/history/"+league+"/"+category+".json");
        outputFile = new File("./data/history/"+league+"/"+category+".tmp");

        daysSinceLeagueStart = getDaysSinceLeagueStart();
    }

    private int getDaysSinceLeagueStart() {
        List<RelationManager.LeagueDurationElement> durationElements = Main.RELATIONS.getLeagueDurationMap();
        if (durationElements == null) {
            return -1;
        } else {
            for (RelationManager.LeagueDurationElement durationElement : durationElements) {
                if (durationElement.name.equals(league)) return durationElement.daysElapsed;
            }
        }

        return -1;
    }

    public void readFile() {
        if (inputFile == null) {
            Main.ADMIN.log_("No variables defined for HistoryController ('"+league+"','"+category+"')", 3);
            return;
        }

        indexMap = new IndexMap();

        // Open up the reader
        try (Reader reader = Misc.defineReader(inputFile)) {
            if (reader == null) throw new IOException();
            indexMap = gson.fromJson(reader, IndexMap.class);
        } catch (IOException ex) {
            Main.ADMIN.log_("Couldn't load '"+inputFile.getName()+"' ('"+league+"')", 3);
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

        if (historyItem.mean == null) historyItem.mean = new ArrayList<>();
        if (historyItem.median == null) historyItem.median = new ArrayList<>();
        if (historyItem.mode == null) historyItem.mode = new ArrayList<>();
        if (historyItem.quantity == null) historyItem.quantity = new ArrayList<>();
        if (historyItem.count == null) historyItem.count = new ArrayList<>();

        if (historyItem.mean.size() < daysSinceLeagueStart) {
            for (int i = 0; i < daysSinceLeagueStart; i++) {
                historyItem.mean.add(0.0);
                historyItem.median.add(0.0);
                historyItem.mode.add(0.0);
                historyItem.quantity.add(0);
                historyItem.count.add(0);
            }
        }

        historyItem.mean.add(dailyEntry.getMean());
        historyItem.median.add(dailyEntry.getMedian());
        historyItem.mode.add(dailyEntry.getMode());
        historyItem.quantity.add(dailyEntry.getQuantity());
        historyItem.count.add(entry.getCount());

        cap(historyItem);

        indexMap.putIfAbsent(index, historyItem);
    }

    private void cap(HistoryItem historyItem) {
        if (!league.equals("Standard") && !league.equals("Hardcore")) return;

        if (historyItem.mean.size() > daysInALeague) {
            historyItem.mean.subList(0, historyItem.mean.size() - daysInALeague).clear();
        }
        if (historyItem.median.size() > daysInALeague) {
            historyItem.median.subList(0, historyItem.median.size() - daysInALeague).clear();
        }
        if (historyItem.mode.size() > daysInALeague) {
            historyItem.mode.subList(0, historyItem.mode.size() - daysInALeague).clear();
        }
        if (historyItem.quantity.size() > daysInALeague) {
            historyItem.quantity.subList(0, historyItem.quantity.size() - daysInALeague).clear();
        }
        if (historyItem.count.size() > daysInALeague) {
            historyItem.count.subList(0, historyItem.count.size() - daysInALeague).clear();
        }
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
        indexMap = null;
        league =  null;
        category =  null;
    }
}
