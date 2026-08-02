package ir.ac.kntu.gui.view.support;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.mail.MessageType;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.support.SupportSection;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * CallCenter support inbox: shows tickets in the agent's assigned sections
 * (via {@link SupportService#getTicketsForAgent}), and lets the agent reply to
 * or close a ticket. Actions run off the FX thread.
 */
public class SupportInboxPanel extends VBox {

    private static final String GHOST_STYLE = "ghost";
    private static final String LABEL_ADD_ITEM = "Add library item";

    private final Persona agent;
    private final TableView<SupportTicket> table = new TableView<>();
    private final ComboBox<String> sectionFilter = new ComboBox<>();

    public SupportInboxPanel(Persona agent) {
        super(16);
        this.agent = agent;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Support Inbox");
        heading.getStyleClass().add("h1");

        buildTable();
        sectionFilter.getItems().add("All sections");
        java.util.Arrays.stream(SupportSection.values())
                .map(Enum::name)
                .forEach(sectionFilter.getItems()::add);
        sectionFilter.getSelectionModel().selectFirst();
        sectionFilter.setOnAction(e -> refresh());

        getChildren().addAll(heading, sectionFilter, actions(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<SupportTicket, String> id = new TableColumn<>("Ticket");
        id.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTicketId()));
        TableColumn<SupportTicket, String> user = new TableColumn<>("User");
        user.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUserId()));
        TableColumn<SupportTicket, String> section = new TableColumn<>("Section");
        section.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(cell.getValue().getSection())));
        TableColumn<SupportTicket, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        title.setPrefWidth(200);
        TableColumn<SupportTicket, String> priority = new TableColumn<>("Priority");
        priority.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPriority()));
        TableColumn<SupportTicket, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));

        table.getColumns().addAll(id, user, section, title, priority, status);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No tickets in your sections."));
    }

    private HBox actions() {
        Button reply = new Button("Reply");
        reply.getStyleClass().add("primary");
        reply.setOnAction(event -> handleReply());

        Button close = new Button("Close ticket");
        close.getStyleClass().add(GHOST_STYLE);
        close.setOnAction(event -> handleClose());

        Button addItem = new Button(LABEL_ADD_ITEM);
        addItem.getStyleClass().add(GHOST_STYLE);
        addItem.setOnAction(event -> handleAddItem());

        Button notifications = new Button("Notifications");
        notifications.getStyleClass().add(GHOST_STYLE);
        notifications.setOnAction(event -> handleNotifications());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add(GHOST_STYLE);
        refresh.setOnAction(event -> refresh());

        HBox box = new HBox(10, reply, close, addItem, notifications, refresh);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void handleReply() {
        SupportTicket ticket = selected();
        if (ticket == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reply to ticket");
        dialog.setHeaderText("Reply to " + ticket.getTicketId());
        dialog.setContentText("Message:");
        dialog.showAndWait().ifPresent(message -> {
            if (message.isBlank()) {
                Dialogs.warn("Empty message", "Reply cannot be empty.");
                return;
            }
            BackgroundJobs.runAction(
                    () -> SupportService.respondToTicket(ticket.getTicketId(), message.trim()),
                    () -> {
                        Dialogs.info("Reply sent", "The user has been notified.");
                        refresh();
                    },
                    error -> Dialogs.error("Reply failed", error));
        });
    }

    private void handleClose() {
        SupportTicket ticket = selected();
        if (ticket == null) {
            return;
        }
        BackgroundJobs.runAction(
                () -> SupportService.updateTicketStatus(ticket.getTicketId(), "CLOSED"),
                () -> {
                    Dialogs.info("Ticket closed", ticket.getTicketId() + " marked as closed.");
                    refresh();
                },
                error -> Dialogs.error("Close failed", error));
    }

    private SupportTicket selected() {
        SupportTicket ticket = table.getSelectionModel().getSelectedItem();
        if (ticket == null) {
            Dialogs.warn("No selection", "Select a ticket first.");
        }
        return ticket;
    }

    private void handleAddItem() {
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle(LABEL_ADD_ITEM);
        idDialog.setHeaderText("Item ID:");
        idDialog.setContentText("ID:");
        idDialog.showAndWait().ifPresent(id -> {
            if (id.isBlank()) {
                Dialogs.warn("Missing ID", "Item ID is required.");
                return;
            }
            TextInputDialog titleDialog = new TextInputDialog();
            titleDialog.setTitle(LABEL_ADD_ITEM);
            titleDialog.setHeaderText("Title for " + id);
            titleDialog.setContentText("Title:");
            titleDialog.showAndWait().ifPresent(title -> {
                if (title.isBlank()) {
                    Dialogs.warn("Missing title", "Title is required.");
                    return;
                }
                LibraryItem item = new ir.ac.kntu.library.Book(id.trim(), title.trim(), "", 2024);
                item.setTotalCopies(1);
                item.setAvailableCopies(1);
                item.setUnitPrice(0);
                BackgroundJobs.runAction(
                        () -> SupportService.addLibraryItemViaSupport(item),
                        () -> Dialogs.info("Item added", "Item " + id.trim() + " added."),
                        error -> Dialogs.error("Add failed", error));
            });
        });
    }

    private void handleNotifications() {
        String email = agent.getEmail();
        BackgroundJobs.run(
                () -> {
                    List<MailMessage> allMessages = MailService.getInbox(email).getMessages();
                    if (allMessages == null) {
                        return new ArrayList<MailMessage>();
                    }
                    return allMessages.stream()
                            .filter(m -> m.getMessageType() == MessageType.SYSTEM_NOTIFICATION)
                            .collect(Collectors.toList());
                },
                list -> {
                    String body = list == null || list.isEmpty()
                            ? "No notifications."
                            : list.stream()
                                    .map(n -> n.getSubject() + ": " + n.getBody())
                                    .collect(Collectors.joining("\n"));
                    Dialogs.info("Notifications", body.trim());
                },
                error -> Dialogs.error("Could not load notifications", error));
    }

    private void refresh() {
        String filter = sectionFilter.getValue();
        BackgroundJobs.run(
                () -> {
                    List<SupportTicket> allTickets = SupportService.getTicketsForAgent(agent);
                    if (filter == null || "All sections".equals(filter)) {
                        return allTickets;
                    }
                    return allTickets.stream()
                            .filter(t -> t.getSection() != null && t.getSection().name().equals(filter))
                            .collect(Collectors.toList());
                },
                list -> table.setItems(FXCollections.observableArrayList(
                        list != null ? list : new ArrayList<>())),
                error -> Dialogs.error("Could not load inbox", error));
    }
}
