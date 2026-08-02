package ir.ac.kntu.gui.view.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.Loan;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.finance.Transaction;
import ir.ac.kntu.gui.component.PagedTable;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.util.PersonaRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Fines screen (Admin / CallCenter): a table of indebted users sorted by debt
 * descending. The debtor list is built entirely with the Java Streams API and
 * computed on a background thread.
 */
public class FinesPanel extends VBox {

    private static final String GHOST_STYLE = "ghost";

    private final TableView<DebtorRow> table = new TableView<>();
    private final PagedTable<DebtorRow> pagedTable = new PagedTable<>(table);

    public FinesPanel() {
        super(16);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Fines — Indebted Users");
        heading.getStyleClass().add("h1");

        buildTable();
        getChildren().addAll(heading, buildActions(), pagedTable);
        VBox.setVgrow(pagedTable, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<DebtorRow, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(new PropertyValueFactory<>("email"));
        email.setPrefWidth(240);
        TableColumn<DebtorRow, String> member = new TableColumn<>("Member ID");
        member.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        TableColumn<DebtorRow, Number> debt = new TableColumn<>("Outstanding debt");
        debt.setCellValueFactory(new PropertyValueFactory<>("debt"));

        table.getColumns().addAll(email, member, debt);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No indebted users."));
    }

    private HBox buildActions() {
        Button drillDown = new Button("View transactions");
        drillDown.getStyleClass().add(GHOST_STYLE);
        drillDown.setOnAction(e -> handleDrillDown());

        Button overdue = new Button("Overdue loans");
        overdue.getStyleClass().add(GHOST_STYLE);
        overdue.setOnAction(e -> handleOverdue());

        Button taxRevenue = new Button("Tax revenue");
        taxRevenue.getStyleClass().add(GHOST_STYLE);
        taxRevenue.setOnAction(e -> handleTaxRevenue());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add(GHOST_STYLE);
        refresh.setOnAction(e -> refresh());

        HBox box = new HBox(10, drillDown, overdue, taxRevenue, refresh);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void handleDrillDown() {
        DebtorRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            Dialogs.warn("No selection", "Select a user first.");
            return;
        }
        String memberId = row.getMemberId();
        BackgroundJobs.run(
                () -> FinanceService.getTransactionsForMember(memberId),
                list -> {
                    StringBuilder sb = new StringBuilder();
                    if (list == null || list.isEmpty()) {
                        sb.append("No transactions.");
                    } else {
                        for (Transaction t : list) {
                            sb.append(t.getType()).append(": ").append(t.getAmount())
                                    .append(" (").append(t.getDescription()).append(")\n");
                        }
                    }
                    Dialogs.info("Transactions for " + row.getEmail(), sb.toString().trim());
                },
                error -> Dialogs.error("Could not load transactions", error));
    }

    private void handleOverdue() {
        BackgroundJobs.run(
                // Delegate to the backend's canonical overdue check instead of
                // re-implementing the day comparison in the GUI.
                () -> LoanService.getOverdueLoans(SimulationClock.getCurrentDay()),
                list -> {
                    StringBuilder sb = new StringBuilder();
                    if (list == null || list.isEmpty()) {
                        sb.append("No overdue loans.");
                    } else {
                        for (Loan l : list) {
                            sb.append(l.getMemberId()).append(" / ").append(l.getItemId())
                                    .append(" due day ").append(l.getDueDay()).append("\n");
                        }
                    }
                    Dialogs.info("Overdue Loans", sb.toString().trim());
                },
                error -> Dialogs.error("Could not load loans", error));
    }

    private void handleTaxRevenue() {
        BackgroundJobs.run(
                () -> FinanceService.getTaxRevenueCollected(),
                total -> Dialogs.info("Tax revenue", "Total tax collected: " + total + " "),
                error -> Dialogs.error("Could not load tax info", error));
    }

    private void refresh() {
        BackgroundJobs.run(
                () -> PersonaRepository.getAllPersonas().stream()
                        .map(p -> new DebtorRow(p.getEmail(), p.getMemberId(),
                                FinanceService.getOutstandingDebt(p.getMemberId())))
                        .filter(row -> row.getDebt() > 0)
                        .sorted(Comparator.comparingInt(DebtorRow::getDebt).reversed())
                        .collect(Collectors.toList()),
                rows -> pagedTable.setItems(rows != null ? rows : new ArrayList<>()),
                error -> Dialogs.error("Could not load fines", error));
    }

    /** Row view-model for the fines table. */
    public static class DebtorRow {
        private final String email;
        private final String memberId;
        private final int debt;

        public DebtorRow(String email, String memberId, int debt) {
            this.email = email;
            this.memberId = memberId;
            this.debt = debt;
        }

        public String getEmail() {
            return email;
        }

        public String getMemberId() {
            return memberId;
        }

        public int getDebt() {
            return debt;
        }
    }
}
