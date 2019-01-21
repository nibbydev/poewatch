package poe.Managers;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;
import poe.Worker.Entry.StatusElement;
import poe.Item.Mappers;
import poe.Worker.Worker;
import poe.Managers.Stat.StatType;
import poe.Managers.Stat.GroupType;
import poe.Managers.Stat.RecordType;

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

    private final StatusElement status;
    private final Gson gson;

    private final ArrayList<Worker> workerList = new ArrayList<>();
    private volatile boolean flagRun = true;
    private volatile boolean readyToExit = false;
    private String nextChangeID;

    public WorkerManager(StatisticsManager sm, LeagueManager lm, RelationManager rm, AccountManager am, Database db, Config cnf) {
        this.statisticsManager = sm;
        this.leagueManager = lm;
        this.relationManager = rm;
        this.accountManager = am;
        this.database = db;
        this.config = cnf;

        this.status = new StatusElement(cnf);
        this.gson = new Gson();
    }

    /**
     * Contains main loop. Checks for open jobs and assigns them to workers
     */
    public void run() {
        logger.info(String.format("Starting %s", WorkerManager.class.getName()));

        status.fixCounters();

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

        // Start cycle timer
        statisticsManager.startTimer(StatType.CYCLE_TOTAL);

        if (status.isTwentyFourBool()) {
            statisticsManager.startTimer(StatType.CYCLE_REMOVE_OLD_ENTRIES);
            database.history.removeOldItemEntries();
            statisticsManager.clkTimer(StatType.CYCLE_REMOVE_OLD_ENTRIES, GroupType.NONE, RecordType.SINGULAR);
        }

        statisticsManager.startTimer(StatType.CYCLE_CALC_PRICES);
        PriceManager.run();
        statisticsManager.clkTimer(StatType.CYCLE_CALC_PRICES, GroupType.NONE, RecordType.SINGULAR);

        statisticsManager.startTimer(StatType.CYCLE_UPDATE_COUNTERS);
        database.flag.updateCounters();
        statisticsManager.clkTimer(StatType.CYCLE_UPDATE_COUNTERS, GroupType.NONE, RecordType.SINGULAR);

        statisticsManager.startTimer(StatType.CYCLE_CALC_EXALTED);
        database.calc.calculateExalted();
        statisticsManager.clkTimer(StatType.CYCLE_CALC_EXALTED, GroupType.NONE, RecordType.SINGULAR);

        if (status.isSixtyBool()) {
            database.calc.countActiveAccounts(statisticsManager);

            statisticsManager.startTimer(StatType.CYCLE_ADD_HOURLY);
            database.history.addHourly();
            statisticsManager.clkTimer(StatType.CYCLE_ADD_HOURLY, GroupType.NONE, RecordType.SINGULAR);

            statisticsManager.startTimer(StatType.CYCLE_CALC_DAILY);
            database.calc.calcDaily();
            statisticsManager.clkTimer(StatType.CYCLE_CALC_DAILY, GroupType.NONE, RecordType.SINGULAR);
        }

        if (status.isTwentyFourBool()) {
            statisticsManager.startTimer(StatType.CYCLE_ADD_DAILY);
            database.history.addDaily();
            statisticsManager.clkTimer(StatType.CYCLE_ADD_DAILY, GroupType.NONE, RecordType.SINGULAR);

            statisticsManager.startTimer(StatType.CYCLE_CALC_SPARK);
            database.calc.calcSpark();
            statisticsManager.clkTimer(StatType.CYCLE_CALC_SPARK, GroupType.NONE, RecordType.SINGULAR);
        }

        if (status.isSixtyBool()) {
            statisticsManager.startTimer(StatType.CYCLE_RESET_COUNTERS);
            database.flag.resetCounters();
            statisticsManager.clkTimer(StatType.CYCLE_RESET_COUNTERS, GroupType.NONE, RecordType.SINGULAR);
        }

        // End cycle timer
        statisticsManager.clkTimer(StatType.CYCLE_TOTAL, GroupType.NONE, RecordType.SINGULAR);

        // Check league API
        if (status.isTenBool()) {
            statisticsManager.startTimer(StatType.CYCLE_LEAGUE_CYCLE);
            leagueManager.cycle();
            statisticsManager.clkTimer(StatType.CYCLE_LEAGUE_CYCLE, GroupType.NONE, RecordType.SINGULAR);
        }

        // Check if there are matching account name changes
        if (status.isTwentyFourBool()) {
            statisticsManager.startTimer(StatType.CYCLE_ACCOUNT_CHANGES);
            accountManager.checkAccountNameChanges();
            statisticsManager.clkTimer(StatType.CYCLE_ACCOUNT_CHANGES, GroupType.NONE, RecordType.SINGULAR);
        }

        // Prepare cycle message
        logger.info(String.format("Cycle finished: %5d ms | %2d / %3d / %4d ",
                statisticsManager.getLast(StatType.CYCLE_TOTAL),
                status.getTenRemainMin(),
                status.getSixtyRemainMin(),
                status.getTwentyFourRemainMin()
        ));

        logger.info(String.format("[%5d][%5d][%5d]",
                    statisticsManager.getLast(StatType.CYCLE_CALC_PRICES),
                    statisticsManager.getLast(StatType.CYCLE_UPDATE_COUNTERS),
                    statisticsManager.getLast(StatType.CYCLE_CALC_EXALTED)
        ));

        if (status.isSixtyBool()) logger.info(String.format("[%5d][%5d][%5d]",
                statisticsManager.getLast(StatType.CYCLE_ADD_HOURLY),
                statisticsManager.getLast(StatType.CYCLE_CALC_DAILY),
                statisticsManager.getLast(StatType.CYCLE_RESET_COUNTERS)
        ));

        if (status.isTwentyFourBool()) logger.info(String.format("[%5d][%5d][%5d][%5d]",
                statisticsManager.getLast(StatType.CYCLE_REMOVE_OLD_ENTRIES),
                statisticsManager.getLast(StatType.CYCLE_ADD_DAILY),
                statisticsManager.getLast(StatType.CYCLE_CALC_SPARK),
                statisticsManager.getLast(StatType.CYCLE_ACCOUNT_CHANGES)
        ));

        // Reset flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);

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
