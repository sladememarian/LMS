package ir.ac.kntu.gui.view.library;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.gui.util.LibraryColumns;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;
import ir.ac.kntu.persona.UserProfile;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.util.SystemSettingsService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Library browser with real-time, background-threaded search and pagination.
 *
 * <p>As the user types, keystrokes are debounced (300 ms) and the actual search
 * runs on a background {@link javafx.concurrent.Task} via {@link BackgroundJobs}
 * so the UI never freezes. Results are paginated client-side, 10 rows per page.
 */
public class LibrarySearchPanel extends BorderPane {

    private static final int PAGE_SIZE = 10;

    private final TextField searchField = new TextField();
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label resultCount = new Label();
    private final TableView<LibraryItem> table = new TableView<>();
    private final Pagination pagination = new Pagination(1, 0);
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    private List<LibraryItem> currentResults = new ArrayList<>();

    public LibrarySearchPanel() {
        getStyleClass().add("content-area");
        setPadding(new Insets(24));
        buildTable();
        setTop(buildHeader());
        setCenter(buildBody());

        debounce.setOnFinished(event -> runSearch(searchField.getText()));
        searchField.textProperty().addListener((obs, old, val) -> debounce.playFromStart());

        // Initial load (empty query -> all items).
        runSearch("");
    }

    private VBox buildHeader() {
        Label heading = new Label("Library & Search");
        heading.getStyleClass().add("h1");

        searchField.setPromptText("Search by title, author, category… (updates as you type)");
        searchField.getStyleClass().add("field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        spinner.setMaxSize(20, 20);
        spinner.setVisible(false);
        resultCount.getStyleClass().add("muted");

        Button borrowBtn = new Button("Borrow selected");
        borrowBtn.getStyleClass().add("primary");
        borrowBtn.setOnAction(event -> handleBorrow());

        Button reserveBtn = new Button("Reserve selected");
        reserveBtn.getStyleClass().add("ghost");
        reserveBtn.setOnAction(event -> handleReserve());

        HBox searchRow = new HBox(10, searchField, borrowBtn, reserveBtn, spinner);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(12, heading, searchRow, resultCount);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private Region buildBody() {
        pagination.setPageFactory(this::renderPage);
        VBox body = new VBox(pagination);
        VBox.setVgrow(pagination, Priority.ALWAYS);
        return body;
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<LibraryItem, String> id = LibraryColumns.text("ID", "itemId");
        TableColumn<LibraryItem, String> title = LibraryColumns.text("Title", "title");
        title.setPrefWidth(260);
        TableColumn<LibraryItem, String> type = LibraryColumns.text("Type", "itemType");
        TableColumn<LibraryItem, String> category = LibraryColumns.text("Category", "category");
        TableColumn<LibraryItem, Number> available = LibraryColumns.number("Available", "availableCopies");
        TableColumn<LibraryItem, Number> total = LibraryColumns.number("Total", "totalCopies");
        TableColumn<LibraryItem, Number> price = LibraryColumns.number("Price", "unitPrice");

        table.getColumns().addAll(id, title, type, category, available, total, price);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No items found."));
    }

    /** Borrows the selected item for the signed-in user (background thread). */
    private void handleBorrow() {
        LibraryItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            Dialogs.warn("No selection", "Select an item to borrow.");
            return;
        }
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            Dialogs.warn("Not signed in", "You must be signed in to borrow.");
            return;
        }
        String error = borrowValidationError(user, item);
        if (error != null) {
            Dialogs.warn("Cannot borrow", error);
            return;
        }
        String itemId = item.getItemId();
        LibraryItem freshItem = LibraryService.getItemById(itemId);
        if (freshItem == null) {
            Dialogs.warn("Not found", "Item not found.");
            return;
        }
        int walkInAvailable = ReservationService.getWalkInAvailableCopies(
                itemId, user.getMemberId(), freshItem.getAvailableCopies());
        if (walkInAvailable <= 0) {
            showUnavailable(itemId);
            return;
        }
        doBorrowLoan(user, freshItem, itemId);
    }

    /** Reserves the selected item for the signed-in user (background thread). */
    private void handleReserve() {
        LibraryItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            Dialogs.warn("No selection", "Select an item to reserve.");
            return;
        }
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            Dialogs.warn("Not signed in", "You must be signed in to reserve.");
            return;
        }
        String memberId = user.getMemberId();
        String itemId = item.getItemId();
        String title = item.getTitle();
        int today = SimulationClock.getCurrentDay();
        BackgroundJobs.run(
                () -> ReservationService.reserve(memberId, itemId, today),
                reservation -> {
                    announceReservation(title, reservation);
                    runSearch(searchField.getText());
                },
                error -> Dialogs.error("Reservation failed", error));
    }

    /** Reports whether the reservation is ready now (ACTIVE) or queued (WAITING). */
    private void announceReservation(String title, ir.ac.kntu.reservation.Reservation reservation) {
        if (reservation.isActive()) {
            Dialogs.info("Reserved",
                    "\"" + title + "\" is ready for pickup. Pick up by day "
                            + reservation.getExpiresOnDay() + ".");
            return;
        }
        int position = ReservationService.getQueuePosition(
                reservation.getReservationId(), reservation.getItemId());
        String place = position > 0 ? " You are #" + position + " in the queue." : "";
        Dialogs.info("Reserved",
                "\"" + title + "\" is currently out. You have been added to the waiting list."
                        + place);
    }

    private String borrowValidationError(Persona user, LibraryItem item) {
        UserProfile profile = user.getUserProfile();
        if (!profile.canBorrow()) {
            return "Your role (" + profile.dashboardLabel() + ") cannot borrow items.";
        }
        if (user.getBorrowCount() >= profile.borrowLimit()) {
            return "Borrow limit reached for role " + profile.dashboardLabel() + ".";
        }
        if (user.hasBorrowed(item.getItemId())) {
            return "You already have this item.";
        }
        return null;
    }

    private void showUnavailable(String itemId) {
        java.util.List<String> holders = ReservationService.getActiveReservationHolders(itemId);
        if (holders.isEmpty()) {
            Dialogs.warn("Unavailable", "All copies are currently checked out.");
        } else {
            Dialogs.warn("Unavailable",
                    "This copy is being held for " + holders.size() + " reservation(s).");
        }
    }

    private void doBorrowLoan(Persona user, LibraryItem freshItem, String itemId) {
        String memberId = user.getMemberId();
        String email = user.getEmail();
        int today = SimulationClock.getCurrentDay();
        int loanPeriodDays = Math.min(freshItem.borrowPeriod(), SystemSettingsService.getBorrowDays());
        BackgroundJobs.runAction(
                () -> {
                    if (!FinanceService.checkBorrowingPermission(memberId)) {
                        throw new IllegalStateException(
                                "Borrowing blocked: settle outstanding debt first.");
                    }
                    LibraryService.executeBorrow(itemId);
                    PersonaService.recordBorrow(email, itemId);
                    LoanService.recordLoan(memberId, itemId, today, loanPeriodDays);
                    ReservationService.completeReservation(memberId, itemId);
                },
                () -> {
                    Dialogs.info("Borrowed", "\"" + freshItem.getTitle() + "\" borrowed successfully.");
                    runSearch(searchField.getText());
                },
                error -> Dialogs.error("Borrow failed", error));
    }

    /** Runs the search off the FX thread and refreshes pagination on success. */
    private void runSearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        spinner.setVisible(true);
        BackgroundJobs.run(
                () -> query.isEmpty()
                        ? LibraryService.getAllItems()
                        : LibraryService.searchItems(query),
                results -> {
                    spinner.setVisible(false);
                    // Deterministic order (by item ID) so the browse/search table
                    // isn't at the mercy of the store's row order.
                    currentResults = results != null ? results : new ArrayList<>();
                    currentResults.sort(java.util.Comparator.comparing(
                            LibraryItem::getItemId, String.CASE_INSENSITIVE_ORDER));
                    resultCount.setText(currentResults.size() + " item(s) found");
                    int pages = Math.max(1,
                            (int) Math.ceil(currentResults.size() / (double) PAGE_SIZE));
                    pagination.setPageCount(pages);
                    // Force the current page to re-render with new data.
                    int current = Math.min(pagination.getCurrentPageIndex(), pages - 1);
                    pagination.setCurrentPageIndex(0);
                    Platform.runLater(() -> pagination.setCurrentPageIndex(current));
                },
                error -> {
                    spinner.setVisible(false);
                    Dialogs.error("Search failed", error);
                });
    }

    /** Pagination page factory: shows PAGE_SIZE rows for the given page index. */
    private TableView<LibraryItem> renderPage(int pageIndex) {
        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, currentResults.size());
        List<LibraryItem> slice = from <= to && from >= 0
                ? currentResults.subList(from, to)
                : new ArrayList<>();
        ObservableList<LibraryItem> pageData = FXCollections.observableArrayList(slice);
        // setItems clears the selection. A debounced search can re-render this
        // page moments after the user picked a row, silently dropping their
        // selection out from under a Borrow/Reserve click; capture it first and
        // restore it if the same item is still on this page.
        LibraryItem previouslySelected = table.getSelectionModel().getSelectedItem();
        table.setItems(pageData);
        if (previouslySelected != null && pageData.contains(previouslySelected)) {
            table.getSelectionModel().select(previouslySelected);
        }
        return table;
    }
}
