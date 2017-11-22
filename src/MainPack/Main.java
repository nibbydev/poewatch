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

        // Placeholder "while" loop
        System.out.println("Press any key to exit");
        TextIO.getln();

        // Stop workers on exit
        workerController.stopAllWorkers();

    }

    private static int askUserForIntInputWithValidation(){
        /*  Name: askUserForIntInputWithValidation()
        *   Date created: 21.11.2017
        *   Last modified: 22.11.2017
        *   Description: Asks the user for an input, has validation so that the input is actually valid
        *   Parent functions:
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
}
