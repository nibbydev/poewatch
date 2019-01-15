package poe.Managers.Relation;

import java.util.HashMap;
import java.util.Map;

public class CategoryEntry {
    private Integer id;
    private Map<String, Integer> groupNameToId = new HashMap<>();

    public void addGroup(String name, Integer id) {
        groupNameToId.put(name, id);
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    //------------------------------------------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public boolean hasGroup(String name) {
        return groupNameToId.containsKey(name);
    }

    public Integer getGroupId(String groupName) {
        return groupNameToId.get(groupName);
    }

    public void setId(Integer id) {
        if (this.id == null) this.id = id;
    }
}
