package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PriceManager {
    private static final Logger logger = LoggerFactory.getLogger(PriceManager.class);
    private static Database database;

    private static final double zScoreLower = 2.5;
    private static final double zScoreUpper = 0.5;
    private static final int trimLower = 5;
    private static final int trimUpper = 50;

    public static void run() {
        List<Result> results = new ArrayList<>();
        logger.info("Calculating prices");

        // Get entries from database
        ResultSet resultSet = database.calc.getEntryStream();
        if (resultSet == null) {
            logger.warn("Could not get entries for price calculation");
            throw new RuntimeException();
        }

        if (!streamEntries(resultSet, results)) {
            logger.warn("Could not calculate prices");
            return;
        }

        // Close the connection
        try {
            resultSet.close();
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            logger.error("Could not close ResultSet");
        }

        // Update entries in database
        database.upload.updateItems(results);
        logger.info("Prices calculated");
    }

    private static boolean streamEntries(ResultSet resultSet, List<Result> results) {
        // If result map is invalid
        if (resultSet == null || results == null || !results.isEmpty()) {
            logger.error("Null/non-empty reference passed");
            return false;
        }

        try {
            // Get first entry
            if (!resultSet.next()) {
                logger.warn("No entries found for price calculation");
                return false;
            }

            int id_l = resultSet.getInt("id_l");
            int id_d = resultSet.getInt("id_d");
            List<Double> entryList = new ArrayList<>();

            do {
                // If league or item changed
                if (id_l != resultSet.getInt("id_l") || id_d != resultSet.getInt("id_d")) {
                    calculate(results, entryList, id_l, id_d);
                    entryList = new ArrayList<>();

                    id_l = resultSet.getInt("id_l");
                    id_d = resultSet.getInt("id_d");
                }

                entryList.add(resultSet.getDouble("price"));
            } while (resultSet.next());

            // Add last values
            calculate(results, entryList, id_l, id_d);
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }

        return true;
    }

    private static List<Double> filterEntries(List<Double> entryList) {
        // Sort by price ascending
        entryList.sort(Double::compareTo);

        // Trim the list to remove outliers
        int lowerTrimBound = entryList.size() * trimLower / 100;
        int upperTrimBound = entryList.size() * (100 - trimUpper) / 100;
        List<Double> tmpTrimmedList = entryList.subList(lowerTrimBound, upperTrimBound);

        if (tmpTrimmedList.size() < 5) {
            return tmpTrimmedList;
        }

        // Find standard deviation and bounds
        double tmpMean = calcMean(tmpTrimmedList);
        double tmpStdDev = calcStdDev(tmpTrimmedList, tmpMean);
        double lowerPredicateBound = tmpMean - zScoreLower * tmpStdDev;
        double upperPredicateBound = tmpMean + zScoreUpper * tmpStdDev;

        List<Double> tmp = tmpTrimmedList.stream()
                .filter(i -> i > lowerPredicateBound && i < upperPredicateBound)
                .collect(Collectors.toList());

        return tmp.size() > 5 ? tmp : tmpTrimmedList;
    }

    private static void calculate(List<Result> results, List<Double> entryList, int id_l, int id_d) {
        // If result list is invalid
        if (results == null) {
            logger.error("Null result list passed");
            throw new RuntimeException();
        }

        // If entry list is invalid
        if (entryList == null || entryList.isEmpty()) {
            logger.error("Null/empty entryList passed");
            throw new RuntimeException();
        }


        List<Double> entries;
        if (entryList.size() > 4) {
            entries = filterEntries(entryList);
        } else {
            entries = entryList;
        }

        // If no entries were left, skip the item
        if (entries.isEmpty()) {
            logger.warn(String.format("Zero remaining entries for %d in %d", id_d, id_l));
            return;
        }

        Result result = new Result(id_l, id_d);
        result.mean = calcMean(entries);
        result.median = calcMedian(entries);
        result.mode = calcMode(entries);
        result.min = calcMin(entries);
        result.max = calcMax(entries);
        result.accepted = entries.size();

        if (result.mode == 0) {
            result.mode = result.median;
        }

        results.add(result);
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
        public int id_l, id_d;
        public double mean, median, mode, min, max;
        public int accepted;

        public Result(int id_l, int id_d) {
            this.id_l = id_l;
            this.id_d = id_d;
        }
    }
}
