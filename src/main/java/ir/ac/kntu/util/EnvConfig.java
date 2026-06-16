package ir.ac.kntu.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class EnvConfig {
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
}