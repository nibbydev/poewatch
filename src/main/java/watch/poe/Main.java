package poe;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Item;
import poe.Item.Parser.ItemParser;
import poe.Item.Parser.Price;
import poe.Managers.*;
import poe.Managers.Stat.StatType;
import poe.Worker.WorkerManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static StatisticsManager sm;
    private static IntervalManager im;
    private static WorkerManager wm;
    private static PriceManager pm;
    private static Database db;
    private static Config cnf;

    /**
     * App entry point
     *
     * @param args CLI args
     */
    public static void main(String[] args) {
        boolean success;

        logger.info("Starting PoeWatch client");

        try {
            im = new IntervalManager();

            if (!loadConfig()) {
                return;
            }

            // Initialize database connector
            db = new Database(cnf);
            success = db.connect();
            if (!success) {
                logger.error("Could not connect to database");
                return;
            }

            sm = new StatisticsManager(db);
            sm.addValue(StatType.APP_STARTUP, null);

            // Instantiate a price manager
            pm = new PriceManager(db);

            // Init league manager
            LeagueManager lm = new LeagueManager(db, cnf);
            success = lm.cycle();
            if (!success) {
                logger.error("Could not get leagues");
                return;
            }

            // Get category, item and currency data
            RelationManager rm = new RelationManager(db);
            success = rm.init();
            if (!success) {
                logger.error("Could not get relations");
                return;
            }

            Item.setRelationManager(rm);
            Price.setRelationManager(rm);

            ItemParser ip = new ItemParser(lm, rm, cnf, sm, db);
            wm = new WorkerManager(cnf, im, db, sm, lm, ip);

            // Get all distinct stash ids that are in the db
            success = db.init.getStashIds(ip.getStashCrcSet());
            if (!success) {
                logger.error("Could not get active stash IDs");
                return;
            }

            // Parse CLI parameters
            success = parseCommandParameters(args);
            if (!success) return;

            // Start controllers
            wm.start();
            pm.start();

            // Initiate main command loop, allowing user some control over the program
            commandLoop();
        } finally {
            if (wm != null) wm.stopController();
            if (pm != null) pm.stopController();

            if (sm != null) {
                sm.addValue(StatType.APP_SHUTDOWN, null);
                sm.upload();
            }

            if (db != null) db.disconnect();
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
            wm.spawnWorkers(cnf.getInt("worker.defaultCount"));
            System.out.println("[INFO] Spawned 3 workers");
        }

        if (!newArgs.contains("-id")) {
            String changeId = db.init.getChangeID();

            if (changeId == null) {
                System.out.println("[ERROR] Local ChangeID not found");
                changeId = wm.getLatestChangeID();
            }

            if (changeId == null) {
                System.out.println("[ERROR] Could not get a change id");
                return false;
            }

            wm.setNextChangeID(changeId);
            System.out.println("[INFO] ChangeID (" + changeId + ") added");
        }

        for (String arg : newArgs) {
            if (!arg.startsWith("-"))
                continue;

            switch (arg) {
                case "-workers":
                    wm.spawnWorkers(Integer.parseInt(newArgs.get(newArgs.lastIndexOf(arg) + 1)));
                    System.out.println("[INFO] Spawned " + newArgs.get(newArgs.lastIndexOf(arg) + 1) + " workers");
                    break;
                case "-id":
                    String changeId = newArgs.get(newArgs.lastIndexOf(arg) + 1);
                    wm.setNextChangeID(changeId);
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
                    default:
                        logger.info(String.format("Unknown command: '%s'. Use 'help'.", userInput[0]));
                        break;
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
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
            wm.printAllWorkers();
        } else if (userInput[1].equalsIgnoreCase("del")) {
            System.out.println("[INFO] Removing " + userInput[2] + " worker..");
            wm.fireWorkers(Integer.parseInt(userInput[2]));
        } else if (userInput[1].equalsIgnoreCase("add")) {
            System.out.println("[INFO] Adding " + userInput[2] + " worker..");
            wm.spawnWorkers(Integer.parseInt(userInput[2]));
        } else {
            System.out.println(helpString);
        }
    }

    /**
     * Prints about page
     */
    private static void commandAbout() {
        String about = "PoeWatch (https://poe.watch)\n"
                + "Made by: Siegrest\n"
                + "Licenced under AGPL-3.0, 2018\n";
        System.out.println(about);
    }

    /**
     * Attempts to load the config or create it if it does not exist
     */
    private static boolean loadConfig() {
        File confFile = new File("config.conf");

        if (!confFile.exists() || !confFile.isFile()) {
            logger.warn("Could not find config");
            String path = "";

            try {
                path = exportResource("/config.conf");
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error("Could not create config");
            }

            logger.info("Config '" + path + "' has been created");
            return false;
        }

        cnf = ConfigFactory.parseFile(confFile);
        return true;
    }

    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/SmartLibrary.dll"
     * @return The path to the exported resource
     */
    static private String exportResource(String resourceName) throws Exception {
        String jarFolder;

        try (InputStream stream = Main.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            byte[] buffer = new byte[4096];
            int readBytes;

            jarFolder = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                    .getParentFile()
                    .getPath()
                    .replace('\\', '/');

            try (OutputStream resStreamOut = new FileOutputStream(jarFolder + resourceName)) {
                while ((readBytes = stream.read(buffer)) > 0) {
                    resStreamOut.write(buffer, 0, readBytes);
                }
            }
        }

        return jarFolder + resourceName;
    }
}
