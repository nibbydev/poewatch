package poe.Managers.Price;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;
import poe.Managers.Price.Bundles.ResultBundle;

import java.util.*;

public class Calculator {
    private static final Logger logger = LoggerFactory.getLogger(Calculator.class);
    private final Config config;

    private double zScoreLower, zScoreUpper, MADModifier;
    private int minStDevCycles, maxStDevCycles;

    public Calculator(Config cf) {
        this.config = cf;

        this.zScoreLower = config.getInt("calculation.zScoreLower");
        this.zScoreUpper = config.getInt("calculation.zScoreUpper");
        this.MADModifier = config.getInt("calculation.MADModifier");

        this.minStDevCycles = config.getInt("calculation.minStDevCycles");
        this.maxStDevCycles = config.getInt("calculation.maxStDevCycles");
    }

    /**
     * Converts entry prices to chaos
     *
     * @param ib Valid ID bundle
     * @param eb Entries to convert
     * @param pb Prices to source from
     */
    public List<Double> convertToChaos(IdBundle ib, List<EntryBundle> eb, List<PriceBundle> pb) {
        List<Double> buffer = new ArrayList<>();

        for (EntryBundle entryBundle : eb) {
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
            if (priceBundle != null) {
                buffer.add(entryBundle.getPrice() * priceBundle.getMean());
            }
        }

        return buffer;
    }

    /**
     * Some accounts love to list 120 separate transmutation orbs for 10 chaos each.
     * This method limits the number of allowed entries for all distinct accounts
     *
     * @param entryLimitPerAccount Max number of entries to keep per account
     * @param eb                   Entries to filter
     */
    public void limitDuplicateEntries(List<EntryBundle> eb, int entryLimitPerAccount) {
        Map<Long, Integer> accountCount = new HashMap<>();
        Iterator<EntryBundle> iterator = eb.iterator();

        while (iterator.hasNext()) {
            EntryBundle bundle = iterator.next();

            // Get matches so far for account
            int currentCount = accountCount.getOrDefault(bundle.getAccountId(), 0);
            // Increment and store new value
            accountCount.put(bundle.getAccountId(), ++currentCount);

            // Remove entry if account exceeded limit
            if (currentCount > entryLimitPerAccount) {
                iterator.remove();
            }
        }
    }

    /**
     * Finds a price for the provided id bundle
     *
     * @param ib     Valid ID bundle
     * @param prices Item prices
     * @return The calculated result
     */
    public ResultBundle calculateResult(IdBundle ib, List<Double> prices) {
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
    public List<Double> filterEntries(List<Double> eb) {
        for (int i = 0; i < maxStDevCycles; i++) {
            if (eb.isEmpty()) {
                break;
            }

            // Find averages
            final double mad = calcMAD(eb);
            final double mean = calcMean(eb);

            // If we've done the minimum number of iterations AND the mean of the set falls around the MAD then stop
            // trimming
            if (i >= minStDevCycles && mean < mad * MADModifier && mean > mad / MADModifier) {
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
    public List<Double> hardTrim(List<Double> eb, int lower, int upper) {
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
    public void stdDevTrim(List<Double> eb) {
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

    private static double calcMean(List<Double> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        } else if (list.size() == 1) {
            return list.get(0);
        }

        double sum = 0;

        for (Double entry : list) {
            sum += entry;
        }

        return sum / list.size();
    }

    private static double calcStdDev(List<Double> list, double mean) {
        if (list == null || list.isEmpty()) {
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
            return 0;
        }

        list.sort(Double::compareTo);
        return list.get((list.size() - 1) / 2);
    }

    private static double calcMin(List<Double> list) {
        return calcMinMax(list, false);
    }

    private static double calcMax(List<Double> list) {
        return calcMinMax(list, true);
    }

    private static double calcMinMax(List<Double> list, boolean calcMax) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        double val = list.get(0);

        for (Double entry : list) {
            if (calcMax) {
                if (entry > val) val = entry;
            } else if (entry < val) val = entry;
        }

        return val;
    }

    private static double calcMode(List<Double> list) {
        if (list == null || list.isEmpty()) {
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
