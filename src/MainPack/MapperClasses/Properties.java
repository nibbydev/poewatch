package MainPack.MapperClasses;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Properties {
    //  Name: Properties
    //  Date created: 28.11.2017
    //  Last modified: 29.11.2017
    //  Description: Class used for deserializing a JSON string

    private String name;
    private List<List<String>> values;

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public String getName() {
        return name;
    }

    public List<List<String>> getValues() {
        return values;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValues(List<List<String>> values) {
        this.values = values;
    }
}
