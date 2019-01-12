package poe.manager.worker;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.entry.EntryManager;
import poe.manager.entry.item.Mappers;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

;

/**
 * Manages worker objects (eg. distributing jobs, adding/removing workers)
 */
public class WorkerManager extends Thread {
    private static Logger logger = LoggerFactory.getLogger(WorkerManager.class);
    private final Gson gson = new Gson();
    private final Object monitor = new Object();
    private Config config;
    private ArrayList<Worker> workerList = new ArrayList<>();
    private volatile boolean flag_Run = true;
    private volatile boolean readyToExit = false;
    private String nextChangeID;
    private EntryManager entryManager;
    private Database database;

    public WorkerManager(EntryManager entryManager, Database database, Config config) {
        this.entryManager = entryManager;
        this.database = database;
        this.config = config;
    }

    /**
     * Contains main loop. Checks for open jobs and assigns them to workers
     */
    public void run() {
        // Run main loop while flag is up
        while (flag_Run) {
            if (nextChangeID == null) waitOnMonitor();

            // While there's a job that needs to be given out
            while (flag_Run && nextChangeID != null) {
                for (Worker worker : workerList) {
                    if (worker.hasJob()) continue;

                    worker.setJob(nextChangeID);
                    nextChangeID = null;
                    break;
                }

                // Wait for a moment if all workers are busy
                if (nextChangeID != null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        readyToExit = true;
    }

    /**
     * Sleeps until monitor object is notified
     */
    private void waitOnMonitor() {
        synchronized (monitor) {
            try {
                monitor.wait(config.getInt("worker.monitorTimeout"));
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Stops all active Workers and this object's process
     */
    public void stopController() {
        flag_Run = false;

        // Loop though every worker and call stop method
        for (Worker worker : workerList) {
            logger.info("Stopping worker (" + worker.getIndex() + ")");
            worker.stopWorker();
            logger.info("Worker (" + worker.getIndex() + ") stopped");
        }

        logger.info("Stopping controller");

        // Wait until run() function is ready to exit
        while (!readyToExit) try {
            Thread.sleep(50);

            // Wake the monitor that's holding up the main loop, allowing safe exit
            synchronized (getMonitor()) {
                getMonitor().notify();
            }

        } catch (InterruptedException ex) {
        }

        logger.info("Controller stopped");
    }

    /**
     * Prints out all active workers and their active jobs
     */
    public void printAllWorkers() {
        for (Worker workerObject : workerList) {
            logger.info("    " + workerObject.getIndex() + ": " + workerObject.getJob());
        }
    }

    /**
     * Spawns new workers
     *
     * @param workerCount Amount of new workers to be added
     */
    public void spawnWorkers(int workerCount) {
        // Get the next available array index
        int nextWorkerIndex = workerList.size();

        // Loop through creation
        for (int i = nextWorkerIndex; i < nextWorkerIndex + workerCount; i++) {
            Worker worker = new Worker(entryManager, this, database, config);

            // Set some worker PROPERTIES and start
            worker.setIndex(i);
            worker.start();

            // Add worker to local list
            workerList.add(worker);
        }
    }

    /**
     * Removes active workers
     *
     * @param workerCount Amount of new workers to be removed
     */
    public void fireWorkers(int workerCount) {
        Worker lastWorker;

        // Get the last available index
        int lastWorkerIndex = workerList.size() - 1;

        // Can't remove what's not there
        if (lastWorkerIndex <= 0 || lastWorkerIndex - workerCount < 0) {
            logger.error("Not enough active workers");
            return;
        }

        // Loop through removal
        for (int i = lastWorkerIndex; i > lastWorkerIndex - workerCount; i--) {
            lastWorker = workerList.get(i);
            lastWorker.stopWorker();
            workerList.remove(lastWorker);
        }
    }

    /**
     * Get a changeID that's close to the stack top
     *
     * @return ChangeID as string
     */
    public String getLatestChangeID() {
        return downloadChangeID("http://api.pathofexile.com/trade/data/change-ids");
    }

    /**
     * Downloads content of ChangeID url and returns it
     *
     * @param url Link to ChangeID resource
     * @return ChangeID as string or null on failure
     */
    private String downloadChangeID(String url) {
        try {
            URL request = new URL(url);
            InputStream input = request.openStream();

            String response = new String(input.readAllBytes());

            try {
                return gson.fromJson(response, Mappers.ChangeID.class).get();
            } catch (Exception ex) {
                logger.error(ex.toString());
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    /**
     * Sets the next change ID in the variable. If the variable has no value, set it to the newChangeID's one,
     * otherwise compare the two and set the newest
     *
     * @param newChangeID ChangeID to be added
     */
    public void setNextChangeID(String newChangeID) {
        if (nextChangeID == null) {
            nextChangeID = newChangeID;
        } else if (Integer.parseInt(newChangeID.substring(newChangeID.lastIndexOf('-') + 1)) >
                Integer.parseInt(nextChangeID.substring(nextChangeID.lastIndexOf('-') + 1))) {
            nextChangeID = newChangeID;
        }

        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    /**
     * Shares monitor to other classes
     *
     * @return Monitor object
     */
    public Object getMonitor() {
        return monitor;
    }

    public boolean getWorkerSleepState() {
        for (Worker worker : workerList) {
            if (!worker.isSleeping()) {
                return false;
            }
        }

        return true;
    }

    public void setWorkerSleepState(boolean state) {
        System.out.println(state ? "Pausing workers.." : "Resuming workers..");

        for (Worker worker : workerList) {
            worker.setSleepFlag(state);
        }
    }
}
