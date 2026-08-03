package ir.ac.kntu.gui;

import javafx.scene.Parent;

// A single screen. Every scene (Login, Dashboard, Library, ...) implements this
// so the Navigator can swap between them uniformly (multi-scene navigation).
public interface View {

    // The root node of this screen, added to the scene graph by the Navigator.
    Parent getRoot();

    // Window title shown while this view is active.
    String title();
}
