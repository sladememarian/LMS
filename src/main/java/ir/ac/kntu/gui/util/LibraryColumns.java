package ir.ac.kntu.gui.util;

import ir.ac.kntu.library.LibraryItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Reusable {@link TableColumn} builders for the {@link LibraryItem} catalogue
 * so the two library tables share one column definition. Pure presentation —
 * the properties read the same backend getters the hand-built columns did.
 */
public final class LibraryColumns {

    private LibraryColumns() {
    }

    /** {@code column("ID", "itemId")} builds a property-mapped column. */
    public static TableColumn<LibraryItem, String> text(String header, String property) {
        TableColumn<LibraryItem, String> column = new TableColumn<>(header);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    /** Number-valued property column (available/total copies, price). */
    public static TableColumn<LibraryItem, Number> number(String header, String property) {
        TableColumn<LibraryItem, Number> column = new TableColumn<>(header);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }
}
