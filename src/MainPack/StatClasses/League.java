package MainPack.StatClasses;

public class League {
    /*   Name: League
     *   Date created: 26.11.2017
     *   Last modified: 27.11.2017
     *   Description: Contains statistical data of a certain league
     */

    private String name = "";
    private int normalCount = 0;
    private int magicCount = 0;
    private int rareCount = 0;
    private int uniqueCount = 0;
    private int gemCount = 0;
    private int currencyCount = 0;
    private int otherCount = 0;
    private int corruptedCount = 0;
    private int unidentifiedCount = 0;

    public League (String league) {
        this.name = league;
    }

    public String getName() {
        return name;
    }

    /*
     * Methods that increment values from outside the class
     */

    public void incOtherCount() {
        this.otherCount++;
    }

    public void incNormalCount() {
        this.normalCount++;
    }

    public void incMagicCount() {
        this.magicCount++;
    }

    public void incRareCount() {
        this.rareCount++;
    }

    public void incUniqueCount() {
        this.uniqueCount++;
    }

    public void incGemCount() {
        this.gemCount++;
    }

    public void incCurrencyCount() {
        this.currencyCount++;
    }

    public void incCorruptedCount() {
        this.corruptedCount++;
    }

    public void incUnidentifiedCount() {
        this.unidentifiedCount++;
    }

    /*
     * Methods that actually do something
     */

    public String buildString() {
        /*  Name: buildString()
        *   Date created: 26.11.2017
        *   Last modified: 27.11.2017
        *   Description: Returns a string containing statistical info
        */

        String returnString = "";
        returnString += "[Item stats] " + name + ":\n";
        returnString += "    Normal items: " + normalCount + "\n";
        returnString += "    Magic items: " + magicCount + "\n";
        returnString += "    Rare items: " + rareCount + "\n";
        returnString += "    Unique items: " + uniqueCount + "\n";
        returnString += "    Gems: " + gemCount + "\n";
        returnString += "    Currency: " + currencyCount + "\n";
        returnString += "    Other items: " + otherCount + "\n";
        returnString += "    Corrupted: " + corruptedCount + "\n";
        returnString += "    Unidentified: " + unidentifiedCount + "\n";

        return returnString;
    }

    public void clearStats() {
        /*  Name: clearStats()
        *   Date created: 27.11.2017
        *   Last modified: 27.11.2017
        *   Description: Replaces all statistical values with 0
        */

        normalCount = 0;
        magicCount = 0;
        rareCount = 0;
        uniqueCount = 0;
        gemCount = 0;
        currencyCount = 0;
        otherCount = 0;
        corruptedCount = 0;
        unidentifiedCount = 0;
    }
}


