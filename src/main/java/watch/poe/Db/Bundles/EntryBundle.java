package poe.Db.Bundles;

public class EntryBundle {
    private double price;
    private Integer currencyId;

    public EntryBundle(double price) {
        this.price = price;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
