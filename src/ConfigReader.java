import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ConfigReader {

    /**
     * Reads and parses the encryptor-config.json file located next to the jar.
     * Uses manual JSON parsing — no external dependencies required.
     */
    public static EncryptorConfig read(File configFile) {
        System.out.println("[Config] Reading config file: " + configFile.getAbsolutePath());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line.trim());
            }
        } catch (Exception e) {
            System.err.println("[Config] ERROR: Failed to read config file: " + e.getMessage());
            System.exit(1);
        }

        String json = sb.toString();

        EncryptorConfig config = new EncryptorConfig();

        // --- parse "path" ---
        String path = extractJsonString(json, "path");
        if (path == null || path.trim().isEmpty()) {
            System.err.println("[Config] ERROR: Missing or empty 'path' property in config file.");
            System.exit(1);
        }
        config.setPath(path);

        // --- parse "key" ---
        String key = extractJsonString(json, "key");
        if (key == null || key.trim().isEmpty()) {
            System.err.println("[Config] ERROR: Missing or empty 'key' property in config file.");
            System.exit(1);
        }
        config.setKey(key);

        // --- parse "properties" array ---
        List<String> properties = extractJsonArray(json, "properties");
        if (properties == null || properties.isEmpty()) {
            System.err.println("[Config] ERROR: Missing or empty 'properties' array in config file.");
            System.exit(1);
        }
        config.setProperties(properties);

        System.out.println("[Config] Config loaded successfully.");
        System.out.println("[Config]   YML path     : " + config.getPath());
        System.out.println("[Config]   Properties   : " + config.getProperties());
        System.out.println("[Config]   Key          : " + maskKey(config.getKey()));
        return config;
    }

    /**
     * Extracts a string value for a given key from a flat JSON string.
     * Handles: "key": "value"
     */
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) return null;

        int openQuote = json.indexOf('"', colon + 1);
        if (openQuote == -1) return null;

        int closeQuote = json.indexOf('"', openQuote + 1);
        if (closeQuote == -1) return null;

        return json.substring(openQuote + 1, closeQuote);
    }

    /**
     * Extracts a JSON string array for a given key.
     * Handles: "key": ["value1", "value2"]
     */
    private static List<String> extractJsonArray(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) return null;

        int openBracket = json.indexOf('[', colon + 1);
        if (openBracket == -1) return null;

        int closeBracket = json.indexOf(']', openBracket + 1);
        if (closeBracket == -1) return null;

        String arrayContent = json.substring(openBracket + 1, closeBracket);
        List<String> result = new ArrayList<>();

        int pos = 0;
        while (pos < arrayContent.length()) {
            int open = arrayContent.indexOf('"', pos);
            if (open == -1) break;
            int close = arrayContent.indexOf('"', open + 1);
            if (close == -1) break;
            String item = arrayContent.substring(open + 1, close).trim();
            if (!item.isEmpty()) result.add(item);
            pos = close + 1;
        }

        return result;
    }

    private static String maskKey(String key) {
        if (key.length() <= 4) return "****";
        return key.substring(0, 2) + "****" + key.substring(key.length() - 2);
    }
}
