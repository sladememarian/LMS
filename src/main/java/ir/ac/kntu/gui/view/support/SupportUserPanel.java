package ir.ac.kntu.gui.view.support;

import java.util.ArrayList;
import java.util.stream.Collectors;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Regular-user support screen: create a ticket and track your own tickets.
 * Creation and loading run off the FX thread.
 */
public class SupportUserPanel extends VBox {

    private final Persona persona;
    private final ComboBox<SupportSection> sectionBox =
            new ComboBox<>(FXCollections.observableArrayList(SupportSection.values()));
    private final TextField titleField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final TableView<SupportTicket> table = new TableView<>();

    public SupportUserPanel(Persona persona) {
        super(16);
        this.persona = persona;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Support");
        heading.getStyleClass().add("h1");

        buildTable();
        VBox body = new VBox(16, buildForm());
        if (persona.getUserProfile().canRequestRoleUpgrade()) {
            body.getChildren().add(buildRoleUpgradeSection());
        }
        body.getChildren().add(new Label("My tickets"));
        body.getChildren().add(table);
        getChildren().addAll(heading, body);
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private VBox buildForm() {
        sectionBox.getSelectionModel().selectFirst();
        sectionBox.setId("sectionBox");
        titleField.setPromptText("Title");
        titleField.setId("titleField");
        titleField.getStyleClass().add("field");
        descriptionArea.setPromptText("Describe your issue…");
        descriptionArea.setId("descriptionArea");
        descriptionArea.setPrefRowCount(3);

        Button submit = new Button("Create ticket");
        submit.getStyleClass().add("primary");
        submit.setOnAction(event -> handleCreate());

        VBox form = new VBox(10, sectionBox, titleField, descriptionArea, submit);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<SupportTicket, String> id = new TableColumn<>("Ticket");
        id.setCellValueFactory(new PropertyValueFactory<>("ticketId"));
        TableColumn<SupportTicket, String> section = new TableColumn<>("Section");
        section.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(cell.getValue().getSection())));
        TableColumn<SupportTicket, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(new PropertyValueFactory<>("title"));
        title.setPrefWidth(200);
        TableColumn<SupportTicket, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<SupportTicket, String> response = new TableColumn<>("Response");
        response.setCellValueFactory(new PropertyValueFactory<>("response"));
        response.setPrefWidth(240);

        table.getColumns().addAll(id, section, title, status, response);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No tickets yet."));
    }

    private void handleCreate() {
        SupportSection section = sectionBox.getValue();
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        if (title.isEmpty() || description.isEmpty()) {
            Dialogs.warn("Missing information", "Title and description are required.");
            return;
        }
        String userId = persona.getMemberId();
        BackgroundJobs.runAction(
                () -> SupportService.createTicket(userId, section, title, description),
                () -> {
                    titleField.clear();
                    descriptionArea.clear();
                    Dialogs.info("Ticket created", "Your ticket has been submitted.");
                    refresh();
                },
                error -> Dialogs.error("Could not create ticket", error));
    }

    private VBox buildRoleUpgradeSection() {
        ComboBox<UserRole> targetRoleBox = new ComboBox<>(
                FXCollections.observableArrayList(
                        UserRole.STUDENT, UserRole.TEACHER));
        targetRoleBox.getSelectionModel().selectFirst();

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Reason for upgrade request...");
        messageArea.setPrefRowCount(2);

        Button requestBtn = new Button("Request role upgrade");
        requestBtn.getStyleClass().add("primary");
        requestBtn.setOnAction(e -> handleRoleRequest(targetRoleBox.getValue(), messageArea));

        VBox section = new VBox(8, new Label("Request Role Upgrade"),
                targetRoleBox, messageArea, requestBtn);
        section.getStyleClass().add("card");
        section.setPadding(new Insets(16));
        return section;
    }

    private void handleRoleRequest(UserRole targetRole, TextArea messageArea) {
        String msg = messageArea.getText() == null ? "" : messageArea.getText().trim();
        BackgroundJobs.runAction(
                () -> RoleRequestService.submit(persona, targetRole.name(), msg),
                () -> {
                    messageArea.clear();
                    Dialogs.info("Request submitted",
                            "Role upgrade request for " + targetRole + " sent to admin.");
                },
                error -> Dialogs.error("Request failed", error));
    }

    private void refresh() {
        String userId = persona.getMemberId();
        String email = persona.getEmail();
        BackgroundJobs.run(
                () -> SupportService.getAllTickets().stream()
                        .filter(t -> matchesUser(t, userId, email))
                        .collect(Collectors.toList()),
                list -> table.setItems(FXCollections.observableArrayList(
                        list != null ? list : new ArrayList<>())),
                error -> Dialogs.error("Could not load tickets", error));
    }

    private static boolean matchesUser(SupportTicket ticket, String userId, String email) {
        String owner = ticket.getUserId();
        return owner != null && (owner.equalsIgnoreCase(userId) || owner.equalsIgnoreCase(email));
    }
}
