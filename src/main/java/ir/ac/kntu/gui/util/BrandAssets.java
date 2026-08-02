package ir.ac.kntu.gui.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.scene.image.Image;

/**
 * Central access to the Collaberry brand image so the splash logo and the
 * stage/taskbar icon load from a single source. The file lives at
 * {@code <project>/icon/Collaberry_groups.png}; when it is missing (e.g. tests
 * running from another directory) loaders simply get {@code null}.
 */
public final class BrandAssets {

    private static final String ICON_FILE = "Collaberry_groups.png";

    private BrandAssets() {
    }

    /** Absolute path to the brand icon under the launch directory. */
    public static Path iconPath() {
        return Paths.get(System.getProperty("user.dir"), "icon", ICON_FILE);
    }

    /**
     * Loads the brand icon, or returns {@code null} if the file is absent so
     * callers can fall back to the JavaFX default rather than crash.
     */
    public static Image loadIcon() {
        Path path = iconPath();
        if (Files.exists(path)) {
            return new Image(path.toUri().toString());
        }
        return null;
    }
}
