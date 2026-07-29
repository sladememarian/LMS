package ir.ac.kntu.gui.view.library;

import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.library.AudioBook;
import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.EBook;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.library.Magazine;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Item management for Admin / CallCenter: add, edit price, adjust quantity and
 * delete library items. Every mutation runs on a background thread and refreshes
 * the table; validation problems surface as {@link Dialogs} alerts.
 */
public class ItemManagementPanel extends VBox {

    private final TableView<LibraryItem> table = new TableView<>();

    private static final String TITLE_LABEL = "Title";

    private final ComboBox<String> typeBox = new ComboBox<>(
            FXCollections.observableArrayList("BOOK", "MAGAZINE", "EBOOK", "AUDIOBOOK"));
    private final TextField idField = new TextField();
    private final TextField titleField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextField yearField = new TextField();
    private final TextField copiesField = new TextField();
    private final TextField priceField = new TextField();

    private static final String GHOST_STYLE = "ghost";
    private static final String NO_SELECTION = "No selection";

    public ItemManagementPanel() {
        super(16);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Item Management");
        heading.getStyleClass().add("h1");

        buildTable();
        getChildren().addAll(heading, buildAddForm(), buildRowActions(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<LibraryItem, String> id = new TableColumn<>("ID");
        id.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        TableColumn<LibraryItem, String> title = new TableColumn<>(TITLE_LABEL);
        title.setCellValueFactory(new PropertyValueFactory<>("title"));
        title.setPrefWidth(220);
        TableColumn<LibraryItem, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        TableColumn<LibraryItem, Number> avail = new TableColumn<>("Available");
        avail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        TableColumn<LibraryItem, Number> total = new TableColumn<>("Total");
        total.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        TableColumn<LibraryItem, Number> price = new TableColumn<>("Price");
        price.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        table.getColumns().addAll(id, title, type, avail, total, price);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No items."));
    }

    private FlowPane buildAddForm() {
        typeBox.getSelectionModel().selectFirst();
        idField.setPromptText("ID");
        titleField.setPromptText(TITLE_LABEL);
        categoryField.setPromptText("Category");
        yearField.setPromptText("Year");
        copiesField.setPromptText("Total copies");
        priceField.setPromptText("Unit price");
        for (TextField f : List.of(idField, titleField, categoryField, yearField, copiesField, priceField)) {
            f.getStyleClass().add("field");
        }

        Button add = new Button("Add item");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> handleAdd());

        FlowPane form = new FlowPane(10, 10, typeBox, idField, titleField, categoryField,
                yearField, copiesField, priceField, add);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    private HBox buildRowActions() {
        Button editPrice = new Button("Edit price");
        editPrice.getStyleClass().add(GHOST_STYLE);
        editPrice.setOnAction(event -> handleEditPrice());

        Button editQty = new Button("Edit quantity");
        editQty.getStyleClass().add(GHOST_STYLE);
        editQty.setOnAction(event -> handleEditQuantity());

        Button delete = new Button("Delete");
        delete.getStyleClass().add(GHOST_STYLE);
        delete.setOnAction(event -> handleDelete());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add(GHOST_STYLE);
        refresh.setOnAction(event -> refresh());

        HBox box = new HBox(10, editPrice, editQty, delete, refresh);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void handleAdd() {
        try {
            String id = required(idField, "ID");
            String title = required(titleField, TITLE_LABEL);
            String category = categoryField.getText() == null ? "" : categoryField.getText().trim();
            int year = parseInt(yearField, "Year");
            int copies = parseInt(copiesField, "Total copies");
            int price = parseInt(priceField, "Unit price");

            LibraryItem item = buildItem(id, title, category, year);
            item.setTotalCopies(copies);
            item.setAvailableCopies(copies);
            item.setUnitPrice(price);

            runMutation(() -> LibraryService.addItem(item), "Item added.");
            clearForm();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn("Invalid input", ex.getMessage());
        }
    }

    private void handleEditPrice() {
        LibraryItem selected = selected();
        if (selected == null) {
            return;
        }
        Integer value = promptInt("New price for " + selected.getTitle());
        if (value != null) {
            runMutation(() -> LibraryService.updateItemPrice(selected.getItemId(), value),
                    "Price updated.");
        }
    }

    private void handleEditQuantity() {
        LibraryItem selected = selected();
        if (selected == null) {
            return;
        }
        Integer value = promptInt("New total copies for " + selected.getTitle());
        if (value != null) {
            runMutation(() ->
                    LibraryService.updateItemQuantityFromCallCenter(selected.getItemId(), value),
                    "Quantity updated.");
        }
    }

    private void handleDelete() {
        LibraryItem selected = selected();
        if (selected == null) {
            return;
        }
        if (Dialogs.confirm("Delete item", "Delete \"" + selected.getTitle() + "\"?")) {
            runMutation(() -> LibraryService.deleteItem(selected.getItemId()), "Item deleted.");
        }
    }

    private void runMutation(Runnable action, String successMessage) {
        BackgroundJobs.runAction(action,
                () -> {
                    Dialogs.info("Done", successMessage);
                    refresh();
                },
                error -> Dialogs.error("Operation failed", error));
    }

    private void refresh() {
        BackgroundJobs.run(LibraryService::getAllItems,
                items -> table.setItems(FXCollections.observableArrayList(
                        items != null ? items : new ArrayList<>())),
                error -> Dialogs.error("Could not load items", error));
    }

    private LibraryItem buildItem(String id, String title, String category, int year) {
        String type = typeBox.getValue();
        switch (type) {
            case "MAGAZINE": return new Magazine(id, title, category, year);
            case "EBOOK": return new EBook(id, title, category, year);
            case "AUDIOBOOK": return new AudioBook(id, title, category, year);
            default: return new Book(id, title, category, year);
        }
    }

    private LibraryItem selected() {
        LibraryItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            Dialogs.warn(NO_SELECTION, "Please select an item in the table first.");
        }
        return item;
    }

    private Integer promptInt(String header) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Enter value");
        dialog.setHeaderText(header);
        dialog.setContentText("Value:");
        return dialog.showAndWait().map(s -> {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ex) {
                Dialogs.warn("Invalid number", "Please enter a whole number.");
                return null;
            }
        }).orElse(null);
    }

    private void clearForm() {
        for (TextField f : List.of(idField, titleField, categoryField, yearField, copiesField, priceField)) {
            f.clear();
        }
    }

    private static String required(TextField field, String name) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }

    private static int parseInt(TextField field, String name) {
        try {
            return Integer.parseInt(field.getText() == null ? "" : field.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " must be a whole number.");
        }
    }
}
