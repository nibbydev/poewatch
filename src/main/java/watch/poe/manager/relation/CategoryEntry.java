package poe.manager.relation;

import java.util.HashMap;
import java.util.Map;

public class CategoryEntry {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Integer id;
    private Map<String, Integer> childCategoryToId = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------
    // Methods
    //------------------------------------------------------------------------------------------------------------

    public void addChild(String childName, Integer childId) {
        childCategoryToId.put(childName, childId);
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters
    //------------------------------------------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public boolean hasChild(String childName) {
        return childCategoryToId.containsKey(childName);
    }

    public Integer getChildCategoryId(String childName) {
        return childCategoryToId.get(childName);
    }

    //------------------------------------------------------------------------------------------------------------
    // Setters
    //------------------------------------------------------------------------------------------------------------

    public void setId(Integer id) {
        if (this.id == null) this.id = id;
    }
}
