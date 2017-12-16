package com.sanderh;

import com.sanderh.Mappers.Item;

public class Statistics {
    //  Name: Statistics
    //  Date created: 16.12.2017
    //  Last modified: 16.12.2017
    //  Description: Object meant to be used as a static statistics collector

    private final long startTime = System.currentTimeMillis();
    private String latestChangeID;

    private int pullCountTotal = 0;
    private int pullCountFailed = 0;
    private int pullCountDuplicate = 0;

    private int itemCountCorrupted = 0;
    private int itemCountUnidentified = 0;
    private int[] itemCountFrameType = {0,0,0,0,0,0,0,0,0,0};

    public void parseItem(Item item) {
        //  Name: parseItem
        //  Date created: 16.12.2017
        //  Last modified: 16.12.2017
        //  Description: Checks some item values, adds result to counters

        if(item.isCorrupted()) itemCountCorrupted++;
        if(!item.isIdentified()) itemCountUnidentified++;
        itemCountFrameType[item.getFrameType()]++;
    }


    public void setLatestChangeID(String latestChangeID) {
        this.latestChangeID = latestChangeID;
    }

    public void incPullCountTotal() {
        this.pullCountTotal++;
    }

    public void incPullCountFailed() {
        this.pullCountFailed++;
    }

    public void incPullCountDuplicate() {
        this.pullCountDuplicate++;
    }
}
