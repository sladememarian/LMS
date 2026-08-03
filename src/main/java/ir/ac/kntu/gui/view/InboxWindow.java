package ir.ac.kntu.gui.view;

import ir.ac.kntu.gui.util.UiTheme;
import ir.ac.kntu.mail.MailMessage;
import ir.ac.kntu.mail.MailService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// A separate window that shows a user's simulated email inbox (via
// MailService.getInbox). It is opened during login so the user can read the
// delivered 2FA verification code and copy it back into the sign-in dialog.
// Purely a viewer over existing backend mail data — it adds no backend behaviour.
public class InboxWindow {

    private final Stage stage = new Stage();
    private final String recipient;
    private final TableView<MailMessage> table = new TableView<>();
    private final TextArea body = new TextArea();

    public InboxWindow(String recipient, UiTheme theme) {
        this.recipient = recipient;
        Scene scene = new Scene(buildRoot(), 560, 420);
        theme.applyTo(scene);
        stage.setScene(scene);
        stage.setTitle("Inbox — " + recipient);
        refresh();
    }

    private VBox buildRoot() {
        Label heading = new Label("Mailbox");
        heading.getStyleClass().add("h1");
        Label hint = new Label(
                "Your 2FA code was delivered here. Select the newest message and copy the code.");
        hint.getStyleClass().add("muted");

        buildTable();
        body.setEditable(false);
        body.setWrapText(true);
        body.setPrefRowCount(4);
        body.getStyleClass().add("field");

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("ghost");
        refresh.setOnAction(event -> refresh());
        HBox actions = new HBox(10, refresh);

        VBox root = new VBox(12, heading, hint, table, body, actions);
        root.getStyleClass().add("content-area");
        root.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<MailMessage, String> subject = new TableColumn<>("Subject");
        subject.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSubject()));
        subject.setPrefWidth(260);
        TableColumn<MailMessage, String> date = new TableColumn<>("Received");
        date.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSentDate()));
        date.setPrefWidth(240);

        table.getColumns().addAll(subject, date);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No messages."));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                body.setText(selected == null ? "" : selected.getBody()));
    }

    private void refresh() {
        // Newest first, so the just-delivered 2FA message is at the top.
        java.util.List<MailMessage> ordered = MailService.getInbox(recipient).getMessages().stream()
                .sorted((a, b) -> b.getSentDate().compareTo(a.getSentDate()))
                .collect(java.util.stream.Collectors.toList());
        table.setItems(FXCollections.observableArrayList(ordered));
        if (!ordered.isEmpty()) {
            table.getSelectionModel().select(0);
        }
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    public void close() {
        stage.close();
    }
}
