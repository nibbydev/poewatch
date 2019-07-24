package poe.Managers;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;
import poe.Managers.Price.Bundles.ResultBundle;
import poe.Managers.Price.Calculation;
import poe.Worker.WorkerManager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PriceManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PriceManager.class);
    private final Database database;
    private final Config config;
    private final WorkerManager workerManager;
    private final Calculation calculator;

    // should the manager be running
    private boolean run = true;
    // is the manager currently running
    private boolean readyToExit = true;

    private long lastCycleTime;
    private Timestamp cycleStart;

    public PriceManager(Database db, Config cnf, WorkerManager wm) {
        this.database = db;
        this.config = cnf;
        this.workerManager = wm;

        this.calculator = new Calculation(cnf);
    }

    /**
     * Main loop of the thread
     */
    public void run() {
        readyToExit = false;

        // Set last time to now to avoid instant cycle activation
        lastCycleTime = System.currentTimeMillis();

        // Get the time of the last calculation on program start
        cycleStart = database.init.getLastItemTime();
        if (cycleStart == null) {
            // Database contained no items
            cycleStart = new Timestamp(0);
        }

        // Main loop of the thread
        while (run) {
            // Minimal delay before starting cycle
            if (!checkIfRun()) continue;

            logger.info("Starting cycle");
            if (config.getBoolean("calculation.pauseWorkers"))
                workerManager.setWorkerSleepState(true, true);

            runCycle();

            if (config.getBoolean("calculation.pauseWorkers"))
                workerManager.setWorkerSleepState(false, true);
            logger.info("Finished cycle");

            lastCycleTime = System.currentTimeMillis();
        }

        readyToExit = true;
    }

    /**
     * Checks whether main loop should run. Will sleep if minimal interval has not passed.
     *
     * @return True if should run
     */
    private boolean checkIfRun() {
        if (lastCycleTime + config.getInt("calculation.minCycleInterval") > System.currentTimeMillis()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            return false;
        }

        return true;
    }

    /**
     * Attempt to run a cycle
     */
    private void runCycle() {
        // Grab newest currency ratios from the database
        List<PriceBundle> priceBundles = new ArrayList<>();
        if (!tryGetPriceBundles(priceBundles)) return;

        // Get ID bundles from database
        List<IdBundle> idBundles = new ArrayList<>();
        if (!tryGetIdBundles(idBundles)) return;

        // Record current time for next cycle. Only items that had updated
        // listings after this timestamp will be used in next cycle
        cycleStart = new Timestamp(System.currentTimeMillis());

        // Now that we have up to date currency rates and a list of items
        // that need their prices calculated, do the rest of the magic
        processBundles(idBundles, priceBundles);
    }

    /**
     * Grab latest currency ratios
     *
     * @param priceBundles Empty list of bundles
     * @return True on success
     */
    private boolean tryGetPriceBundles(List<PriceBundle> priceBundles) {
        logger.debug("Fetching latest currency rates");

        if (!database.calc.getPriceBundles(priceBundles)) {
            logger.error("Could not get currency rates for price calculation");

            try {
                Thread.sleep(config.getInt("calculation.currencyRetryDelay"));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            return false;
        }

        logger.debug("Got {} currency items", priceBundles.size());

        return true;
    }

    /**
     * Grab a list of items that need their prices calculated
     *
     * @param idBundles Empty list of bundles
     * @return True on success
     */
    private boolean tryGetIdBundles(List<IdBundle> idBundles) {
        logger.debug("Fetching id bundles");

        if (!database.calc.getIdBundles(idBundles, cycleStart)) {
            logger.error("Could not get ids for price calculation");

            try {
                Thread.sleep(config.getInt("calculation.itemRetryDelay"));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            return false;
        }

        if (idBundles.isEmpty()) {
            logger.warn("Id bundle list was empty");

            try {
                Thread.sleep(config.getInt("calculation.itemRetryDelay"));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            return false;
        }

        logger.debug("Got {} items for price calculation", idBundles.size());

        return true;
    }

    /**
     * Takes all id bundles returned from the database and calculates prices for them at a steady pace
     *
     * @param idBundles    Valid list of ids
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
            boolean success = database.calc.getEntryBundles(entryBundles, idBundles.get(i),
                    config.getInt("calculation.lastAccountActivity"));

            if (!success) {
                logger.error(String.format("Could not query entries for %d %d",
                        idBundles.get(i).getLeagueId(), idBundles.get(i).getItemId()));
                continue;
            } else if (entryBundles.isEmpty()) {
                logger.warn(String.format("Empty entry bundle %d %d\n",
                        idBundles.get(i).getLeagueId(), idBundles.get(i).getItemId()));
                continue;
            }

            int entryCount = entryBundles.size();

            // Limit duplicate entries per account
            if (config.getBoolean("calculation.enableAccountLimit")) {
                calculator.limitDuplicateEntries(entryBundles, config.getInt("calculation.accountLimit"));

                // Send a warning message if too many were removed from duplicate accounts
                int percentRemoved = Math.round(100 - (float) entryBundles.size() / entryCount * 100f);
                if (percentRemoved >= 50 && entryCount > 10) {
                    logger.warn("[{}| {}] duplicate accounts - {}/{} removed ({}%)",
                            idBundles.get(i).getLeagueId(),
                            idBundles.get(i).getItemId(),
                            entryCount - entryBundles.size(),
                            entryCount,
                            percentRemoved);
                }
            }

            // Convert all entry prices to chaos value
            List<Double> prices = calculator.convertToChaos(idBundles.get(i), entryBundles, priceBundles);

            if (prices.isEmpty()) {
                logger.warn("[{}| {}] price conversion - all removed ({})",
                        idBundles.get(i).getLeagueId(),
                        idBundles.get(i).getItemId(),
                        entryCount);
                continue;
            }

            // Remove outliers
            calculator.filterEntries(prices);

            // Hard trim entries
            if (config.getBoolean("calculation.enableHardTrim")) {
                prices = calculator.hardTrim(prices,
                        config.getInt("calculation.hardTrimLower"),
                        config.getInt("calculation.hardTrimUpper"));
            }


            // If no entries were left, skip the item
            if (prices.isEmpty()) {
                logger.warn("[{}| {}] filter - all removed ({})",
                        idBundles.get(i).getLeagueId(),
                        idBundles.get(i).getItemId(),
                        entryCount);
                continue;
            }

            // Calculate the prices for this item
            ResultBundle rb = calculator.calculateResult(idBundles.get(i), prices);
            if (rb == null) continue;

            // Update item in database
            database.upload.updateItem(rb);

            statusMessage(i, idBundles.size());
            try {
                Thread.sleep(config.getInt("calculation.itemDelay"));
            } catch (InterruptedException ex) {
                logger.error(ex.toString());
            }
        }
    }

    /**
     * Displays a status message every n-th item
     *
     * @param current Current item index
     * @param total   Total number of items this cycle
     */
    private void statusMessage(int current, int total) {
        int frequency = (int) Math.ceil((float) total / config.getInt("calculation.statusMsgCount"));

        if (current % frequency == 0) {
            int percentage = (int) Math.floor((float) current / total * 100f);
            logger.debug("{}% done ({} out of {})", percentage, current, total);
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

    /**
     * Resets the last cycle timestamp, allowing all item to have their prices calculated
     */
    public void resetCycleStamp() {
        cycleStart = new Timestamp(0);
    }
}
