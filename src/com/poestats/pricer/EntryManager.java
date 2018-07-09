package com.poestats.pricer;

import com.google.gson.Gson;
import com.poestats.*;
import com.poestats.league.LeagueEntry;
import com.poestats.pricer.itemdata.ItemdataEntry;
import com.poestats.pricer.RawMaps.*;
import com.poestats.relations.CategoryEntry;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class EntryManager {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Set<RawEntry> entrySet = new HashSet<>();
    private Map<Integer, Map<String, Double>> currencyLeagueMap = new HashMap<>();
    private StatusElement status = new StatusElement();
    private Gson gson;

    //------------------------------------------------------------------------------------------------------------
    // Main methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Loads data in from file on object initialization
     */
    public void init() {
        loadStartParameters();
        loadCurrency();
    }

    /**
     * Loads status data from file on program start
     */
    private void loadStartParameters() {
        fixCounters();

        String tenMinDisplay = "[10m:" + String.format("%3d", 10 - (System.currentTimeMillis() - status.tenCounter) / 60000) + " min]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%5d", 1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000) + " min]";
        Main.ADMIN.log_("Loaded params: " + tenMinDisplay + resetTimeDisplay + twentyHourDisplay, -1);
    }

    //------------------------------------------------------------------------------------------------------------
    // Methods for multi-db file structure
    //------------------------------------------------------------------------------------------------------------

    /**
     * Loads in currency rates on program start
     */
    private void loadCurrency() {
        Main.DATABASE.getCurrency(currencyLeagueMap);
    }

    /**
     * Writes all collected data to database
     */
    private void cycle() {
        long a;
        long a10 = 0, a11 = 0, a12 = 0, a13 = 0, a14 = 0, a15 = 0, a16 = 0, a17 = 0;
        long a20 = 0, a21 = 0, a22 = 0, a23 = 0, a24 = 0, a25 = 0, a26 = 0;
        long a30 = 0, a31 = 0, a32 = 0;

        if (status.isSixtyBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.updateVolatile();
            a20 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.calculateVolatileMedian();
            a21 += System.currentTimeMillis() - a;
        }

        a = System.currentTimeMillis();
        Main.DATABASE.updateApproved();
        a10 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.updateCounters();
        a11 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.calculateMean();
        a12 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.calculateMedian();
        a13 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.calculateMode();
        a14 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.removeOldItemEntries();
        a15 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.calculateExalted();
        a16 += System.currentTimeMillis() - a;

        a = System.currentTimeMillis();
        Main.DATABASE.addMinutely();
        a17 += System.currentTimeMillis() - a;

        System.out.printf("{1X series} > [10%5d][11%5d][12%5d][13%5d][14%5d][15%5d][16%5d][17%5d]\n", a10, a11, a12, a13, a14, a15, a16, a17);

        if (status.isSixtyBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.calcQuantity();
            a22 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.updateMultipliers();
            a23 += System.currentTimeMillis() - a;

            // Ought to be before addHourly()
            a = System.currentTimeMillis();
            Main.DATABASE.removeOldHistoryEntries( 1, Config.sql_interval_60m);
            a24 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.addHourly();
            a25 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.resetCounters();
            a26 += System.currentTimeMillis() - a;

            System.out.printf("{2X series} > [20%5d][21%5d][22%5d][23%5d][24%5d][25%5d][26%5d]\n", a20, a21, a22, a23, a24, a25, a26);
        }

        if (status.isTwentyFourBool()) {
            // Ought to be before addDaily()
            a = System.currentTimeMillis();
            Main.DATABASE.removeOldHistoryEntries(2, Config.sql_interval_24h);
            a30 += System.currentTimeMillis() - a;

            // TODO: don't delete inactive league entries
            //a = System.currentTimeMillis();
            //Main.DATABASE.removeOldHistoryEntries(3, Config.sql_interval_120d);
            //a31 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.addDaily();
            a32 += System.currentTimeMillis() - a;

            System.out.printf("{3X series} > [30%5d][31%5d][32%5d]\n", a30, a31, a32);
        }
    }

    private void upload() {
        Set<RawEntry> entrySet = this.entrySet;
        this.entrySet = new HashSet<>();

        Main.DATABASE.uploadRaw(entrySet);
    }

    private void generateOutputFiles() {
        List<String> oldOutputFiles = new ArrayList<>();
        List<String> newOutputFiles = new ArrayList<>();

        Main.DATABASE.getOutputFiles(oldOutputFiles);
        Config.folder_output_get.mkdirs();

        for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
            Integer leagueId = leagueEntry.getId();
            String league = leagueEntry.getName();

            for (Map.Entry<String, CategoryEntry> category : Main.RELATIONS.getCategoryRelations().entrySet()) {
                Map<Integer, ParcelEntry> tmpParcel = new LinkedHashMap<>();

                int categoryId = category.getValue().getId();

                Main.DATABASE.getOutputItems(leagueId, tmpParcel, categoryId);
                Main.DATABASE.getOutputHistory(leagueId, tmpParcel);

                List<ParcelEntry> parcel = new ArrayList<>();
                for (ParcelEntry parcelEntry : tmpParcel.values()) {
                    parcelEntry.calcSpark();
                    parcel.add(parcelEntry);
                }

                String fileName = league + "_" + category.getKey() + "_" + System.currentTimeMillis() + ".json";
                File outputFile = new File(Config.folder_output_get, fileName);

                try (Writer writer = Misc.defineWriter(outputFile)) {
                    if (writer == null) throw new IOException();
                    gson.toJson(parcel, writer);
                } catch (IOException ex) {
                    Main.ADMIN._log(ex, 4);
                    Main.ADMIN.log_("Couldn't write output JSON to file", 3);
                }

                try {
                    String path = outputFile.getCanonicalPath();
                    newOutputFiles.add(path);
                    Main.DATABASE.addOutputFile(league, category.getKey(), path);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Main.ADMIN.log_("Couldn't get file's actual path", 3);
                }
            }
        }

        File[] outputFiles = Config.folder_output_get.listFiles();
        if (outputFiles == null) return;

        try {
            for (File outputFile : outputFiles) {
                if (oldOutputFiles.contains(outputFile.getCanonicalPath())) continue;
                if (newOutputFiles.contains(outputFile.getCanonicalPath())) continue;

                boolean success = outputFile.delete();
                if (!success) Main.ADMIN.log_("Could not delete old output file", 3);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not delete old output files", 3);
        }
    }

    private void generateItemDataFile() {
        List<String> oldItemdataFiles = new ArrayList<>();
        Main.DATABASE.getOutputFiles(oldItemdataFiles);

        Config.folder_output_itemdata.mkdirs();

        List<ItemdataEntry> parcel = new ArrayList<>();
        Main.DATABASE.getItemdata(parcel);

        String fileName = "itemdata_" + System.currentTimeMillis() + ".json";
        File itemdataFile = new File(Config.folder_output_itemdata, fileName);

        try (Writer writer = Misc.defineWriter(itemdataFile)) {
            if (writer == null) throw new IOException();
            gson.toJson(parcel, writer);
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
            Main.ADMIN.log_("Couldn't write itemdata to file", 3);
        }

        try {
            String path = itemdataFile.getCanonicalPath();
            Main.DATABASE.addOutputFile("itemdata", "itemdata", path);
        } catch (IOException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Couldn't get file's actual path", 3);
        }

        File[] itemdataFiles = Config.folder_output_itemdata.listFiles();
        if (itemdataFiles == null) return;

        try {
            for (File file : itemdataFiles) {
                if (oldItemdataFiles.contains(file.getCanonicalPath())) continue;
                else if (itemdataFile.getCanonicalPath().equals(file.getCanonicalPath())) continue;

                boolean success = file.delete();
                if (!success) Main.ADMIN.log_("Could not delete old itemdata file", 3);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not delete old itemdata files", 3);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Often called controller methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Main loop of the pricing service. Can be called whenever, only runs after specific amount of time has passed
     */
    public void run() {
        long current = System.currentTimeMillis();

        // Run every minute (-ish)
        if (current - status.lastRunTime < Config.entryController_sleepMS) return;
        status.lastRunTime = System.currentTimeMillis();

        // Allow workers to pause
        try { Thread.sleep(50); } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }

        // Run once every 10min
        if (current - status.tenCounter > Config.entryController_tenMS) {
            status.tenCounter += (current - status.tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            status.setTenBool(true);
            Main.ADMIN.log_("10 activated", 0);

            Main.LEAGUE_MANAGER.download();
        }

        // Run once every 60min
        if (current - status.sixtyCounter > Config.entryController_sixtyMS) {
            status.sixtyCounter += (current - status.sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            status.setSixtyBool(true);
            Main.ADMIN.log_("60 activated", 0);
        }

        // Run once every 24h
        if (current - status.twentyFourCounter > Config.entryController_twentyFourMS) {
            if (status.twentyFourCounter == 0) status.twentyFourCounter -= Config.entryController_counterOffset;
            status.twentyFourCounter += (current - status.twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS ;
            status.setTwentyFourBool(true);
            Main.ADMIN.log_("24 activated", 0);
        }

        // Upload gathered prices
        long time_upload = System.currentTimeMillis();
        upload();
        time_upload = System.currentTimeMillis() - time_upload;

        // Sort JSON
        long time_cycle = System.currentTimeMillis();
        cycle();
        time_cycle = System.currentTimeMillis() - time_cycle;

        // Get latest currency rates
        long time_load_currency = System.currentTimeMillis();
        loadCurrency();
        time_load_currency = System.currentTimeMillis() - time_load_currency;

        // Build JSON
        long time_json = System.currentTimeMillis();
        //generateOutputFiles();
        time_json = System.currentTimeMillis() - time_json;

        // Build itemdata
        if (Main.RELATIONS.isNewIndexedItem()) {
            long time_itemdata = System.currentTimeMillis();
            generateItemDataFile();
            time_itemdata = System.currentTimeMillis() - time_itemdata;
            Main.ADMIN.log_(String.format("Itemdata rebuilt (%4d ms)", time_itemdata), 0);
        }

        // Prepare message
        String timeElapsedDisplay = "[Took:" + String.format("%5d", System.currentTimeMillis() - status.lastRunTime) + " ms]";
        String tenMinDisplay = "[10m:" + String.format("%2d", 10 - (System.currentTimeMillis() - status.tenCounter) / 60000) + " min]";
        String resetTimeDisplay = "[1h:" + String.format("%3d", 60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000) + " min]";
        String twentyHourDisplay = "[24h:" + String.format("%4d", 1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000) + " min]";
        String timeTookDisplay = "(C:" + String.format("%5d", time_cycle) + " ms)(J:" + String.format("%4d", time_json) +
                " ms)(C:" + String.format("%3d", time_load_currency) + " ms)(U:" + String.format("%4d", time_upload) + " ms)";
        Main.ADMIN.log_(timeElapsedDisplay + tenMinDisplay + resetTimeDisplay + twentyHourDisplay + timeTookDisplay, -1);

        // Switch off flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);
    }

    /**
     * Adds entries to the databases
     *
     * @param reply APIReply object that a worker has downloaded and deserialized
     */
    public void parseItems(Mappers.APIReply reply) {
        for (Mappers.Stash stash : reply.stashes) {
            Integer leagueId = null;

            for (Item item : stash.items) {
                if (!Main.WORKER_MANAGER.isFlag_Run()) return;

                if (leagueId == null) leagueId = Main.LEAGUE_MANAGER.getLeagueId(item.getLeague());
                if (leagueId == null) continue;

                item.fix();
                item.parseItem();
                if (item.isDiscard()) continue;

                RawEntry rawEntry = new RawEntry();
                rawEntry.load(item);

                boolean discard = rawEntry.convertPrice(currencyLeagueMap.get(leagueId));
                if (discard) continue; // Couldn't convert the listed currency to chaos

                Integer itemId = Main.RELATIONS.indexItem(item, leagueId);
                if (itemId == null) continue;

                rawEntry.setItemId(itemId);
                rawEntry.setLeagueId(leagueId);
                rawEntry.setAccountName(stash.accountName);

                entrySet.add(rawEntry);
            }
        }
    }

    /**
     * Makes sure counters don't fall behind
     */
    private void fixCounters() {
        long current = System.currentTimeMillis();

        if (current - status.tenCounter > Config.entryController_tenMS) {
            long gap = (current - status.tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            status.tenCounter += gap;
        }

        if (current - status.sixtyCounter > Config.entryController_sixtyMS) {
            long gap = (current - status.sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            status.sixtyCounter += gap;
        }

        if (current - status.twentyFourCounter > Config.entryController_twentyFourMS) {
            if (status.twentyFourCounter == 0) status.twentyFourCounter -= Config.entryController_counterOffset;
            long gap = (current - status.twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS;
            status.twentyFourCounter += gap;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Getters and setters
    //------------------------------------------------------------------------------------------------------------

    public StatusElement getStatus() {
        return status;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }
}
