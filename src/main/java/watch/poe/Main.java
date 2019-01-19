package poe;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Managers.*;
import poe.Item.ItemParser;
import poe.Worker.Worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    private static AccountManager accountManager;
    private static WorkerManager workerManager;
    private static Database database;
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    private static Config config;

    /**
     * App entry point
     *
     * @param args CLI args
     */
    public static void main(String[] args) {
        boolean success;

        logger.info("Starting PoeWatch");

        try {
            // Load config
            config = ConfigFactory.load("config");

            // Initialize database connector
            database = new Database(config);
            success = database.connect();
            if (!success) {
                logger.error("Could not connect to database");
                return;
            }

            // Set static DB accessor in honour of spaghetti code
            PriceManager.setDatabase(database);

            // Init league manager
            LeagueManager leagueManager = new LeagueManager(database, config);
            success = leagueManager.cycle();
            if (!success) {
                logger.error("Could not get a list of leagues");
                return;
            }

            // Get category, item and currency data
            RelationManager relations = new RelationManager(database);
            success = relations.init();
            if (!success) {
                logger.error("Could not get relations");
                return;
            }

            accountManager = new AccountManager(database);
            workerManager = new WorkerManager(leagueManager, relations, accountManager, database, config);

            // Get all distinct stash ids that are in the db
            success = database.init.getStashIds(Worker.getDbStashes());
            if (!success) {
                logger.error("Could not get active stash IDs");
                return;
            }

            ItemParser.setConfig(config);
            ItemParser.setRelationManager(relations);
            ItemParser.setWorkerManager(workerManager);

            // Parse CLI parameters
            success = parseCommandParameters(args);
            if (!success) return;

            // Start controllers
            accountManager.start();
            workerManager.start();

            // Initiate main command loop, allowing user some control over the program
            commandLoop();
        } finally {
            if (accountManager != null) accountManager.stopController();
            if (workerManager != null) workerManager.stopController();
            if (database != null) database.disconnect();
        }
    }

    /**
     * Checks CLI parameters
     *
     * @param args Passed CLI args
     * @return False if app should exit
     */
    private static boolean parseCommandParameters(String[] args) {
        ArrayList<String> newArgs = new ArrayList<>(Arrays.asList(args));

        if (!newArgs.contains("-workers")) {
            workerManager.spawnWorkers(config.getInt("worker.defaultCount"));
            System.out.println("[INFO] Spawned 3 workers");
        }

        if (!newArgs.contains("-id")) {
            String changeId = database.init.getChangeID();

            if (changeId == null) {
                System.out.println("[ERROR] Local ChangeID not found");
                changeId = workerManager.getLatestChangeID();
            }

            if (changeId == null) {
                System.out.println("[ERROR] Could not get a change id");
                return false;
            }

            workerManager.setNextChangeID(changeId);
            System.out.println("[INFO] ChangeID (" + changeId + ") added");
        }

        for (String arg : newArgs) {
            if (!arg.startsWith("-"))
                continue;

            switch (arg) {
                case "-workers":
                    workerManager.spawnWorkers(Integer.parseInt(newArgs.get(newArgs.lastIndexOf(arg) + 1)));
                    System.out.println("[INFO] Spawned " + newArgs.get(newArgs.lastIndexOf(arg) + 1) + " workers");
                    break;
                case "-id":
                    String changeId = newArgs.get(newArgs.lastIndexOf(arg) + 1);
                    workerManager.setNextChangeID(changeId);
                    System.out.println("[INFO] New ChangeID (" + changeId + ") added");
                    break;
                default:
                    System.out.println("[ERROR] Unknown CLI parameter: " + arg);
                    break;
            }
        }

        return true;
    }

    /**
     * Main loop. Allows for some primitive command input through the console
     */
    private static void commandLoop() {
        String helpString = "[INFO] Available commands include:\n"
                + "    help - display this help page\n"
                + "    exit - exit the script safely\n"
                + "    worker - manage workers\n"
                + "    acc - account manager commands\n"
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
                    case "worker":
                        commandWorker(userInput);
                        break;
                    case "about":
                        commandAbout();
                        break;
                    case "acc":
                        commandAcc(userInput);
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
            workerManager.printAllWorkers();
        } else if (userInput[1].equalsIgnoreCase("del")) {
            System.out.println("[INFO] Removing " + userInput[2] + " worker..");
            workerManager.fireWorkers(Integer.parseInt(userInput[2]));
        } else if (userInput[1].equalsIgnoreCase("add")) {
            System.out.println("[INFO] Adding " + userInput[2] + " worker..");
            workerManager.spawnWorkers(Integer.parseInt(userInput[2]));
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
}
