package MainPack.MapperClasses;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Socket {
    /*   Name: Sockets
     *   Date created: 28.11.2017
     *   Last modified: 28.11.2017
     *   Description: Class used for deserializing a JSON string
     */

    private int group;
    private String attr;

    public int getGroup() {
        return group;
    }

    public String getAttr() {
        return attr;
    }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public void setGroup(int group) {
        this.group = group;
    }
}
