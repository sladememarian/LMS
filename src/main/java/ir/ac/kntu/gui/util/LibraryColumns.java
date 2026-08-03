package ir.ac.kntu.gui.util;

import ir.ac.kntu.library.LibraryItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

// Reusable TableColumn builders for the LibraryItem catalogue, so the two
// library tables share one column definition. Presentation only — the
// properties read the same backend getters as before.
public final class LibraryColumns {

    private LibraryColumns() {
        // utility class
    }

    // A property-mapped text column, e.g. text("ID", "itemId").
    public static TableColumn<LibraryItem, String> text(String header, String property) {
        TableColumn<LibraryItem, String> column = new TableColumn<>(header);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    // A number-valued column (available/total copies, price).
    public static TableColumn<LibraryItem, Number> number(String header, String property) {
        TableColumn<LibraryItem, Number> column = new TableColumn<>(header);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }
}
