package poe.Item.Parser;

import poe.Item.Deserializers.Stash;

import java.util.Objects;

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

    public User(int l, Stash stash) {
        this.leagueId = l;
        this.accountName = stash.accountName;
        this.characterName = stash.lastCharacterName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return leagueId == user.leagueId &&
                accountName.equals(user.accountName) &&
                Objects.equals(characterName, user.characterName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountName, characterName, leagueId);
    }
}
