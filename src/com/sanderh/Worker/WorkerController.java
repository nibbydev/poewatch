package com.sanderh.Worker;

import com.sanderh.Mappers;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import static com.sanderh.Main.CONFIG;
import static com.sanderh.Main.PRICER_CONTROLLER;

public class WorkerController extends Thread {
    //  Name: WorkerController
    //  Date created: 21.11.2017
    //  Last modified: 23.12.2017
    //  Description: Object that's used to manage worker objects

    private volatile boolean flagLocalRun = true;
    private ArrayList<Worker> workerList = new ArrayList<>();
    private final Object monitor = new Object();
    private String nextChangeID;

    /////////////////////////////
    // Actually useful methods //
    /////////////////////////////

    public void run() {
        //  Name: run()
        //  Date created: 22.11.2017
        //  Last modified: 23.12.2017
        //  Description: Assigns jobs to workers

        // Run main loop while flag is up
        while (flagLocalRun) {
            // Sleep only if there is no job to be given out
            if (nextChangeID == null)
                checkMonitor();

            // While there's a job that needs to be given out
            while (flagLocalRun && nextChangeID != null) {
                // Loop through workers
                for (Worker worker : workerList) {
                    // Check if worker is free
                    if (worker.hasJob()) continue;
                    // Give the new job to the worker
                    worker.setJob(nextChangeID);
                    // Remove old job
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

            // Run pricer controller. As that Class has no actual loop and this method executes pretty often, pricer
            // controller itself has some checks whether it should run on method call or not
            PRICER_CONTROLLER.run();
        }
    }

    private void checkMonitor() {
        //  Name: checkMonitor()
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Sleeps until monitor object is notified?

        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public void stopController() {
        //  Name: stopController()
        //  Date created: 11.12.2017
        //  Last modified: 21.12.2017
        //  Description: Stops all running workers and this object's process

        flagLocalRun = false;

        // Loop though every worker and call stop method
        for (Worker worker : workerList) {
            worker.stopWorker();
        }

        // Wake the monitor, allowing it to exit its loop
        synchronized (getMonitor()) {
            getMonitor().notify();
        }
    }

    ///////////////////////////////////
    // Methods for worker management //
    ///////////////////////////////////

    public void listAllWorkers() {
        //  Name: listAllWorkers()
        //  Date created: 22.11.2017
        //  Last modified: 29.11.2017
        //  Description: Prints out all active workers and their active jobs

        // Loop though every worker and print out its job
        for (Worker workerObject : workerList) {
            System.out.println("    " + workerObject.getIndex() + ": " + workerObject.getJob());
        }
    }

    public void spawnWorkers(int workerCount) {
        //  Name: spawnWorkers()
        //  Date created: 21.11.2017
        //  Last modified: 21.12.2017
        //  Description: Creates <workerCount> amount of new workers

        // Get the next available index
        int nextWorkerIndex = workerList.size();

        // Forbid spawning over limit
        if (nextWorkerIndex + workerCount > CONFIG.workerLimit) {
            System.out.println("[ERROR] Maximum amount of workers is " + CONFIG.workerLimit);
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

    public void fireWorkers(int workerCount) {
        //  Name: fireWorkers()
        //  Date created: 22.11.2017
        //  Last modified: 11.12.2017
        //  Description: Removes <workerCount> amount running workers

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

    public String getLatestChangeID() {
        //  Name: getLatestChangeID()
        //  Date created: 29.11.2017
        //  Last modified: 21.12.2017
        //  Description: Get a changeID that's close to the stack top

        String idOne = downloadChangeID("http://api.poe.ninja/api/Data/GetStats");
        String idTwo = downloadChangeID("http://poe-rates.com/actions/getLastChangeId.php");

        if (Integer.parseInt(idOne.substring(idOne.lastIndexOf('-') + 1, idOne.length())) <
                Integer.parseInt(idTwo.substring(idTwo.lastIndexOf('-') + 1, idTwo.length())))
            return idTwo;
        else
            return idOne;
    }

    private String downloadChangeID(String url) {
        //  Name: downloadChangeID()
        //  Date created: 30.11.2017
        //  Last modified: 19.12.2017
        //  Description: Downloads content of <url> and returns it as String

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

    public String getLocalChangeID() {
        //  Name: getLocalChangeID()
        //  Date created: 19.12.2017
        //  Last modified: 19.12.2017
        //  Description: Gets local ChangeID

        return downloadChangeID("http://api.poe.ovh/ChangeID");
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void setNextChangeID(String newChangeID) {
        //  Name: getLocalChangeID()
        //  Date created: 22.11.2017
        //  Last modified: 21.12.2017
        //  Description: Sets the next change ID in the variable. If the variable has no value, set it to the
        //               newChangeID's one, otherwise compare the two and set the newest

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

    public Object getMonitor() {
        return monitor;
    }
}
