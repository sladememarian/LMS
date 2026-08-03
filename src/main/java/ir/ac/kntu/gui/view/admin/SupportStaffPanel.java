package ir.ac.kntu.gui.view.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ir.ac.kntu.gui.component.PagedTable;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.gui.util.GuiValidation;
import ir.ac.kntu.persona.AdminManagementService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportSection;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Admin Callcenter Management: create Callcenter agents and assign their
// support responsibility areas (SupportSection). All work runs off the FX
// thread.
public class SupportStaffPanel extends VBox {

    private final Persona actor;
    private final TableView<Persona> table = new TableView<>();
    private final PagedTable<Persona> pagedTable = new PagedTable<>(table);
    private final TextField emailField = new TextField();
    private final TextField passwordField = new TextField();

    public SupportStaffPanel(Persona actor) {
        super(16);
        this.actor = actor;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Callcenter Management");
        heading.getStyleClass().add("h1");

        buildTable();
        getChildren().addAll(heading, buildCreateForm(),
                buildAssignRow(), new Label("Callcenter agents"), pagedTable);
        VBox.setVgrow(pagedTable, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<Persona, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        email.setPrefWidth(220);
        TableColumn<Persona, String> sections = new TableColumn<>("Assigned sections");
        sections.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getAssignedSupportSections().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", "))));
        sections.setPrefWidth(320);

        table.getColumns().addAll(email, sections);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No callcenter agents."));
    }

    private HBox buildCreateForm() {
        emailField.setPromptText("Agent email");
        passwordField.setPromptText("Temp password");
        emailField.getStyleClass().add("field");
        passwordField.getStyleClass().add("field");

        Button create = new Button("Create agent");
        create.getStyleClass().add("primary");
        create.setOnAction(event -> handleCreate());

        HBox box = new HBox(10, emailField, passwordField, create);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox buildAssignRow() {
        Label label = new Label("Assign sections to selected agent:");
        FlowPane checks = new FlowPane(12, 8);
        List<CheckBox> boxes = new ArrayList<>();
        // Control-flow loop: each iteration adds to two targets (the boxes list
        // we keep for reading state later, and the FlowPane's children).
        for (SupportSection section : SupportSection.values()) {
            CheckBox cb = new CheckBox(section.name());
            boxes.add(cb);
            checks.getChildren().add(cb);
        }
        Button apply = new Button("Apply sections");
        apply.getStyleClass().add("ghost");
        apply.setOnAction(event -> handleAssign(boxes));

        VBox box = new VBox(8, label, checks, apply);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));
        return box;
    }

    private void handleCreate() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pw = passwordField.getText() == null ? "" : passwordField.getText().trim();
        if (!GuiValidation.requireValidEmailAndPassword(email, pw)) {
            return;
        }
        BackgroundJobs.runAction(
                () -> AdminManagementService.createCallCenter(actor, email, pw),
                () -> {
                    emailField.clear();
                    passwordField.clear();
                    Dialogs.info("Created", "Callcenter agent created.");
                    refresh();
                },
                error -> Dialogs.error("Create failed", error));
    }

    private void handleAssign(List<CheckBox> boxes) {
        Persona agent = table.getSelectionModel().getSelectedItem();
        if (agent == null) {
            Dialogs.warn("No selection", "Select an agent first.");
            return;
        }
        Set<SupportSection> selected = boxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> SupportSection.valueOf(cb.getText()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SupportSection.class)));
        BackgroundJobs.runAction(
                () -> AdminManagementService.assignSupportSections(actor, agent.getEmail(), selected),
                () -> {
                    Dialogs.info("Updated", "Sections assigned to " + agent.getEmail() + ".");
                    refresh();
                },
                error -> Dialogs.error("Assignment failed", error));
    }

    private void refresh() {
        BackgroundJobs.run(
                () -> AdminManagementService.listAllUsers().stream()
                        .filter(p -> p.getRole() == UserRole.CALLCENTER)
                        .sorted(Comparator.comparing(Persona::getEmail,
                                String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList()),
                list -> pagedTable.setItems(list != null ? list : new ArrayList<>()),
                error -> Dialogs.error("Could not load staff", error));
    }
}
