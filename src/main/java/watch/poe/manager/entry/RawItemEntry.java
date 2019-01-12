package poe.manager.entry;

public class RawItemEntry {
    public int id_l, id_d;
    public long account_crc, stash_crc, item_crc;
    public double price;

    public RawItemEntry(int id_l, int id_d, long account_crc, long stash_crc, long item_crc, double price) {
        this.id_l = id_l;
        this.id_d = id_d;

        this.account_crc = account_crc;
        this.stash_crc = stash_crc;
        this.item_crc = item_crc;

        this.price = price;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!RawItemEntry.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final RawItemEntry other = (RawItemEntry) obj;

        if (this.id_l != other.id_l) {
            return false;
        }

        if (this.account_crc != other.account_crc) {
            return false;
        }

        if (this.id_d != other.id_d) {
            return false;
        }

        if (this.item_crc != other.item_crc) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;

        hash = 53 * hash + this.id_l;
        hash = 53 * hash + this.id_d;
        hash = 53 * hash + Long.hashCode(this.account_crc);
        hash = 53 * hash + Long.hashCode(this.item_crc);

        return hash;
    }
}
