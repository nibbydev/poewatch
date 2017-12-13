package com.sanderh;

import com.sanderh.PricerClasses.PricerController;
import NotMadeByMe.TextIO;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

public class Main {
    public static final Properties PROPERTIES = readProperties();
    public static final WorkerController WORKER_CONTROLLER = new WorkerController();
    public static final PricerController PRICER_CONTROLLER = new PricerController();

    public static void main(String[] args) {
        //  Name: main()
        //  Date created: 21.11.2017
        //  Last modified: 12.12.2017
        //  Description: The main class. Run this to run the program

        // Ask the user how many workers should be spawned and spawn them
        WORKER_CONTROLLER.spawnWorkers(askUserForIntInputWithValidation());

        // Set default values and start controllers
        WORKER_CONTROLLER.start();
        PRICER_CONTROLLER.start();

        // Initiate main command loop, allowing user some control over the program
        commandLoop();

        // Stop workers on exit
        WORKER_CONTROLLER.stopController();
        PRICER_CONTROLLER.stopController();
    }

    private static int askUserForIntInputWithValidation() {
        //  Name: askUserForIntInputWithValidation()
        //  Date created: 21.11.2017
        //  Last modified: 11.12.2017
        //  Description: Asks the user for an input, has validation so that the input is actually valid
        //  Parent methods:
        //      main()

        int userInput = -1;
        int workerLimit = Integer.parseInt(PROPERTIES.getProperty("workerLimit"));

        System.out.println("How many workers should be spawned (1 - " + workerLimit + ")?");

        while (userInput <= 0 || userInput > workerLimit) {
            userInput = TextIO.getlnInt();

            if (userInput > workerLimit)
                System.out.println("That is way too many workers!");
        }

        return userInput;
    }

    private static void commandLoop() {
        //  Name: commandLoop()
        //  Date created: 22.11.2017
        //  Last modified: 13.12.2017
        //  Description: Command loop method. Allows the user some interaction with the script as it is running.
        //  Parent methods:
        //      main()

        String[] userInput;

        String helpString = "[INFO] Available commands include:\n"
                + "    help - display this help page\n"
                + "    exit - exit the script safely\n"
                + "    pause - pause item parsing\n"
                + "    worker - manage workers\n"
                + "    id - add a start changeID\n"
                + "    about - show about page\n";
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
                    commandPause();
                    break;
                case "id":
                    commandIdAdd(userInput);
                    break;
                case "worker":
                    commandWorker(userInput);
                    break;
                case "about":
                    commandAbout();
                    break;
                default:
                    System.out.println("[ERROR] Unknown command: \"" + userInput[0] + "\". Use \"help\" for help");
                    break;
            }
        }
    }

    public static String timeStamp(){
        //  Name: timeStamp()
        //  Date created: 06.12.2017
        //  Last modified: 11.12.2017
        //  Description: Returns time in the format of [HH:MM]

        StringBuilder stringBuilder = new StringBuilder();
        int timeZone = Integer.parseInt(PROPERTIES.getProperty("timeZoneOffset"));

        // Refresh calendar (this is a bug with Calendar, I've heard)
        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY) + timeZone;
        int minute = calendar.get(Calendar.MINUTE);

        stringBuilder.append("[");

        // Format hour (timezones can be set in Calendar by default, I know)
        if (hour > 24)
            hour -= 24;

        // Add 0 so that "[09:32]" is returned instead of "[9:32]"
        if(hour < 10)
            stringBuilder.append(0);

        stringBuilder.append(hour);
        stringBuilder.append(":");

        // Format minute
        if (minute < 10)
            stringBuilder.append(0);

        stringBuilder.append(minute);
        stringBuilder.append("]");

        // Return [HH:MM]
        return stringBuilder.toString();
    }

    private static Properties readProperties(){
        //  Name: readProperties()
        //  Date created: 11.12.2017
        //  Last modified: 11.12.2017
        //  Description: Reads in config file from classpath. Probably crashes the program if no valid data is found

        Properties properties = new Properties();
        FileInputStream fileInputStream = null;

        // Writes values from statistics to file
        try {
            fileInputStream = new FileInputStream("./config.properties");
            properties.load(fileInputStream);
        } catch (IOException ex) {
            System.out.println("[ERROR] Could not read properties file:");
            ex.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return properties;
    }

    ////////////////////////////////////////
    // Methods extracted from commandLoop //
    ////////////////////////////////////////

    private static void commandIdAdd(String[] userInput) {
        //  Name: commandIdAdd()
        //  Date created: 27.11.2017
        //  Last modified: 13.12.2017
        //  Description: Adds a ChangeID to the queue


        String helpString = "[INFO] Available changeID commands:\n";
        helpString += "    'id <string>' - Add optional string to job queue\n";
        helpString += "    'id default' - Add middle-ground string to job queue\n";
        helpString += "    'id new' - Add newest string to job queue (recommended)\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        switch (userInput[1]) {
            case "default":
                WorkerController.setNextChangeID(PROPERTIES.getProperty("defaultChangeID"));
                break;
            case "new":
                WorkerController.setNextChangeID(WORKER_CONTROLLER.getLatestChangeID());
                break;
            default:
                WorkerController.setNextChangeID(userInput[1]);
        }

        PRICER_CONTROLLER.setFlagPause(false);
        System.out.println("[INFO] New ChangeID added");
    }

    private static void commandWorker(String[] userInput) {
        //  Name: commandWorker()
        //  Date created: 27.11.2017
        //  Last modified: 11.11.2017
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
            WORKER_CONTROLLER.listAllWorkers();
        } else if (userInput[1].equalsIgnoreCase("del")) {
            System.out.println("[INFO] Removing " + userInput[2] + " worker..");
            WORKER_CONTROLLER.fireWorkers(Integer.parseInt(userInput[2]));
        } else if (userInput[1].equalsIgnoreCase("add")) {
            System.out.println("[INFO] Adding " + userInput[2] + " worker..");
            WORKER_CONTROLLER.spawnWorkers(Integer.parseInt(userInput[2]));
        } else {
            System.out.println(helpString);
        }
    }

    private static void commandPause() {
        //  Name: commandIdAdd()
        //  Date created: 27.11.2017
        //  Last modified: 11.12.2017
        //  Description: Pauses or resumes the script

        if (PRICER_CONTROLLER.isFlagPause()) {
            PRICER_CONTROLLER.setFlagPause(false);
            System.out.println("[INFO] Resumed");
        } else {
            PRICER_CONTROLLER.setFlagPause(true);
            System.out.println("[INFO] Paused");
        }
    }

    private static void commandAbout(){
        //  Name: commandAbout()
        //  Date created: 13.12.2017
        //  Last modified: 13.12.2017
        //  Description: Prints about page

        String about = "Project name: PoE stash API JSON statistics generator\n"
                + "Made by: Sander H. (179900IVSB)\n"
                + "Licenced under MIT licence, 2017\n";
        System.out.println(about);
    }

}
