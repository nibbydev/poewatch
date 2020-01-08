package poe.Price;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Database.Database;
import poe.Price.Bundles.EntryBundle;
import poe.Price.Bundles.IdBundle;
import poe.Price.Bundles.PriceBundle;
import poe.Price.Bundles.ResultBundle;
import poe.Worker.WorkerManager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The class in charge of deciding when to calculate prices and for what items
 */
public class PriceManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PriceManager.class);
    private final Database database;
    private final Config config;
    private final WorkerManager workerManager;
    private final Calculator calculator;

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

        this.calculator = new Calculator(cnf);
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
            if (config.getBoolean("calculation.pauseWorkers")){
                workerManager.setWorkerSleepState(true, true);
            }

            runCycle();

            if (config.getBoolean("calculation.pauseWorkers")) {
                workerManager.setWorkerSleepState(false, true);
            }

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
        Set<PriceBundle> priceBundles = new HashSet<>();
        if (!tryGetPriceBundles(priceBundles)) {
            return;
        }

        // Get ID bundles from database. Or in other words list of items that have had updates since the last cycle and
        // need to have their prices calculated again
        Set<IdBundle> idBundles = new HashSet<>();
        if (!tryGetIdBundles(idBundles)) {
            return;
        }

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
    private boolean tryGetPriceBundles(Set<PriceBundle> priceBundles) {
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
    private boolean tryGetIdBundles(Set<IdBundle> idBundles) {
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
    private void processBundles(Set<IdBundle> idBundles, Set<PriceBundle> priceBundles) {
        if (idBundles == null || idBundles.isEmpty()) {
            throw new RuntimeException("Invalid list provided");
        }

        int counter = 0;

        // Loop through all the items we should calculate a price for
        for (IdBundle idBundle : idBundles) {
            if (!run) {
                break;
            }

            long startTime = System.currentTimeMillis();

            // Query entries from the database for this item
            Set<EntryBundle> entryBundles = new HashSet<>();
            boolean success = database.calc.getEntryBundles(entryBundles, idBundle,
                    config.getInt("calculation.lastAccountActivity"));

            if (!success) {
                logger.error(String.format("Could not query entries for %d %d",
                        idBundle.getLeagueId(), idBundle.getItemId()));
                continue;
            } else if (entryBundles.isEmpty()) {
                logger.warn(String.format("Empty entry bundle %d %d\n",
                        idBundle.getLeagueId(), idBundle.getItemId()));
                continue;
            }

            if (idBundle.getLeagueId() == 30) {
                logger.debug("got {} entries for {}. took {} s", entryBundles.size(), idBundle.getItemId(), System.currentTimeMillis() / startTime / 1000);
                startTime = System.currentTimeMillis();
            }

            int entryCount = entryBundles.size();

            // Limit duplicate entries per account
            if (config.getBoolean("calculation.enableAccountLimit")) {
                calculator.limitDuplicateEntries(entryBundles, config.getInt("calculation.accountLimit"));

                // Send a warning message if too many were removed from duplicate accounts
                int percentRemoved = Math.round(100 - (float) entryBundles.size() / entryCount * 100f);
                if (percentRemoved >= 50 && entryCount > 10) {
                    logger.warn("[{}| {}] duplicate accounts - {}/{} removed ({}%)",
                            idBundle.getLeagueId(),
                            idBundle.getItemId(),
                            entryCount - entryBundles.size(),
                            entryCount,
                            percentRemoved);
                }

                if (idBundle.getLeagueId() == 30) {
                    logger.debug("removing duplicates took {} s", System.currentTimeMillis() / startTime / 1000);
                    startTime = System.currentTimeMillis();
                }
            }

            // Convert all entry prices to chaos value
            List<Double> prices = calculator.convertToChaos(idBundle, entryBundles, priceBundles);

            if (prices.isEmpty()) {
                logger.warn("[{}| {}] price conversion - all removed ({})",
                        idBundle.getLeagueId(),
                        idBundle.getItemId(),
                        entryCount);
                continue;
            }

            if (idBundle.getLeagueId() == 30) {
                logger.debug("chaos conversion took {} s", System.currentTimeMillis() / startTime / 1000);
                startTime = System.currentTimeMillis();
            }

            // Remove outliers
            calculator.filterEntries(prices);

            // Hard trim entries
            if (config.getBoolean("calculation.enableHardTrim")) {
                prices = calculator.hardTrim(prices,
                        config.getInt("calculation.hardTrimLower"),
                        config.getInt("calculation.hardTrimUpper"));
            }

            if (idBundle.getLeagueId() == 30) {
                logger.debug("filtering took {} s", System.currentTimeMillis() / startTime / 1000);
                startTime = System.currentTimeMillis();
            }

            // If no entries were left, skip the item
            if (prices.isEmpty()) {
                logger.warn("[{}| {}] filter - all removed ({})",
                        idBundle.getLeagueId(),
                        idBundle.getItemId(),
                        entryCount);
                continue;
            }

            // Calculate the prices for this item
            ResultBundle rb = calculator.calculateResult(idBundle, prices);
            if (rb == null) continue;

            // Update item in database
            database.upload.updateItem(rb);
            statusMessage(counter++, idBundles.size());

            if (idBundle.getLeagueId() == 30) {
                logger.debug("update took {} s", System.currentTimeMillis() / startTime / 1000);
            }

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
