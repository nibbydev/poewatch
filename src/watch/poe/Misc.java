package watch.poe;

import java.io.*;
import java.util.Calendar;
import java.util.List;

public class Misc {
    //------------------------------------------------------------------------------------------------------------
    // Object creation
    //------------------------------------------------------------------------------------------------------------

    /**
     * Create a BufferedReader instance
     *
     * @param inputFile File to read
     * @return Created BufferedReader instance
     */
    public static BufferedReader defineReader(File inputFile) {
        if (!inputFile.exists()) return null;

        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 3);
            return null;
        }
    }

    /**
     * Creates a BufferedWriter instance
     *
     * @param outputFile File to write
     * @return Created BufferedWriter instance
     */
    public static BufferedWriter defineWriter(File outputFile) {

        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
        } catch (IOException ex) {
            Main.ADMIN._log(ex, 3);
            return null;
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Generic methods
    //------------------------------------------------------------------------------------------------------------

    /**
     * Removes any unnecessary fields from the item's icon
     *
     * @param icon An item's bloated URL
     * @return Formatted icon URL
     */
    public static String formatIconURL(String icon) {
        String[] splitURL = icon.split("\\?", 2);
        String fullIcon = splitURL[0];

        if (splitURL.length > 1) {
            StringBuilder paramBuilder = new StringBuilder();

            for (String param : splitURL[1].split("&")) {
                String[] splitParam = param.split("=");

                switch (splitParam[0]) {
                    case "scale":
                    case "w":
                    case "h":
                    case "mr": // shaped
                    case "mn": // background
                    case "mt": // tier
                    case "relic":
                        paramBuilder.append("&");
                        paramBuilder.append(splitParam[0]);
                        paramBuilder.append("=");
                        paramBuilder.append(splitParam[1]);
                        break;
                    default:
                        break;
                }
            }

            // If there are parameters that should be kept, add them to fullIcon
            if (paramBuilder.length() > 0) {
                // Replace the first "&" symbol with "?"
                paramBuilder.setCharAt(0, '?');
                fullIcon += paramBuilder.toString();
            }
        }

        return fullIcon;
    }

    /**
     * Allows converting exception stack traces to strings
     *
     * @param ex Exception to convert to string
     * @return Exception as string
     */
    public static String stackTraceToString(Exception ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    //------------------------------------------------------------------------------------------------------------
    // Primitive array manipulation
    //------------------------------------------------------------------------------------------------------------

    public static void shiftArrayLeft(int[] array, int amount) {
        if (amount < 0) {
            amount = amount % array.length * -1;
            int[] tmp = new int[amount];

            System.arraycopy(array, array.length - amount, tmp, 0, amount);
            System.arraycopy(array, 0, array, amount, array.length - amount);
            System.arraycopy(tmp, 0, array, 0, amount);
        } else {
            amount = amount % array.length;
            int[] tmp = new int[amount];

            System.arraycopy(array, 0, tmp, 0, amount);
            System.arraycopy(array, amount, array, 0, array.length - amount);
            System.arraycopy(tmp, 0, array, array.length - amount, amount);
        }
    }

    public static void shiftArrayLeft(double[] array, int amount) {
        if (amount < 0) {
            amount = amount % array.length * -1;
            double[] tmp = new double[amount];

            System.arraycopy(array, array.length - amount, tmp, 0, amount);
            System.arraycopy(array, 0, array, amount, array.length - amount);
            System.arraycopy(tmp, 0, array, 0, amount);
        } else {
            amount = amount % array.length;
            double[] tmp = new double[amount];

            System.arraycopy(array, 0, tmp, 0, amount);
            System.arraycopy(array, amount, array, 0, array.length - amount);
            System.arraycopy(tmp, 0, array, array.length - amount, amount);
        }
    }

    //------------------------------------------------------------------------------------------------------------
    // Timestamps
    //------------------------------------------------------------------------------------------------------------

    public static String timeStamp() {
        StringBuilder stringBuilder = new StringBuilder();

        // Refresh calendar
        Calendar calendar = Calendar.getInstance();

        // Form [HH:MM:SS]
        stringBuilder.append("[");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.MINUTE)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.SECOND)));
        stringBuilder.append("]");

        // Return [HH:MM:SS]
        return stringBuilder.toString();
    }

    public static String dateStamp() {
        StringBuilder stringBuilder = new StringBuilder();

        // Refresh calendar
        Calendar calendar = Calendar.getInstance();

        // Form [DD.MM.YYYY]
        stringBuilder.append("[");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.MONTH)));
        stringBuilder.append(".");
        stringBuilder.append(String.format("%04d", calendar.get(Calendar.YEAR)));
        stringBuilder.append("]");

        // Return [DD.MM.YYYY]
        return stringBuilder.toString();
    }

    //------------------------------------------------------------------------------------------------------------
    // Mics
    //------------------------------------------------------------------------------------------------------------

    /**
     * Recursively populates provided list with file objects from provided folder and its subfolders
     *
     * @param dir Directory which to scan
     * @param fileList List of files to populate
     */
    public static void getAllFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles();

        if (files == null) return;

        for (File file : files) {
            fileList.add(file);

            if (file.isDirectory()) {
                getAllFiles(file, fileList);
            }
        }
    }

}
