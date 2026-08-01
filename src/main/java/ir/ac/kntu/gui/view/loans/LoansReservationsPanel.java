package ir.ac.kntu.gui.view.loans;

import java.util.ArrayList;
import java.util.stream.Collectors;

import ir.ac.kntu.exception.InsufficientFundsException;
import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.Loan;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.reservation.Reservation;
import ir.ac.kntu.reservation.ReservationService;
import javafx.collections.FXCollections;
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
 * Regular-user Loans &amp; Reservations screen: active loans (with due day and
 * overdue status computed via the simulation clock) plus the user's reservations.
 * Return and extend actions reuse the existing services and run off the FX thread.
 */
public class LoansReservationsPanel extends VBox {

    private static final int EXTENSION_FEE = 25_000;
    private static final int EXTENSION_DAYS = 7;
    private static final String GHOST_STYLE = "ghost";
    private static final String NO_SELECTION = "No selection";

    private final Persona persona;
    private final TableView<LoanRow> loanTable = new TableView<>();
    private final TableView<Reservation> reservationTable = new TableView<>();

    public LoansReservationsPanel(Persona persona) {
        super(16);
        this.persona = persona;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Loans & Reservations");
        heading.getStyleClass().add("h1");

        buildLoanTable();
        buildReservationTable();

        getChildren().addAll(heading,
                new Label("Active loans"), loanActions(), loanTable,
                new Label("My reservations"), reservationActions(), reservationTable);
        VBox.setVgrow(loanTable, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildLoanTable() {
        TableColumn<LoanRow, String> item = new TableColumn<>("Item");
        item.setCellValueFactory(new PropertyValueFactory<>("itemTitle"));
        item.setPrefWidth(240);
        TableColumn<LoanRow, Number> borrow = new TableColumn<>("Borrowed day");
        borrow.setCellValueFactory(new PropertyValueFactory<>("borrowDay"));
        TableColumn<LoanRow, Number> due = new TableColumn<>("Due day");
        due.setCellValueFactory(new PropertyValueFactory<>("dueDay"));
        TableColumn<LoanRow, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        loanTable.getColumns().addAll(item, borrow, due, status);
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loanTable.setPlaceholder(new Label("No active loans."));
    }

    @SuppressWarnings("unchecked")
    private void buildReservationTable() {
        TableColumn<Reservation, String> item = new TableColumn<>("Item ID");
        item.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        TableColumn<Reservation, Number> reserved = new TableColumn<>("Reserved day");
        reserved.setCellValueFactory(new PropertyValueFactory<>("reservedOnDay"));
        TableColumn<Reservation, Number> expires = new TableColumn<>("Expires day");
        expires.setCellValueFactory(new PropertyValueFactory<>("expiresOnDay"));
        TableColumn<Reservation, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cell.getValue().getStatus())));
        TableColumn<Reservation, Number> queue = new TableColumn<>("Queue #");
        queue.setCellValueFactory(cell -> {
            Reservation reservation = cell.getValue();
            int pos = ReservationService.getQueuePosition(reservation.getReservationId(), reservation.getItemId());
            return new javafx.beans.property.SimpleIntegerProperty(pos < 0 ? 0 : pos + 1);
        });
        reservationTable.getColumns().addAll(item, reserved, expires, queue, status);
        reservationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reservationTable.setPlaceholder(new Label("No reservations."));
        reservationTable.setPrefHeight(180);
    }

    private HBox loanActions() {
        Button returnBtn = new Button("Return selected");
        returnBtn.getStyleClass().add(GHOST_STYLE);
        returnBtn.setOnAction(event -> handleReturn());

        Button extendBtn = new Button("Extend (+" + EXTENSION_DAYS + " days, fee " + EXTENSION_FEE + " + tax)");
        extendBtn.getStyleClass().add(GHOST_STYLE);
        extendBtn.setOnAction(event -> handleExtend());

        HBox box = new HBox(10, returnBtn, extendBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox reservationActions() {
        Button cancelBtn = new Button("Cancel reservation");
        cancelBtn.getStyleClass().add(GHOST_STYLE);
        cancelBtn.setOnAction(event -> handleCancelReservation());
        HBox box = new HBox(10, cancelBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void handleReturn() {
        LoanRow row = loanTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            Dialogs.warn(NO_SELECTION, "Select a loan to return.");
            return;
        }
        String memberId = persona.getMemberId();
        String email = persona.getEmail();

        if (!persona.hasBorrowed(row.getItemId())) {
            Dialogs.warn("Not borrowed", "You have not borrowed that item.");
            return;
        }
        LibraryItem item = LibraryService.getItemById(row.getItemId());
        if (item == null) {
            Dialogs.warn("Not found", "Item does not exist.");
            return;
        }

        BackgroundJobs.runAction(
                () -> {
                    LibraryService.executeReturn(row.getItemId());
                    PersonaService.recordReturn(email, row.getItemId());
                    LoanService.clearLoan(memberId, row.getItemId());
                    ReservationService.processReturn(row.getItemId(), SimulationClock.getCurrentDay());
                },
                () -> {
                    Dialogs.info("Returned", "Item returned successfully.");
                    refresh();
                },
                error -> Dialogs.error("Return failed", error));
    }

    private void handleExtend() {
        LoanRow row = loanTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            Dialogs.warn(NO_SELECTION, "Select a loan to extend.");
            return;
        }
        String memberId = persona.getMemberId();
        String extendError = extendValidationError(row);
        if (extendError != null) {
            Dialogs.warn("Cannot extend", extendError);
            return;
        }
        // Fee + tax computation and the funds guard live in
        // FinanceService.proccessExtentionPayment; the GUI does not duplicate them.
        doExtendLoan(row, memberId);
    }

    private String extendValidationError(LoanRow row) {
        if (!persona.getUserProfile().canExtend()) {
            return persona.getRole() + " cannot extend return dates.";
        }
        if (!persona.hasBorrowed(row.getItemId())) {
            return "You have not borrowed that item.";
        }
        return null;
    }

    private void doExtendLoan(LoanRow row, String memberId) {
        BackgroundJobs.run(
                () -> {
                    FinanceService.proccessExtentionPayment(persona, EXTENSION_FEE);
                    return LoanService.extendLoan(memberId, row.getItemId(), EXTENSION_DAYS);
                },
                ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        Dialogs.info("Extended",
                                "Loan extended by " + EXTENSION_DAYS + " days.");
                        refresh();
                    } else {
                        Dialogs.warn("Not extended", "The loan could not be extended.");
                    }
                },
                error -> {
                    if (error instanceof InsufficientFundsException) {
                        // Surface the service's own accurate message.
                        Dialogs.warn("Insufficient funds", error.getMessage());
                    } else {
                        Dialogs.error("Extend failed", error);
                    }
                });
    }

    private void handleCancelReservation() {
        Reservation reservation = reservationTable.getSelectionModel().getSelectedItem();
        if (reservation == null) {
            Dialogs.warn(NO_SELECTION, "Select a reservation to cancel.");
            return;
        }
        BackgroundJobs.runAction(
                () -> ReservationService.cancel(reservation.getReservationId()),
                () -> {
                    Dialogs.info("Cancelled", "Reservation cancelled.");
                    refresh();
                },
                error -> Dialogs.error("Cancel failed", error));
    }

    private void refresh() {
        String memberId = persona.getMemberId();
        int today = SimulationClock.getCurrentDay();

        BackgroundJobs.run(
                () -> LoanService.getLoans().stream()
                        .filter(loan -> memberId != null && memberId.equals(loan.getMemberId()))
                        .map(loan -> toRow(loan, today))
                        .collect(Collectors.toList()),
                rows -> loanTable.setItems(FXCollections.observableArrayList(
                        rows != null ? rows : new ArrayList<>())),
                error -> Dialogs.error("Could not load loans", error));

        BackgroundJobs.run(
                () -> ReservationService.getMemberReservations(memberId),
                list -> reservationTable.setItems(FXCollections.observableArrayList(
                        list != null ? list : new ArrayList<>())),
                error -> Dialogs.error("Could not load reservations", error));
    }

    private LoanRow toRow(Loan loan, int today) {
        LibraryItem item = LibraryService.getItemById(loan.getItemId());
        String title = item != null ? item.getTitle() : loan.getItemId();
        String status = loan.isOverdue(today)
                ? "OVERDUE (" + loan.daysOverdue(today) + "d)"
                : "On time";
        return new LoanRow(loan.getItemId(), title, loan.getBorrowDay(), loan.getDueDay(), status);
    }

    /** Row view-model for the loan table (public getters for PropertyValueFactory). */
    public static class LoanRow {
        private final String itemId;
        private final String itemTitle;
        private final int borrowDay;
        private final int dueDay;
        private final String status;

        public LoanRow(String itemId, String itemTitle, int borrowDay, int dueDay, String status) {
            this.itemId = itemId;
            this.itemTitle = itemTitle;
            this.borrowDay = borrowDay;
            this.dueDay = dueDay;
            this.status = status;
        }

        public String getItemId() {
            return itemId;
        }

        public String getItemTitle() {
            return itemTitle;
        }

        public int getBorrowDay() {
            return borrowDay;
        }

        public int getDueDay() {
            return dueDay;
        }

        public String getStatus() {
            return status;
        }
    }
}
