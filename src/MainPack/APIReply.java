package MainPack;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIReply {
    /*   Name: APIReply
     *   Date created: 23.11.2017
     *   Last modified: 23.11.2017
     *   Description: Class used for deserializing a JSON string
     */

    private String next_change_id;
    private List<Stash> stashes;

    /*
     * Methods that set values from outside the class
     */

    public String getNext_change_id() {
        return next_change_id;
    }

    public List<Stash> getStashes() {
        return stashes;
    }

    /*
     * Methods that set values from outside the class
     */

    public void setStashes(List<Stash> stashes) {
        this.stashes = stashes;
    }

    public void setNext_change_id(String next_change_id) {
        this.next_change_id = next_change_id;
    }
}
