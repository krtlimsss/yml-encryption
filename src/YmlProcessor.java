import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class YmlProcessor {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";
    private static final DateTimeFormatter BACKUP_FMT =
            DateTimeFormatter.ofPattern("MMddyyyy_HHmmss");

    public static void process(EncryptorConfig config) {

        // ── Step 2: Validate YML file ──────────────────────────────────────────
        File ymlFile = new File(config.getPath());

        System.out.println();
        System.out.println("[YML] Reading YML file: " + ymlFile.getAbsolutePath());

        if (!ymlFile.exists()) {
            System.err.println("[YML] ERROR: File does not exist: " + ymlFile.getAbsolutePath());
            System.exit(1);
        }

        if (!ymlFile.getName().endsWith(".yml") && !ymlFile.getName().endsWith(".yaml")) {
            System.err.println("[YML] ERROR: File is not a YML file: " + ymlFile.getName());
            System.exit(1);
        }

        System.out.println("[YML] File found and validated.");

        // ── Step 3: Create backup copy ─────────────────────────────────────────
        String timestamp = LocalDateTime.now().format(BACKUP_FMT);
        String backupName = ymlFile.getName().replaceFirst("\\.(yml|yaml)$", "")
                + "(" + timestamp + ").yml";

        File backupFile = new File(ymlFile.getParent(), backupName);

        System.out.println();
        System.out.println("[Backup] Creating backup: " + backupFile.getAbsolutePath());

        List<String> originalLines = readLines(ymlFile);
        writeLines(backupFile, originalLines);

        System.out.println("[Backup] Backup created successfully.");

        // ── Step 4: Encrypt properties in the original YML ────────────────────
        System.out.println();
        System.out.println("[Encryption] Starting encryption of " +
                config.getProperties().size() + " properties...");

        List<String> updatedLines = new ArrayList<>(originalLines);
        int successCount = 0;
        int failCount    = 0;

        for (String property : config.getProperties()) {
            System.out.println("[Encryption] Encrypting: " + property);

            // Convert dot-notation to YAML key (last segment is the yaml key,
            // indented under its parent — we do a best-effort line scan)
            int lineIndex = findPropertyLine(updatedLines, property);

            if (lineIndex == -1) {
                System.err.println("[Encryption] Failed:    " + property + " — property not found in YML.");
                failCount++;
                continue;
            }

            String line        = updatedLines.get(lineIndex);
            String currentValue = extractYmlValue(line);

            if (currentValue == null || currentValue.trim().isEmpty()) {
                System.err.println("[Encryption] Failed:    " + property + " — property has no value.");
                failCount++;
                continue;
            }

            // Skip if already encrypted
            if (currentValue.trim().startsWith(ENC_PREFIX)) {
                System.out.println("[Encryption] Skipped:   " + property + " — already encrypted.");
                continue;
            }

            try {
                String encrypted = AesUtil.encrypt(currentValue.trim(), config.getKey());
                String encValue  = ENC_PREFIX + encrypted + ENC_SUFFIX;

                // Preserve indentation and key, replace only the value
                String updatedLine = replaceYmlValue(line, encValue);
                updatedLines.set(lineIndex, updatedLine);

                System.out.println("[Encryption] Success:   " + property);
                successCount++;
            } catch (Exception e) {
                System.err.println("[Encryption] Failed:    " + property + " — " + e.getMessage());
                failCount++;
            }
        }

        // Write updated lines back to original YML file
        writeLines(ymlFile, updatedLines);

        System.out.println();
        System.out.println("[Encryption] Done. Encrypted: " + successCount +
                " | Failed: " + failCount +
                " | Total: " + config.getProperties().size());
        System.out.println("[Encryption] Original file updated: " + ymlFile.getAbsolutePath());
        System.out.println("[Encryption] Backup preserved at  : " + backupFile.getAbsolutePath());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Finds the line index in the YML that corresponds to a dot-notation property.
     * Supports nested keys by tracking indentation level as it traverses segments.
     * e.g. "spring.datasource.username" -> looks for the line containing "username:"
     * under the "datasource:" block under the "spring:" block.
     */
    private static int findPropertyLine(List<String> lines, String property) {
        String[] segments = property.split("\\.");
        int segmentIndex  = 0;
        int expectedDepth = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line        = lines.get(i);
            String trimmed     = line.stripLeading();
            int    actualDepth = line.length() - trimmed.length();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            // Must be at the right indent level for the current segment
            if (actualDepth != expectedDepth) continue;

            String currentSegment = segments[segmentIndex];

            if (trimmed.startsWith(currentSegment + ":")) {
                if (segmentIndex == segments.length - 1) {
                    // Last segment — this is the property line
                    return i;
                } else {
                    // Intermediate segment — go deeper
                    segmentIndex++;
                    expectedDepth += 2;
                }
            }
        }
        return -1;
    }

    /**
     * Extracts the value portion from a YML line like:
     *   "  username: postgres"  ->  "postgres"
     */
    private static String extractYmlValue(String line) {
        int colon = line.indexOf(':');
        if (colon == -1) return null;
        String value = line.substring(colon + 1).trim();
        // Strip surrounding quotes if present
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'")  && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Replaces the value portion of a YML line, preserving indentation and key.
     */
    private static String replaceYmlValue(String line, String newValue) {
        int colon = line.indexOf(':');
        if (colon == -1) return line;
        return line.substring(0, colon + 1) + " " + newValue;
    }

    private static List<String> readLines(File file) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (Exception e) {
            System.err.println("[YML] ERROR: Failed to read file: " + e.getMessage());
            System.exit(1);
        }
        return lines;
    }

    private static void writeLines(File file, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("[YML] ERROR: Failed to write file: " + e.getMessage());
            System.exit(1);
        }
    }
}
