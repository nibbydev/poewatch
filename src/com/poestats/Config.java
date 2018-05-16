package com.poestats;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

public class Config {
    //------------------------------------------------------------------------------------------------------------
    // File and folder locations
    //------------------------------------------------------------------------------------------------------------

    public static final File folder_root        = new File(".");
    public static final File folder_data        = new File(folder_root.getPath(), "data");
    public static final File folder_database    = new File(folder_data.getPath(), "database");
    public static final File folder_output      = new File(folder_data.getPath(), "output");
    public static final File folder_history     = new File(folder_data.getPath(), "history");
    public static final File folder_backups     = new File(folder_root.getPath(), "backups");

    public static final File file_leagueData    = new File(folder_data.getPath(),"leagueData.json");
    public static final File file_leagueList    = new File(folder_data.getPath(),"leagueList.json");
    public static final File file_config        = new File(folder_root.getPath(),"config.cfg");
    public static final File file_relations     = new File(folder_data.getPath(),"currencyRelations.json");
    public static final File file_itemData      = new File(folder_data.getPath(),"itemData.json");
    public static final File file_categories    = new File(folder_data.getPath(),"categories.json");
    public static final File file_changeID      = new File(folder_data.getPath(),"changeID.json");
    public static final File file_status        = new File(folder_data.getPath(),"status.csv");

    public static final URL  resource_config     = Main.class.getResource("/resources/" + file_config.getName());
    public static final URL  resource_relations  = Main.class.getResource("/resources/" + file_relations.getName());

    //------------------------------------------------------------------------------------------------------------
    // Index definitions
    //------------------------------------------------------------------------------------------------------------

    public static final String index_superBase = "0000";
    public static final String index_subBase = "00";
    public static final String index_separator = "-";
    public static final int index_superSize = index_superBase.length();
    public static final int index_subSize = index_subBase.length();
    public static final int index_size = index_superSize + index_separator.length() + index_subSize;

    //------------------------------------------------------------------------------------------------------------
    // Admin
    //------------------------------------------------------------------------------------------------------------

    public static final int admin_logSize = 2048;
    public static final int admin_zipBufferSize = 1024;

    //------------------------------------------------------------------------------------------------------------
    // Workers
    //------------------------------------------------------------------------------------------------------------

    public static final String worker_APIBaseURL = "http://www.pathofexile.com/api/public-stash-tabs?id=";
    public static final Pattern worker_changeIDRegexPattern = Pattern.compile("\\d*-\\d*-\\d*-\\d*-\\d*");
    public static final int worker_downloadDelayMS = 1200;
    public static final int worker_downloadBufferSize = 128;
    public static final int worker_readTimeoutMS = 12000;
    public static final int worker_connectTimeoutMS = 10000;
    public static final int worker_lockTimeoutMS = 5000;
    public static final int worker_defaultWorkerCount = 3;

    //------------------------------------------------------------------------------------------------------------
    // Entry
    //------------------------------------------------------------------------------------------------------------

    public static final int entry_itemsSize = 64;
    public static final int entry_tempSize = 16;
    public static final int entry_minutelySize = 6;
    public static final int entry_hourlySize = 24;
    public static final int entry_dailySize = 7;

    public static final int entry_pluckPercentLT2 = 10000;
    public static final int entry_pluckPercentGT2 = 500;

    public static final int entry_shiftPercent = 80;
    public static final int entry_shiftPercentNew = 60;

    public static final int entryController_sleepMS         = 60 * 1000;
    public static final int entryController_tenMS           = 10 * 60 * 1000;
    public static final int entryController_sixtyMS         = 60 * 60 * 1000;
    public static final int entryController_twentyFourMS    = 24 * 60 * 60 * 1000;

    public static final long entryController_counterOffset  = 5 * 60 * 60 * 1000;

    //------------------------------------------------------------------------------------------------------------
    // League manager
    //------------------------------------------------------------------------------------------------------------

    public static final String league_APIBaseURL = "http://api.pathofexile.com/leagues?type=main&compact=1";
    public static final String league_timeFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final int league_readTimeoutMS = 3000;
    public static final int league_connectTimeoutMS = 5000;
    public static final int league_downloadBufferSize = 64;
    public static final int league_millisecondsInDay = 24 * 60 * 60 * 1000;

    //------------------------------------------------------------------------------------------------------------
    // Other?
    //------------------------------------------------------------------------------------------------------------

    public static final double item_pricePrecision = 1000.0;
    public static final int misc_defaultLeagueLength = 90;
    public static final int monitorTimeoutMS = 500;
    public static final long startTime = System.currentTimeMillis();
}
