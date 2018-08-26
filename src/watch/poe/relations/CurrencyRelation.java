package watch.poe.relations;

public class CurrencyRelation {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private String name;
    private String[] aliases;

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases;
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public void setName(String name) {
        this.name = name;
    }
}