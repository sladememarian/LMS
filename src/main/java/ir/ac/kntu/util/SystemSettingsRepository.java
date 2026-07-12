package ir.ac.kntu.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence for the {@code system_settings} key/value table. Split out of
 * {@link DatabaseAccess} to keep that file under the CheckStyle line-count
 * limit and to give Step 11's configurable values their own home.
 */
public final class SystemSettingsRepository {

    private SystemSettingsRepository() {
    }

    public static void clear() {
        Database.executeUpdate("DELETE FROM system_settings");
    }

    public static void save(String key, String value) {
        Database.withPs("MERGE INTO system_settings USING (VALUES (?, ?)) AS s(setting_key, setting_value) "
                + "ON system_settings.setting_key = s.setting_key "
                + "WHEN MATCHED THEN UPDATE SET setting_value = s.setting_value "
                + "WHEN NOT MATCHED THEN INSERT (setting_key, setting_value) VALUES (s.setting_key, s.setting_value)",
                ps -> {
                    ps.setString(1, key);
                    ps.setString(2, value);
                    ps.executeUpdate();
                });
    }

    public static Map<String, String> getAll() {
        List<Map.Entry<String, String>> rows = Database.queryAll("SELECT * FROM system_settings",
                rs -> Map.entry(rs.getString("setting_key"), rs.getString("setting_value")));
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> row : rows) {
            result.put(row.getKey(), row.getValue());
        }
        return result;
    }
}
