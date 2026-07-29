package ir.ac.kntu.gui.shell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Builds a titled placeholder content panel. Each functional screen (Library,
 * Wallet, Support, dashboards, ...) is stubbed with one of these in Step 2 and
 * replaced by the real implementation in the step that owns it.
 */
public final class Placeholder {

    private Placeholder() {
    }

    public static Node build(String title, String description) {
        Label heading = new Label(title);
        heading.getStyleClass().add("h1");

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("muted");
        desc.setMaxWidth(640);

        Label badge = new Label("Coming in a later migration step");
        badge.getStyleClass().add("muted");

        VBox card = new VBox(12, heading, desc, badge);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(720);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(24));
        wrapper.setAlignment(Pos.TOP_LEFT);
        return wrapper;
    }
}
