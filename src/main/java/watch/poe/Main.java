package poe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.db.Database;
import poe.manager.account.AccountManager;
import poe.manager.admin.AdminSuite;
import poe.manager.entry.EntryManager;
import poe.manager.league.LeagueManager;
import poe.manager.relation.RelationManager;
import poe.manager.worker.WorkerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private static AccountManager accountManager;
    private static WorkerManager manager;
    private static EntryManager entryManager;
    private static Database database;
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * App entry point
     *
     * @param args CLI args
     */
    public static void main(String[] args) {
        boolean success;

        try {
            // Initialize database connector
            database = new Database();
            success = database.connect();
            if (!success) return;


            // Init admin suite
            AdminSuite adminSuite = new AdminSuite(database);


            // Init league manager
            LeagueManager leagueManager = new LeagueManager(database);
            success = leagueManager.cycle();
            if (!success) return;

            // Get category, item and currency data
            RelationManager relations = new RelationManager(database);
            success = relations.init();
            if (!success) return;

            accountManager = new AccountManager(database);
            manager = new WorkerManager(entryManager, manager, adminSuite);

            // Parse CLI parameters
            parseCommandParameters(args);

            // Start controllers
            entryManager.start();
            manager.start();
            accountManager.start();

            entryManager = new EntryManager(database, manager, leagueManager, accountManager, relations);

            // Initiate main command loop, allowing user some control over the program
            commandLoop();
        } finally {
            if (accountManager != null) accountManager.stopController();
            if (manager != null) manager.stopController();
            if (entryManager != null) entryManager.stopController();
            if (database != null) database.disconnect();
        }
    }

    /**
     * Checks CLI parameters
     *
     * @param args Passed CLI args
     */
    private static void parseCommandParameters(String[] args) {
        ArrayList<String> newArgs = new ArrayList<>(Arrays.asList(args));

        if (!newArgs.contains("-workers")) {
            manager.spawnWorkers(Config.worker_defaultWorkerCount);
            System.out.println("[INFO] Spawned 3 workers");
        }
        if (!newArgs.contains("-id")) {
            manager.setNextChangeID(manager.getLatestChangeID());
            System.out.println("[INFO] New ChangeID added");
        }

        for (String arg : newArgs) {
            if (!arg.startsWith("-"))
                continue;

            switch (arg) {
                case "-workers":
                    manager.spawnWorkers(Integer.parseInt(newArgs.get(newArgs.lastIndexOf(arg) + 1)));
                    System.out.println("[INFO] Spawned " + newArgs.get(newArgs.lastIndexOf(arg) + 1) + " workers");
                    break;
                case "-id":
                    switch (newArgs.get(newArgs.lastIndexOf(arg) + 1)) {
                        case "local":
                            manager.setNextChangeID(manager.getLocalChangeID());
                            System.out.println("[INFO] Local ChangeID added");
                            break;
                        case "new":
                            manager.setNextChangeID(manager.getLatestChangeID());
                            System.out.println("[INFO] New ChangeID added");
                            break;
                        default:
                            manager.setNextChangeID(newArgs.get(newArgs.lastIndexOf(arg) + 1));
                            System.out.println("[INFO] Custom ChangeID added");
                            break;
                    }
                    break;
                default:
                    System.out.println("[ERROR] Unknown CLI parameter: " + arg);
                    break;
            }
        }
    }

    /**
     * Main loop. Allows for some primitive command input through the console
     */
    private static void commandLoop() {
        String helpString = "[INFO] Available commands include:\n"
                + "    help - display this help page\n"
                + "    exit - exit the script safely\n"
                + "    worker - manage workers\n"
                + "    id - add a start changeID\n"
                + "    acc - account manager commands\n"
                + "    moveitem - move inactive item entries to separate table\n"
                + "    about - show about page\n";
        System.out.println(helpString);

        // Define reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        String[] userInput;
        while (true) {
            try {
                userInput = reader.readLine().split(" ");

                switch (userInput[0]) {
                    case "help":
                        System.out.println(helpString);
                        break;
                    case "exit":
                        System.out.println("[INFO] Shutting down..");
                        return;
                    case "id":
                        commandIdAdd(userInput);
                        break;
                    case "worker":
                        commandWorker(userInput);
                        break;
                    case "about":
                        commandAbout();
                        break;
                    case "acc":
                        commandAcc(userInput);
                        break;
                    case "moveitem":
                        commandMoveItem(userInput);
                        break;
                    default:
                        logger.info(String.format("Unknown command: '%s'. Use 'help'.", userInput[0]));
                        break;
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Command loop controllers
    //------------------------------------------------------------------------------------------------------------

    /**
     * Adds a ChangeID to the queue
     *
     * @param userInput The changeID to be added
     */
    private static void commandIdAdd(String[] userInput) {
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
                manager.setNextChangeID(manager.getLocalChangeID());
                System.out.println("[INFO] Local ChangeID added");
                break;
            case "new":
                manager.setNextChangeID(manager.getLatestChangeID());
                System.out.println("[INFO] New ChangeID added");
                break;
            default:
                manager.setNextChangeID(userInput[1]);
                System.out.println("[INFO] Custom ChangeID added");
                break;

        }

        // Wake worker controller
        synchronized (manager.getMonitor()) {
            manager.getMonitor().notifyAll();
        }
    }

    /**
     * Holds commands that have something to do with worker operation
     *
     * @param userInput Input string
     */
    private static void commandWorker(String[] userInput) {
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
            manager.printAllWorkers();
        } else if (userInput[1].equalsIgnoreCase("del")) {
            System.out.println("[INFO] Removing " + userInput[2] + " worker..");
            manager.fireWorkers(Integer.parseInt(userInput[2]));
        } else if (userInput[1].equalsIgnoreCase("add")) {
            System.out.println("[INFO] Adding " + userInput[2] + " worker..");
            manager.spawnWorkers(Integer.parseInt(userInput[2]));
        } else {
            System.out.println(helpString);
        }
    }

    /**
     * Prints about page
     */
    private static void commandAbout() {
        String about = "PoeWatch (http://poe.watch)\n"
                + "Made by: Siegrest\n"
                + "Licenced under AGPL-3.0, 2018\n";
        System.out.println(about);
    }

    private static void commandAcc(String[] userInput) {
        switch (userInput[1]) {
            case "run":
                logger.info("Starting account matching");
                accountManager.checkAccountNameChanges();
                logger.info("Account matching finished");
                break;

            default:
                logger.info(String.format("Unknown command: '%s'. Use 'help'.", userInput[1]));
                break;
        }
    }

    private static void commandMoveItem(String[] userInput) {
        logger.info("Moving inactive item entries to separate table...");
        database.moveInactiveItemEntries();
        logger.info("Moving finished");
    }
}
