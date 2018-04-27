package ovh.poe;

import java.io.*;

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
            ex.printStackTrace();
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
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Removes any unnecessary fields from the item's icon
     *
     * @param icon An item's bloated URL
     * @return Formatted icon URL
     */
    public static String formatIconURL(String icon) {
        String[] splitURL = icon.split("\\?");
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
}
