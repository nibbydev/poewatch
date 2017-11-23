package MainPack;

import java.util.ArrayList;

public class WorkerController extends Thread {
    /*  Name: WorkerController()
    *   Date created: 21.11.2017
    *   Last modified: 22.11.2017
    *   Description: Object that's used to manage various worker-related tasks
    */

    private ArrayList<Worker> workerList;
    public int maxNrOfWorkers;
    private boolean flagLocalRun;
    private String nextChangeID;
    private boolean flagLocalStop;

    public WorkerController() {
        this.workerList = new ArrayList<>();
        this.maxNrOfWorkers = 7;
        this.flagLocalRun = true;
        this.flagLocalStop = false;
        this.nextChangeID = "";
    }

    public void run(){
        /*  Name: run()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Main loop that assigns jobs to workers
        */

        // Run main loop while flag is up
        while(this.flagLocalRun){
            try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();}

            // this.nextChangeID has a value if one of the workers found a new job
            if(this.nextChangeID.equals("")) {
                // Loop through every worker
                for (Worker worker: this.workerList) {
                    // Check if a worker has a job available
                    if (!worker.nextChangeID.equals("")) {
                        // If yes, copy it over to the local variable, which will be assigned to a worker on the next
                        // iteration of the upper while loop
                        this.nextChangeID = worker.nextChangeID;
                        worker.nextChangeID = "";
                        break;
                    }
                }
            } else {
                // Loop through every worker
                for (Worker worker: this.workerList) {
                    // Check if the worker has no job
                    if (worker.job.equals("")){
                        // Give the worker the job
                        worker.addJob(this.nextChangeID);
                        // Clear the variable, indicating no free jobs
                        this.nextChangeID = "";
                        break;
                    }
                }
            }
        }

        // If we got to this point, that means we should exit the WorkerController
        this.flagLocalStop = true;
    }

    public void stopWorkerController(){
        /*  Name: stopWorkerController()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Method used to stop the worker controller safely
        */

        this.flagLocalRun = false;

        // Wait until process finishes safely
        while(!this.flagLocalStop)
            try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
    }

    public void stopAllWorkers(){
        /*  Name: stopAllWorkers()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Used to stop all running workers
        */

        // Loop though every worker and call the stop function
        for (Worker workerObject: this.workerList) {
            workerObject.stopWorker();
        }

        this.workerList.clear();

    }

    public void listAllWorkers(){
        /*  Name: listAllWorkers()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Prints out all active workers
        */

        // Loop though every worker and call the stop function
        for (Worker workerObject: this.workerList) {
            System.out.println("    " + workerObject.workerIndex + ": " + workerObject.job);
        }
    }

    public void spawnWorkers(int workerCount){
        /*  Name: spawnWorkers()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Used to spawn x amount of new threads.
        */

        // Get the next available index
        int nextWorkerIndex = this.workerList.size();

        // Forbid spawning more than our max
        if(nextWorkerIndex + workerCount > 10){
            System.out.println("[ERROR] Maximum amount of workers is: " + this.maxNrOfWorkers);
            return;
        }

        // Loop through creation
        for (int i = nextWorkerIndex; i < nextWorkerIndex + workerCount; i++) {
            Worker newWorkerObject = new Worker(i);

            // Set some worker properties and start
            newWorkerObject.setDaemon(true);
            newWorkerObject.start();

            // Add worker to list
            this.workerList.add(newWorkerObject);
        }

        // DEV. add a job TODO: remove this!!
        this.workerList.get(0).addJob("109146384-114458199-107400880-123773152-115750588");

    }

    public void fireWorkers(int workerCount){
        /*  Name: fireWorkers()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Used to remove x amount running threads.
        */

        Worker lastWorker;

        // Get the last available index
        int lastWorkerIndex = this.workerList.size() - 1;

        // Can't remove what's not there
        if(lastWorkerIndex <= 0 || lastWorkerIndex - workerCount <= 0){
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
}
