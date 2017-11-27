package MainPack;

import MainPack.StatClasses.StatController;

import java.util.ArrayList;

public class WorkerController extends Thread {
    /*   Name: WorkerController()
     *   Date created: 21.11.2017
     *   Last modified: 26.11.2017
     *   Description: Object that's used to manage various worker-related tasks
     */

    private ArrayList<Worker> workerList = new ArrayList<>();
    private int workerLimit = 5;
    private boolean flagLocalRun = true;
    private String nextChangeID = "";
    private ArrayList<String> searchParameters;
    private StatController statController;

    /*
     * Methods that get/set values from outside the class
     */

    public void setFlagLocalRun(boolean flagLocalRun) {
        this.flagLocalRun = flagLocalRun;
    }

    public void setWorkerLimit(int workerLimit) {
        this.workerLimit = workerLimit;
    }

    public void setNextChangeID(String nextChangeID) {
        this.nextChangeID = nextChangeID;
    }

    public void setSearchParameters(ArrayList<String> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public int getWorkerLimit() {
        return workerLimit;
    }

    public void setStatController(StatController statController) {
        this.statController = statController;
    }

    /*
     * Methods that actually do something
     */

    public void run() {
        /*  Name: run()
        *   Date created: 22.11.2017
        *   Last modified: 23.11.2017
        *   Description: Main loop that assigns jobs to workers
        */

        // Run main loop while flag is up
        while (this.flagLocalRun) {
            // Sleep for 100ms
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Check if nextChangeID has a value
            if (this.nextChangeID.equals("")) {
                // Loop through every worker as there's a new job to be given out
                for (Worker worker : this.workerList) {
                    // Check if a worker has a job available
                    if (!worker.getNextChangeID().equals("")) {
                        // Copy the new job over to the local variable, which will be assigned to a worker on the next
                        // iteration of the while loop
                        this.nextChangeID = worker.getNextChangeID();
                        worker.setNextChangeID("");
                        break;
                    }
                }
            } else {
                // Loop through every worker to check if any of them have found a new job
                for (Worker worker : this.workerList) {
                    // Check if the current worker has no active job
                    if (worker.getJob().equals("")) {
                        // Give the job to the worker
                        worker.setJob(this.nextChangeID);
                        // Remove the job that was just given out
                        this.nextChangeID = "";
                        break;
                    }
                }
            }
        }
    }

    public void stopAllWorkers() {
        /*  Name: stopAllWorkers()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Used to stop all running workers
        */

        // Loop though every worker and raise the stop flag
        for (Worker workerObject : this.workerList) {
            workerObject.setFlagLocalRun(false);
        }

        this.workerList.clear();

    }

    public void listAllWorkers() {
        /*  Name: listAllWorkers()
        *   Date created: 22.11.2017
        *   Last modified: 23.11.2017
        *   Description: Prints out all active workers and their active jobs
        */

        // Loop though every worker and print out its job
        for (Worker workerObject : this.workerList) {
            System.out.println("    " + workerObject.getIndex() + ": " + workerObject.getJob());
        }
    }

    public void spawnWorkers(int workerCount) {
        /*  Name: spawnWorkers()
        *   Date created: 21.11.2017
        *   Last modified: 26.11.2017
        *   Description: Used to create x amount of new workers
        */

        // Get the next available index
        int nextWorkerIndex = this.workerList.size();

        // Forbid spawning over limit
        if (nextWorkerIndex + workerCount > this.workerLimit) {
            System.out.println("[ERROR] Maximum amount of workers is " + this.workerLimit);
            return;
        }

        // Loop through creation
        for (int i = nextWorkerIndex; i < nextWorkerIndex + workerCount; i++) {
            Worker worker = new Worker();

            // Set some worker properties and start
            worker.setSearchParameters(this.searchParameters);
            worker.setStatController(this.statController);
            worker.setIndex(i);
            worker.start();

            // Add worker to local list
            this.workerList.add(worker);
        }
    }

    public void fireWorkers(int workerCount) {
        /*  Name: fireWorkers()
        *   Date created: 22.11.2017
        *   Last modified: 26.11.2017
        *   Description: Used to remove x amount running threads.
        */

        Worker lastWorker;

        // Get the last available index
        int lastWorkerIndex = this.workerList.size() - 1;

        // Can't remove what's not there
        if (lastWorkerIndex <= 0 || lastWorkerIndex - workerCount < 0) {
            System.out.println("[ERROR] Not enough active workers");
            return;
        }

        // Loop through removal
        for (int i = lastWorkerIndex; i > lastWorkerIndex - workerCount; i--) {
            lastWorker = workerList.get(i);
            lastWorker.setFlagLocalRun(false);
            workerList.remove(lastWorker);
        }
    }
}
