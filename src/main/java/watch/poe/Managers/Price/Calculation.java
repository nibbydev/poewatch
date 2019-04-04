package poe.Managers.Price;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;
import poe.Managers.Price.Bundles.ResultBundle;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Calculation {
    private static final Logger logger = LoggerFactory.getLogger(Calculation.class);
    private static final double zScoreLower = 2.5;
    private static final double zScoreUpper = 0.5;
    private static final int trimLower = 5;
    private static final int trimUpper = 50;

    public static void convertToChaos(IdBundle idBundle, List<EntryBundle> entryBundles, List<PriceBundle> priceBundles) {
        Iterator<EntryBundle> entryBundleIterator = entryBundles.iterator();
        while (entryBundleIterator.hasNext()) {
            EntryBundle entryBundle = entryBundleIterator.next();

            // Already in chaos
            if (entryBundle.getCurrencyId() == null) continue;

            // Find matching price bundle
            PriceBundle priceBundle = priceBundles.stream()
                    .filter(t -> t.getLeagueId() == idBundle.getLeagueId())
                    .filter(t -> entryBundle.getCurrencyId().equals(t.getItemId()))
                    .findFirst()
                    .orElse(null);

            // No match, remove entry
            if (priceBundle == null) {
                entryBundleIterator.remove();
                continue;
            }

            entryBundle.setCurrencyId(null);
            entryBundle.setPrice(entryBundle.getPrice() * priceBundle.getMean());
        }
    }

    public static ResultBundle calculateResult(IdBundle idBundle, List<EntryBundle> entryBundles) {
        entryBundles = filterEntries(entryBundles);

        // If no entries were left, skip the item
        if (entryBundles.isEmpty()) {
            logger.warn(String.format("Zero remaining entries for %d in %d", idBundle.getLeagueId(), idBundle.getItemId()));
            return null;
        }

        ResultBundle result = new ResultBundle(
                idBundle,
                calcMean(entryBundles),
                calcMedian(entryBundles),
                calcMode(entryBundles),
                calcMin(entryBundles),
                calcMax(entryBundles),
                entryBundles.size()
        );

        // A problem with the algorithm that i've not bothered to fix
        if (result.getMode() == 0) {
            result.setMode(result.getMedian());
        }

        return result;
    }

    private static List<EntryBundle> filterEntries(List<EntryBundle> entryBundles) {
        // Sort by price ascending
        entryBundles.sort(Comparator.comparingDouble(EntryBundle::getPrice));

        // Trim the list to remove outliers
        int lowerTrimBound = entryBundles.size() * trimLower / 100;
        int upperTrimBound = entryBundles.size() * (100 - trimUpper) / 100;
        List<EntryBundle> trimmedBundles = entryBundles.subList(lowerTrimBound, upperTrimBound);

        if (trimmedBundles.size() < 5) {
            return entryBundles;
        }

        // Find standard deviation and bounds
        double tmpMean = calcMean(trimmedBundles);
        double tmpStdDev = calcStdDev(trimmedBundles, tmpMean);
        double lowerPredicateBound = tmpMean - zScoreLower * tmpStdDev;
        double upperPredicateBound = tmpMean + zScoreUpper * tmpStdDev;

        List<EntryBundle> doubleTrimmedBundles = trimmedBundles.stream()
                .filter(t -> t.getPrice() > lowerPredicateBound)
                .filter(t -> t.getPrice() < upperPredicateBound)
                .collect(Collectors.toList());

        return doubleTrimmedBundles.size() > 5 ? doubleTrimmedBundles : trimmedBundles;
    }

    private static double calcMean(List<EntryBundle> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to mean");
            return 0;
        }

        double sum = 0;

        for (EntryBundle entry : list) {
            sum += entry.getPrice();
        }

        return sum / list.size();
    }

    private static double calcStdDev(List<EntryBundle> list, double mean) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to standard deviation");
            return 0;
        }

        double sum = 0;

        for (EntryBundle entry : list) {
            sum += Math.pow((entry.getPrice() - mean), 2);
        }

        return Math.sqrt(sum / (list.size() - 1));
    }

    private static double calcMedian(List<EntryBundle> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to median");
            return 0;
        }

        list.sort(Comparator.comparingDouble(EntryBundle::getPrice));
        return list.get((list.size() - 1) / 2).getPrice();
    }

    private static double calcMin(List<EntryBundle> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to min");
            return 0;
        }

        double min = list.get(0).getPrice();

        for (EntryBundle entry : list) {
            if (entry.getPrice() < min) {
                min = entry.getPrice();
            }
        }

        return min;
    }

    private static double calcMax(List<EntryBundle> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to max");
            return 0;
        }

        double max = list.get(0).getPrice();

        for (EntryBundle entry : list) {
            if (entry.getPrice() > max) {
                max = entry.getPrice();
            }
        }

        return max;
    }

    private static double calcMode(List<EntryBundle> list) {
        if (list == null || list.isEmpty()) {
            logger.warn("Null/empty list passed to mode");
            return 0;
        }

        HashMap<Double, Integer> map = new HashMap<>();
        double max = 1, tmp = 0;

        for (EntryBundle entry : list) {
            if (map.get(entry.getPrice()) != null) {
                int count = map.get(entry.getPrice());
                map.put(entry.getPrice(), ++count);

                if (count > max) {
                    max = count;
                    tmp = entry.getPrice();
                }
            } else {
                map.put(entry.getPrice(), 1);
            }
        }

        return tmp;
    }
}
