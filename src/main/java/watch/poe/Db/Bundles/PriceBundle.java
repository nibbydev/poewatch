package poe.Db.Bundles;

public class PriceBundle {
    private int leagueId, itemId;
    private double mean;

    public PriceBundle(int leagueId, int itemId, double mean) {
        this.leagueId = leagueId;
        this.itemId = itemId;
        this.mean = mean;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public int getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(int leagueId) {
        this.leagueId = leagueId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
}
