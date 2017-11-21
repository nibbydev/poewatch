package MainPack;

import NotMadeByMe.TextIO;

public class Main {
    public static void main(String[] args) {
        /*  Name: main()
        *   Date created: 21.11.2017
        *   Last modified: 21.11.2017
        *   Description: The main main main. Run this to run the script
        */

        WorkerController workerController = new WorkerController();
        int workerCount;

        // Ask the user how many workers should we spawn
        workerCount = askUserForIntInputWithValidation();
        // Spawn x amount of workers
        workerController.spawnWorkers(workerCount);

        // Placeholder "while" loop
        askUserForIntInputWithValidation();

        // Stop workers on exit
        workerController.stopAllWorkers();

    }

    private static int askUserForIntInputWithValidation(){
        /*  Name: askUserForIntInputWithValidation()
        *   Date created: 21.11.2017
        *   Last modified: 21.11.2017
        *   Description: Asks the user for an input, has validation so that the input is actually valid
        *   Parent functions:
        *       main()
        */

        int userInput = -1;

        // TODO: validation that input is valid int
        while(userInput < 0){
            System.out.println("How many threads should be spawned?\n> ");
            userInput = TextIO.getlnInt();
        }

        return userInput;
    }
}
