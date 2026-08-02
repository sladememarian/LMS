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
import ir.ac.kntu.gui.util.CardValidator;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Wallet &amp; Transactions screen: shows balance and outstanding debt, allows
 * top-ups and debt payment, and lists the user's transaction history. All
 * finance calls run off the FX thread.
 */
public class WalletPanel extends VBox {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final String FIELD_STYLE = "field";

    private final Persona persona;
    private final StatCard balanceCard = new StatCard("Balance", "…", "wallet");
    private final StatCard debtCard = new StatCard("Outstanding debt", "…", "unpaid fines");
    private final TextField amountField = new TextField();
    private final TextField cardField = new TextField();
    private final TextField holderField = new TextField();
    private final PasswordField cvvField = new PasswordField();
    private final TextField expiryField = new TextField();
    private final TableView<Transaction> table = new TableView<>();
    private final PagedTable<Transaction> pagedTable = new PagedTable<>(table);
    private final Button topUp = new Button("Top up");
    private final Button payDebt = new Button("Pay debt");
    private final ProgressIndicator actionProgress = new ProgressIndicator();

    public WalletPanel(Persona persona) {
        super(16);
        this.persona = persona;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Wallet & Transactions");
        heading.getStyleClass().add("h1");

        buildTable();
        FlowPane cards = new FlowPane(16, 16, balanceCard, debtCard);
        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("ghost");
        refresh.setOnAction(event -> refresh());
        HBox historyHeader = new HBox(10, new Label("Transaction history"), refresh);
        historyHeader.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(heading, cards, buildActions(),
                historyHeader, pagedTable);
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

    private VBox buildActions() {
        amountField.setPromptText("Amount");
        amountField.getStyleClass().add(FIELD_STYLE);
        cardField.setPromptText("Card (6037-xxxx-xxxx-xxxx)");
        cardField.getStyleClass().add(FIELD_STYLE);
        holderField.setPromptText("Card Holder");
        holderField.getStyleClass().add(FIELD_STYLE);
        cvvField.setPromptText("CVV");
        cvvField.getStyleClass().add(FIELD_STYLE);
        expiryField.setPromptText("Expiry (MM/YY)");
        expiryField.getStyleClass().add(FIELD_STYLE);

        Button topUp = this.topUp;
        topUp.getStyleClass().add("primary");
        topUp.setOnAction(event -> handleTopUp());

        Button payDebt = this.payDebt;
        payDebt.getStyleClass().add("ghost");
        payDebt.setOnAction(event -> handlePayDebt());

        actionProgress.setMaxSize(18, 18);
        actionProgress.setVisible(false);

        HBox row1 = new HBox(10, amountField, cardField, holderField);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(10, cvvField, expiryField, topUp, payDebt, actionProgress);
        row2.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, row1, row2);
    }

    /** Toggles the small inline spinner and disables the action buttons. */
    private void setActionBusy(boolean busy) {
        actionProgress.setVisible(busy);
        topUp.setDisable(busy);
        payDebt.setDisable(busy);
    }

    private void handleTopUp() {
        int amount;
        try {
            amount = Integer.parseInt(amountField.getText() == null ? "" : amountField.getText().trim());
        } catch (NumberFormatException ex) {
            Dialogs.warn("Invalid amount", "Enter a whole number to top up.");
            return;
        }
        if (amount <= 0) {
            Dialogs.warn("Invalid amount", "Amount must be positive.");
            return;
        }
        String card = cardField.getText() == null ? "" : cardField.getText().trim();
        String holder = holderField.getText() == null ? "" : holderField.getText().trim();
        String cvv = cvvField.getText() == null ? "" : cvvField.getText().trim();
        String expiry = expiryField.getText() == null ? "" : expiryField.getText().trim();
        if (!CardValidator.isValidCard(card, holder, cvv, expiry)) {
            Dialogs.warn("Invalid card",
                    "Enter a 16-digit card number, card holder, 3–4 digit CVV and MM/YY expiry.");
            return;
        }
        setActionBusy(true);
        BackgroundJobs.runAction(
                () -> FinanceService.proccessWalletCharge(persona, amount),
                () -> {
                    setActionBusy(false);
                    amountField.clear();
                    cardField.clear();
                    holderField.clear();
                    cvvField.clear();
                    expiryField.clear();
                    Dialogs.info("Top up complete", "Wallet charged by " + amount + ".");
                    refresh();
                },
                error -> {
                    setActionBusy(false);
                    Dialogs.error("Top up failed", error);
                });
    }

    private void handlePayDebt() {
        setActionBusy(true);
        BackgroundJobs.runAction(
                () -> FinanceService.payDebt(persona),
                () -> {
                    setActionBusy(false);
                    Dialogs.info("Debt paid", "Your outstanding debt has been cleared.");
                    refresh();
                },
                error -> {
                    setActionBusy(false);
                    Dialogs.error("Payment failed", error);
                });
    }

    private void refresh() {
        String email = persona.getEmail();
        String memberId = persona.getMemberId();
        BackgroundJobs.run(
                () -> new int[]{
                        PersonaService.getWalletBalance(email),
                        FinanceService.getOutstandingDebt(memberId)
                },
                values -> {
                    balanceCard.setValue(values[0] + "");
                    debtCard.setValue(values[1] + "");
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
