package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;

import java.util.*;

public class PriceManager {
    private static final Logger logger = LoggerFactory.getLogger(PriceManager.class);
    private static Database database;

    private static final double zScoreLower = 2.0;
    private static final double zScoreUpper = 1.0;
    private static final int trimLower = 5;
    private static final int trimUpper = 5;

    public static void run() {
        Map<Integer, Map<Integer, List<Double>>> entries = new HashMap<>();
        Map<Integer, Map<Integer, Result>> results = new HashMap<>();

        logger.info("Calculating prices");

        // Get entries from database
        if (!database.calc.getEntries(entries)) {
            logger.warn("Could not get entries for price calculation");
            return;
        }

        // Calculate mean, median, mode, etc
        if (!calculate(entries, results)) {
            logger.warn("Could not calculate prices");
            return;
        }

        // Update entries in database
        database.upload.updateItems(results);

        logger.info("Prices calculated");
    }

    private static boolean calculate(Map<Integer, Map<Integer, List<Double>>> entries, Map<Integer, Map<Integer, Result>> results) {
        // If entry map is invalid
        if (entries == null || entries.isEmpty()) {
            logger.error("Null/empty reference passed");
            return false;
        }

        // If result map is invalid
        if (results == null || !results.isEmpty()) {
            logger.error("Null/empty reference passed");
            return false;
        }

        // Loop though leagues
        for (int id_l : entries.keySet()) {
            Map<Integer, List<Double>> entryMap = entries.get(id_l);
            Map<Integer, Result> resultMap = new HashMap<>();

            // If league map is invalid
            if (entryMap == null || entryMap.isEmpty()) {
                logger.error("Null/empty entryMap passed");
                return false;
            }

            // Loop through items
            for (int id_d : entryMap.keySet()) {
                List<Double> entryList = entryMap.get(id_d);

                // If league entry list is invalid
                if (entryList == null || entryList.isEmpty()) {
                    logger.error("Null/empty entryList passed");
                    return false;
                }

                // Sort by price ascending
                entryList.sort(Double::compareTo);

                // Trim the list to remove outliers
                int currentCount = entryList.size();
                int lowerTrimBound = currentCount * trimLower / 100;
                int upperTrimBound = currentCount * (100 - trimUpper) / 100;
                List<Double> tmpTrimmedList = entryList.subList(lowerTrimBound, upperTrimBound);

                // If trimming resulted in an empty list, use the original
                if (tmpTrimmedList.isEmpty()) {
                    tmpTrimmedList = entryList;
                }

                // Find standard deviation and bounds
                double tmpMean = calcMean(tmpTrimmedList);
                double tmpStdDev = calcStdDev(tmpTrimmedList, tmpMean);
                double lowerPredicateBound = tmpMean - zScoreLower * tmpStdDev;
                double upperPredicateBound = tmpMean + zScoreUpper * tmpStdDev;

                // Remove all entries that don't fall within the bounds
                entryList.removeIf(i -> i < lowerPredicateBound || i > upperPredicateBound);

                // If no entries were left, skip the item
                if (entryList.isEmpty()) {
                    continue;
                }

                Result result = new Result() {{
                    mean = calcMean(entryList);
                    median = calcMedian(entryList);
                    mode = calcMode(entryList);
                    min = calcMin(entryList);
                    max = calcMax(entryList);
                    current = currentCount;
                }};

                resultMap.put(id_d, result);
            }

            results.put(id_l, resultMap);
        }

        return true;
    }

    private static double calcMean(List<Double> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to mean");
            return 0;
        }

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
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to standard deviation");
            return 0;
        }

        double sum = 0;

        for (Double entry : list) {
            sum += Math.pow((entry - mean), 2);
        }

        return Math.sqrt(sum / (list.size() - 1));
    }

    private static double calcMedian(List<Double> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to median");
            return 0;
        }

        list.sort(Double::compareTo);
        return list.get((list.size() - 1) / 2);
    }

    private static double calcMin(List<Double> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to min");
            return 0;
        }

        double min = list.get(0);

        for (Double entry : list) {
            if (entry < min) {
                min = entry;
            }
        }

        return min;
    }

    private static double calcMax(List<Double> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to max");
            return 0;
        }

        double max = list.get(0);

        for (Double entry : list) {
            if (entry > max) {
                max = entry;
            }
        }

        return max;
    }

    private static double calcMode(List<Double> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to mode");
            return 0;
        }

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
        PriceManager.database = database;
    }

    public static class Result {
        public double mean, median, mode, min, max;
        public int current;
    }
}
