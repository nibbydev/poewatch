package com.poestats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.poestats.history.HistoryManager;
import com.poestats.league.LeagueManager;
import com.poestats.pricer.EntryManager;
import com.poestats.relations.RelationManager;
import com.poestats.worker.WorkerManager;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private static GsonBuilder gsonBuilder;
    public static Config CONFIG;
    public static WorkerManager WORKER_MANAGER;
    public static EntryManager ENTRY_MANAGER;
    public static RelationManager RELATIONS;
    public static AdminSuite ADMIN;
    public static HistoryManager HISTORY_MANAGER;
    public static LeagueManager LEAGUE_MANAGER;
    public static Database DATABASE;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * The main class. Run this to run the program
     *
     * @param args CLI args
     */
    public static void main(String[] args) {
        gsonBuilder = new GsonBuilder();
        gsonBuilder.disableHtmlEscaping();

        DATABASE = new Database();
        DATABASE.connect();

        // Init admin suite
        ADMIN = new AdminSuite();

        // Make sure basic folder structure exists
        buildFolderFileStructure();

        CONFIG = new Config();
        RELATIONS = new RelationManager();

        // Init league manager
        LEAGUE_MANAGER = new LeagueManager();
        boolean leagueLoadResult = LEAGUE_MANAGER.loadLeaguesOnStartup();
        if (!leagueLoadResult) {
            Main.ADMIN.log_("Unable to get league list", 5);
            System.exit(0);
        }

        WORKER_MANAGER = new WorkerManager();
        ENTRY_MANAGER = new EntryManager();
        HISTORY_MANAGER = new HistoryManager();

        // Parse CLI parameters
        parseCommandParameters(args);

        // Start controller
        WORKER_MANAGER.start();

        // Initiate main command loop, allowing user some control over the program
        commandLoop();

        // Stop workers on exit
        WORKER_MANAGER.stopController();

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
                + "    backup - backup commands\n"
                + "    counter - counter commands\n"
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
                    case "counter":
                        commandCounter(userInput);
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
     * Creates basic file structure on program launch
     */
    private static void buildFolderFileStructure() {
        boolean createdFile = notifyMkDir(Config.folder_data);
        createdFile = notifyMkDir(Config.folder_database)   || createdFile;
        createdFile = notifyMkDir(Config.folder_output)     || createdFile;
        createdFile = notifyMkDir(Config.folder_history)    || createdFile;
        createdFile = notifyMkDir(Config.folder_backups)    || createdFile;

        createdFile = saveResource(Config.resource_config, Config.file_config)          || createdFile;
        createdFile = saveResource(Config.resource_relations, Config.file_relations)    || createdFile;

        if (createdFile) {
            Main.ADMIN.log_("Created new file(s)/folder(s)!", 0);
            Main.ADMIN.log_("Configure these and restart the program", 0);
            Main.ADMIN.log_("Press enter to continue...", 0);
            try { System.in.read(); } catch (IOException ex) {}
            System.exit(0);
        }
    }

    /**
     * Creates a folder and logs it
     *
     * @param file Folder to be created
     * @return True if success
     */
    private static boolean notifyMkDir(File file) {
        if (file.mkdir()) {
            try {
                Main.ADMIN.log_("Created: " + file.getCanonicalPath(), 1);
            } catch (IOException ex) { }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Reads files from the .jar and writes them to filepath
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

            Main.ADMIN.log_("Created file: " + output.getCanonicalPath(), 1);
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
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
                Main.ADMIN._log(ex, 4);
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
        String about = "Project id: PoE stash API JSON statistics generator\n"
                + "Made by: Siegrest\n"
                + "Licenced under MIT licence, 2018\n";
        System.out.println(about);
    }

    /**
     * Allows creating specific backups from the CLI
     *
     * @param userInput Input string
     */
    private static void commandBackup(String[] userInput) {
        String helpString = "[INFO] Available backup commands:\n";
        helpString += "    'backup 1' - Backup crucial files\n";
        helpString += "    'backup 2' - Backup everything in output directory\n";
        helpString += "    'backup 3' - Backup everything in data directory\n";

        if (userInput.length < 2) {
            System.out.println(helpString);
            return;
        }

        switch (userInput[1]) {
            case "1":
                ADMIN.backup(new File("./data/database"), "cli_output");
                ADMIN.backup(new File("./data/history"), "cli_history");
                ADMIN.backup(new File("./data/itemData.json"), "cli_itemdata");
                break;
            case "2":
                ADMIN.backup(new File("./data/output"), "cli_output");
                break;
            case "3":
                ADMIN.backup(new File("./data/"), "cli_all");
                break;
            default:
                System.out.println(helpString);
                break;
        }
    }

    /**
     * Holds commands that control time counters
     *
     * @param userInput Input string
     */
    private static void commandCounter(String[] userInput) {
        String helpString = "[INFO] Available counter commands:\n";
        helpString += "    'counter <type> + <amount>' - Add <amount> MS to <type> counter\n";
        helpString += "    'counter <type> - <amount>' - Remove <amount> MS from <type> counter\n";
        helpString += "    'counter <type> = <amount>' - Set <type> counter to <amount> MS\n";

        if (userInput.length < 4) {
            System.out.println(helpString);
            return;
        }

        long value, oldValue, newValue;
        try {
            value = Long.parseLong(userInput[3]);
        } catch (Exception ex) {
            System.out.println("Invalid value");
            return;
        }

        switch (userInput[1]) {
            case "24":
                oldValue = ENTRY_MANAGER.getTwentyFourCounter();

                switch (userInput[2]) {
                    case "+": newValue = oldValue + value; break;
                    case "-": newValue = oldValue - value; break;
                    case "=": newValue = value;            break;
                    default:
                        System.out.println("Unknown sign");
                        return;
                }

                ENTRY_MANAGER.setTwentyFourCounter(newValue);
                break;
            case "60":
                oldValue = ENTRY_MANAGER.getSixtyCounter();

                switch (userInput[2]) {
                    case "+": newValue = oldValue + value; break;
                    case "-": newValue = oldValue - value; break;
                    case "=": newValue = value;            break;
                    default:
                        System.out.println("Unknown sign");
                        return;
                }

                ENTRY_MANAGER.setSixtyCounter(newValue);
                break;
            case "10":
                oldValue = ENTRY_MANAGER.getTenCounter();

                switch (userInput[2]) {
                    case "+": newValue = oldValue + value; break;
                    case "-": newValue = oldValue - value; break;
                    case "=": newValue = value;            break;
                    default:
                        System.out.println("Unknown sign");
                        return;
                }

                ENTRY_MANAGER.setTenCounter(newValue);
                break;
            default:
                System.out.println("Unknown type");
                return;
        }

        System.out.println("Value of '"+userInput[1]+"' changed ("+oldValue+") -> ("+newValue+") [change: "+(newValue-oldValue)+"]");
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
