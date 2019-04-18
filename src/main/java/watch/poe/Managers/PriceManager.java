package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;
import poe.Managers.Price.Bundles.ResultBundle;
import poe.Managers.Price.Calculation;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PriceManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PriceManager.class);
    private final Database database;

    private volatile boolean run = true;
    private volatile boolean readyToExit = false;
    private static final int CALC_DELAY = 50;

    public PriceManager(Database database) {
        this.database = database;
    }

    /**
     * Main loop of the thread
     */
    public void run() {
        // Get the time of the last calculation on program start
        Timestamp cycleStart = database.init.getLastItemTime();
        if (cycleStart == null) {
            // Database contained no items
            cycleStart = new Timestamp(0);
        }

        // Main loop of the thread
        while (run) {
            logger.info("Fetching latest currency rates");

            // Grab newest currency ratios from the database
            List<PriceBundle> priceBundles = new ArrayList<>();
            boolean success = database.calc.getPriceBundles(priceBundles);

            if (!success) {
                logger.error("Could not get currency rates for price calculation");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                continue;
            }

            // Get ID bundles from database
            List<IdBundle> idBundles = new ArrayList<>();
            success = database.calc.getNewIdBundles(idBundles, cycleStart);

            if (!success) {
                logger.error("Could not get ids for price calculation");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                continue;
            } else if (idBundles.isEmpty()) {
                logger.warn("Id bundle list was empty");

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                continue;
            }

            // Record current time for next cycle
            cycleStart = new Timestamp(System.currentTimeMillis());

            logger.info("Starting cycle");
            processBundles(idBundles, priceBundles);
            logger.info("Finished cycle");
        }

        readyToExit = true;
    }

    /**
     * Takes all id bundles returned from the database and calculates prices for them at a steady pace
     *
     * @param idBundles Valid list of ids
     * @param priceBundles Valid list of currency rates
     */
    private void processBundles(List<IdBundle> idBundles, List<PriceBundle> priceBundles) {
        if (idBundles == null || idBundles.isEmpty()) {
            throw new RuntimeException("Invalid list provided");
        }

        // Loop through all the items we should calculate a price for
        for (int i = 0; run && i < idBundles.size(); i++) {
            // Query entries from the database for this item
            List<EntryBundle> entryBundles = new ArrayList<>();
            boolean success = database.calc.getEntryBundles(entryBundles, idBundles.get(i));

            if (!success) {
                logger.error(String.format("Could not query entries for %d %d",
                        idBundles.get(i).getLeagueId(), idBundles.get(i).getItemId()));
                continue;
            } else if (entryBundles.isEmpty()) {
                logger.warn(String.format("Empty entry bundle %d %d\n",
                        idBundles.get(i).getLeagueId(), idBundles.get(i).getItemId()));
                continue;
            }

            // Convert all entry prices to chaos for this item
            Calculation.convertToChaos(idBundles.get(i), entryBundles, priceBundles);

            // Calculate the prices for this item
            ResultBundle rb = Calculation.calculateResult(idBundles.get(i), entryBundles);
            if (rb == null) continue;

            // Update item in database
            database.upload.updateItem(rb);

            // Just a status string, not worth logging
            System.out.printf("[%2d|%5d] %4d\\%4d\n",
                    idBundles.get(i).getLeagueId(), idBundles.get(i).getItemId(), i, idBundles.size());

            try {
                Thread.sleep(CALC_DELAY);
            } catch (InterruptedException ex) {
                logger.error(ex.toString());
            }
        }
    }

    /**
     * Stops the controller
     */
    public void stopController() {
        logger.info("Stopping controller");

        run = false;

        while (!readyToExit) try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        logger.info("Controller stopped");
    }
}
