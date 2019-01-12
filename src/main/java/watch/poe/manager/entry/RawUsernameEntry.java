package poe.manager.entry;

public class RawUsernameEntry {
    public String account, character;
    public Integer league;

    public RawUsernameEntry(String account, String character, Integer league) {
        this.account = account;
        this.character = character;
        this.league = league;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!RawUsernameEntry.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final RawUsernameEntry other = (RawUsernameEntry) obj;

        if ((this.account == null) ? (other.account != null) : !this.account.equals(other.account)) {
            return false;
        }

        return (this.character == null) ? (other.character == null) : this.character.equals(other.character);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.account != null ? this.account.hashCode() : 0);
        hash = 53 * hash + (this.character != null ? this.character.hashCode() : 0);
        return hash;
    }
}
