package poe.Managers.Price;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;
import poe.Managers.Price.Bundles.ResultBundle;

import java.util.*;

public class Calculation {
    private static final Logger logger = LoggerFactory.getLogger(Calculation.class);
    private static final double zScoreLower = 2.5;
    private static final double zScoreUpper = 0.5;
    private static final int trimLower = 5;
    private static final int trimUpper = 80;

    /**
     * Converts entry prices to chaos
     *
     * @param ib Valid ID bundle
     * @param eb Entries to convert
     * @param pb Prices to source from
     */
    public static List<Double> convertToChaos(IdBundle ib, List<EntryBundle> eb, List<PriceBundle> pb) {
        List<Double> buffer = new ArrayList<>(eb.size());

        Iterator<EntryBundle> entryBundleIterator = eb.iterator();
        while (entryBundleIterator.hasNext()) {
            EntryBundle entryBundle = entryBundleIterator.next();

            // Already in chaos
            if (entryBundle.getCurrencyId() == null) {
                buffer.add(entryBundle.getPrice());
                continue;
            }

            // Find matching price bundle
            PriceBundle priceBundle = pb.stream()
                    .filter(t -> t.getLeagueId() == ib.getLeagueId())
                    .filter(t -> entryBundle.getCurrencyId().equals(t.getItemId()))
                    .findFirst()
                    .orElse(null);

            // No match, remove entry
            if (priceBundle == null) {
                entryBundleIterator.remove();
                continue;
            }

            buffer.add(entryBundle.getPrice() * priceBundle.getMean());
        }

        return buffer;
    }

    /**
     * Finds a price for the provided id bundle
     *
     * @param ib     Valid ID bundle
     * @param prices Item prices
     * @return The calculated result
     */
    public static ResultBundle calculateResult(IdBundle ib, List<Double> prices) {
        filterEntries(prices);

        // If no entries were left, skip the item
        if (prices.isEmpty()) {
            logger.warn(String.format("Zero remaining entries for %d in %d", ib.getLeagueId(), ib.getItemId()));
            return null;
        }

        ResultBundle result = new ResultBundle(
                ib,
                calcMean(prices),
                calcMedian(prices),
                calcMode(prices),
                calcMin(prices),
                calcMax(prices),
                prices.size()
        );

        // A problem with the algorithm that i've not bothered to fix
        if (result.getMode() == 0) {
            result.setMode(result.getMedian());
        }

        return result;
    }

    /**
     * Filters out the ones that would negatively affect calculation
     *
     * @param eb List of item entries
     */
    static List<Double> filterEntries(List<Double> eb) {
        // Trim the list to remove potential outliers
        //eb = hardTrim(eb, trimLower, trimUpper);

        // Trim according to standard deviation
        for (int i = 0; i < 6; i++) {
            // Find averages
            final double mad = calcMAD(eb);
            final double mean = calcMean(eb);

            // If we've done at least two iterations AND the
            // mean of the set falls around the MAD then stop
            if (i > 2 && mean < 1.8f * mad && mean > mad / 1.8f) {
                break;
            }

            // Find standard deviation and bounds
            final double stdDev = calcStdDev(eb, mean);
            final double lowerPredicateBound = mean - zScoreLower * stdDev;
            final double upperPredicateBound = mean + zScoreUpper * stdDev;

            // Remove outliers according to standard deviation
            eb.removeIf(t -> t < lowerPredicateBound || t > upperPredicateBound);
        }

        return eb;
    }

    /**
     * Hard trims a list of entries
     *
     * @param eb    Sorted list of entries
     * @param lower Lower bound as percentage (0-100)
     * @param upper Upper bound as percentage (0-100)
     * @return Trimmed list
     */
    private static List<Double> hardTrim(List<Double> eb, int lower, int upper) {
        // Sort by price ascending
        eb.sort(Double::compareTo);

        // Find bounds
        final int lowerBound = eb.size() * lower / 100;
        final int upperBound = eb.size() * upper / 100;

        return eb.subList(lowerBound, upperBound);
    }

    /**
     * Trims entries according to standard deviation
     *
     * @param eb List of prices
     */
    static void stdDevTrim(List<Double> eb) {
        // Find standard deviation and bounds
        final double mean = calcMean(eb);
        final double stdDev = calcStdDev(eb, mean);
        final double lowerPredicateBound = mean - zScoreLower * stdDev;
        final double upperPredicateBound = mean + zScoreUpper * stdDev;

        // Remove outliers according to standard deviation
        eb.removeIf(t -> t < lowerPredicateBound || t > upperPredicateBound);
    }

    /**
     * Calculates the Median Absolute Deviation
     *
     * @param eb List of prices
     * @return Median Absolute Deviation
     */
    static double calcMAD(List<Double> eb) {
        List<Double> buffer = new ArrayList<>(eb.size());
        final double median = calcMedian(eb);

        for (Double e : eb) {
            buffer.add(Math.abs(e - median));
        }

        final double newMedian = calcMedian(buffer);
        return newMedian == 0 ? median : newMedian;
    }

    static double calcMean(List<Double> list) {
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

    static double calcStdDev(List<Double> list, double mean) {
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
}
