package ir.ac.kntu.gui.notification;

import java.util.ArrayList;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.mail.Inbox;
import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.persona.Persona;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Notification inbox: lists all received messages (read and unread) and lets the
 * user mark them read. Loading runs off the FX thread.
 */
public class NotificationsPanel extends VBox {

    private final Persona persona;
    private final TableView<MailMessage> table = new TableView<>();
    private final Runnable onRead;

    public NotificationsPanel(Persona persona) {
        this(persona, null);
    }

    /**
     * @param onRead optional callback fired when the user's latest notifications
     *               are read (mark-all-read), so the shell can clear its unread
     *               indicator. May be {@code null}.
     */
    public NotificationsPanel(Persona persona, Runnable onRead) {
        super(16);
        this.persona = persona;
        this.onRead = onRead;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Notifications");
        heading.getStyleClass().add("h1");

        buildTable();
        getChildren().addAll(heading, actions(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<MailMessage, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSentDate()));
        date.setPrefWidth(150);
        TableColumn<MailMessage, String> subject = new TableColumn<>("Subject");
        subject.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSubject()));
        subject.setPrefWidth(220);
        TableColumn<MailMessage, String> body = new TableColumn<>("Message");
        body.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBody()));
        body.setPrefWidth(320);
        TableColumn<MailMessage, String> read = new TableColumn<>("Read");
        read.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().isRead() ? "Yes" : "No"));

        table.getColumns().addAll(date, subject, body, read);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No notifications."));
    }

    private HBox actions() {
        Button markRead = new Button("Mark all as read");
        markRead.getStyleClass().add("ghost");
        markRead.setOnAction(event -> handleMarkAllRead());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("ghost");
        refresh.setOnAction(event -> refresh());

        HBox box = new HBox(10, markRead, refresh);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void handleMarkAllRead() {
        String email = persona.getEmail();
        BackgroundJobs.runAction(
                () -> MailService.markInboxRead(email),
                () -> {
                    refresh();
                    if (onRead != null) {
                        onRead.run();
                    }
                },
                error -> Dialogs.error("Could not update", error));
    }

    private void refresh() {
        String email = persona.getEmail();
        BackgroundJobs.run(
                () -> {
                    Inbox inbox = MailService.getInbox(email);
                    return inbox != null ? inbox.getMessages() : new ArrayList<MailMessage>();
                },
                list -> table.setItems(FXCollections.observableArrayList(
                        list != null ? list : new ArrayList<>())),
                error -> Dialogs.error("Could not load notifications", error));
    }
}
