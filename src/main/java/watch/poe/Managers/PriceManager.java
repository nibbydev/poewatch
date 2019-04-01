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
import java.util.Iterator;
import java.util.List;

public class PriceManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PriceManager.class);
    private final Database database;
    private final List<ResultBundle> resultBundles = new ArrayList<>(4000);
    private final Object queueMonitor = new Object();
    private final Object cycleMonitor = new Object();
    private volatile boolean run = true;
    private volatile boolean inProgress = false;
    private volatile boolean readyToExit = false;
    private List<IdBundle> idBundles;
    private List<PriceBundle> priceBundles;
    private int msPerIteration = 0;

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

            if (idBundles == null || idBundles.isEmpty()) {
                continue;
            }

            inProgress = true;
            long iterationDelay;

            Iterator<IdBundle> idBundleIterator = idBundles.iterator();
            while (run && idBundleIterator.hasNext()) {
                iterationDelay = System.currentTimeMillis();
                IdBundle idBundle = idBundleIterator.next();

                // Query entries from the database for this item
                List<EntryBundle> entryBundles = database.calc.getEntryBundles(idBundle);

                if (entryBundles.isEmpty()) {
                    System.out.printf("Empty entry bundle %d %d\n", idBundle.getLeagueId(), idBundle.getItemId());
                    continue;
                }

                // Convert all entry prices to chaos for this item
                Calculation.convertToChaos(idBundle, entryBundles, priceBundles);

                // Calculate the prices for this item
                ResultBundle rb = Calculation.calculateResult(idBundle, entryBundles);
                if (rb == null) continue;
                resultBundles.add(rb);

                long sleepTime = msPerIteration - (System.currentTimeMillis() - iterationDelay);
                if (sleepTime > 0) {
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
            msPerIteration = 0;
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

        // If there are any results to upload
        if (!resultBundles.isEmpty()) {
            database.upload.updateItems(resultBundles);
            resultBundles.clear();
        }

        logger.info("Starting price calculation cycle");

        // Get list of items that need to have their prices recalculated
        idBundles = database.calc.getNewItemIdBundles();
        if (idBundles == null) {
            logger.warn("Could not get ids for price calculation");
            throw new RuntimeException();
        } else if (idBundles.isEmpty()) {
            logger.warn("Id bundle list was empty");
            return;
        }

        // Get fresh currency rates
        priceBundles = database.calc.getPriceBundles();
        if (priceBundles == null) {
            logger.warn("Could not get currency rates for price calculation");
            throw new RuntimeException();
        }

        msPerIteration = (int) TimeFrame.M_10.getRemaining() / idBundles.size();
        synchronized (queueMonitor) {
            queueMonitor.notify();
        }

        logger.info(String.format("Queued %d items for price calculation with delay %d", idBundles.size(), msPerIteration));
    }
}
