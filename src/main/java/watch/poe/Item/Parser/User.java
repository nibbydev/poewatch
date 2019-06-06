package poe.Item.Parser;

public class User {
    public String accountName;
    public long accountId;

    public String characterName;
    public int leagueId;

    public User(int l, String a, String c) {
        this.leagueId = l;
        this.accountName = a;
        this.characterName = c;
    }
}
