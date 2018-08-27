package watch.poe.pricer;

import com.google.gson.Gson;
import watch.poe.*;
import watch.poe.pricer.itemdata.ItemdataEntry;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class EntryManager extends Thread {
    //------------------------------------------------------------------------------------------------------------
    // Class variables
    //------------------------------------------------------------------------------------------------------------

    private Set<AccountEntry> accountSet = new HashSet<>();
    private Set<RawEntry> entrySet = new HashSet<>();
    private Map<Integer, Map<String, Double>> currencyLeagueMap = new HashMap<>();
    private StatusElement status = new StatusElement();
    private Gson gson;

    private volatile boolean flagLocalRun = true;
    private volatile boolean readyToExit = false;
    private final Object monitor = new Object();

    //------------------------------------------------------------------------------------------------------------
    // Thread control
    //------------------------------------------------------------------------------------------------------------

    /**
     * Method override for starting this object as a Thread
     */
    public void run() {
        // Loads in currency rates on program start
        currencyLeagueMap = Main.DATABASE.getCurrencyMap();

        // Round counters
        fixCounters();

        // Display counters
        String modtString = String.format("Loaded params: [10m:%3d min][1h:%3d min][24h:%5d min]",
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000);
        Main.ADMIN.log_(modtString, -1);

        // Main thread loop
        while (flagLocalRun) {
            // Wait on monitor
            synchronized (monitor) {
                try {
                    monitor.wait(100);
                } catch (InterruptedException e) { }
            }

            // If monitor was woken, check if correct interval has passed
            if (System.currentTimeMillis() - status.lastRunTime > Config.entryController_sleepMS) {
                status.lastRunTime = System.currentTimeMillis();
                cycle();
            }
        }

        // If main loop was interrupted, raise flag indicating program is ready to safely exit
        readyToExit = true;
    }

    /**
     * Stops the threaded controller and saves all ephemeral data
     */
    public void stopController() {
        Main.ADMIN.log_("Stopping EntryManager", 1);

        flagLocalRun = false;

        while (!readyToExit) try {
            synchronized (monitor) {
                monitor.notify();
            }

            Thread.sleep(50);
        } catch (InterruptedException ex) { }

        uploadRawEntries();
        uploadAccounts();

        Main.ADMIN.log_("EntryManager stopped", 1);
    }

    //------------------------------------------------------------------------------------------------------------
    // Upon initialization
    //------------------------------------------------------------------------------------------------------------

    /**
     * Rounds counters on program start
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
    // Data saving
    //------------------------------------------------------------------------------------------------------------

    private void uploadRawEntries() {
        Set<RawEntry> entrySet = this.entrySet;
        this.entrySet = new HashSet<>();

        Main.DATABASE.uploadRaw(entrySet);
    }

    private void uploadAccounts() {
        Set<AccountEntry> accountSet = this.accountSet;
        this.accountSet = new HashSet<>();

        Main.DATABASE.uploadAccountNames(accountSet);
    }

    //------------------------------------------------------------------------------------------------------------
    // File generation
    //------------------------------------------------------------------------------------------------------------

    /**
     * Creates JSON files for the Get API
     */
    public void generateOutputFiles() {
        List<String> oldFiles = new ArrayList<>();
        List<FileEntry> newFiles = new ArrayList<>();

        Main.DATABASE.getOutputFiles(oldFiles);
        Config.folder_output_get.mkdirs();

        // Process database data and write it to file as JSON
        Main.DATABASE.getOutputData(newFiles);

        // Update database file pointers
        Main.DATABASE.addNewFilePaths(newFiles);

        // Delete old unused files
        deleteGetFiles(oldFiles, newFiles);
    }

    /**
     * Encodes provided parcelEntryList as a JSON file
     *
     * @param parcelEntryList List of ParcelEntry objects, each defining one item
     * @param league Exact string name of league
     * @param category Exact string name of category
     * @return Canonical path of file that was written to or null on error
     */
    public String writeGetFile(List<ParcelEntry> parcelEntryList, String league, String category) {
        // Replace spaces in league name
        league = league.replace(' ', '-');

        String fileName = String.format("%s_%s_%d.json", league, category, System.currentTimeMillis());
        File outputFile = new File(Config.folder_output_get, fileName);

        try (Writer writer = Misc.defineWriter(outputFile)) {
            if (writer == null) throw new IOException();
            gson.toJson(parcelEntryList, writer);
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 4);
            Main.ADMIN.log_("Couldn't write output JSON to file", 3);
        }

        try {
            return outputFile.getCanonicalPath();
        } catch (IOException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Couldn't get file's actual path", 3);
        }

        return null;
    }

    /**
     * Deletes old Get API files from the output directory, keeping the previous set
     *
     * @param oldFiles List of files that were present before generation
     * @param newFiles List of new files that were generated
     */
    private void deleteGetFiles(List<String> oldFiles, List<FileEntry> newFiles) {
        File[] currentFiles = Config.folder_output_get.listFiles();
        if (currentFiles == null) return;

        try {
            for (File currentFile : currentFiles) {
                String currentCanonicalPath = currentFile.getCanonicalPath();

                if (oldFiles.contains(currentCanonicalPath)) continue;

                for (FileEntry newFileEntry : newFiles) {
                    if (newFileEntry.path.equals(currentCanonicalPath)) {
                        currentCanonicalPath = null;
                        break;
                    }
                }

                // Check if previous loop wants this one to skip
                if (currentCanonicalPath == null) continue;

                // Delete the file
                if (!currentFile.delete()) {
                    Main.ADMIN.log_("Could not delete old output file", 3);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Main.ADMIN.log_("Could not delete old output files", 3);
        }
    }

    /**
     * Recreates the itemdata files when a new item has been found
     */
    public void generateItemDataFile() {
        long startTime = System.currentTimeMillis();

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

        Main.ADMIN.log_(String.format("Itemdata rebuilt (%4d ms)", System.currentTimeMillis() - startTime), 0);
    }

    //------------------------------------------------------------------------------------------------------------
    // Cycle
    //------------------------------------------------------------------------------------------------------------

    /**
     * Control method for starting up the minutely cycle
     */
    private void cycle() {
        Main.ADMIN.log_("Cycle starting", 0);
        checkIntervalFlagStates();

        // Check league API every 10 minutes
        if (status.isTenBool()) {
            Main.LEAGUE_MANAGER.download();
        }

        // Check if there are matching account name changes
        if (status.isTwentyFourBool()) {
            Main.ACCOUNT_MANAGER.checkAccountNameChanges();
        }

        // Upload gathered prices
        long time_upload = System.currentTimeMillis();
        uploadRawEntries();
        time_upload = System.currentTimeMillis() - time_upload;

        // Upload account names
        long time_account = System.currentTimeMillis();
        uploadAccounts();
        time_account = System.currentTimeMillis() - time_account;

        // Recalculate prices in database
        long time_cycle = System.currentTimeMillis();
        cycleDatabase();
        time_cycle = System.currentTimeMillis() - time_cycle;

        // Get latest currency rates
        long time_prices = System.currentTimeMillis();
        currencyLeagueMap = Main.DATABASE.getCurrencyMap();
        time_prices = System.currentTimeMillis() - time_prices;

        // Build JSON
        long time_json = System.currentTimeMillis();
        generateOutputFiles();
        time_json = System.currentTimeMillis() - time_json;

        // Build itemdata
        if (Main.RELATIONS.isNewIndexedItem()) {
            generateItemDataFile();
        }

        // Prepare cycle message
        String cycleMsg = String.format("Cycle finished: %5d ms | %2d / %3d / %4d | c:%6d / j:%5d / p:%2d / u:%4d / a:%4d",
                System.currentTimeMillis() - status.lastRunTime,
                10 - (System.currentTimeMillis() - status.tenCounter) / 60000,
                60 - (System.currentTimeMillis() - status.sixtyCounter) / 60000,
                1440 - (System.currentTimeMillis() - status.twentyFourCounter) / 60000,
                time_cycle, time_json, time_prices,
                time_upload, time_account);
        Main.ADMIN.log_(cycleMsg, -1);

        // Switch off flags
        status.setTwentyFourBool(false);
        status.setSixtyBool(false);
        status.setTenBool(false);
    }

    /**
     * Raises certain flags after certain intervals
     */
    private void checkIntervalFlagStates() {
        long current = System.currentTimeMillis();

        // Run once every 10min
        if (current - status.tenCounter > Config.entryController_tenMS) {
            status.tenCounter += (current - status.tenCounter) / Config.entryController_tenMS * Config.entryController_tenMS;
            status.setTenBool(true);
            Main.ADMIN.log_("10 activated", 0);
        }

        // Run once every 60min
        if (current - status.sixtyCounter > Config.entryController_sixtyMS) {
            status.sixtyCounter += (current - status.sixtyCounter) / Config.entryController_sixtyMS * Config.entryController_sixtyMS;
            status.setSixtyBool(true);
            Main.ADMIN.log_("60 activated", 0);
        }

        // Run once every 24h
        if (current - status.twentyFourCounter > Config.entryController_twentyFourMS) {
            if (status.twentyFourCounter == 0) {
                status.twentyFourCounter -= Config.entryController_counterOffset;
            }

            status.twentyFourCounter += (current - status.twentyFourCounter) / Config.entryController_twentyFourMS * Config.entryController_twentyFourMS ;
            status.setTwentyFourBool(true);
            Main.ADMIN.log_("24 activated", 0);
        }
    }

    /**
     * Recalculates database data
     */
    private void cycleDatabase() {
        long a;
        long a10 = 0, a11 = 0, a12 = 0, a13 = 0, a14 = 0, a15 = 0, a16 = 0;
        long a20 = 0, a21 = 0, a22 = 0, a23 = 0, a24 = 0, a25 = 0;
        long a30 = 0;

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

        System.out.printf("{1X series} > [10%5d][11%5d][12%5d][13%5d][14%5d][15%5d][16%5d]\n", a10, a11, a12, a13, a14, a15, a16);

        if (status.isSixtyBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.calcQuantity();
            a22 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.updateMultipliers();
            a23 += System.currentTimeMillis() - a;

            a = System.currentTimeMillis();
            Main.DATABASE.addHourly();
            a24 += System.currentTimeMillis() - a;
        }

        if (status.isTwentyFourBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.addDaily();
            a30 += System.currentTimeMillis() - a;

            System.out.printf("{3X series} > [30%5d]\n", a30);
        }

        if (status.isSixtyBool()) {
            a = System.currentTimeMillis();
            Main.DATABASE.resetCounters();
            a25 += System.currentTimeMillis() - a;

            System.out.printf("{2X series} > [20%5d][21%5d][22%5d][23%5d][24%5d][25%5d]\n", a20, a21, a22, a23, a24, a25);
        }
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

                if (leagueId == null) {
                    leagueId = Main.LEAGUE_MANAGER.getLeagueId(item.getLeague());
                    if (leagueId == null) break;
                }

                item.parseItem();
                if (item.isDiscard()) continue;

                // If the Item's price is not in chaos, convert it to chaos using the latest currency ratios
                item.convertPrice(currencyLeagueMap.get(leagueId));

                Integer itemId = Main.RELATIONS.indexItem(item, leagueId);
                if (itemId == null) continue;

                // Create a RawEntry
                RawEntry rawEntry = new RawEntry();

                // Get the Item's values
                rawEntry.setItemId(itemId);
                rawEntry.setLeagueId(leagueId);
                rawEntry.setAccountName(stash.accountName);
                rawEntry.setPrice(item.getPrice());

                // Add it to the db queue
                entrySet.add(rawEntry);
            }

            if (stash.accountName != null && stash.lastCharacterName != null && leagueId != null) {
                accountSet.add(new AccountEntry(stash.accountName, stash.lastCharacterName, leagueId));
            }
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
