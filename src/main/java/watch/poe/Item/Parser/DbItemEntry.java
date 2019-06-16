package poe.Item.Parser;

public class DbItemEntry {
    public int id_l, id_d;
    public long stash_crc, item_crc;
    public Double price;
    public Integer id_price, stackSize;
    public User user;

    DbItemEntry(int id_l, int id_d, long stash_crc, long item_crc, Integer stackSize, Price price, User user) {
        this.id_l = id_l;
        this.id_d = id_d;
        this.stash_crc = stash_crc;
        this.item_crc = item_crc;
        this.stackSize = stackSize;
        this.user = user;

        if (price.hasPrice()) {
            this.price = price.getPrice();
            this.id_price = price.getCurrencyId();
        }
    }
}
