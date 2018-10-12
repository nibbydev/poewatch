package poe;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class Config {
    //------------------------------------------------------------------------------------------------------------
    // Database
    //------------------------------------------------------------------------------------------------------------

    public static final String db_address = "jdbc:mysql://localhost:3306?serverTimezone=UTC&useSSL=false&allowMultiQueries=true&useUnicode=true&character_set_server=utf8mb4";
    public static final String db_username = "pw_app";
    private static final String db_password = "password goes here";
    public static final String db_database = "pw";

    public static String getDb_password() {
        return db_password;
    }

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

    public static final double entry_volatileRatio = 0.5;
    public static final double entry_approvedMin = 1.1;
    public static final double entry_approvedMax = 2.0;
    public static final double entry_approvedDiv = 250.0;
    public static final int entry_maxCount = 96;

    public static final int entryController_sleepMS         = 60 * 1000;
    public static final int entryController_tenMS           = 10 * 60 * 1000;
    public static final int entryController_sixtyMS         = 60 * 60 * 1000;
    public static final int entryController_twentyFourMS    = 24 * 60 * 60 * 1000;
    public static final long entryController_counterOffset  = 12 * 60 * 60 * 1000;

    //------------------------------------------------------------------------------------------------------------
    // league manager
    //------------------------------------------------------------------------------------------------------------

    public static final String league_APIBaseURL = "http://api.pathofexile.com/leagues?type=main";
    public static final String league_timeFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final int league_readTimeoutMS = 3000;
    public static final int league_connectTimeoutMS = 5000;
    public static final int league_downloadBufferSize = 64;
    public static final int league_millisecondsInDay = 24 * 60 * 60 * 1000;

    //------------------------------------------------------------------------------------------------------------
    // Price precision
    //------------------------------------------------------------------------------------------------------------

    public static final int precision = 8;
    public static final double pricePrecision = Math.pow(10, precision);
    private static final String formatPattern = "#." + new String(new char[Config.precision]).replace("\0", "#");
    public static final DecimalFormat decimalFormat = new DecimalFormat(formatPattern);

    //------------------------------------------------------------------------------------------------------------
    // Other?
    //------------------------------------------------------------------------------------------------------------

    public static final int timerLogHistoryLength = 5;
    public static final int monitorTimeoutMS = 500;
    public static final String enchantment_icon = "http://web.poecdn.com/image/Art/2DItems/Currency/Enchantment.png?scale=1&w=1&h=1";
}
