package com.poestats;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

public class Config {
    //------------------------------------------------------------------------------------------------------------
    // Database
    //------------------------------------------------------------------------------------------------------------

    public static final String db_address = "jdbc:mysql://localhost:3306?serverTimezone=UTC&useSSL=false&allowMultiQueries=true";
    public static final String db_username = "root";
    private static final String db_password = "";
    public static final String db_database = "ps2_database";

    public static String getDb_password() {
        return db_password;
    }

    public static final String sql_interval_1h = "1 HOUR";
    public static final String sql_interval_1d = "1 DAY";
    public static final String sql_interval_7d = "7 DAY";

    public static final int sql_id_category_history_hourly = 2;

    //------------------------------------------------------------------------------------------------------------
    // File and folder locations
    //------------------------------------------------------------------------------------------------------------

    public static final File folder_root            = new File(".");
    public static final File folder_output          = new File(folder_root.getPath(), "output");
    public static final File folder_output_get      = new File(folder_output.getPath(), "get");
    public static final File folder_output_itemdata = new File(folder_output.getPath(), "itemdata");
    public static final File file_config            = new File(folder_root.getPath(),"config.cfg");
    public static final URL  resource_config        = Main.class.getResource("/resources/" + file_config.getName());

    //------------------------------------------------------------------------------------------------------------
    // Index definitions
    //------------------------------------------------------------------------------------------------------------

    public static final String index_separator = ".";
    public static final String index_separator_regex = "\\.";
    public static final String index_superBase = "00000";
    public static final String index_subBase = "00";
    public static final int index_superSize = index_superBase.length();
    public static final int index_subSize = index_subBase.length();
    public static final int index_size = index_superSize + index_subSize;

    //------------------------------------------------------------------------------------------------------------
    // Admin
    //------------------------------------------------------------------------------------------------------------

    public static final int admin_logSize = 2048;

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

    public static final int entry_volatileFlat = 10;
    public static final double entry_volatileRatio = 0.5;
    public static final int outlier_hoursCalculated = 1;
    public static final double outlier_discardRatio = 0.7;
    public static final double outlier_devMulti = 2.0;
    public static final double outlier_priceMulti = 2.0;
    public static final int outlier_minCount = 5;

    public static final int entry_maxCount = 96;

    public static final int entryController_sleepMS         = 60 * 1000;
    public static final int entryController_tenMS           = 10 * 60 * 1000;
    public static final int entryController_sixtyMS         = 60 * 60 * 1000;
    public static final int entryController_twentyFourMS    = 24 * 60 * 60 * 1000;

    public static final long entryController_counterOffset  = 5 * 60 * 60 * 1000;

    //------------------------------------------------------------------------------------------------------------
    // league manager
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

    public static final double item_pricePrecision = 10000.0;
    public static final int item_pricePrecision2 = 4;
    public static final int monitorTimeoutMS = 500;
    public static final long startTime = System.currentTimeMillis();
    public static final String enchantment_icon = "http://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
}
