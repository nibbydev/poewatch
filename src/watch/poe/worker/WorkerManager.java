package watch.poe.worker;

import com.google.gson.Gson;
import watch.poe.Config;
import watch.poe.Main;
import watch.poe.admin.Flair;
import watch.poe.item.Mappers;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * Manages worker objects (eg. distributing jobs, adding/removing workers)
 */
public class WorkerManager extends Thread {
    private ArrayList<Worker> workerList = new ArrayList<>();
    private final Gson gson = new Gson();
    private final Object monitor = new Object();
    private volatile boolean flag_Run = true;
    private volatile boolean readyToExit = false;
    private String nextChangeID;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

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
                monitor.wait(Config.monitorTimeoutMS);
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
            Main.ADMIN.log("Stopping worker (" + worker.getIndex() + ")", Flair.INFO);
            worker.stopWorker();
            Main.ADMIN.log("Worker (" + worker.getIndex() + ") stopped", Flair.INFO);
        }

        Main.ADMIN.log("Stopping controller", Flair.INFO);

        // Wait until run() function is ready to exit
        while (!readyToExit) try {
            Thread.sleep(50);

            // Wake the monitor that's holding up the main loop, allowing safe exit
            synchronized (getMonitor()) {
                getMonitor().notify();
            }

        } catch (InterruptedException ex) { }

        Main.ADMIN.log("Controller stopped", Flair.INFO);
    }

    //------------------------------------------------------------------------------------------------------------
    // worker management
    //------------------------------------------------------------------------------------------------------------
    /**
     * Prints out all active workers and their active jobs
     */
    public void printAllWorkers() {
        for (Worker workerObject : workerList) {
            Main.ADMIN.log("    " + workerObject.getIndex() + ": " + workerObject.getJob(), Flair.INFO);
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
            Worker worker = new Worker();

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
            Main.ADMIN.log("Not enough active workers", Flair.ERROR);
            return;
        }

        // Loop through removal
        for (int i = lastWorkerIndex; i > lastWorkerIndex - workerCount; i--) {
            lastWorker = workerList.get(i);
            lastWorker.stopWorker();
            workerList.remove(lastWorker);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getting starting change ID
    //------------------------------------------------------------------------------------------------------------

    /**
     * Get a changeID that's close to the stack top
     *
     * @return ChangeID as string
     */
    public String getLatestChangeID() {
        // Get data from API
        String idOne = downloadChangeID("http://poe-rates.com/actions/getLastChangeId.php");
        String idTwo = downloadChangeID("http://api.pathofexile.com/trade/data/change-ids");

        // Get current cluster index
        int sizeTwo = Integer.parseInt(idOne.substring(idOne.lastIndexOf('-') + 1));
        int sizeThree = Integer.parseInt(idTwo.substring(idTwo.lastIndexOf('-') + 1));

        // Compare cluster indexes and return latest
        if (sizeThree < sizeTwo) return idOne;
        else return idTwo;
    }

    /**
     * Downloads content of ChangeID url and returns it
     *
     * @param url Link to ChangeID resource
     * @return ChangeID as string
     */
    private String downloadChangeID(String url) {
        String response;

        // Download data
        try {
            URL request = new URL(url);
            InputStream input = request.openStream();

            response = new String(input.readAllBytes());
        } catch (Exception ex) {
            response = "0-0-0-0-0";
        }

        try {
            response = gson.fromJson(response, Mappers.ChangeID.class).get();
        } catch (Exception ex) {
            Main.ADMIN.log("Could not download ChangeID from: " + url, Flair.ERROR);
            Main.ADMIN.logException(ex, Flair.ERROR);
        }

        return response;
    }

    /**
     * Gets local ChangeID
     *
     * @return ChangeID as string
     */
    public String getLocalChangeID() {
        return downloadChangeID("http://api.poe.watch/id.php");
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
        } else if (Integer.parseInt(newChangeID.substring(newChangeID.lastIndexOf('-') + 1, newChangeID.length())) >
                Integer.parseInt(nextChangeID.substring(nextChangeID.lastIndexOf('-') + 1, nextChangeID.length()))) {
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

    public boolean isFlag_Run() {
        return flag_Run;
    }
}
