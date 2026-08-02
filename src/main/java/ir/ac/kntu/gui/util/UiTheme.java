package ir.ac.kntu.gui.util;

import java.net.URL;
import java.util.Objects;

import javafx.scene.Scene;

/**
 * Light/Dark theme handling. Stylesheets live under
 * {@code resources/ir/ac/kntu/gui/css/}. The current theme is applied to the
 * active {@link Scene}; persistence per user is wired up in later steps via
 * {@code SsoService.changeTheme}.
 */
public enum UiTheme {

    LIGHT("/ir/ac/kntu/gui/css/light.css"),
    DARK("/ir/ac/kntu/gui/css/dark.css");

    private final String path;

    UiTheme(String path) {
        this.path = path;
    }

    /** Parses a stored theme string ("dark"/"light"); defaults to LIGHT. */
    public static UiTheme from(String value) {
        if (value != null && value.trim().equalsIgnoreCase("dark")) {
            return DARK;
        }
        return LIGHT;
    }

    /** Removes any known theme stylesheet and applies this one to the scene. */
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
