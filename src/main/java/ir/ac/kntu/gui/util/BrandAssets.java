package ir.ac.kntu.gui.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.scene.image.Image;

// One place to load the Collaberry brand image, so the splash logo and the
// stage/taskbar icon come from the same source. The file lives at
// <project>/icon/Collaberry_groups.png. If it is missing (for example, tests
// run from another directory), loaders just get null.
public final class BrandAssets {

    private static final String ICON_FILE = "Collaberry_groups.png";

    private BrandAssets() {
        // utility class
    }

    // Absolute path to the brand icon under the launch directory.
    public static Path iconPath() {
        return Paths.get(System.getProperty("user.dir"), "icon", ICON_FILE);
    }

    // Loads the brand icon, or returns null if the file is missing so callers
    // can fall back to the JavaFX default instead of crashing.
    public static Image loadIcon() {
        Path path = iconPath();
        if (Files.exists(path)) {
            return new Image(path.toUri().toString());
        }
        return null;
    }
}
