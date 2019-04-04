package poe.Managers.Price.Bundles;

public class IdBundle {
    private int leagueId, itemId;

    public IdBundle(int leagueId, int itemId) {
        this.leagueId = leagueId;
        this.itemId = itemId;
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
