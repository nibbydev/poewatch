package MainPack;

import java.util.ArrayList;

public class WorkerController {
    /*  Name: WorkerController()
    *   Date created: 21.11.2017
    *   Last modified: 22.11.2017
    *   Description: Object that's used to manage various worker-related tasks
    */

    private ArrayList<Worker> workerList;

    public WorkerController() {
        this.workerList = new ArrayList<>();

    }

    public void spawnWorkers(int workerCount){
        /*  Name: spawnWorkers()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Used to spawn x amount of new threads.
        */

        // Get the next available index
        int nextWorkerIndex = this.workerList.size();

        // Loop through creation
        for (int i = nextWorkerIndex; i < nextWorkerIndex + workerCount; i++) {
            Worker newWorkerObject = new Worker();

            // Set some worker properties
            newWorkerObject.setDaemon(true);
            newWorkerObject.inizilize(i);
            newWorkerObject.job = "";

            // Start the worker
            newWorkerObject.start();

            // Add worker to list
            this.workerList.add(newWorkerObject);
        }

    }

    public void stopAllWorkers(){
        /*  Name: stopAllWorkers()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Used to stop all running workers
        */

        // Loop though every worker and call the stop function
        for (Worker workerObject: this.workerList) {
            workerObject.stopThisNonsense();
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
            System.out.println("[Error] Not enough active workers");
            return;
        }

        // TODO: refine this. A normal remove() should also work now
        // Loop through removal
        for (int i = lastWorkerIndex; i > lastWorkerIndex - workerCount; i--) {
            lastWorker = workerList.get(i);
            lastWorker.stopThisNonsense();
            workerList.remove(lastWorker);
        }
    }
}
