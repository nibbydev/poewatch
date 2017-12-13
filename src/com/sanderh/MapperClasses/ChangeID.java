package com.sanderh.MapperClasses;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeID {
    //  Name: ChangeID()
    //  Date created: 30.11.2017
    //  Last modified: 30.11.2017
    //  Description: Maps http://poe.ninja 's and http://poe-rates.com 's JSON API to an object

    private String next_change_id;
    private String changeId;

    ///////////////////////
    // Getters / Setters //
    ///////////////////////

    public String getNext_change_id() {
        return next_change_id;
    }

    public void setNext_change_id(String next_change_id) {
        this.next_change_id = next_change_id;
    }

    public String getChangeId() {
        return next_change_id;
    }

    public void setChangeId(String changeId) {
        this.next_change_id = changeId;
    }
}
