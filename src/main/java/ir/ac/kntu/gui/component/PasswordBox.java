package ir.ac.kntu.gui.component;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

// A password field with a small eye icon that toggles between masked and
// plain-text display. It stacks a PasswordField (masked) and a TextField
// (revealed) sharing one text value via a bidirectional binding, so switching
// visibility never loses what the user typed. Only one shows at a time; the eye
// button flips between them. It's a drop-in replacement for a PasswordField:
// stretches to fill its parent and forwards prompt text, the text value, and the
// fx:id to the underlying masked field.
public class PasswordBox extends StackPane {

    private static final String FIELD_STYLE = "field";

    // Open-eye glyph (password shown).
    private static final String EYE_OPEN =
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 "
            + "11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17a5 5 0 110-10 5 5 0 010 "
            + "10zm0-8a3 3 0 100 6 3 3 0 000-6z";

    // Crossed-out eye glyph (password masked).
    private static final String EYE_OFF =
            "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 "
            + "2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 "
            + "2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 "
            + "10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 "
            + "22 21 20.73 3.27 3 2 4.27z";

    private final PasswordField masked = new PasswordField();
    private final TextField plain = new TextField();
    private final SVGPath icon = new SVGPath();
    private final Button toggle = new Button();
    private boolean revealed;

    public PasswordBox() {
        setMaxWidth(Double.MAX_VALUE);
        configureFields();
        configureToggle();
        getChildren().addAll(masked, plain, toggle);
        StackPane.setAlignment(toggle, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggle, new Insets(0, 6, 0, 0));
    }

    private void configureFields() {
        // Both fields share one text value, so toggling preserves input.
        plain.textProperty().bindBidirectional(masked.textProperty());
        masked.getStyleClass().add(FIELD_STYLE);
        plain.getStyleClass().add(FIELD_STYLE);
        masked.getStyleClass().add("password-field");
        plain.getStyleClass().add("password-field");
        plain.setManaged(false);
        plain.setVisible(false);
    }

    private void configureToggle() {
        icon.setContent(EYE_OFF);
        icon.getStyleClass().add("eye-icon");
        icon.setScaleX(0.75);
        icon.setScaleY(0.75);
        toggle.setGraphic(icon);
        toggle.getStyleClass().add("eye-toggle");
        toggle.setFocusTraversable(false);
        toggle.setOnAction(event -> toggleVisibility());
    }

    private void toggleVisibility() {
        revealed = !revealed;
        plain.setVisible(revealed);
        plain.setManaged(revealed);
        masked.setVisible(!revealed);
        masked.setManaged(!revealed);
        icon.setContent(revealed ? EYE_OPEN : EYE_OFF);

        TextField active = revealed ? plain : masked;
        active.requestFocus();
        String text = active.getText();
        active.positionCaret(text == null ? 0 : text.length());
    }

    // Sets the prompt text on both the masked and revealed fields.
    public void setPromptText(String prompt) {
        masked.setPromptText(prompt);
        plain.setPromptText(prompt);
    }

    // Assigns the fx:id to the underlying masked field (for lookups).
    public void setFieldId(String id) {
        masked.setId(id);
    }

    public StringProperty textProperty() {
        return masked.textProperty();
    }

    public String getText() {
        return masked.getText();
    }

    public void setText(String text) {
        masked.setText(text);
    }

    public void clear() {
        masked.clear();
    }
}
