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
        workerCount = askUserForIntInputWithValidation();
        // Spawn x amount of workers
        workerController.spawnWorkers(workerCount);

        // Allow user to have some control over the flow of the program
        commandLoop(workerController);

        // Stop workers on exit
        workerController.stopAllWorkers();

    }

    private static int askUserForIntInputWithValidation(){
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

        System.out.print("How many threads should be spawned?\n> ");
        while(userInput < 0 || userInput > 10){
            userInput = TextIO.getlnInt();

            if(userInput > 10)
                System.out.print("That is way too many threads, pick something reasonable!\n> ");
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
                    helpString += "    lw - list all workers";
                    helpString += "    fire - fire 1 worker";
                    helpString += "    hire - hire 1 worker";
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
                    System.out.println("[INFO] How many to remove?");
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
