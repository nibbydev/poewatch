package poe.Item.Deserializers;

/**
 * Universal deserializer for poe.ninja, poe.rates and pathofexile.com/api
 */
public class ChangeID {
    public String next_change_id;
    public String changeId;
    public String psapi;

    public String get() {
        if (next_change_id != null) return next_change_id;
        else if (changeId != null) return changeId;
        else return psapi;
    }
}
