package ovh.poe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ovh.poe.Pricer.EntryController;
import ovh.poe.Worker.WorkerController;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    private static GsonBuilder gsonBuilder;
    public static ConfigReader CONFIG;
    public static WorkerController WORKER_CONTROLLER;
    public static EntryController ENTRY_CONTROLLER;
    public static RelationManager RELATIONS;
    public static AdminSuite ADMIN;
    public static HistoryController HISTORY_CONTROLLER;

    /**
     * The main class. Run this to run the program
     *
     * @param args CLI args
     */
    public static void main(String[] args) {
        gsonBuilder = new GsonBuilder();
        gsonBuilder.disableHtmlEscaping();

        // Init admin suite
        ADMIN = new AdminSuite();

        // Make sure basic folder structure exists
        buildFolderFileStructure();

        CONFIG = new ConfigReader("config.cfg");

        // Init relation manager
        RELATIONS = new RelationManager();
        RELATIONS.downloadLeagueList();

        WORKER_CONTROLLER = new WorkerController();
        ENTRY_CONTROLLER = new EntryController();
        HISTORY_CONTROLLER = new HistoryController();

        // Parse CLI parameters
        parseCommandParameters(args);

        // Start controller
        WORKER_CONTROLLER.start();

        // Initiate main command loop, allowing user some control over the program
        commandLoop();

        // Stop workers on exit
        WORKER_CONTROLLER.stopController();

        // Save generated item data
        RELATIONS.saveData();
    }

    /**
     * Checks CLI parameters
     *
     * @param args Passed CLI args
     */
    private static void parseCommandParameters(String[] args) {
        ArrayList<String> newArgs = new ArrayList<>(Arrays.asList(args));

        if (!newArgs.contains("-workers")) {
            Main.ADMIN.log_("Missing CLI option: -workers <1-5>", 5);
            System.exit(0);
        } else if (!newArgs.contains("-id")) {
            Main.ADMIN.log_("Missing CLI option: -id <'new'/'local'/custom>", 5);
            System.exit(0);
        }

        for (String arg : newArgs) {
            if (!arg.startsWith("-"))
                continue;

            switch (arg) {
                case "-workers":
                    WORKER_CONTROLLER.spawnWorkers(Integer.parseInt(newArgs.get(newArgs.lastIndexOf(arg) + 1)));
                    System.out.println("[INFO] Spawned " + newArgs.get(newArgs.lastIndexOf(arg) + 1) + " workers");
                    break;
                case "-id":
                    switch (newArgs.get(newArgs.lastIndexOf(arg) + 1)) {
                        case "local":
                            WORKER_CONTROLLER.setNextChangeID(WORKER_CONTROLLER.getLocalChangeID());
                            System.out.println("[INFO] Local ChangeID added");
                            break;
                        case "new":
                            WORKER_CONTROLLER.setNextChangeID(WORKER_CONTROLLER.getLatestChangeID());
                            System.out.println("[INFO] New ChangeID added");
                            break;
                        default:
                            WORKER_CONTROLLER.setNextChangeID(newArgs.get(newArgs.lastIndexOf(arg) + 1));
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
                + "    backup - backup commands\n"
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
                        ADMIN.changeIDElement.setStatus(3);
                        ADMIN.saveChangeID();
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
                    case "backup":
                        commandBackup(userInput);
                        break;
                    default:
                        System.out.println("[ERROR] Unknown command: \"" + userInput[0] + "\". Use \"help\" for help");
                        break;
                }
            } catch (IOException ex) {
                Main.ADMIN._log(ex, 4);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // File structure setup
    //------------------------------------------------------------------------------------------------------------

    /**
     * Creates all missing http files and folders on startup
     */
    private static void buildFolderFileStructure() {
        // Make sure output folders exist
        new File("./data/database").mkdirs();
        new File("./data/output").mkdirs();
        new File("./data/current_history").mkdirs();
        new File("./backups").mkdirs();

        // Create ./config.cfg if missing
        saveResource("/", "config.cfg");

        // Create ./currencyRelations.json if missing
        saveResource("/data/", "currencyRelations.json");
    }

    /**
     * Reads files from the .jar and writes them to filepath
     *
     * @param outputDirectory local path to directory
     * @param name            filename
     */
    private static void saveResource(String outputDirectory, String name) {
        // Remove id from file name
        String outputName = name.split("---")[0];

        // Get the current path
        String workingDir = System.getProperty("user.dir");
        File out = new File(workingDir + outputDirectory, outputName);

        if (out.exists()) return;

        Main.ADMIN.log_("Created file: " + outputDirectory + outputName, 1);

        // Define I/O so they can be closed later
        BufferedInputStream reader = null;
        OutputStream writer = null;

        try {
            // Assign I/O
            reader = new BufferedInputStream(Main.class.getResourceAsStream("/resources/" + name));
            writer = new BufferedOutputStream(new FileOutputStream(out));

            // Define I/O helpers
            byte[] buffer = new byte[1024];
            int length;

            // Read and write at the same time
            while ((length = reader.read(buffer, 0, 1024)) > 0) {
                writer.write(buffer, 0, length);
            }
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
        } finally {
            try {
                if (reader != null)
                    reader.close();

                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException ex) {
                Main.ADMIN._log(ex, 4);
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
                WORKER_CONTROLLER.setNextChangeID(WORKER_CONTROLLER.getLocalChangeID());
                System.out.println("[INFO] Local ChangeID added");
                break;
            case "new":
                WORKER_CONTROLLER.setNextChangeID(WORKER_CONTROLLER.getLatestChangeID());
                System.out.println("[INFO] New ChangeID added");
                break;
            default:
                WORKER_CONTROLLER.setNextChangeID(userInput[1]);
                System.out.println("[INFO] Custom ChangeID added");
                break;

        }

        // Wake worker controller
        synchronized (WORKER_CONTROLLER.getMonitor()) {
            WORKER_CONTROLLER.getMonitor().notifyAll();
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
            WORKER_CONTROLLER.printAllWorkers();
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

    /**
     * Prints about page
     */
    private static void commandAbout() {
        String about = "Project name: PoE stash API JSON statistics generator\n"
                + "Made by: Siegrest\n"
                + "Licenced under MIT licence, 2018\n";
        System.out.println(about);
    }

    /**
     * Presents the user with a "Are you sure (yes/no)?" prompt for potentially destructive commands
     *
     * @return Result of the question
     */
    private static boolean commandConfirm() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        try {
            System.out.println("[Info] Are you sure? (yes/no)");
            userInput = reader.readLine();
        } catch (IOException ex) {
            Main.ADMIN.log_("Couldn't read user input for verification", 3);
            Main.ADMIN._log(ex, 4);
            return false;
        }

        return userInput.equals("yes");
    }

    /**
     * Allows creating specific backups from the CLI
     *
     * @param userInput Input string
     */
    private static void commandBackup(String[] userInput) {
        String helpString = "[INFO] Available backup commands:\n";
        helpString += "    'backup data' - Backup database.txt file\n";
        helpString += "    'backup output' - Backup everything in output directory\n";
        helpString += "    'backup all' - Backup everything in data directory\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        switch (userInput[1]) {
            case "data":
                ADMIN.backup(new File("./data/database.txt"), "cli_database");
                ADMIN.backup(new File("./data/itemData.json"), "cli_itemdata");
                break;
            case "output":
                ADMIN.backup(new File("./data/output"), "cli_output");
                break;
            case "all":
                ADMIN.backup(new File("./data/"), "cli_all");
                break;
            default:
                System.out.println(helpString);
                break;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    /**
     * Creates an instance of Gson
     *
     * @return Gson instance
     */
    public static Gson getGson() {
        return gsonBuilder.create();
    }
}
