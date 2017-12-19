package com.sanderh.Worker;

import com.sanderh.Mappers;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import static com.sanderh.Main.PROPERTIES;

public class WorkerController extends Thread {
    //  Name: WorkerController
    //  Date created: 21.11.2017
    //  Last modified: 17.12.2017
    //  Description: Object that's used to manage worker objects

    private static final int workerLimit = Integer.parseInt(PROPERTIES.getProperty("workerLimit"));
    private static boolean flagLocalRun = true;
    private static ArrayList<Worker> workerList = new ArrayList<>();
    private static final ArrayList<String> nextChangeIDs = new ArrayList<>();
    private static final Object monitor = new Object();

    /////////////////////////////
    // Actually useful methods //
    /////////////////////////////

    public void run() {
        //  Name: run()
        //  Date created: 22.11.2017
        //  Last modified: 16.12.2017
        //  Description: Assigns jobs to workers

        // Run main loop while flag is up
        while (flagLocalRun) {
            checkMonitor();

            // While there's a job that needs to be given out
            while (!nextChangeIDs.isEmpty() && flagLocalRun) {
                // Loop through workers
                for (Worker worker : workerList) {
                    // Check if worker is free
                    if (worker.hasJob()) continue;
                    // Give the LATEST job to the worker
                    worker.setJob(nextChangeIDs.get(nextChangeIDs.size() - 1));
                    // Remove all older jobs from the list
                    nextChangeIDs.clear();
                    // Wake the worker so it can start working on the job
                    wakeWorkerMonitor(worker);
                    break;
                }

                // Wait for a moment if all workers are busy
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
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

    private void wakeWorkerMonitor(Worker worker) {
        //  Name: wakeWorkerMonitor()
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Wakes Worker

        synchronized (worker.getMonitor()) {
            worker.getMonitor().notifyAll();
        }
    }

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
        //  Last modified: 11.12.2017
        //  Description: Creates <workerCount> amount of new workers

        // Get the next available index
        int nextWorkerIndex = workerList.size();

        // Forbid spawning over limit
        if (nextWorkerIndex + workerCount > workerLimit) {
            System.out.println("[ERROR] Maximum amount of workers is " + workerLimit);
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

    public void stopController() {
        //  Name: stopController()
        //  Date created: 11.12.2017
        //  Last modified: 16.12.2017
        //  Description: Stops all running workers and this object's process

        flagLocalRun = false;

        // Loop though every worker and call stop method
        for (Worker worker : workerList) {
            worker.stopWorker();
            wakeWorkerMonitor(worker);
        }

        workerList.clear();
    }

    //////////////////////////////////////////////////////////
    // Methods for getting a ChangeID from external sources //
    //////////////////////////////////////////////////////////

    public String getLatestChangeID() {
        //  Name: getLatestChangeID()
        //  Date created: 29.11.2017
        //  Last modified: 19.12.2017
        //  Description: Get a changeID that's close to the stack top

        String idOne = downloadChangeID("http://api.poe.ninja/api/Data/GetStats");
        String idTwo = downloadChangeID("http://poe-rates.com/actions/getLastChangeId.php");

        return compareChangeIDs(idOne, idTwo);
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

    private String compareChangeIDs(String idOne, String idTwo) {
        //  Name: compareChangeIDs()
        //  Date created: 30.11.2017
        //  Last modified: 30.11.2017
        //  Description: Compares changeIDs and returns the newest

        int countOne = 0;
        int countTwo = 0;

        // I'd rather do lambda at this point but I couldn't understand the syntax for it
        for (String s : idOne.split("-")) {
            countOne += Integer.parseInt(s);
        }

        for (String s : idTwo.split("-")) {
            countOne += Integer.parseInt(s);
        }

        if (countOne > countTwo)
            return idOne;
        else
            return idTwo;
    }

    public String getLocalChangeID(){
        //  Name: getLocalChangeID()
        //  Date created: 19.12.2017
        //  Last modified: 19.12.2017
        //  Description: Gets local ChangeID

        return downloadChangeID("http://api.poe.ovh/ChangeID");
    }

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public static void setNextChangeID(String newChangeID) {
        nextChangeIDs.add(newChangeID);
    }

    public static Object getMonitor() {
        return monitor;
    }
}
