package MainPack;

import MainPack.PricerClasses.PricerController;
import NotMadeByMe.TextIO;

public class Main {
    public static void main(String[] args) {
        //  Name: main()
        //  Date created: 21.11.2017
        //  Last modified: 30.11.2017
        //  Description: The main class. Run this to run the program

        WorkerController workerController = new WorkerController();
        PricerController pricerController = new PricerController();

        // Set default values and start the worker controller
        workerController.setPricerController(pricerController);
        workerController.setWorkerLimit(5);
        workerController.start();

        // Set default values and start the pricer controller
        pricerController.setSleepLength(1);
        pricerController.start();

        // Ask the user how many workers should be spawned and spawn tem
        workerController.spawnWorkers(askUserForIntInputWithValidation(workerController));

        // Initiate main command loop, allowing user some control over the program
        commandLoop(workerController, pricerController);

        // Stop workers on exit
        workerController.stopAllWorkers();
        workerController.setFlagLocalRun(false);
        pricerController.setFlagLocalRun(false);
    }

    private static int askUserForIntInputWithValidation(WorkerController workerController) {
        //  Name: askUserForIntInputWithValidation()
        //  Date created: 21.11.2017
        //  Last modified: 29.11.2017
        //  Description: Asks the user for an input, has validation so that the input is actually valid
        //  Parent methods:
        //      main()

        int userInput = -1;

        System.out.println("How many workers should be spawned (1 - " + workerController.getWorkerLimit() + ")?");

        while (userInput <= 0 || userInput > workerController.getWorkerLimit()) {
            userInput = TextIO.getlnInt();

            if (userInput > workerController.getWorkerLimit())
                System.out.println("That is way too many workers!");
        }

        return userInput;
    }

    private static void commandLoop(WorkerController workerController, PricerController pricerController) {
        //  Name: commandLoop()
        //  Date created: 22.11.2017
        //  Last modified: 30.11.2017
        //  Description: Command loop method. Allows the user some interaction with the script as it is running.
        //  Parent methods:
        //      main()

        String helpString = "[INFO] Available commands include:\n";
        String[] userInput;

        helpString += "    help - display this help page\n";
        helpString += "    exit - exit the script safely\n";
        helpString += "    pause - pause item parsing\n";
        helpString += "    worker - manage workers\n";
        helpString += "    id - add a start changeID\n";
        helpString += "    data - prints out all gathered data\n";

        System.out.println(helpString);

        while (true) {
            userInput = TextIO.getlnString().split(" ");

            switch (userInput[0]) {
                case "help":
                    System.out.println(helpString);
                    break;
                case "exit":
                    System.out.println("[INFO] Shutting down..");
                    return;
                case "pause":
                    commandPause(pricerController);
                    break;
                case "id":
                    commandIdAdd(workerController, userInput);
                    break;
                case "worker":
                    commandWorker(workerController, userInput);
                    break;
                case "data":
                    pricerController.devPrintData();
                    break;
                default:
                    System.out.println("[ERROR] Unknown command: \"" + userInput[0] + "\". Use \"help\" for help");
                    break;
            }
        }
    }

    ////////////////////////////////////////
    // Methods extracted from commandLoop //
    ////////////////////////////////////////

    private static void commandIdAdd(WorkerController workerController, String[] userInput) {
        //  Name: commandIdAdd()
        //  Date created: 27.11.2017
        //  Last modified: 29.11.2017
        //  Description: Adds a ChangeID to the queue


        String helpString = "[INFO] Available changeID commands:\n";
        helpString += "    'id <string>' - Add optional string to job queue\n";
        helpString += "    'id default - Add middle-ground string to job queue\n";
        helpString += "    'id new - Add newest string to job queue (recommended)\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        switch (userInput[1]){
            case "default":
                workerController.setNextChangeID("109146384-114458199-107400880-123773152-115750588");
                break;
            case "new":
                workerController.setNextChangeID(workerController.getLatestChangeID());
                break;
            default:
                workerController.setNextChangeID(userInput[1]);
        }

        System.out.println("[INFO] New ChangeID added");
    }

    private static void commandWorker(WorkerController workerController, String[] userInput) {
        //  Name: commandWorker()
        //  Date created: 27.11.2017
        //  Last modified: 29.11.2017
        //  Description: Holds commands that have something to do with worker operation

        String helpString = "[INFO] Available worker commands:\n";
        helpString += "    'worker list' - List all active workers\n";
        helpString += "    'worker del <count>' - Remove <count> amount of workers\n";
        helpString += "    'worker add <count>' - Add <count> amount of workers\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        if (userInput[1].equalsIgnoreCase("list")) {
            System.out.println("[INFO] List of active Workers:");
            workerController.listAllWorkers();
        } else if (userInput[1].equalsIgnoreCase("del")) {
            System.out.println("[INFO] Removing " + userInput[2] + " worker..");
            workerController.fireWorkers(Integer.parseInt(userInput[2]));
        } else if (userInput[1].equalsIgnoreCase("add")) {
            System.out.println("[INFO] Adding " + userInput[2] + " worker..");
            workerController.spawnWorkers(Integer.parseInt(userInput[2]));
        } else {
            System.out.println(helpString);
        }
    }

    private static void commandPause(PricerController pricerController) {
        //  Name: commandIdAdd()
        //  Date created: 27.11.2017
        //  Last modified: 29.11.2017
        //  Description: Pauses or resumes the script

        if(pricerController.isFlagPause()){
            pricerController.setFlagPause(false);
            System.out.println("[INFO] Resumed");
        } else {
            pricerController.setFlagPause(true);
            System.out.println("[INFO] Paused");
        }
    }

}
