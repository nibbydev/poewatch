package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.Interval.TimeFrame;
import poe.Managers.Price.Bundles.EntryBundle;
import poe.Managers.Price.Bundles.IdBundle;
import poe.Managers.Price.Bundles.PriceBundle;
import poe.Managers.Price.Bundles.ResultBundle;
import poe.Managers.Price.Calculation;

import java.util.ArrayList;
import java.util.List;

public class PriceManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PriceManager.class);
    private final Database database;
    private final Object queueMonitor = new Object();
    private final Object cycleMonitor = new Object();
    private volatile boolean run = true;
    private volatile boolean inProgress = false;
    private volatile boolean readyToExit = false;
    private volatile boolean sleepPerIteration = true;
    private final List<IdBundle> idBundles = new ArrayList<>();
    private final List<PriceBundle> priceBundles = new ArrayList<>();

    public PriceManager(Database database) {
        this.database = database;
    }

    public void run() {
        while (run) {
            synchronized (queueMonitor) {
                try {
                    queueMonitor.wait();
                } catch (InterruptedException ex) {
                    continue;
                }
            }

            if (idBundles.isEmpty()) {
                continue;
            }

            inProgress = true;
            long iterationDelay;
            int iterationIndex = 0;

            for (IdBundle idBundle : idBundles) {
                iterationDelay = System.currentTimeMillis();

                // Query entries from the database for this item
                List<EntryBundle> entryBundles = new ArrayList<>();
                boolean success = database.calc.getEntryBundles(entryBundles, idBundle);
                if (!success) {
                    return;
                } else if (entryBundles.isEmpty()) {
                    System.out.printf("Empty entry bundle %d %d\n", idBundle.getLeagueId(), idBundle.getItemId());
                    continue;
                }

                // Convert all entry prices to chaos for this item
                Calculation.convertToChaos(idBundle, entryBundles, priceBundles);

                // Calculate the prices for this item
                ResultBundle rb = Calculation.calculateResult(idBundle, entryBundles);
                if (rb == null) continue;

                // Update item entry in database
                database.upload.updateItem(rb);

                // Calculate how long this iteration took and how long we should sleep until the next one
                long normMsPerIteration = TimeFrame.M_10.getRemaining() / (idBundles.size() - iterationIndex++);
                long sleepTime = normMsPerIteration - (System.currentTimeMillis() - iterationDelay);

                System.out.printf("[%2d|%5d] %3d\\%3d ms - %4d\\%4d - remain %3d s\n",
                        idBundle.getLeagueId(), idBundle.getItemId(),
                        sleepTime > 0 ? sleepTime : 0, normMsPerIteration, iterationIndex,
                        idBundles.size(), TimeFrame.M_10.getRemaining() / 1000);

                if (sleepPerIteration && sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ex) {
                        logger.error(ex.toString());
                    }
                }
            }

            logger.info("Finished cycle");
            inProgress = false;
            synchronized (cycleMonitor) {
                cycleMonitor.notify();
            }
        }

        readyToExit = true;
    }

    public void stopController() {
        logger.info("Stopping controller");

        run = false;
        sleepPerIteration = false;

        synchronized (queueMonitor) {
            queueMonitor.notify();
        }

        while (!readyToExit) try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        logger.info("Controller stopped");
    }

    public void startCycle() {
        if (inProgress) {
            logger.warn("Waiting for previous cycle to finish");
            sleepPerIteration = false;
        }

        // Wait until last cycle is done
        while (run && inProgress) {
            synchronized (cycleMonitor) {
                try {
                    cycleMonitor.wait();
                } catch (InterruptedException ex) {
                    logger.error(ex.toString());
                }
            }
        }

        if (!run) {
            return;
        }

        logger.info("Starting price calculation cycle");

        // Get list of items that need to have their prices recalculated
        synchronized (idBundles) {
            idBundles.clear();

            boolean success = database.calc.getNewItemIdBundles(idBundles);
            if (!success) {
                logger.error("Could not get ids for price calculation");
                return;
            } else if (idBundles.isEmpty()) {
                logger.warn("Id bundle list was empty");
                return;
            }
        }

        // Get fresh currency rates
        synchronized (priceBundles) {
            priceBundles.clear();

            boolean success = database.calc.getPriceBundles(priceBundles);
            if (!success) {
                logger.error("Could not get currency rates for price calculation");
                return;
            }
        }

        synchronized (queueMonitor) {
            queueMonitor.notify();
        }

        sleepPerIteration = true;
        long delay = TimeFrame.M_10.getRemaining() / idBundles.size();
        logger.info(String.format("Queued %d items for price calculation with delay ~%d", idBundles.size(), delay));
    }
}
