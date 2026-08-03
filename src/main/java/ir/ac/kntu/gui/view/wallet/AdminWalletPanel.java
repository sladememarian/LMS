package ir.ac.kntu.gui.view.wallet;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.Transaction;
import ir.ac.kntu.gui.component.PagedTable;
import ir.ac.kntu.gui.component.StatCard;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Admin wallet view: how much the admin holds from collected taxes and how
// much they owe from borrowing books. Read-only summary plus the admin's own
// transaction history. All finance calls run off the FX thread.
public class AdminWalletPanel extends VBox {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final String LOADING = "…";

    private final Persona persona;
    private final StatCard balanceCard = new StatCard("Wallet balance", LOADING, "tax revenue lands here");
    private final StatCard taxCard = new StatCard("Tax collected", LOADING, "system-wide");
    private final StatCard debtCard = new StatCard("Borrowing debt", LOADING, "what you owe");
    private final TableView<Transaction> table = new TableView<>();
    private final PagedTable<Transaction> pagedTable = new PagedTable<>(table);

    public AdminWalletPanel(Persona persona) {
        super(16);
        this.persona = persona;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Admin Wallet");
        heading.getStyleClass().add("h1");

        buildTable();
        FlowPane cards = new FlowPane(16, 16, balanceCard, taxCard, debtCard);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("ghost");
        refresh.setOnAction(event -> refresh());
        HBox historyHeader = new HBox(10, new Label("My transaction history"), refresh);
        historyHeader.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(heading, cards, historyHeader, pagedTable);
        VBox.setVgrow(pagedTable, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<Transaction, String> when = new TableColumn<>("Date");
        when.setCellValueFactory(cell -> new SimpleStringProperty(
                formatTimestamp(cell.getValue().getTimestamp())));
        when.setPrefWidth(150);
        TableColumn<Transaction, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Transaction, Number> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Transaction, String> desc = new TableColumn<>("Description");
        desc.setCellValueFactory(new PropertyValueFactory<>("description"));
        desc.setPrefWidth(280);

        table.getColumns().addAll(when, type, amount, desc);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No transactions."));
    }

    private void refresh() {
        String email = persona.getEmail();
        String memberId = persona.getMemberId();
        // Balance, total tax collected and the admin's own borrowing debt all
        // come straight from the existing finance services — no new logic.
        BackgroundJobs.run(
                () -> new int[]{
                        PersonaService.getWalletBalance(email),
                        FinanceService.getTaxRevenueCollected(),
                        FinanceService.getOutstandingDebt(memberId)
                },
                values -> {
                    balanceCard.setValue(values[0] + "");
                    taxCard.setValue(values[1] + "");
                    debtCard.setValue(values[2] + "");
                },
                error -> Dialogs.error("Could not load wallet", error));

        BackgroundJobs.run(
                () -> FinanceService.getTransactionsForMember(memberId),
                list -> pagedTable.setItems(list != null ? list : new ArrayList<>()),
                error -> Dialogs.error("Could not load transactions", error));
    }

    private static String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0) {
            return "—";
        }
        return TS_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }
}
