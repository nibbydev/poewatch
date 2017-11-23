package MainPack;

import NotMadeByMe.TextIO;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        /*   Name: main()
         *   Date created: 21.11.2017
         *   Last modified: 23.11.2017
         *   Description: The main main main. Run this to run the script
         */

        ArrayList<String> searchParameters = new ArrayList<>();
        WorkerController workerController = new WorkerController();
        int workerCount;

        // Set default values and start the controller
        workerController.setSearchParameters(searchParameters);
        workerController.setWorkerLimit(7);
        workerController.setDaemon(true);
        workerController.start();

        // Ask the user how many workers should be spawned
        workerCount = askUserForIntInputWithValidation(workerController);
        workerController.spawnWorkers(workerCount);

        // Ask the user a start ChangeID

        // Allow user to have some control over the flow of the program
        commandLoop(workerController, searchParameters);

        // Stop workers on exit
        workerController.stopAllWorkers();
        workerController.setFlagLocalRun(false);
    }

    private static int askUserForIntInputWithValidation(WorkerController workerController) {
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

        System.out.println("How many workers should be spawned (1 - " + workerController.getWorkerLimit() + ")?");
        while (userInput <= 0 || userInput > workerController.getWorkerLimit()) {
            userInput = TextIO.getlnInt();

            if (userInput > workerController.getWorkerLimit())
                System.out.println("That is way too many workers!");
        }
        return userInput;
    }

    private static void commandLoop(WorkerController workerController, ArrayList searchParameters) {
        /*  Name: commandLoop()
        *   Date created: 22.11.2017
        *   Last modified: 23.11.2017
        *   Description: Command loop method. Allows the user some interaction with the script as it is running.
        *   Parent methods:
        *       main()
        */

        StringBuilder helpString = new StringBuilder();
        String userInputString;
        int userInputInt;

        helpString.append("[INFO] Available commands include:\n");
        helpString.append("    'help' - display this help page\n");
        helpString.append("    'exit' - exit the script safely\n");
        helpString.append("    'w list' - list all workers\n");
        helpString.append("    'w hire' - hire 1 worker\n");
        helpString.append("    'w fire' - fire 1 worker\n");
        helpString.append("    'search list' - list all active searches\n");
        helpString.append("    'search add' - add new search string\n");
        helpString.append("    'search del' - remove existing search string\n");
        helpString.append("    'id add' - add a start changeID\n");

        System.out.println(helpString.toString());

        while (true) {
            userInputString = TextIO.getlnString();

            switch (userInputString) {
                case "help":
                    System.out.println(helpString.toString());
                    break;

                case "exit":
                    System.out.println("[INFO] Shutting down..");
                    return;

                case "id add":
                    System.out.println("[INFO] Enter start ChangeID (leave empty to input the default):");
                    userInputString = TextIO.getlnString();

                    if (userInputString.equals("")) {
                        workerController.setNextChangeID("109146384-114458199-107400880-123773152-115750588");
                    } else {
                        workerController.setNextChangeID(userInputString);
                        System.out.println("[INFO] Added \"" + userInputString + "\" to the list");
                    }
                    break;

                case "w list":
                    System.out.println("[INFO] List of active Workers:");
                    workerController.listAllWorkers();
                    break;

                case "w fire":
                    System.out.println("[INFO] How many to remove?");
                    userInputInt = TextIO.getlnInt();

                    System.out.println("[INFO] Removing " + userInputInt + " worker..");
                    workerController.fireWorkers(userInputInt);
                    break;

                case "w hire":
                    System.out.println("[INFO] How many to employ?");
                    userInputInt = TextIO.getlnInt();

                    System.out.println("[INFO] Adding " + userInputInt + " worker..");
                    workerController.spawnWorkers(userInputInt);
                    break;

                case "search add":
                    System.out.println("[INFO] Enter search string (leave empty to cancel)");
                    userInputString = TextIO.getlnString();

                    if (userInputString.equals("")) {
                        System.out.println("[INFO] Cancelling");
                    } else {
                        searchParameters.add(userInputString);
                        System.out.println("[INFO] Added \"" + userInputString + "\" to the list");
                    }
                    break;

                case "search list":
                    System.out.println("[INFO] Current search parameters:");
                    searchParameters.forEach(i -> System.out.println("[" + searchParameters.indexOf(i) + "] \"" + i + "\""));
                    break;

                case "search del":
                    System.out.println("[INFO] Current searches:");
                    searchParameters.forEach(i -> System.out.println("[" + searchParameters.indexOf(i) + "] \"" + i + "\""));

                    System.out.println("[INFO] Insert index to remove");
                    userInputInt = TextIO.getlnInt();

                    if (searchParameters.size() == 0 || userInputInt > searchParameters.size() || userInputInt < 0) {
                        System.out.println("[INFO] Invalid input");
                        break;
                    }

                    searchParameters.remove(userInputInt);
                    System.out.println("[INFO] Removed [" + userInputString + "] from the list");
                    break;

                default:
                    System.out.println("[ERROR] Unknown command: \"" + userInputString + "\". Use \"help\" for help");
                    break;
            }
        }
    }
}
