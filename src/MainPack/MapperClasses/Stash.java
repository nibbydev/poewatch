package MainPack.MapperClasses;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Stash {
    //  Name: Stash
    //  Date created: 23.11.2017
    //  Last modified: 29.11.2017
    //  Description: Class used for deserializing a JSON string

    private String id;
    private String accountName;
    private String stash;
    private String lastCharacterName;
    private List<Item> items;

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void setLastCharacterName(String lastCharacterName) {
        this.lastCharacterName = lastCharacterName;
    }

    public void setStash(String stash) {
        this.stash = stash;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getId() {
        return id;
    }

    public String getLastCharacterName() {
        return lastCharacterName;
    }

    public String getStash() {
        return stash;
    }
}
