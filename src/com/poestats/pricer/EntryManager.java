package com.poestats.pricer;

import com.google.gson.Gson;
import com.poestats.*;
import com.poestats.database.Database;
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

    private List<Le2Id2Ac2Raw> entryMapList = new ArrayList<>();
    private Map<String, List<Integer>> leagueToIds = new HashMap<>();
    private Map<String, Map<String, Double>> currencyLeagueMap;
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
        currencyLeagueMap = new HashMap<>();

        for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
            String league = leagueEntry.getName();

            Map<String, Double> currencyMap = currencyLeagueMap.getOrDefault(league, new HashMap<>());
            Main.DATABASE.getCurrency(league, currencyMap);
            currencyLeagueMap.putIfAbsent(league, currencyMap);
        }
    }

    private void upload() {
        List<Le2Id2Ac2Raw> entryMaps = entryMapList;
        entryMapList = new ArrayList<>();

        Le2Id2Ac2Raw mergedMap = new Le2Id2Ac2Raw();

        // Merge all gathered data
        for (Le2Id2Ac2Raw entryMap : entryMaps) {
            for (String league : entryMap.keySet()) {
                Id2Ac2Raw id2Ac2Raw = entryMap.get(league);
                Id2Ac2Raw mergedId2Ac2Raw = mergedMap.getOrDefault(league, new Id2Ac2Raw());
                List<Integer> idList = leagueToIds.getOrDefault(league, new ArrayList<>());

                for (Integer id : id2Ac2Raw.keySet()) {
                    Ac2Raw ac2Raw = id2Ac2Raw.get(id);
                    Ac2Raw mergedAc2Raw = mergedId2Ac2Raw.getOrDefault(id, new Ac2Raw());

                    for (Map.Entry<String, RawEntry> entry : ac2Raw.entrySet()) {
                        mergedAc2Raw.put(entry.getKey(), entry.getValue());
                    }

                    mergedId2Ac2Raw.putIfAbsent(id, mergedAc2Raw);
                    if (!idList.contains(id)) idList.add(id);
                }

                mergedMap.putIfAbsent(league, mergedId2Ac2Raw);
                leagueToIds.putIfAbsent(league, idList);
            }
        }

        // Upload merged data
        for (String league : mergedMap.keySet()) {
            Id2Ac2Raw idToAccountToRawEntry = mergedMap.get(league);

            Main.DATABASE.uploadRaw(league, idToAccountToRawEntry);
        }
    }

    /**
     * Writes all collected data to database
     */
    private void cycle() {
        Map<String, List<Integer>> leagueToIds = this.leagueToIds;
        this.leagueToIds = new HashMap<>();

        // Allow workers to switch to new map
        try { Thread.sleep(150); } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }

        if (status.isSixtyBool()) {
            for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
                String league = leagueEntry.getName();
                Main.DATABASE.updateVolatile(league);
            }
        }

        long a, a0 = 0, a1 = 0, a2 = 0, a3 = 0, a4 = 0, a5 = 0, a6 = 0, a7 = 0, a8 = 0;

        for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
            String league = leagueEntry.getName();

            a = System.currentTimeMillis();
            Main.DATABASE.updateApproved(league);
            a0 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.updateCounters(league);
            a1 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.calculateMean(league);
            a2 += System.currentTimeMillis() - a;

            List<Integer> idList = leagueToIds.get(league);

            if (idList != null) {
                a = System.currentTimeMillis();
                Main.DATABASE.calculateMedian(league, idList);
                a3 += System.currentTimeMillis() - a;

                a = System.currentTimeMillis();
                Main.DATABASE.calculateMode(league, idList);
                a4 += System.currentTimeMillis() - a;

                a = System.currentTimeMillis();
                Main.DATABASE.removeOldItemEntries(league, idList);
                a5 += System.currentTimeMillis() - a;
            }

            a = System.currentTimeMillis();
            Main.DATABASE.calculateExalted(league);
            a6 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.addMinutely(league);
            a7 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.removeOldHistoryEntries(league, 1, Config.sql_interval_1h);
            a8 += System.currentTimeMillis() - a;
        }

        if (status.isSixtyBool()) {
            for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
                String league = leagueEntry.getName();

                Main.DATABASE.removeOldHistoryEntries(league, 2, Config.sql_interval_1d);
                Main.DATABASE.addHourly(league);
                Main.DATABASE.resetVolatile(league);
                Main.DATABASE.calcQuantity(league);
            }
        }

        if (status.isTwentyFourBool()) {
            for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
                String league = leagueEntry.getName();

                Main.DATABASE.addDaily(league);
                Main.DATABASE.removeOldHistoryEntries(league, 3, Config.sql_interval_120d);
            }
        }

        //System.out.printf("0(%4d) 1(%4d) 2(%4d) 3(%4d) 4(%4d) 5(%4d) 6(%4d) 7(%4d) 8(%4d)\n", a0, a1, a2, a3, a4, a5, a6, a7, a8);
    }

    private void generateOutputFiles() {
        List<String> oldOutputFiles = new ArrayList<>();
        List<String> newOutputFiles = new ArrayList<>();

        Main.DATABASE.getOutputFiles(oldOutputFiles);
        Config.folder_output_get.mkdirs();

        for (LeagueEntry leagueEntry : Main.LEAGUE_MANAGER.getLeagues()) {
            String league = Database.formatLeague(leagueEntry.getName());

            for (Map.Entry<String, CategoryEntry> category : Main.RELATIONS.getCategoryRelations().entrySet()) {
                Map<Integer, ParcelEntry> tmpParcel = new LinkedHashMap<>();

                int categoryId = category.getValue().getId();

                Main.DATABASE.getOutputItems(league, tmpParcel, categoryId);
                Main.DATABASE.getOutputHistory(league, tmpParcel);

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
        generateOutputFiles();
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
        Le2Id2Ac2Raw le2Id2Ac2Raw = new Le2Id2Ac2Raw();

        for (Mappers.Stash stash : reply.stashes) {
            String league = null;

            for (Item item : stash.items) {
                if (!Main.WORKER_MANAGER.isFlag_Run()) return;

                if (league == null) league = item.getLeague();

                item.fix();
                item.parseItem();
                if (item.isDiscard()) continue;

                RawEntry rawEntry = new RawEntry();
                rawEntry.load(item);

                boolean discard = rawEntry.convertPrice(currencyLeagueMap.get(league));
                if (discard) continue; // Couldn't convert the listed currency to chaos

                Integer id = Main.RELATIONS.indexItem(item, league);
                if (id == null) continue;

                Id2Ac2Raw id2Ac2Raw = le2Id2Ac2Raw.getOrDefault(league, new Id2Ac2Raw());
                Ac2Raw ac2Raw = id2Ac2Raw.getOrDefault(id, new Ac2Raw());

                ac2Raw.put(stash.accountName, rawEntry);

                id2Ac2Raw.putIfAbsent(id, ac2Raw);
                le2Id2Ac2Raw.putIfAbsent(league, id2Ac2Raw);
            }
        }

        entryMapList.add(le2Id2Ac2Raw);
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
