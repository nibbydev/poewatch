package poe.Managers;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Item.Mappers;
import poe.Managers.Interval.TimeFrame;
import poe.Worker.Worker;
import poe.Managers.Stat.StatType;

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
    private final StatisticsManager statisticsManager;

    private final IntervalManager intervalManager;
    private final Gson gson = new Gson();

    private final ArrayList<Worker> workerList = new ArrayList<>();
    private volatile boolean flagRun = true;
    private volatile boolean readyToExit = false;
    private String nextChangeID;

    public WorkerManager(Config cnf, IntervalManager se, Database db, StatisticsManager sm, LeagueManager lm, RelationManager rm, AccountManager am) {
        this.statisticsManager = sm;
        this.relationManager = rm;
        this.accountManager = am;
        this.leagueManager = lm;
        this.intervalManager = se;
        this.database = db;
        this.config = cnf;
    }

    /**
     * Contains main loop. Checks for open jobs and assigns them to workers
     */
    public void run() {
        logger.info(String.format("Starting %s", WorkerManager.class.getName()));
        logger.info(String.format("Loaded params: [1m: %2d sec][10m: %2d min][60m: %2d min][24h: %2d h]",
                TimeFrame.M_1.getRemaining() / 1000,
                TimeFrame.M_10.getRemaining() / 60000,
                TimeFrame.M_60.getRemaining() / 60000,
                TimeFrame.H_24.getRemaining() / 3600000
        ));

        while (flagRun) {
            intervalManager.checkFlagStates();

            // If cycle should be initiated
            if (intervalManager.isBool(TimeFrame.M_1)) {
                setWorkerSleepState(true, true);
                cycle();
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

            intervalManager.resetFlags();

            try {
                Thread.sleep(100);
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
        // Start cycle timer
        statisticsManager.startTimer(StatType.TIME_CYCLE_TOTAL);

        if (intervalManager.isBool(TimeFrame.H_24)) {
            statisticsManager.startTimer(StatType.TIME_REMOVE_ENTRIES);
            database.history.removeOldItemEntries();
            statisticsManager.clkTimer(StatType.TIME_REMOVE_ENTRIES);
        }

        statisticsManager.startTimer(StatType.TIME_CALC_PRICES);
        PriceManager.run();
        statisticsManager.clkTimer(StatType.TIME_CALC_PRICES);

        statisticsManager.startTimer(StatType.TIME_UPDATE_COUNTERS);
        database.flag.updateCounters();
        statisticsManager.clkTimer(StatType.TIME_UPDATE_COUNTERS);

        statisticsManager.startTimer(StatType.TIME_CALC_EXALT);
        database.calc.calcExalted();
        statisticsManager.clkTimer(StatType.TIME_CALC_EXALT);

        if (intervalManager.isBool(TimeFrame.M_60)) {
            database.stats.countActiveAccounts(statisticsManager);

            statisticsManager.startTimer(StatType.TIME_ADD_HOURLY);
            database.history.addHourly();
            statisticsManager.clkTimer(StatType.TIME_ADD_HOURLY);

            statisticsManager.startTimer(StatType.TIME_CALC_DAILY);
            database.calc.calcDaily();
            statisticsManager.clkTimer(StatType.TIME_CALC_DAILY);

            statisticsManager.startTimer(StatType.TIME_CALC_CURRENT);
            database.calc.calcCurrent();
            statisticsManager.clkTimer(StatType.TIME_CALC_CURRENT);
        }

        if (intervalManager.isBool(TimeFrame.H_24)) {
            statisticsManager.startTimer(StatType.TIME_ADD_DAILY);
            database.history.addDaily();
            statisticsManager.clkTimer(StatType.TIME_ADD_DAILY);

            statisticsManager.startTimer(StatType.TIME_CALC_SPARK);
            database.calc.calcSpark();
            statisticsManager.clkTimer(StatType.TIME_CALC_SPARK);
        }

        if (intervalManager.isBool(TimeFrame.M_60)) {
            statisticsManager.startTimer(StatType.TIME_RESET_COUNTERS);
            database.flag.resetCounters();
            statisticsManager.clkTimer(StatType.TIME_RESET_COUNTERS);
        }

        // End cycle timer
        statisticsManager.clkTimer(StatType.TIME_CYCLE_TOTAL);

        // Check league API
        if (intervalManager.isBool(TimeFrame.M_10)) {
            statisticsManager.startTimer(StatType.TIME_CYCLE_LEAGUES);
            leagueManager.cycle();
            statisticsManager.clkTimer(StatType.TIME_CYCLE_LEAGUES);
        }

        // Check if there are matching account name changes
        if (intervalManager.isBool(TimeFrame.H_24)) {
            statisticsManager.startTimer(StatType.TIME_ACCOUNT_CHANGES);
            accountManager.checkAccountNameChanges();
            statisticsManager.clkTimer(StatType.TIME_ACCOUNT_CHANGES);
        }

        // Prepare cycle message
        logger.info(String.format("Cycle finished: %5d ms", statisticsManager.getLast(StatType.TIME_CYCLE_TOTAL)));
        logger.info(String.format("Status: [1m: %2d sec][10m: %2d min][60m: %2d min][24h: %2d h]",
                TimeFrame.M_1.getRemaining() / 1000 + 1,
                TimeFrame.M_10.getRemaining() / 60000 + 1,
                TimeFrame.M_60.getRemaining() / 60000 + 1,
                TimeFrame.H_24.getRemaining() / 3600000 + 1
        ));

        logger.info(String.format("[%5d][%5d][%5d]",
                    statisticsManager.getLast(StatType.TIME_CALC_PRICES),
                    statisticsManager.getLast(StatType.TIME_UPDATE_COUNTERS),
                    statisticsManager.getLast(StatType.TIME_CALC_EXALT)
        ));

        if (intervalManager.isBool(TimeFrame.M_60)) logger.info(String.format("[%5d][%5d][%5d]",
                statisticsManager.getLast(StatType.TIME_ADD_HOURLY),
                statisticsManager.getLast(StatType.TIME_CALC_DAILY),
                statisticsManager.getLast(StatType.TIME_RESET_COUNTERS)
        ));

        if (intervalManager.isBool(TimeFrame.H_24)) logger.info(String.format("[%5d][%5d][%5d][%5d]",
                statisticsManager.getLast(StatType.TIME_REMOVE_ENTRIES),
                statisticsManager.getLast(StatType.TIME_ADD_DAILY),
                statisticsManager.getLast(StatType.TIME_CALC_SPARK),
                statisticsManager.getLast(StatType.TIME_ACCOUNT_CHANGES)
        ));

        // Upload stats to database
        statisticsManager.upload();
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
            Worker worker = new Worker(this, statisticsManager, leagueManager, relationManager, database, config, i);
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
}
