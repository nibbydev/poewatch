package poe.Utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Main;

import java.io.*;

public class Utility {
    private static final Logger logger = LoggerFactory.getLogger(Utility.class);

    /**
     * Attempts to load a data file. If the operation fails, the default resource file is exported to the expected
     * directory
     *
     * @param fileName Name of the config without leading slashes
     * @return Valid file
     */
    public static String loadFile(String fileName) {
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        File file = new File(fileName);

        if (!file.exists() || !file.isFile()) {
            logger.warn("Could not find file \"" + fileName + "\"");

            try {
                String path = exportResource("/" + fileName);
                logger.info("File \"" + fileName + "\" has been created at \"" + path + "\"");
            } catch (Exception ex) {
                logger.error("Could not create file \"" + fileName + "\"", ex);
            }

            return null;
        }

        // Read in the file
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            return sb.toString();
        } catch (IOException ex) {
            logger.error("Could not load file \"" + fileName + "\"", ex);
            return null;
        }
    }

    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/SmartLibrary.dll"
     * @return The path to the exported resource
     */
    public static String exportResource(String resourceName) throws Exception {
        String jarFolder;

        try (InputStream stream = Main.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            byte[] buffer = new byte[4096];
            int readBytes;

            jarFolder = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                    .getParentFile()
                    .getPath()
                    .replace('\\', '/');

            try (OutputStream resStreamOut = new FileOutputStream(jarFolder + resourceName)) {
                while ((readBytes = stream.read(buffer)) > 0) {
                    resStreamOut.write(buffer, 0, readBytes);
                }
            }
        }

        return jarFolder + resourceName;
    }
}
