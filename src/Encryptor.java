

import java.io.File;
import java.net.URISyntaxException;

public class Encryptor {

    public static void main(String[] args) {

        System.out.println("================================================");
        System.out.println("  YML Property Encryptor");
        System.out.println("================================================");

        // ── Step 1: Locate config file next to the jar ────────────────────────
        File configFile = resolveConfigFile();

        System.out.println("[Config] Looking for config file: " + configFile.getAbsolutePath());

        if (!configFile.exists()) {
            System.err.println("[Config] ERROR: Config file not found: " + configFile.getAbsolutePath());
            System.err.println("[Config]        Place 'encryptor-config.json' next to the jar file.");
            System.exit(1);
        }

        // ── Step 1b: Read and validate config ─────────────────────────────────
        EncryptorConfig config = ConfigReader.read(configFile);

        // ── Steps 2–4: Process the YML file ───────────────────────────────────
        YmlProcessor.process(config);

        System.out.println();
        System.out.println("================================================");
        System.out.println("  Encryptor finished.");
        System.out.println("================================================");
    }

    /**
     * Resolves the directory where the jar file is located,
     * then looks for encryptor-config.json in that same directory.
     * Works on both Windows and Linux.
     */
    private static File resolveConfigFile() {
        try {
            File jarLocation = new File(
                    Encryptor.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            // When running as a jar, jarLocation is the jar file itself
            // When running in an IDE, jarLocation is the classes directory
            File directory = jarLocation.isFile()
                    ? jarLocation.getParentFile()
                    : jarLocation;

            return new File(directory, "encryptor-config.json");

        } catch (URISyntaxException e) {
            // Fallback: look in current working directory
            System.err.println("[Config] WARN: Could not resolve jar location, " +
                    "falling back to working directory.");
            return new File(System.getProperty("user.dir"), "encryptor-config.json");
        }
    }
}