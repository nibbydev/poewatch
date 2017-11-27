package MainPack.StatClasses;

import java.util.ArrayList;

public class StatController {
    /*   Name: StatController
     *   Date created: 26.11.2017
     *   Last modified: 27.11.2017
     *   Description: Contains statistical data
     */

    private ArrayList<League> leagues = new ArrayList<>();
    private int bytesDownloaded = 0;
    private int totalPullCount = 0;
    private int successfulPullCount = 0;

    /*
     * Methods that access values from outside the class
     */

    public void addBytesDownloaded(int bytesDownloaded) {
        this.bytesDownloaded += bytesDownloaded;
    }

    public void incTotalPullCount() {
        this.totalPullCount++;
    }

    public void incSuccessfulPullCount() {
        this.successfulPullCount++;
    }

    public League getLeague(String leagueName) {
        /*  Name: getLeague()
        *   Date created: 26.11.2017
        *   Last modified: 27.11.2017
        *   Description: Looks for matching league object. If not present, creates one and returns it
        */

        // Search for a matching league
        for (League leagueObject: leagues) {
            if (leagueObject.getName().equals(leagueName))
                return leagueObject;
        }

        // Create a new one and return it
        League newLeagueObject = new League(leagueName);
        leagues.add(newLeagueObject);
        return newLeagueObject;
    }

    /*
     * Methods that actually do something
     */

    public void printStats() {
        /*  Name: printStats()
        *   Date created: 27.11.2017
        *   Last modified: 27.11.2017
        *   Description: Prints out all gathered info
        */

        for (League l: leagues) {
            System.out.print(l.buildString());
        }

        String tempString = "";
        tempString += "MB downloaded: " + (double)bytesDownloaded / 1000 / 1000 + "\n";
        tempString += "Total pulls: " + totalPullCount + "\n";
        tempString += "Successful pulls: " + successfulPullCount + "\n";

        System.out.println(tempString);
    }

    public void clearStats() {
        /*  Name: printStats()
        *   Date created: 27.11.2017
        *   Last modified: 27.11.2017
        *   Description: Replaces all statistical values with 0
        */

        for (League l: leagues) {
            l.clearStats();
        }

        bytesDownloaded = 0;
        totalPullCount = 0;
        successfulPullCount = 0;
    }
}
