package ir.ac.kntu.gui.component;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Reusable client-side pagination wrapper around a TableView: it holds the full
// result list, computes the page count, and re-renders the visible slice via a
// Pagination page-factory. Every result table in the app uses this so they all
// paginate the same way (10 rows per page by default), just with different
// columns/content. T is the row type of the wrapped table.
public class PagedTable<T> extends VBox {

    // Default rows shown per page, matching the Library/Search reference.
    public static final int DEFAULT_PAGE_SIZE = 10;

    private final TableView<T> table;
    private final Pagination pagination = new Pagination(1, 0);
    private final int pageSize;

    private List<T> items = new ArrayList<>();

    public PagedTable(TableView<T> table) {
        this(table, DEFAULT_PAGE_SIZE);
    }

    public PagedTable(TableView<T> table, int pageSize) {
        this.table = table;
        this.pageSize = Math.max(1, pageSize);
        pagination.setPageFactory(this::renderPage);
        getChildren().add(pagination);
        VBox.setVgrow(pagination, Priority.ALWAYS);
    }

    // The wrapped table, e.g. for selection-model access.
    public TableView<T> getTable() {
        return table;
    }

    // Replaces the backing data, recomputes the page count and re-renders the
    // current page (clamped to the new bounds). Call on the FX thread.
    public void setItems(List<T> newItems) {
        this.items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        int pages = Math.max(1, (int) Math.ceil(items.size() / (double) pageSize));
        int current = Math.min(Math.max(0, pagination.getCurrentPageIndex()), pages - 1);
        pagination.setPageCount(pages);
        // Setting the index only re-invokes the page-factory when the value
        // actually changes; for a fresh panel whose data lands on page 0 the
        // index is already 0, so we must render the slice explicitly or the
        // table would stay empty. Render directly, then sync the control.
        if (pagination.getCurrentPageIndex() != current) {
            pagination.setCurrentPageIndex(current);
        } else {
            renderPage(current);
        }
    }

    private TableView<T> renderPage(int pageIndex) {
        int from = pageIndex * pageSize;
        int to = Math.min(from + pageSize, items.size());
        List<T> slice = from >= 0 && from <= to ? items.subList(from, to) : new ArrayList<>();
        table.setItems(FXCollections.observableArrayList(slice));
        return table;
    }
}
