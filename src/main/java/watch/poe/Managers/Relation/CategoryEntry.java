package poe.Managers.Relation;

import java.util.HashMap;
import java.util.Map;

public class CategoryEntry {
    private final Integer categoryId;
    private final Map<String, Integer> groupNameToId = new HashMap<>();

    public CategoryEntry(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public void addGroup(String groupName, Integer groupId) {
        groupNameToId.put(groupName, groupId);
    }

    public int getCategoryId() {
        return categoryId;
    }

    public boolean hasGroup(String groupName) {
        return groupNameToId.containsKey(groupName);
    }

    public Integer getGroupId(String groupName) {
        return groupNameToId.get(groupName);
    }
}
