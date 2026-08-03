package ir.ac.kntu.gui.util;

import java.net.URL;
import java.util.Objects;

import javafx.scene.Scene;

// Light/Dark theme handling. Stylesheets live under
// resources/ir/ac/kntu/gui/css/. The current theme is applied to the active
// Scene. Per-user persistence is added later via SsoService.changeTheme.
public enum UiTheme {

    LIGHT("/ir/ac/kntu/gui/css/light.css"),
    DARK("/ir/ac/kntu/gui/css/dark.css");

    private final String path;

    UiTheme(String path) {
        this.path = path;
    }

    // Reads a stored theme string ("dark"/"light"); defaults to LIGHT.
    public static UiTheme from(String value) {
        if (value != null && value.trim().equalsIgnoreCase("dark")) {
            return DARK;
        }
        return LIGHT;
    }

    // Removes any known theme stylesheet, then applies this one to the scene.
    public void applyTo(Scene scene) {
        if (scene == null) {
            return;
        }
        java.util.Arrays.stream(values())
                .map(theme -> UiTheme.class.getResource(theme.path))
                .filter(Objects::nonNull)
                .forEach(url -> scene.getStylesheets().remove(url.toExternalForm()));
        URL url = UiTheme.class.getResource(path);
        scene.getStylesheets().add(
                Objects.requireNonNull(url, "Missing stylesheet: " + path).toExternalForm());
    }

    public String key() {
        return name().toLowerCase();
    }
}
