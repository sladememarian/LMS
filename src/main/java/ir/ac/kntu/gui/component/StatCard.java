package ir.ac.kntu.gui.component;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Reusable dashboard info-card: a small caption, a large value and an optional
 * sub-line. Used across the User/Support/Admin dashboards.
 */
public class StatCard extends VBox {

    public StatCard(String caption, String value, String sub) {
        super(6);
        getStyleClass().add("stat-card");
        setPadding(new Insets(18));
        setPrefWidth(220);

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-caption");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        getChildren().addAll(captionLabel, valueLabel);
        if (sub != null && !sub.isBlank()) {
            Label subLabel = new Label(sub);
            subLabel.getStyleClass().add("muted");
            getChildren().add(subLabel);
        }
    }
}
