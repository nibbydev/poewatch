package com.sanderh;

import com.sanderh.Pricer.PricerController;
import NotMadeByMe.TextIO;
import com.sanderh.Worker.WorkerController;

import java.io.*;
import java.util.Calendar;

    /* Solved TODO:
    [x][15.12.17] change: "Superior Ashen Wood" = "Ashen Wood"
    [x][16.12.17] change: replace sleep() with wait/notify
    [x][17.12.17] change: use new API category slot instead of icon for sorting
    [x][18.12.17] add: http://id.poe.ovh/
    [x][18.12.17] change: static php files read data from different directory
    [x][18.12.17] add: generate php + htaccess files
    [x][18.12.17] fix: output means appearing almost exactly as medians
    [x][19.12.17] change: use local changeID
    [x][19.12.17] change: allow count to reach 1000
    [x][19.12.17] change: add counters to output
     */

public class Main {
    public static final ConfigReader CONFIG = new ConfigReader();
    public static final WorkerController WORKER_CONTROLLER = new WorkerController();
    public static final PricerController PRICER_CONTROLLER = new PricerController();
    public static final Statistics STATISTICS = new Statistics();

    public static void main(String[] args) {
        //  Name: main()
        //  Date created: 21.11.2017
        //  Last modified: 18.12.2017
        //  Description: The main class. Run this to run the program

        // Make sure basic folder structure exists
        buildFolderFileStructure();

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
        //  Last modified: 20.12.2017
        //  Description: Asks the user for an input, has validation so that the input is actually valid

        int userInput = -1;
        int workerLimit = CONFIG.getAsInt("workerLimit");

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
        //  Last modified: 18.12.2017
        //  Description: Command loop method. Allows the user some interaction with the script as it is running.

        String helpString = "[INFO] Available commands include:\n"
                + "    help - display this help page\n"
                + "    exit - exit the script safely\n"
                + "    pause - pause item parsing\n"
                + "    worker - manage workers\n"
                + "    id - add a start changeID\n"
                + "    about - show about page\n";
        System.out.println(helpString);

        String[] userInput;
        while (true) {
            userInput = TextIO.getlnString().split(" ");

            switch (userInput[0]) {
                case "help":
                    System.out.println(helpString);
                    break;
                case "exit":
                    System.out.println("[INFO] Shutting down..");
                    STATISTICS.setStatus(1);
                    STATISTICS.writeChangeID();
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

    public static String timeStamp() {
        //  Name: timeStamp()
        //  Date created: 06.12.2017
        //  Last modified: 20.12.2017
        //  Description: Returns time in the format of [HH:MM]

        StringBuilder stringBuilder = new StringBuilder();

        // Refresh calendar (this is a bug with Calendar, I've heard)
        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY) + CONFIG.getAsInt("timeZoneOffset");
        int minute = calendar.get(Calendar.MINUTE);

        stringBuilder.append("[");

        // Format hour (timezones can be set in Calendar by default, I know)
        if (hour > 24)
            hour -= 24;

        // Add 0 so that "[09:32]" is returned instead of "[9:32]"
        if (hour < 10)
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

    private static void wakeControllers() {
        //  Name: wakeControllers()
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Wakes all objects from sleep

        // Wake price controller
        synchronized (PricerController.getMonitor()) {
            PricerController.getMonitor().notifyAll();
        }
        // Wake worker controller
        synchronized (WorkerController.getMonitor()) {
            WorkerController.getMonitor().notifyAll();
        }
    }

    //////////////////////////
    // File structure setup //
    //////////////////////////

    private static void buildFolderFileStructure() {
        //  Name: buildFolderFileStructure()
        //  Date created: 18.12.2017
        //  Last modified: 20.12.2017
        //  Description: Creates all missing http files and folders on startup

        // Make sure output folders exist
        new File("./http/data").mkdirs();

        // Create ./http/.htaccess if missing
        saveResource("/http/", ".htaccess");

        // Create ./http/ChangeID.php if missing
        saveResource("/http/", "ChangeID.php");

        // Create ./http/index.php if missing
        saveResource("/http/", "index.php");

        // Create ./http/Stats.php if missing
        saveResource("/http/", "Stats.php");

        // Create ./http/data/index.php if missing
        saveResource("/http/data/", "index.php");

        // Create ./config.cfg if missing
        saveResource("/", "config.cfg");
    }

    private static void saveResource(String outputDirectory, String name)  {
        //  Name: saveResource()
        //  Date created: 20.12.2017
        //  Last modified: 20.12.2017
        //  Description: Reads files from the .jar and writes them to filepath

        // Get the current path
        String workingDir = System.getProperty("user.dir");
        File out = new File(workingDir + outputDirectory, name);

        // No real need to overwrite on startup
        if(out.exists())
            return;

        // Define I/O so they can be closed later
        BufferedInputStream reader = null;
        OutputStream writer = null;

        try{
            // Assign I/O
            reader = new BufferedInputStream(Main.class.getResourceAsStream("/com/sanderh/Resource/" + name));
            writer = new BufferedOutputStream(new FileOutputStream(out));

            // Define I/O helpers
            byte[] buffer = new byte[1024];
            int position = 0;
            int length;

            // Read and write at the same time
            while((length = reader.read(buffer, 0, 1024)) > 0) {
                writer.write(buffer, position, length);
                position += length;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();

                if(writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    ////////////////////////////////////////
    // Methods extracted from commandLoop //
    ////////////////////////////////////////

    private static void commandIdAdd(String[] userInput) {
        //  Name: commandIdAdd()
        //  Date created: 27.11.2017
        //  Last modified: 19.12.2017
        //  Description: Adds a ChangeID to the queue

        String helpString = "[INFO] Available changeID commands:\n";
        helpString += "    'id <string>' - Add optional string to job queue\n";
        helpString += "    'id local' - Add last locally used job to queue\n";
        helpString += "    'id new' - Add newest string to job queue (recommended)\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        switch (userInput[1]) {
            case "local":
                WorkerController.setNextChangeID(WORKER_CONTROLLER.getLocalChangeID());
                break;
            case "new":
                WorkerController.setNextChangeID(WORKER_CONTROLLER.getLatestChangeID());
                break;
            default:
                WorkerController.setNextChangeID(userInput[1]);
        }

        wakeControllers();
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

    private static void commandAbout() {
        //  Name: commandAbout()
        //  Date created: 13.12.2017
        //  Last modified: 16.12.2017
        //  Description: Prints about page

        String about = "Project name: PoE stash API JSON statistics generator\n"
                + "Made by: Sander H.\n"
                + "Licenced under MIT licence, 2017\n";
        System.out.println(about);
    }
}
