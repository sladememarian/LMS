package ir.ac.kntu.gui.view.admin;

import java.util.ArrayList;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.persona.AdminManagementService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.util.Validator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Admin User Management: list/search users and enable/disable, reset password
 * or delete them. All mutations run off the FX thread and use the signed-in
 * admin as the acting persona.
 */
public class UserManagementPanel extends VBox {

    private final Persona actor;
    private final TextField searchField = new TextField();
    private final TableView<Persona> table = new TableView<>();
    private final TextField createEmailField = new TextField();
    private final TextField createPasswordField = new TextField();

    private static final String GHOST_STYLE = "ghost";
    private static final String FIELD_STYLE = "field";
    private static final String LABEL_EDIT_PROFILE = "Edit profile";
    private static final String LABEL_INVALID_ROLE = "Invalid role";

    public UserManagementPanel(Persona actor) {
        super(16);
        this.actor = actor;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("User Management");
        heading.getStyleClass().add("h1");

        searchField.setPromptText("Search users…");
        searchField.getStyleClass().add(FIELD_STYLE);
        searchField.textProperty().addListener((obs, old, val) -> refresh(val));

        buildTable();
        getChildren().addAll(heading, buildCreateForm(), searchField, actions(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh("");
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<Persona, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        email.setPrefWidth(220);
        TableColumn<Persona, String> role = new TableColumn<>("Role");
        role.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(cell.getValue().getRole())));
        TableColumn<Persona, String> member = new TableColumn<>("Member ID");
        member.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMemberId()));
        TableColumn<Persona, String> active = new TableColumn<>("Active");
        active.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isActive() ? "Yes" : "No"));

        table.getColumns().addAll(email, role, member, active);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No users."));
    }

    private HBox actions() {
        Button toggle = new Button("Toggle active");
        toggle.getStyleClass().add(GHOST_STYLE);
        toggle.setOnAction(event -> handleToggle());

        Button resetPw = new Button("Reset password");
        resetPw.getStyleClass().add(GHOST_STYLE);
        resetPw.setOnAction(event -> handleResetPassword());

        Button delete = new Button("Delete");
        delete.getStyleClass().add(GHOST_STYLE);
        delete.setOnAction(event -> handleDelete());

        Button promote = new Button("Promote");
        promote.getStyleClass().add(GHOST_STYLE);
        promote.setOnAction(event -> handlePromote());

        Button demote = new Button("Demote");
        demote.getStyleClass().add(GHOST_STYLE);
        demote.setOnAction(event -> handleDemote());

        Button editProfile = new Button("Edit profile");
        editProfile.getStyleClass().add(GHOST_STYLE);
        editProfile.setOnAction(event -> handleEditProfile());

        HBox box = new HBox(10, toggle, resetPw, delete, promote, demote, editProfile);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox buildCreateForm() {
        createEmailField.setPromptText("New user email");
        createEmailField.getStyleClass().add(FIELD_STYLE);
        createPasswordField.setPromptText("Temp password");
        createPasswordField.getStyleClass().add(FIELD_STYLE);

        ComboBox<UserRole> roleBox = new ComboBox<>(
                FXCollections.observableArrayList(UserRole.ADMIN, UserRole.CALLCENTER));
        roleBox.getSelectionModel().selectFirst();

        Button createBtn = new Button("Create " + roleBox.getValue());
        roleBox.valueProperty().addListener((obs, old, val) ->
                createBtn.setText("Create " + val));
        createBtn.getStyleClass().add("primary");
        createBtn.setOnAction(e -> handleCreateUser(roleBox.getValue()));

        HBox box = new HBox(10, createEmailField, createPasswordField, roleBox, createBtn);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void handleCreateUser(UserRole role) {
        String email = createEmailField.getText() == null ? "" : createEmailField.getText().trim();
        String pw = createPasswordField.getText() == null ? "" : createPasswordField.getText().trim();
        if (email.isEmpty() || pw.isEmpty()) {
            Dialogs.warn("Missing information", "Email and password are required.");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            Dialogs.warn("Invalid email", "Please enter a valid email address.");
            return;
        }
        if (!Validator.isValidPassword(pw)) {
            Dialogs.warn("Weak password",
                    "Password must be at least 8 characters with uppercase, lowercase, digit, and special character.");
            return;
        }
        BackgroundJobs.runAction(
                () -> {
                    if (role == UserRole.ADMIN) {
                        AdminManagementService.createAdmin(actor, email, pw);
                    } else {
                        AdminManagementService.createCallCenter(actor, email, pw);
                    }
                },
                () -> {
                    createEmailField.clear();
                    createPasswordField.clear();
                    Dialogs.info("Created", role + " account created.");
                    refresh(searchField.getText());
                },
                error -> Dialogs.error("Create failed", error));
    }

    private void handlePromote() {
        Persona user = selected();
        if (user == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Promote user");
        dialog.setHeaderText("Promote " + user.getEmail() + " to (ADMIN or CALLCENTER):");
        dialog.setContentText("Role:");
        dialog.showAndWait().ifPresent(roleStr -> {
            UserRole role;
            try {
                role = UserRole.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                Dialogs.warn(LABEL_INVALID_ROLE, "Enter ADMIN or CALLCENTER.");
                return;
            }
            if (role != UserRole.ADMIN && role != UserRole.CALLCENTER) {
                Dialogs.warn(LABEL_INVALID_ROLE, "Enter ADMIN or CALLCENTER.");
                return;
            }
            BackgroundJobs.runAction(
                    () -> AdminManagementService.promoteAdmin(actor, user.getEmail(), role),
                    () -> {
                        Dialogs.info("Promoted", user.getEmail() + " promoted to " + role);
                        refresh(searchField.getText());
                    },
                    error -> Dialogs.error("Promote failed", error));
        });
    }

    private void handleDemote() {
        Persona user = selected();
        if (user == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Demote user");
        dialog.setHeaderText("Demote " + user.getEmail() + " to (STUDENT, TEACHER or GUEST):");
        dialog.setContentText("Role:");
        dialog.showAndWait().ifPresent(roleStr -> {
            UserRole role;
            try {
                role = UserRole.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                Dialogs.warn(LABEL_INVALID_ROLE, "Enter a valid role name.");
                return;
            }
            BackgroundJobs.runAction(
                    () -> AdminManagementService.demoteAdmin(actor, user.getEmail(), role),
                    () -> {
                        Dialogs.info("Demoted", user.getEmail() + " demoted to " + role);
                        refresh(searchField.getText());
                    },
                    error -> Dialogs.error("Demote failed", error));
        });
    }

    private void handleEditProfile() {
        Persona user = selected();
        if (user == null) {
            return;
        }
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle(LABEL_EDIT_PROFILE);
        nameDialog.setHeaderText("New display name for " + user.getEmail());
        nameDialog.setContentText("First name:");
        nameDialog.showAndWait().ifPresent(firstName -> {
            TextInputDialog lastDialog = new TextInputDialog();
            lastDialog.setTitle(LABEL_EDIT_PROFILE);
            lastDialog.setHeaderText("Last name for " + user.getEmail());
            lastDialog.setContentText("Last name:");
            lastDialog.showAndWait().ifPresent(lastName -> {
                TextInputDialog phoneDialog = new TextInputDialog();
                phoneDialog.setTitle(LABEL_EDIT_PROFILE);
                phoneDialog.setHeaderText("Phone for " + user.getEmail() + " (optional):");
                phoneDialog.setContentText("Phone:");
                phoneDialog.showAndWait().ifPresent(phone ->
                        BackgroundJobs.runAction(
                                () -> AdminManagementService.editUserProfile(user.getEmail(), firstName, lastName, phone),
                                () -> Dialogs.info("Done", "Profile updated."),
                                error -> Dialogs.error("Edit failed", error)));
            });
        });
    }

    private void handleToggle() {
        Persona user = selected();
        if (user == null) {
            return;
        }
        BackgroundJobs.run(
                () -> AdminManagementService.toggleActive(actor, user.getEmail()),
                nowActive -> {
                    Dialogs.info("Updated",
                            user.getEmail() + " is now " + (Boolean.TRUE.equals(nowActive)
                                    ? "active" : "inactive") + ".");
                    refresh(searchField.getText());
                },
                error -> Dialogs.error("Update failed", error));
    }

    private void handleResetPassword() {
        Persona user = selected();
        if (user == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reset password");
        dialog.setHeaderText("New password for " + user.getEmail());
        dialog.setContentText("Password:");
        dialog.showAndWait().ifPresent(pw -> {
            if (pw.isBlank()) {
                Dialogs.warn("Empty password", "Password cannot be empty.");
                return;
            }
            BackgroundJobs.runAction(
                    () -> AdminManagementService.resetPassword(actor, user.getEmail(), pw.trim()),
                    () -> Dialogs.info("Done", "Password reset."),
                    error -> Dialogs.error("Reset failed", error));
        });
    }

    private void handleDelete() {
        Persona user = selected();
        if (user == null) {
            return;
        }
        if (Dialogs.confirm("Delete user", "Delete " + user.getEmail() + "?")) {
            BackgroundJobs.runAction(
                    () -> AdminManagementService.deleteUser(actor, user.getEmail()),
                    () -> {
                        Dialogs.info("Deleted", "User removed.");
                        refresh(searchField.getText());
                    },
                    error -> Dialogs.error("Delete failed", error));
        }
    }

    private Persona selected() {
        Persona user = table.getSelectionModel().getSelectedItem();
        if (user == null) {
            Dialogs.warn("No selection", "Select a user first.");
        }
        return user;
    }

    private void refresh(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        BackgroundJobs.run(
                () -> query.isEmpty()
                        ? AdminManagementService.listAllUsers()
                        : AdminManagementService.searchUsers(query),
                list -> table.setItems(FXCollections.observableArrayList(
                        list != null ? list : new ArrayList<>())),
                error -> Dialogs.error("Could not load users", error));
    }
}
