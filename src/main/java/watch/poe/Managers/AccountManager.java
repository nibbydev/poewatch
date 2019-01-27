package poe.Managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Db.Database;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(AccountManager.class);
    private final Object monitor = new Object();
    private final Database database;

    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private long lastRunTime;
    private List<AccountRelation> queue = new ArrayList<>();
    private List<AccountRelation> processed = new ArrayList<>();

    public AccountManager(Database database) {
        this.database = database;
    }

    //------------------------------------------------------------------------------------------------------------
    // Thread controllers
    //------------------------------------------------------------------------------------------------------------

    public void run() {
        logger.info(String.format("Starting %s", AccountManager.class.getName()));

        while (flagLocalRun) {
            synchronized (monitor) {
                try {
                    monitor.wait(100);
                } catch (InterruptedException ignored) {
                }
            }

            if (queue.isEmpty()) {
                if (!processed.isEmpty()) {
                    List<AccountRelation> tmp = new ArrayList<>(processed);
                    processed = new ArrayList<>();

                    database.account.createAccountRelation(tmp);
                }

                continue;
            }

            if (System.currentTimeMillis() - lastRunTime < 2000) {
                continue;
            }

            lastRunTime = System.currentTimeMillis();
            processRelation(queue.remove(0));
        }

        readyToExit = true;
    }

    public void stopController() {
        logger.info("Stopping AccountManager");

        flagLocalRun = false;

        while (!readyToExit) try {
            synchronized (monitor) {
                monitor.notify();
            }

            Thread.sleep(50);
        } catch (InterruptedException ex) {
        }

        logger.info("AccountManager stopped");
    }

    //------------------------------------------------------------------------------------------------------------
    // Class methods
    //------------------------------------------------------------------------------------------------------------

    public void checkAccountNameChanges() {
        List<AccountRelation> accountRelations = new ArrayList<>();
        database.account.getAccountRelations(accountRelations);

        if (accountRelations.isEmpty()) {
            return;
        }

        queue.addAll(accountRelations);
        logger.info(String.format("Found %3d new character matches: ", queue.size()));

        for (AccountRelation accountRelation : accountRelations) {
            logger.info(String.format("  %20s (%8d) -> %20s (%8d) (%2d matches)", accountRelation.oldAccountName,
                    accountRelation.oldAccountId,
                    accountRelation.newAccountName,
                    accountRelation.newAccountId,
                    accountRelation.matches));
        }
    }

    private void processRelation(AccountRelation accountRelation) {
        accountRelation.statusCode = requestStatusCode(accountRelation.oldAccountName);

        switch (accountRelation.statusCode) {
            case 404: // doesn't exist
                accountRelation.moved = 1;
                break;
            case 403: // exists but private
            case 200: // exists and public
            default:  // something went wrong
                accountRelation.moved = 0;
                break;
        }

        logger.info(String.format("Account %20s had status: %3d", accountRelation.oldAccountName, accountRelation.statusCode));

        processed.add(accountRelation);
    }

    private int requestStatusCode(String account) {
        try {
            URL request = new URL("http://pathofexile.com/character-window/get-characters?accountName=" + account);
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();

            connection.setReadTimeout(2000);
            connection.setConnectTimeout(1000);

            return connection.getResponseCode();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return 0;
    }

    public static class AccountRelation {
        public String oldAccountName, newAccountName;
        public long oldAccountId, newAccountId;
        public int matches;

        public int statusCode, moved;

        public void load (ResultSet resultSet) throws SQLException {
            oldAccountName = resultSet.getString("oldAccountName");
            newAccountName = resultSet.getString("newAccountName");
            oldAccountId = resultSet.getLong("oldAccountId");
            newAccountId = resultSet.getLong("newAccountId");
            matches = resultSet.getInt("matches");
        }
    }
}
