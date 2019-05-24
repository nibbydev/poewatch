package poe.Managers.Price;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CalculationTest {
    @Test
    void currencyConversionTest() {
        // Listings
        List<EntryBundle> eb = new ArrayList<>() {{
            add(new EntryBundle(null, 1));
            add(new EntryBundle(null, 0));
            add(new EntryBundle(120, 1));
            add(new EntryBundle(null, 99999));
            add(new EntryBundle(142, 0.02));
            add(new EntryBundle(142, 1000));
            add(new EntryBundle(142, 0));
            add(new EntryBundle(156, 11));
        }};

        // Currency values
        List<PriceBundle> pb = new ArrayList<>() {{
            add(new PriceBundle(1, 120, 49));
            add(new PriceBundle(1, 142, 159.548));
        }};

        // Dummy league id
        IdBundle ib = new IdBundle();
        ib.setLeagueId(1);

        // Convert all to base currency
        List<Double> prices = Calculation.convertToChaos(ib, eb, pb);
        List<Double> expected = new ArrayList<>(Arrays.asList(1d, 0d, 49d, 99999d, 3.19096d, 159548d, 0d));

        // Unknown currency was removed
        assertEquals(expected.size(), eb.size());

        // Check if elements match
        for (int i = 0; i < expected.size(); i++) {
            // Prices match
            assertEquals(expected.get(i), prices.get(i),
                    String.format("Price %f != %f at index %d\n", prices.get(i), expected.get(i), i));
        }
    }

    @Test
    void filterTestSmall() {
        double big1 = 99999d;
        double big2 = 999999d;
        double med1 = 12d;

        List<Double> eb = new ArrayList<>(Arrays.asList(1d, 1d, 1d, 2d, med1, big1, big2, big2));
        List<Double> result = Calculation.filterEntries(eb);

        assertEquals(4, result.size());
        assertFalse(result.contains(big1));
        assertFalse(result.contains(big2));
        assertFalse(result.contains(med1));
    }
}