package ir.ac.kntu.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class EnvConfig {
    // config: because hardcoding is for amateurs
    private static final Properties PROPS = new Properties();

    static {
        File envFile = new File(".env");
        if (envFile.exists()) {
            try (FileInputStream fis = new FileInputStream(envFile)) {
                PROPS.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading .env: " + e.getMessage());
            }
        }
    }

    public static String get(String key, String defaultValue) {
        String val = PROPS.getProperty(key);
        return val != null ? val.trim() : defaultValue;
    }

    // Returns the master key or null when not configured. When set, it
    // bypasses both password-format validation and credential checks so
    // testers can skip the tedious "Passw0rd!" dance.
    public static String getMasterKey() {
        String key = PROPS.getProperty("MASTER_KEY");
        if (key == null) {
            return null;
        }
        key = key.trim();
        return key.isEmpty() ? null : key;
    }

    // True when a master key is configured and the given value matches it.
    public static boolean isMasterKey(String value) {
        String master = getMasterKey();
        return master != null && master.equals(value);
    }
}