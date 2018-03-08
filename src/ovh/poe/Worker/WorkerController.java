package ovh.poe.Worker;

import ovh.poe.Mappers;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import ovh.poe.Main;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * Manages Worker objects (eg. distributing jobs, adding/removing workers)
 */
public class WorkerController extends Thread {
    private volatile boolean flag_Run = true;
    private ArrayList<Worker> workerList = new ArrayList<>();
    private final Object monitor = new Object();
    private String nextChangeID;

    /////////////////////////////
    // Actually useful methods //
    /////////////////////////////

    /**
     * Contains main loop. Checks for open jobs and assigns them to workers
     */
    public void run() {
        // Run main loop while flag is up
        while (flag_Run) {
            // Sleep if there is no job to be given out
            if (nextChangeID == null) waitOnMonitor();

            // While there's a job that needs to be given out
            while (flag_Run && nextChangeID != null) {
                // Loop through workers
                for (Worker worker : workerList) {
                    // Check if worker is free
                    if (worker.hasJob()) continue;
                    // Give the new job to the worker
                    worker.setJob(nextChangeID);
                    // Remove old job
                    nextChangeID = null;
                    // Exit the topmost while loop
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

            // Run PRICER_CONTROLLER. As that Class has no actual loop and this method executes pretty often, pricer
            // controller itself has some checks whether it should run on method call or not
            Main.PRICER_CONTROLLER.run();
        }
    }

    /**
     * Sleeps until monitor object is notified
     */
    private void waitOnMonitor() {
        synchronized (monitor) {
            try {
                monitor.wait(500);
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
            worker.stopWorker();
        }

        // Wake the monitor that's holding up the main loop, allowing safe exit
        synchronized (getMonitor()) {
            getMonitor().notify();
        }
    }

    ///////////////////////////////////
    // Methods for worker management //
    ///////////////////////////////////

    /**
     * Prints out all active workers and their active jobs
     */
    public void printAllWorkers() {
        // Loop though every worker and print out its job
        for (Worker workerObject : workerList)
            System.out.println("    " + workerObject.getIndex() + ": " + workerObject.getJob());
    }

    /**
     * Spawns new workers
     *
     * @param workerCount Amount of new workers to be added
     */
    public void spawnWorkers(int workerCount) {
        // Get the next available array index
        int nextWorkerIndex = workerList.size();

        // Forbid spawning over limit
        if (nextWorkerIndex + workerCount > Main.CONFIG.workerLimit) {
            System.out.println("[ERROR] Maximum amount of workers is " + Main.CONFIG.workerLimit);
            return;
        }

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
            System.out.println("[ERROR] Not enough active workers");
            return;
        }

        // Loop through removal
        for (int i = lastWorkerIndex; i > lastWorkerIndex - workerCount; i--) {
            lastWorker = workerList.get(i);
            lastWorker.stopWorker();
            workerList.remove(lastWorker);
        }
    }

    //////////////////////////////////////////////////////////
    // Methods for getting a ChangeID from external sources //
    //////////////////////////////////////////////////////////

    /**
     * Get a changeID that's close to the stack top
     *
     * @return ChangeID as string
     */
    public String getLatestChangeID() {
        String idOne = downloadChangeID("http://api.poe.ninja/api/Data/GetStats");
        String idTwo = downloadChangeID("http://poe-rates.com/actions/getLastChangeId.php");

        if (Integer.parseInt(idOne.substring(idOne.lastIndexOf('-') + 1, idOne.length())) <
                Integer.parseInt(idTwo.substring(idTwo.lastIndexOf('-') + 1, idTwo.length())))
            return idTwo;
        else
            return idOne;
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

        // Map data
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

            // Map the JSON string to an object
            response = mapper.readValue(response, Mappers.ChangeID.class).getNext_change_id();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return response;
    }

    /**
     * Gets local ChangeID
     *
     * @return ChangeID as string
     */
    public String getLocalChangeID() {
        return downloadChangeID("http://api.poe.ovh/ChangeID");
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

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
}
