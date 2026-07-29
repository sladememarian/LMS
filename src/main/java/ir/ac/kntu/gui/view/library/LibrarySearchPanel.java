package ir.ac.kntu.gui.view.library;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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

        HBox searchRow = new HBox(10, searchField, spinner);
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
        TableColumn<LibraryItem, String> id = new TableColumn<>("ID");
        id.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        TableColumn<LibraryItem, String> title = new TableColumn<>("Title");
        title.setCellValueFactory(new PropertyValueFactory<>("title"));
        title.setPrefWidth(260);
        TableColumn<LibraryItem, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        TableColumn<LibraryItem, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<LibraryItem, Number> available = new TableColumn<>("Available");
        available.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        TableColumn<LibraryItem, Number> total = new TableColumn<>("Total");
        total.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        TableColumn<LibraryItem, Number> price = new TableColumn<>("Price");
        price.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        table.getColumns().addAll(id, title, type, category, available, total, price);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No items found."));
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
                    currentResults = results != null ? results : new ArrayList<>();
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
        table.setItems(pageData);
        return table;
    }
}
