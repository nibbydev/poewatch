package MainPack;

import NotMadeByMe.TextIO;

public class Main {
    public static void main(String[] args) {
        /*  Name: main()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: The main main main. Run this to run the script
        */

        WorkerController workerController = new WorkerController();
        int workerCount;

        // Ask the user how many workers should we spawn
        workerCount = askUserForIntInputWithValidation(workerController);
        // Spawn x amount of workers
        workerController.spawnWorkers(workerCount);

        // Start the "job office"
        workerController.setDaemon(true);
        workerController.start();

        // Allow user to have some control over the flow of the program
        commandLoop(workerController);

        // Stop workers on exit
        workerController.stopAllWorkers();
        workerController.stopWorkerController();
    }

    private static int askUserForIntInputWithValidation(WorkerController workerController){
        /*  Name: askUserForIntInputWithValidation()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Asks the user for an input, has validation so that the input is actually valid
        *   Parent methods:
        *       main()
        *
        *   :return: Integer indicating how many threads/workers will be spawned
        */

        int userInput = -1;

        System.out.println("How many workers should be spawned (1 - " + workerController.maxNrOfWorkers + ")?");
        while(userInput <= 0 || userInput > workerController.maxNrOfWorkers){
            userInput = TextIO.getlnInt();

            if(userInput > workerController.maxNrOfWorkers)
                System.out.println("That is way too many workers!");
        }
        return userInput;
    }

    private static void commandLoop(WorkerController workerController){
        /*  Name: commandLoop()
        *   Date created: 22.11.2017
        *   Last modified: 22.11.2017
        *   Description: Command loop method. Allows the user some interaction with the script as it is running.
        *   Parent methods:
        *       main()
        */

        String userInputString;
        int userInputInt;

        System.out.println("[INFO] Enter command (type \"help\" for help):");

        while(true){
            userInputString = TextIO.getlnString();

            switch (userInputString) {
                case "help":
                    String helpString = "[INFO] Available commands include:\n";
                    helpString += "    help - display this help page\n";
                    helpString += "    exit - exit the script safely\n";
                    helpString += "    lw - list all workers\n";
                    helpString += "    fire - fire 1 worker\n";
                    helpString += "    hire - hire 1 worker\n";
                    System.out.println(helpString);
                    break;

                case "exit":
                    System.out.println("[INFO] Shutting down..");
                    return;

                case "lw":
                    System.out.println("[INFO] List of active Workers:");
                    workerController.listAllWorkers();
                    break;

                case "fire":
                    System.out.println("[INFO] How many to remove?");
                    userInputInt = TextIO.getlnInt();

                    System.out.println("[INFO] Removing " + userInputInt + " worker..");
                    workerController.fireWorkers(userInputInt);
                    break;

                case "hire":
                    System.out.println("[INFO] How many to employ?");
                    userInputInt = TextIO.getlnInt();

                    System.out.println("[INFO] Adding " + userInputInt + " worker..");
                    workerController.spawnWorkers(userInputInt);
                    break;

                default:
                    System.out.println("[ERROR] Unknown command: \"" + userInputString + "\". Use \"help\" for help");
                    break;
            }
        }
    }
}
