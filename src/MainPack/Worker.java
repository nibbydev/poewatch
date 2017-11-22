package MainPack;

public class Worker extends Thread {
    /*  Name: Worker()
    *   Date created: 21.11.2017
    *   Last modified: 22.11.2017
    *   Description: Contains a worker used to download and parse a batch from the PoE API. Runs in a separate loop.
    *   Example usage: *to be added*
    */

    private boolean flagLocalRun;
    private boolean flagLocalStop;
    public int workerIndex;
    public String job;

    public void inizilize(int workerIndex){
        /*  Name: inizilize()
        *   Date created: 21.11.2017
        *   Last modified: 21.11.2017
        *   Description: Method used to pass arguments to the thread before execution
        */

        this.flagLocalRun = true;
        this.workerIndex = workerIndex;
    }

    public void run() {
        /*  Name: run()
        *   Date created: 21.11.2017
        *   Last modified: 21.11.2017
        *   Description: Contains the main loop of the thread.
        *   Child functions:
        *       downloadMyData()
        */

        // Run until stop flag is raised
        while(!this.flagLocalRun){
            // Check for new jobs
            if(!this.job.equals("")) {
                // Download and parse data according to the changeID.
                downloadMyData(this.job);

                // Empty the job string, indicating this worker is ready for another job
                this.job = "";
            }

            // Somehow sleep for 0.1 seconds
            try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
        }
        this.flagLocalStop = true;
    }

    private void downloadMyData(String lastJob){
        /*  Name: downloadMyData()
        *   Date created: 21.11.2017
        *   Last modified: 21.11.2017
        *   Description: Contains the method that downloads data from the API and then parses it
        *   Parent functions:
        *       run()
        */

        // TODO: this
        System.out.println(lastJob);
    }

    public void stopThisNonsense(){
        /*  Name: stopThisNonsense()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Method used to stop the worker safely
        */

        this.flagLocalRun = false;

        // Wait until process finishes safely
        while(!flagLocalStop){
            try{Thread.sleep(100);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
        }
    }

}
