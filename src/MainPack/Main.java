package MainPack;

import MainPack.PricerClasses.PricerController;
import MainPack.StatClasses.StatController;
import NotMadeByMe.TextIO;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        /*   Name: main()
         *   Date created: 21.11.2017
         *   Last modified: 27.11.2017
         *   Description: The main main main. Run this to run the script
         */

        ArrayList<String> searchParameters = new ArrayList<>();
        WorkerController workerController = new WorkerController();
        StatController statController = new StatController();
        PricerController pricerController = new PricerController();

        // Set default values and start the worker controller
        workerController.setSearchParameters(searchParameters);
        workerController.setStatController(statController);
        workerController.setPricerController(pricerController);
        workerController.setWorkerLimit(5);
        workerController.start();

        // Set default values and start the pricer controller
        pricerController.start();

        // Ask the user how many workers should be spawned
        int workerCount = askUserForIntInputWithValidation(workerController);
        workerController.spawnWorkers(workerCount);

        // Allow user to have some control over the flow of the program
        commandLoop(workerController, searchParameters, statController);

        // Stop workers on exit
        workerController.stopAllWorkers();
        workerController.setFlagLocalRun(false);
        pricerController.setFlagLocalRun(false);
    }

    private static int askUserForIntInputWithValidation(WorkerController workerController) {
        /*  Name: askUserForIntInputWithValidation()
        *   Date created: 21.11.2017
        *   Last modified: 27.11.2017
        *   Description: Asks the user for an input, has validation so that the input is actually valid
        *   Parent methods:
        *       main()
        */

        int userInput = -1;

        System.out.println("How many workers should be spawned (1 - " + workerController.getWorkerLimit() + ")?");
        while (userInput <= 0 || userInput > workerController.getWorkerLimit()) {
            userInput = TextIO.getlnInt();

            if (userInput > workerController.getWorkerLimit())
                System.out.println("That is way too many workers!");
        }
        return userInput;
    }

    private static void commandLoop(WorkerController workerController, ArrayList<String> searchParameters, StatController statController) {
        /*  Name: commandLoop()
        *   Date created: 22.11.2017
        *   Last modified: 27.11.2017
        *   Description: Command loop method. Allows the user some interaction with the script as it is running.
        *   Parent methods:
        *       main()
        */

        String helpString = "[INFO] Available commands include:\n";
        String[] userInput;

        helpString += "    help - display this help page\n";
        helpString += "    exit - exit the script safely\n";
        helpString += "    worker - manage workers\n";
        helpString += "    search - manage search parameters \n";
        helpString += "    id - add a start changeID\n";
        helpString += "    stats - manage statistical information\n";

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
                case "id":
                    commandIdAdd(workerController, userInput);
                    break;
                case "worker":
                    commandWorker(workerController, userInput);
                    break;
                case "search":
                    commandSearch(searchParameters, userInput);
                    break;
                case "stats":
                    commandStats(statController, userInput);
                    break;
                default:
                    System.out.println("[ERROR] Unknown command: \"" + userInput[0] + "\". Use \"help\" for help");
                    break;
            }
        }
    }

    /*
     * Methods extracted from commandLoop
     */

    private static void commandIdAdd(WorkerController workerController, String[] userInput){
        /*  Name: commandIdAdd()
        *   Date created: 27.11.2017
        *   Last modified: 27.11.2017
        *   Description: Adds a ChangeID to the queue
        */


        String helpString = "[INFO] Available changeID commands:\n";
        helpString += "    'id <string/'default'>' - Add string to job queue\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        if (userInput[1].equalsIgnoreCase("default")) {
            workerController.setNextChangeID("109146384-114458199-107400880-123773152-115750588");
        } else {
            workerController.setNextChangeID(userInput[1]);
        }

        System.out.println("[INFO] New ChangeID added");
    }

    private static void commandWorker(WorkerController workerController, String[] userInput){
        /*  Name: commandWorker()
        *   Date created: 27.11.2017
        *   Last modified: 27.11.2017
        *   Description: Holds commands that have something to do with worker operation
        */

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

    private static void commandSearch(ArrayList<String> searchParameters, String[] userInput){
        /*  Name: commandSearch()
        *   Date created: 27.11.2017
        *   Last modified: 27.11.2017
        *   Description: Holds commands that have something to do with search parameters
        */

        String helpString = "[INFO] Available search commands:\n";
        helpString += "    'search list' - List all active search parameters\n";
        helpString += "    'search del <index>' - Remove search parameter at index <index>\n";
        helpString += "    'search add <string>' - Add search parameter <string> to the list\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        if (userInput[1].equalsIgnoreCase("list")) {
            System.out.println("[INFO] Current search parameters:");
            searchParameters.forEach(i -> System.out.println("[" + searchParameters.indexOf(i) + "] \"" + i + "\""));
        } else if (userInput[1].equalsIgnoreCase("del")) {
            searchParameters.remove(Integer.parseInt(userInput[2]));
            System.out.println("[INFO] Removed [" + Integer.parseInt(userInput[2]) + "] from the list");
        } else if (userInput[1].equalsIgnoreCase("add")) {
            searchParameters.add(userInput[2]);
            System.out.println("[INFO] Added \"" + userInput[2] + "\" to the list");
        } else {
            System.out.println(helpString);
        }
    }

    private static void commandStats(StatController statController, String[] userInput){
        /*  Name: commandStats()
        *   Date created: 27.11.2017
        *   Last modified: 27.11.2017
        *   Description: Holds commands that have something to do with statistics
        */

        String helpString = "[INFO] Available statistics commands:\n";
        helpString += "    'stats list' - List all statistics\n";
        helpString += "    'stats clear' - Zero all gathered statistics\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        if (userInput[1].equalsIgnoreCase("list")) {
            statController.printStats();
        } else if (userInput[1].equalsIgnoreCase("clear")) {
            statController.clearStats();
            System.out.println("[INFO] Statistical info cleared");
        } else {
            System.out.println(helpString);
        }
    }
}
