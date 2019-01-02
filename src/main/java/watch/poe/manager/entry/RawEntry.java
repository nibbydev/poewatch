package poe.manager.entry;

/**
 * The default format that new entries are stored as before uploading to database
 */
public class RawEntry {
    private double price;
    private int id_l, id_d;
    private long itmCrc, accCrc;

    private static int precision;

    //------------------------------------------------------------------------------------------------------------
    // Equality methods to root out duplicates in a Set
    //------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!RawEntry.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final RawEntry other = (RawEntry) obj;

        if (this.accCrc != other.accCrc) {
            return false;
        }

        if (this.id_l != other.id_l) {
            return false;
        }

        return this.id_d == other.id_d;
    }

    @Override
    public int hashCode() {
        int hash = 3;

        hash = 53 * hash + Long.hashCode(this.accCrc);
        hash = 53 * hash + this.id_l;
        hash = 53 * hash + this.id_d;

        return hash;
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    //------------------------------------------------------------------------------------------------------------

    public String getPrice() {
        String price = Double.toString(this.price);
        int index = price.indexOf('.');

        if (price.length() - index > precision) {
            return price.substring(0, price.indexOf('.') + precision + 1);
        }

        return price;
    }

    public void setAccCrc(long accCrc) {
        this.accCrc = accCrc;
    }

    public void setLeagueId(int id) {
        this.id_l = id;
    }

    public void setItemId(int id) {
        this.id_d = id;
    }

    public int getLeagueId() {
        return id_l;
    }

    public int getItemId() {
        return id_d;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getAccCrc() {
        return accCrc;
    }

    public void setItmCrc(long itmCrc) {
        this.itmCrc = itmCrc;
    }

    public long getItmCrc() {
        return itmCrc;
    }

    public static void setPrecision(int precision) {
        RawEntry.precision = precision;
    }
}
