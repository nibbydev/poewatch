package MainPack.StatClasses;

import java.util.ArrayList;

public class StatController {
    /*   Name: StatController
     *   Date created: 26.11.2017
     *   Last modified: 26.11.2017
     *   Description: Contains statistical data
     */

    private ArrayList<League> leagues = new ArrayList<>();

    public League getLeague(String leagueName) {
        // Search for a matching league
        for (League leagueObject: leagues) {
            if (leagueObject.getName().equals(leagueName))
                return leagueObject;
        }

        // League doesn't exist; create it; return it
        League newLeagueObject = new League(leagueName);
        leagues.add(newLeagueObject);
        return newLeagueObject;
    }

    public ArrayList<League> getLeagues() {
        return leagues;
    }
}
