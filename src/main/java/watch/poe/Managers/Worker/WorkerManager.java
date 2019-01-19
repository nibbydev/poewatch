package poe.Managers.Worker;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.PriceCalculator;
import poe.Managers.Account.AccountManager;
import poe.Managers.Worker.Entry.StatusElement;
import poe.Item.Mappers;
import poe.Managers.Worker.Timer.Timer;
import poe.Managers.League.LeagueManager;
import poe.Managers.Relation.RelationManager;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Manages worker objects (eg. distributing jobs, adding/removing workers)
 */
public class WorkerManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(WorkerManager.class);

    private final Config config;
    private final Database database;
    private final LeagueManager leagueManager;
    private final RelationManager relationManager;
    private final AccountManager accountManager;

    private final StatusElement status;
    private final Timer timer;
    private final Gson gson;

    private Map<Integer, Map<String, Double>> currencyLeagueMap = new HashMap<>();
    private final ArrayList<Worker> workerList = new ArrayList<>();
    private volatile boolean flagRun = true;
    private volatile boolean readyToExit = false;
    private String nextChangeID;

    public WorkerManager(LeagueManager lm, RelationManager rm, AccountManager am, Database db, Config cnf) {
        this.leagueManager = lm;
        this.relationManager = rm;
        this.accountManager = am;
        this.database = db;
        this.config = cnf;

        this.timer = new Timer(db);
        this.status = new StatusElement(cnf);
        this.gson = new Gson();

        if (!cnf.getBoolean("misc.enableTimers")) {
            timer.stop();
        }
    }

    /**
     * Contains main loop. Checks for open jobs and assigns them to workers
     */
    public void run() {
        logger.info(String.format("Starting %s", WorkerManager.class.getName()));

        status.fixCounters();
        timer.getDelays();

        logger.info(String.format("Loaded params: [10m:%3d min][1h:%3d min][24h:%5d min]",
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000));

        while (flagRun) {
            // If minutely cycle should be initiated
            if (System.currentTimeMillis() - status.lastRunTime > config.getInt("entry.sleepTime")) {
                // Wait until all workers are paused
                setWorkerSleepState(true, true);

                status.lastRunTime = System.currentTimeMillis();
                cycle();

                // Wait until all workers are resumed
                setWorkerSleepState(false, true);
            }

            // While there's a job that needs to be given out
            if (nextChangeID != null) {
                for (Worker worker : workerList) {
                    if (worker.getJob() != null) {
                        continue;
                    }

                    worker.setJob(nextChangeID);
                    nextChangeID = null;
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        // If main loop was interrupted, raise flag indicating program is ready to safely exit
        readyToExit = true;
    }

    /**
     * Minutely cycle init
     */
    private void cycle() {
        status.checkFlagStates();
        timer.computeCycleDelays(status);

        // Start cycle timer
        timer.start("cycle", Timer.TimerType.NONE);

        if (status.isTwentyFourBool()) {
            timer.start("a30", Timer.TimerType.TWENTY);
            database.history.removeOldItemEntries();
            timer.clk("a30");
        }

        timer.start("a10");
        PriceCalculator.run();
        timer.clk("a10");

        timer.start("a11");
        database.flag.updateCounters();
        timer.clk("a11");

        timer.start("a12");
        database.calc.calculateExalted();
        timer.clk("a12");

        if (status.isSixtyBool()) {
            timer.start("a22", Timer.TimerType.SIXTY);
            database.history.addHourly();
            timer.clk("a22");

            timer.start("a24", Timer.TimerType.SIXTY);
            database.calc.calcDaily();
            timer.clk("a24");
        }

        if (status.isTwentyFourBool()) {
            timer.start("a31", Timer.TimerType.TWENTY);
            database.history.addDaily();
            timer.clk("a31");

            timer.start("a32", Timer.TimerType.TWENTY);
            database.calc.calcSpark();
            timer.clk("a32");
        }

        if (status.isSixtyBool()) {
            timer.start("a25", Timer.TimerType.SIXTY);
            database.flag.resetCounters();
            timer.clk("a25");
        }

        // End cycle timer
        timer.clk("cycle");

        // Get latest currency rates
        timer.start("prices");
        loadCurrency();
        timer.clk("prices");

        // Check league API
        if (status.isTenBool()) {
            timer.start("leagues", Timer.TimerType.TEN);
            leagueManager.cycle();
            timer.clk("leagues");
        }

        // Check if there are matching account name changes
        if (status.isTwentyFourBool()) {
            timer.start("accountChanges", Timer.TimerType.TWENTY);
            accountManager.checkAccountNameChanges();
            timer.clk("accountChanges");
        }

        // Prepare cycle message
        logger.info(String.format("Cycle finished: %5d ms | %2d / %3d / %4d | c:%6d / p:%2d",
                System.currentTimeMillis() - status.lastRunTime,
                status.getTenRemainMin(),
                status.getSixtyRemainMin(),
                status.getTwentyFourRemainMin(),
                timer.getLatest("cycle"),
                timer.getLatest("prices")));

        logger.info(String.format("[10%5d][11%5d][12%5d]",
                timer.getLatest("a10"),
                timer.getLatest("a11"),
                timer.getLatest("a12")));

        if (status.isSixtyBool()) logger.info(String.format("[21%5d][22%5d][23%5d][24%5d][25%5d]",
                timer.getLatest("a21"),
                timer.getLatest("a22"),
                timer.getLatest("a23"),
                timer.getLatest("a24"),
                timer.getLatest("a25")));

        if (status.isTwentyFourBool()) logger.info(String.format("[30%5d][31%5d][32%5d]",
                timer.getLatest("a30"),
                timer.getLatest("a31"),
                timer.getLatest("a32")));

        // Reset flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);

        // Add new delays to database
        timer.uploadDelays(status);
    }

    /**
     * Stops all active Workers and this object's process
     */
    public void stopController() {
        flagRun = false;

        // Loop though every worker and call stop method
        for (Worker worker : workerList) {
            logger.info("Stopping worker (" + worker.getWorkerId() + ")");
            worker.stopWorker();
            logger.info("Worker (" + worker.getWorkerId() + ") stopped");
        }

        logger.info("Stopping controller");

        // Wait until run() function is ready to exit
        while (!readyToExit) try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        logger.info("Controller stopped");
    }

    /**
     * Prints out all active workers and their active jobs
     */
    public void printAllWorkers() {
        for (Worker workerObject : workerList) {
            logger.info("    " + workerObject.getWorkerId() + ": " + workerObject.getJob());
        }
    }

    /**
     * Spawns new workers
     *
     * @param workerCount Amount of new workers to be added
     */
    public void spawnWorkers(int workerCount) {
        // Get the next available array index
        int nextWorkerIndex = workerList.size();

        // Loop through creation
        for (int i = nextWorkerIndex; i < nextWorkerIndex + workerCount; i++) {
            Worker worker = new Worker(this, leagueManager, relationManager, database, config, i);
            worker.start();

            // Add worker to local list
            workerList.add(worker);
        }
    }

    /**
     * Removes active workers
     *
     * @param workerCount Amount of new workers to be removed
     */
    public void fireWorkers(int workerCount) {
        Worker lastWorker;

        // Get the last available index
        int lastWorkerIndex = workerList.size() - 1;

        // Can't remove what's not there
        if (lastWorkerIndex <= 0 || lastWorkerIndex - workerCount < 0) {
            logger.error("Not enough active workers");
            return;
        }

        // Loop through removal
        for (int i = lastWorkerIndex; i > lastWorkerIndex - workerCount; i--) {
            lastWorker = workerList.get(i);
            lastWorker.stopWorker();
            workerList.remove(lastWorker);
        }
    }

    /**
     * Get a changeID that's close to the stack top
     *
     * @return ChangeID as string
     */
    public String getLatestChangeID() {
        return downloadChangeID("http://api.pathofexile.com/trade/data/change-ids");
    }

    /**
     * Downloads content of ChangeID url and returns it
     *
     * @param url Link to ChangeID resource
     * @return ChangeID as string or null on failure
     */
    private String downloadChangeID(String url) {
        try {
            URL request = new URL(url);
            InputStream input = request.openStream();

            String response = new String(input.readAllBytes());

            try {
                return gson.fromJson(response, Mappers.ChangeID.class).get();
            } catch (Exception ex) {
                logger.error(ex.toString());
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
        }

        return null;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    /**
     * Sets the next change ID in the variable. If the variable has no value, set it to the newChangeID's one,
     * otherwise compare the two and set the newest
     *
     * @param newChangeID ChangeID to be added
     */
    public void setNextChangeID(String newChangeID) {
        if (nextChangeID == null) {
            nextChangeID = newChangeID;
        } else if (Integer.parseInt(newChangeID.substring(newChangeID.lastIndexOf('-') + 1)) >
                Integer.parseInt(nextChangeID.substring(nextChangeID.lastIndexOf('-') + 1))) {
            nextChangeID = newChangeID;
        }
    }

    private void setWorkerSleepState(boolean state, boolean wait) {
        System.out.println(state ? "Pausing workers.." : "Resuming workers..");

        for (Worker worker : workerList) {
            worker.setPauseFlag(state);
        }

        // User wants to wait until all workers are paused/resumed
        while (wait) {
            boolean tmp = false;

            // If there's at least 1 worker that doesn't match the state
            for (Worker worker : workerList) {
                if (worker.isPaused() != state) {
                    tmp = true;
                    break;
                }
            }

            if (!tmp) {
                break;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void loadCurrency() {
        Map<Integer, Map<String, Double>> map = database.init.getCurrencyMap();

        if (map == null) {
            return;
        }

        currencyLeagueMap = map;
    }

    public Map<String, Double> getCurrencyLeagueMap(int leagueId) {
        return currencyLeagueMap.get(leagueId);
    }
}
