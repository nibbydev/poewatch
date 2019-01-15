package poe.Logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;

import java.util.*;

public class PriceCalculator {
    private static final Logger logger = LoggerFactory.getLogger(PriceCalculator.class);
    private static Database database;

    private static final double zScoreUpper = 2.0;
    private static final double zScoreLower = 2.0;
    private static final int trimUpper = 5;
    private static final int trimLower = 5;

    public static void run() {
        Map<Integer, Map<Integer, List<Double>>> entries = new HashMap<>();
        Map<Integer, Map<Integer, Result>> results = new HashMap<>();

        // Get entries from database
        if (!database.calc.getEntries(entries)) {
            return;
        }

        // Calculate mean, median, mode, etc
        if (!calculate(entries, results)) {
            return;
        }

        // Update entries in database
        database.upload.updateItems(results);
    }

    private static boolean calculate(Map<Integer, Map<Integer, List<Double>>> entries, Map<Integer, Map<Integer, Result>> results) {
        // If entry map is invalid
        if (entries == null || entries.isEmpty()) {
            return false;
        }

        // If result map is invalid
        if (results == null || !results.isEmpty()) {
            return false;
        }

        // Loop though leagues
        for (int id_l : entries.keySet()) {
            Map<Integer, List<Double>> entryMap = entries.get(id_l);
            Map<Integer, Result> resultMap = new HashMap<>();

            // If league map is invalid
            if (entryMap == null || entryMap.isEmpty()) {
                return false;
            }

            // Loop through items
            for (int id_d : entryMap.keySet()) {
                List<Double> entryList = entryMap.get(id_d);

                // Sort by price and trim the list to remove outliers
                entryList.sort(Double::compareTo);
                List<Double> tmpTrimmedList = entryList.subList(entryList.size() * trimLower / 100, entryList.size() - entryList.size() * trimUpper / 100);
                double tmpMean = calcMean(tmpTrimmedList);
                double tmpStdDev = calcStdDev(tmpTrimmedList, tmpMean);
                double lowerBound = tmpMean - zScoreLower * tmpStdDev;
                double upperBound = tmpMean + zScoreUpper * tmpStdDev;

                // Remove all entries that don't fall within the range
                entryList.removeIf(i -> i < lowerBound || i > upperBound);

                Result result = new Result() {{
                    mean = calcMean(entryList);
                    median = calcMedian(entryList);
                    mode = calcMode(entryList);
                    min = calcMin(entryList);
                    max = calcMax(entryList);
                }};

                resultMap.put(id_d, result);
            }

            results.put(id_l, resultMap);
        }

        return true;
    }

    private static double calcMean(List<Double> list) {
        double sum = 0;

        for (Double entry : list) {
            sum += entry;
        }

        return sum / list.size();
    }

    /**
     * Finds the standard deviation of a sample
     *
     * @param list Non-null non-empty list of entries
     * @param mean Mean of the list
     * @return Standard deviation of the sample
     */
    private static double calcStdDev(List<Double> list, double mean) {
        double sum = 0;

        for (Double entry : list) {
            sum += Math.pow((entry - mean), 2);
        }

        return Math.sqrt(sum / (list.size() - 1));
    }

    private static double calcMedian(List<Double> list) {
        list.sort(Double::compareTo);
        return list.get((list.size() - 1) / 2);
    }

    private static double calcMin(List<Double> list) {
        double min = list.get(0);

        for (Double entry : list) {
            if (entry < min) {
                min = entry;
            }
        }

        return min;
    }

    private static double calcMax(List<Double> list) {
        double max = list.get(0);

        for (Double entry : list) {
            if (entry > max) {
                max = entry;
            }
        }

        return max;
    }

    private static double calcMode(List<Double> list) {
        HashMap<Double, Integer> map = new HashMap<>();
        double max = 1, tmp = 0;

        for (Double entry : list) {
            if (map.get(entry) != null) {
                int count = map.get(entry);
                map.put(entry, ++count);

                if (count > max) {
                    max = count;
                    tmp = entry;
                }
            } else {
                map.put(entry, 1);
            }
        }

        return tmp;
    }

    public static void setDatabase(Database database) {
        PriceCalculator.database = database;
    }

    public static class Result {
        public double mean, median, mode, min, max;
    }
}
