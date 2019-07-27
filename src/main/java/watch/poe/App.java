package poe;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Database.Database;
import poe.Item.Item;
import poe.Item.Parser.ItemParser;
import poe.Item.Parser.Price;
import poe.Interval.IntervalManager;
import poe.League.LeagueManager;
import poe.Relation.Indexer;
import poe.Relation.RelationResources;
import poe.Statistics.StatisticsManager;
import poe.Price.PriceManager;
import poe.Statistics.StatType;
import poe.Utility.Utility;
import poe.Worker.WorkerManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final String configName = "config.conf";

    private StatisticsManager sm;
    private RelationResources rr;
    private IntervalManager im;
    private WorkerManager wm;
    private PriceManager pm;
    private Database db;
    private Indexer ix;
    private Config cnf;

    private String[] args;

    public App(String[] launchArgs) {
        this.args = launchArgs;
    }

    /**
     * Initialises the application. Main loop must be called separately
     */
    public boolean init() {
        logger.info("Starting application");

        String configString = Utility.loadFile(configName);
        if (configString == null) {
            // If the config did not exist then exit the app to allow initial configurations
            logger.info("Edit \"" + configName + "\" and try again. Exiting...");
            return false;
        } else {
            cnf = ConfigFactory.parseString(configString);
            logger.info("Config \"" + configName + "\" loaded");
        }

        // Initialize the controllers
        boolean success = setupControllers();
        if (!success) {
            logger.error("Could not start controllers. Exiting...");
            return false;
        }

        // Parse CLI parameters
        success = parseCommandParameters(args);
        if (!success) {
            logger.error("Could not parse application parameters. Exiting...");
            return false;
        }

        // Start worker manager
        wm.start();

        // Start price calculators
        if (cnf.getBoolean("calculation.enable")) {
            pm.start();
        }

        return true;
    }

    /**
     * Shuts down controllers and the application
     */
    public void stop() {
        if (pm != null) {
            pm.stopController();
        }

        if (wm != null) {
            wm.stopController();
        }

        if (sm != null) {
            sm.addValue(StatType.APP_SHUTDOWN, null);
            sm.upload();
        }

        if (db != null) {
            db.disconnect();
        }
    }

    /**
     * Initializes all controllers
     *
     * @return True on success, false if something went wrong
     */
    private boolean setupControllers() {
        logger.info("Setting up controllers");

        im = new IntervalManager();

        // Initialize database connector
        db = new Database(cnf);
        if (!db.connect()) {
            logger.error("Could not connect to database");
            return false;
        }

        sm = new StatisticsManager(db);
        sm.addValue(StatType.APP_STARTUP, null);

        // Init league manager
        LeagueManager lm = new LeagueManager(db, cnf);
        if (!lm.cycle()) {
            logger.error("Could not get leagues");
            return false;
        }

        // Setup item data indexer
        ix = new Indexer(db);
        if (!ix.init()) {
            logger.error("Could not initialize indexer");
            return false;
        }

        rr = new RelationResources(db, ix);
        if (!rr.init()) {
            logger.error("Could not initialize relation resources");
            return false;
        }

        Item.setIndexer(ix);
        Item.setRelationResources(rr);
        Price.setRelationResources(rr);

        ItemParser ip = new ItemParser(lm, ix, cnf, sm, db);
        if (!ip.init()) {
            logger.error("Could not initialize item parser");
            return false;
        }

        wm = new WorkerManager(cnf, im, db, sm, lm, ip);

        // Instantiate a price manager
        pm = new PriceManager(db, cnf, wm);

        logger.info("Finished setting up controllers");
        return true;
    }

    /**
     * Checks CLI parameters
     *
     * @param args Passed CLI args
     * @return False if app should exit
     */
    private boolean parseCommandParameters(String[] args) {
        ArrayList<String> newArgs = new ArrayList<>(Arrays.asList(args));

        if (!newArgs.contains("-workers")) {
            wm.spawnWorkers(cnf.getInt("worker.defaultCount"));
            System.out.println("[INFO] Spawned 3 workers");
        }

        if (!newArgs.contains("-id")) {
            String changeId = db.init.getChangeID();

            if (changeId == null) {
                System.out.println("[ERROR] Could not get a change id");
                return false;
            }

            wm.setNextChangeID(changeId);
            System.out.printf("[INFO] Change ID (%s) added%n", changeId);
        }

        for (String arg : newArgs) {
            if (!arg.startsWith("-")) continue;
            arg = arg.substring(1);

            switch (arg) {
                case "workers":
                    wm.spawnWorkers(Integer.parseInt(newArgs.get(newArgs.lastIndexOf(arg) + 1)));
                    System.out.printf("[INFO] Spawned %s workers%n", newArgs.get(newArgs.lastIndexOf(arg) + 1));
                    break;

                case "id":
                    String changeId = newArgs.get(newArgs.lastIndexOf(arg) + 2);
                    wm.setNextChangeID(changeId);
                    System.out.printf("[INFO] New Change ID (%s) added%n", changeId);
                    break;

                default:
                    System.out.printf("[ERROR] Unknown CLI parameter: %s%n", arg);
                    break;
            }
        }

        return true;
    }

    /**
     * Main loop. Allows for some primitive command input through the console
     */
    public void mainLoop() {
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

                    case "calculateAll":
                        pm.resetCycleStamp();
                        logger.info("Cycle timestamp reset");
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
    private void commandWorker(String[] userInput) {
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
            wm.printWorkers();
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
    private void commandAbout() {
        String about = "PoeWatch (https://poe.watch)\n"
                + "Made by: Siegrest\n"
                + "Licenced under AGPL-3.0, 2018\n";
        System.out.println(about);
    }
}
