package poe.Utility;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poe.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Utility {
    private static final Logger logger = LoggerFactory.getLogger(Utility.class);

    /**
     * Attempts to load a config or create it if it does not exist
     *
     * @param fileName Name of the config without leading slashes
     * @return Loaded config or null if config did not exist
     */
    public static Config loadConfig(String fileName) {
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        File confFile = new File(fileName);

        if (!confFile.exists() || !confFile.isFile()) {
            logger.warn("Could not find config");

            try {
                String path = exportResource("/" + fileName);
                logger.info("Config '" + path + "' has been created");
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error("Could not create config");
            }

            return null;
        }

        return ConfigFactory.parseFile(confFile);
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
