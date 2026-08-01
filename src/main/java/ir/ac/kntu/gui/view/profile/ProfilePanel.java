package ir.ac.kntu.gui.view.profile;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.component.PasswordBox;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.gui.util.UiTheme;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.sso.SsoService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Profile screen: lets the signed-in user review their identity, edit the
 * name/phone fields, change their password and switch the visual theme. Every
 * mutation is delegated to the existing {@link SsoService} backend (no new
 * business logic here) and runs off the FX thread.
 *
 * <p>Editing the e-mail address and uploading a profile photo are intentionally
 * out of scope: the CLI backend keys accounts by e-mail and stores no avatar, so
 * exposing either from the GUI would require changing phase-1/2 code. Both are
 * documented as skipped in {@code docs/gui.md}.</p>
 */
public class ProfilePanel extends VBox {

    private static final String FIELD_STYLE = "field";
    private static final String GHOST_STYLE = "ghost";
    private static final String MUTED_STYLE = "muted";
    private static final String SAVE_FAILED = "Could not save profile";

    private final Persona persona;
    private final Navigator navigator;

    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField phoneField = new TextField();

    private final PasswordBox currentPassword = new PasswordBox();
    private final PasswordBox newPassword = new PasswordBox();
    private final PasswordBox confirmPassword = new PasswordBox();

    private final Label themeValue = new Label();

    public ProfilePanel(Persona persona, Navigator navigator) {
        super(16);
        this.persona = persona;
        this.navigator = navigator;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Profile");
        heading.getStyleClass().add("h1");

        getChildren().addAll(heading, buildIdentityCard(),
                buildDetailsCard(), buildPasswordCard(), buildThemeCard());
        loadProfile();
    }

    private VBox buildIdentityCard() {
        Label email = new Label("Email: " + safe(persona.getEmail()));
        email.getStyleClass().add(MUTED_STYLE);
        Label role = new Label("Role: " + persona.getRole());
        role.getStyleClass().add(MUTED_STYLE);
        Label note = new Label("Email cannot be changed here — it is your account key.");
        note.getStyleClass().add(MUTED_STYLE);
        return card("Account", email, role, note);
    }

    private VBox buildDetailsCard() {
        firstNameField.setPromptText("First name");
        firstNameField.getStyleClass().add(FIELD_STYLE);
        lastNameField.setPromptText("Last name");
        lastNameField.getStyleClass().add(FIELD_STYLE);
        phoneField.setPromptText("Phone number");
        phoneField.getStyleClass().add(FIELD_STYLE);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("First name"), firstNameField);
        grid.addRow(1, new Label("Last name"), lastNameField);
        grid.addRow(2, new Label("Phone"), phoneField);

        Button save = new Button("Save changes");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> handleSaveDetails());

        return card("Personal details", grid, save);
    }

    private VBox buildPasswordCard() {
        currentPassword.setPromptText("Current password");
        newPassword.setPromptText("New password");
        confirmPassword.setPromptText("Confirm new password");

        Button change = new Button("Update password");
        change.getStyleClass().add("primary");
        change.setOnAction(event -> handleChangePassword());

        return card("Change password",
                currentPassword, newPassword, confirmPassword, change);
    }

    private VBox buildThemeCard() {
        themeValue.getStyleClass().add(MUTED_STYLE);

        Button toggle = new Button("Toggle Light / Dark");
        toggle.getStyleClass().add(GHOST_STYLE);
        toggle.setOnAction(event -> handleToggleTheme());

        HBox row = new HBox(12, themeValue, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        return card("Appearance", row);
    }

    private void handleSaveDetails() {
        String first = trimmed(firstNameField.getText());
        String last = trimmed(lastNameField.getText());
        String phone = trimmed(phoneField.getText());
        String email = persona.getEmail();
        BackgroundJobs.runAction(
                () -> SsoService.editProfile(email, first, last, phone),
                () -> {
                    persona.setFirstName(first);
                    persona.setLastName(last);
                    persona.setPhoneNumber(phone);
                    Dialogs.info("Profile updated", "Your details have been saved.");
                },
                error -> Dialogs.error(SAVE_FAILED, error));
    }

    private void handleChangePassword() {
        String current = currentPassword.getText();
        String next = newPassword.getText();
        String confirm = confirmPassword.getText();
        if (isBlank(current) || isBlank(next)) {
            Dialogs.warn("Missing fields", "Enter your current and new password.");
            return;
        }
        String email = persona.getEmail();
        BackgroundJobs.runAction(
                () -> SsoService.changePassword(email, current, next, confirm),
                () -> {
                    currentPassword.clear();
                    newPassword.clear();
                    confirmPassword.clear();
                    Dialogs.info("Password changed", "Your password has been updated.");
                },
                error -> Dialogs.error("Could not change password", error));
    }

    private void handleToggleTheme() {
        navigator.toggleTheme();
        UiTheme theme = navigator.getTheme();
        themeValue.setText("Current theme: " + theme.name());
        String email = persona.getEmail();
        BackgroundJobs.runAction(
                () -> SsoService.changeTheme(email, theme.name()),
                () -> persona.setTheme(theme.name()),
                error -> Dialogs.error("Could not save theme", error));
    }

    private void loadProfile() {
        firstNameField.setText(safe(persona.getFirstName()));
        lastNameField.setText(safe(persona.getLastName()));
        phoneField.setText(safe(persona.getPhoneNumber()));
        themeValue.setText("Current theme: " + navigator.getTheme().name());
    }

    private VBox card(String title, javafx.scene.Node... body) {
        Label header = new Label(title);
        header.getStyleClass().add("h2");
        VBox box = new VBox(10, header);
        box.getChildren().addAll(body);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(16));
        return box;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
