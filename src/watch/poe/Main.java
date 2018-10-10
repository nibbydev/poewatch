package watch.poe;

import watch.poe.manager.account.AccountManager;
import watch.poe.manager.admin.AdminSuite;
import watch.poe.manager.admin.Flair;
import watch.poe.manager.league.LeagueManager;
import watch.poe.manager.entry.EntryManager;
import watch.poe.manager.relation.RelationManager;
import watch.poe.manager.worker.WorkerManager;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    public static RelationManager RELATIONS;
    public static AccountManager ACCOUNT_MANAGER;
    public static LeagueManager LEAGUE_MANAGER;
    public static WorkerManager WORKER_MANAGER;
    public static EntryManager ENTRY_MANAGER;
    public static AdminSuite ADMIN;
    public static Database DATABASE;

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
            // Init admin suite
            ADMIN = new AdminSuite();

            // Generate config, if missing
            saveResource(Config.resource_config, Config.file_config);

            // Initialize database connector
            DATABASE = new Database();
            success = DATABASE.connect();
            if (!success) return;

            // Init league manager
            LEAGUE_MANAGER = new LeagueManager();
            success = LEAGUE_MANAGER.cycle();
            if (!success) return;

            // Get category, item and currency data
            RELATIONS = new RelationManager();
            success = RELATIONS.init();
            if (!success) return;

            ACCOUNT_MANAGER = new AccountManager();
            WORKER_MANAGER = new WorkerManager();
            ENTRY_MANAGER = new EntryManager();

            // Parse CLI parameters
            parseCommandParameters(args);

            // Start controllers
            ENTRY_MANAGER.start();
            WORKER_MANAGER.start();
            ACCOUNT_MANAGER.start();

            // Initiate main command loop, allowing user some control over the program
            commandLoop();
        } finally {
            if (ACCOUNT_MANAGER != null) ACCOUNT_MANAGER.stopController();
            if (WORKER_MANAGER != null) WORKER_MANAGER.stopController();
            if (ENTRY_MANAGER != null) ENTRY_MANAGER.stopController();
            if (DATABASE != null) DATABASE.disconnect();
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
            WORKER_MANAGER.spawnWorkers(Config.worker_defaultWorkerCount);
            System.out.println("[INFO] Spawned 3 workers");
        }
        if (!newArgs.contains("-id")) {
            WORKER_MANAGER.setNextChangeID(WORKER_MANAGER.getLatestChangeID());
            System.out.println("[INFO] New ChangeID added");
        }

        for (String arg : newArgs) {
            if (!arg.startsWith("-"))
                continue;

            switch (arg) {
                case "-workers":
                    WORKER_MANAGER.spawnWorkers(Integer.parseInt(newArgs.get(newArgs.lastIndexOf(arg) + 1)));
                    System.out.println("[INFO] Spawned " + newArgs.get(newArgs.lastIndexOf(arg) + 1) + " workers");
                    break;
                case "-id":
                    switch (newArgs.get(newArgs.lastIndexOf(arg) + 1)) {
                        case "local":
                            WORKER_MANAGER.setNextChangeID(WORKER_MANAGER.getLocalChangeID());
                            System.out.println("[INFO] Local ChangeID added");
                            break;
                        case "new":
                            WORKER_MANAGER.setNextChangeID(WORKER_MANAGER.getLatestChangeID());
                            System.out.println("[INFO] New ChangeID added");
                            break;
                        default:
                            WORKER_MANAGER.setNextChangeID(newArgs.get(newArgs.lastIndexOf(arg) + 1));
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
                        ADMIN.log(String.format("Unknown command: '%s'. Use 'help'.", userInput[0]), Flair.ERROR);
                        break;
                }
            } catch (IOException ex) {
                Main.ADMIN.logException(ex, Flair.ERROR);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // File structure setup
    //------------------------------------------------------------------------------------------------------------

    /**
     * Reads resource files from the .jar and writes them to filepath
     *
     * @param input URL object to resource
     * @param output File object to output file
     * @return True if created resource
     */
    private static boolean saveResource(URL input, File output) {
        if (output.exists()) return false;

        BufferedInputStream reader = null;
        OutputStream writer = null;

        try {
            // Assign I/O
            reader = new BufferedInputStream(input.openStream());
            writer = new BufferedOutputStream(new FileOutputStream(output));

            // Define I/O helpers
            byte[] buffer = new byte[1024];
            int length;

            // Read and write at the same time
            while ((length = reader.read(buffer, 0, 1024)) > 0) {
                writer.write(buffer, 0, length);
            }

            Main.ADMIN.log("Created file: " + output.getCanonicalPath(), Flair.INFO);
        } catch (IOException ex) {
            Main.ADMIN.logException(ex, Flair.ERROR);
            return false;
        } finally {
            try {
                if (reader != null)
                    reader.close();

                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException ex) {
                Main.ADMIN.logException(ex, Flair.ERROR);
            }
        }

        return true;
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
                WORKER_MANAGER.setNextChangeID(WORKER_MANAGER.getLocalChangeID());
                System.out.println("[INFO] Local ChangeID added");
                break;
            case "new":
                WORKER_MANAGER.setNextChangeID(WORKER_MANAGER.getLatestChangeID());
                System.out.println("[INFO] New ChangeID added");
                break;
            default:
                WORKER_MANAGER.setNextChangeID(userInput[1]);
                System.out.println("[INFO] Custom ChangeID added");
                break;

        }

        // Wake worker controller
        synchronized (WORKER_MANAGER.getMonitor()) {
            WORKER_MANAGER.getMonitor().notifyAll();
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
            WORKER_MANAGER.printAllWorkers();
        } else if (userInput[1].equalsIgnoreCase("del")) {
            System.out.println("[INFO] Removing " + userInput[2] + " worker..");
            WORKER_MANAGER.fireWorkers(Integer.parseInt(userInput[2]));
        } else if (userInput[1].equalsIgnoreCase("add")) {
            System.out.println("[INFO] Adding " + userInput[2] + " worker..");
            WORKER_MANAGER.spawnWorkers(Integer.parseInt(userInput[2]));
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
                ADMIN.log("Starting account matching", Flair.INFO);
                ACCOUNT_MANAGER.checkAccountNameChanges();
                ADMIN.log("Account matching finished", Flair.INFO);
                break;

            default:
                ADMIN.log(String.format("Unknown command: '%s'. Use 'help'.", userInput[1]), Flair.ERROR);
                break;
        }
    }

    private static void commandMoveItem(String[] userInput) {
        ADMIN.log("Moving inactive item entries to separate table...", Flair.INFO);
        DATABASE.moveInactiveItemEntries();
        ADMIN.log("Moving finished", Flair.INFO);
    }
}
