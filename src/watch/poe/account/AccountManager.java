package watch.poe.account;

import watch.poe.Main;
import watch.poe.admin.Flair;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AccountManager extends Thread {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private final Object monitor = new Object();

    private long lastRunTime;
    private List<AccountRelation> queue = new ArrayList<>();
    private List<AccountRelation> processed = new ArrayList<>();

    //------------------------------------------------------------------------------------------------------------
    // Thread controllers
    //------------------------------------------------------------------------------------------------------------

    public void run() {
        while (flagLocalRun) {
            synchronized (monitor) {
                try {
                    monitor.wait(100);
                } catch (InterruptedException e) { }
            }

            if (queue.isEmpty()) {
                if (!processed.isEmpty()) {
                    List<AccountRelation> tmp = new ArrayList<>(processed);
                    processed = new ArrayList<>();

                    Main.DATABASE.createAccountRelation(tmp);
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
        Main.ADMIN.log("Stopping AccountManager", Flair.INFO);

        flagLocalRun = false;

        while (!readyToExit) try {
            synchronized (monitor) {
                monitor.notify();
            }

            Thread.sleep(50);
        } catch (InterruptedException ex) { }

        Main.ADMIN.log("AccountManager stopped", Flair.INFO);
    }

    //------------------------------------------------------------------------------------------------------------
    // Class methods
    //------------------------------------------------------------------------------------------------------------

    public void checkAccountNameChanges() {
        List<AccountRelation> accountRelations = new ArrayList<>();
        Main.DATABASE.getAccountRelations(accountRelations);

        if (accountRelations.isEmpty()) {
            return;
        }

        queue.addAll(accountRelations);
        Main.ADMIN.log(String.format("Found %3d new character matches: \n", queue.size()), Flair.INFO);

        for (AccountRelation accountRelation : accountRelations) {
            Main.ADMIN.log(String.format("  %20s (%8d) -> %s (%20d) (%2d matches)\n", accountRelation.oldAccountName,
                                                                                       accountRelation.oldAccountId,
                                                                                       accountRelation.newAccountName,
                                                                                       accountRelation.newAccountId,
                                                                                       accountRelation.matches), Flair.INFO);
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

        Main.ADMIN.log(String.format("Account %32s had status: %3d\n", accountRelation.oldAccountName, accountRelation.statusCode), Flair.INFO);

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
            Main.ADMIN.logException(ex, Flair.ERROR);
        }

        return 0;
    }
}
