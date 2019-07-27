package poe.Price.Bundles;

public class EntryBundle {
    private long accountId;
    private double price;
    private Integer currencyId;

    public EntryBundle(){}

    public EntryBundle(long accountId, Integer currencyId, double price) {
        this.accountId = accountId;
        this.currencyId = currencyId;
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

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }
}
