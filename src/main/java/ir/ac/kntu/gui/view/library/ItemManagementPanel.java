package ir.ac.kntu.gui.view.library;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import ir.ac.kntu.gui.component.PagedTable;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.gui.util.LibraryColumns;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.library.AudioBook;
import ir.ac.kntu.library.Book;
import ir.ac.kntu.library.EBook;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.library.Magazine;
import ir.ac.kntu.util.Validator;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// Item management for Admin / CallCenter: add, edit price, adjust quantity and
// delete library items. Every mutation runs on a background thread and refreshes
// the table; validation problems surface as Dialogs alerts.
public class ItemManagementPanel extends VBox {

    private final TableView<LibraryItem> table = new TableView<>();
    private final PagedTable<LibraryItem> pagedTable = new PagedTable<>(table);
    private final ProgressIndicator loadProgress = new ProgressIndicator();
    private final StackPane tableStack = new StackPane(pagedTable, loadProgress);

    private final ComboBox<String> typeBox = new ComboBox<>(
            FXCollections.observableArrayList("BOOK", "MAGAZINE", "EBOOK", "AUDIOBOOK"));
    private final TextField idField = new TextField();
    private final TextField titleField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextField yearField = new TextField();
    private final TextField copiesField = new TextField();
    private final TextField priceField = new TextField();
    private final TextField extraField = new TextField();

    private static final String TITLE_LABEL = "Title";
    private static final String GHOST_STYLE = "ghost";
    private static final String STR_MAGAZINE = "MAGAZINE";
    private static final String STR_EBOOK = "EBOOK";
    private static final String STR_AUDIOBOOK = "AUDIOBOOK";

    // GUI-local, relaxed item-id rule: a plain ITEM-<number> (e.g. ITEM-12). The
    // stricter backend Validator.isValidItemId (typed prefix + 8 digits) is left
    // untouched — it is shared with the CLI and its own tests — so this only
    // loosens what the add-item form accepts.
    private static final Pattern GUI_ITEM_ID = Pattern.compile("^ITEM-\\d+$");

    public ItemManagementPanel() {
        super(16);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("Item Management");
        heading.getStyleClass().add("h1");

        buildTable();
        loadProgress.setMaxSize(48, 48);
        loadProgress.setVisible(false);
        getChildren().addAll(heading, buildAddForm(), buildRowActions(), tableStack);
        VBox.setVgrow(tableStack, Priority.ALWAYS);
        refresh();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        TableColumn<LibraryItem, String> id = LibraryColumns.text("ID", "itemId");
        TableColumn<LibraryItem, String> title = LibraryColumns.text(TITLE_LABEL, "title");
        title.setPrefWidth(220);
        TableColumn<LibraryItem, String> type = LibraryColumns.text("Type", "itemType");
        TableColumn<LibraryItem, Number> avail = LibraryColumns.number("Available", "availableCopies");
        TableColumn<LibraryItem, Number> total = LibraryColumns.number("Total", "totalCopies");
        TableColumn<LibraryItem, Number> price = LibraryColumns.number("Price", "unitPrice");

        table.getColumns().addAll(id, title, type, avail, total, price);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No items."));
    }

    private FlowPane buildAddForm() {
        typeBox.getSelectionModel().selectFirst();
        idField.setPromptText("ID (e.g. ITEM-12)");
        titleField.setPromptText(TITLE_LABEL);
        categoryField.setPromptText("Category");
        yearField.setPromptText("Year");
        copiesField.setPromptText("Total copies");
        priceField.setPromptText("Unit price");
        extraField.setPromptText("ISBN");
        List.of(idField, titleField, categoryField, yearField, copiesField, priceField, extraField)
                .forEach(f -> f.getStyleClass().add("field"));

        typeBox.valueProperty().addListener((obs, old, val) -> {
            String prompt;
            switch (val) {
                case STR_MAGAZINE:
                    prompt = "ISSN";
                    break;
                case STR_EBOOK:
                    prompt = "Download URL";
                    break;
                case STR_AUDIOBOOK:
                    prompt = "Download URL";
                    break;
                default:
                    prompt = "ISBN";
                    break;
            }
            extraField.setPromptText(prompt);
        });

        Button add = new Button("Add item");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> handleAdd());

        FlowPane form = new FlowPane(10, 10, typeBox, idField, titleField, categoryField,
                yearField, copiesField, priceField, extraField, add);
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
            String type = typeBox.getValue();
            String id = required(idField, "ID");
            if (!GUI_ITEM_ID.matcher(id).matches()) {
                Dialogs.warn("Invalid ID", "Item ID must look like ITEM-<number>, e.g. ITEM-12.");
                return;
            }
            String title = required(titleField, "Title");
            String category = categoryField.getText() == null ? "" : categoryField.getText().trim();
            int year = parseInt(yearField, "Year");
            if (!Validator.isValidPublishYear(year)) {
                Dialogs.warn("Invalid year", "Year must be between 1450 and current year.");
                return;
            }
            int copies = parseInt(copiesField, "Total copies");
            int price = parseInt(priceField, "Unit price");
            String extra = extraField.getText() == null ? "" : extraField.getText().trim();

            LibraryItem item = buildItem(new ItemParams(type, id, title, category, year));
            item.setTotalCopies(copies);
            item.setAvailableCopies(copies);
            item.setUnitPrice(price);

            if (!applyTypeSpecifics(item, extra)) {
                return;
            }

            runMutation(() -> LibraryService.addItem(item), "Item added.");
            clearForm();
        } catch (IllegalArgumentException ex) {
            Dialogs.warn("Invalid input", ex.getMessage());
        }
    }

    private boolean applyTypeSpecifics(LibraryItem item, String extra) {
        if (item instanceof Book book) {
            return applyBookSpecifics(book, extra);
        }
        if (item instanceof Magazine mag) {
            return applyMagazineSpecifics(mag, extra);
        }
        if (item instanceof EBook ebook && !extra.isEmpty()) {
            if (!Validator.isValidDownloadUrl(extra)) {
                Dialogs.warn("Invalid URL", "Download URL must start with https://.");
                return false;
            }
            ebook.setDownloadUrl(extra);
        }
        if (item instanceof AudioBook audio && !extra.isEmpty()) {
            if (!Validator.isValidDownloadUrl(extra)) {
                Dialogs.warn("Invalid URL", "Download URL must start with https://.");
                return false;
            }
            audio.setDownloadUrl(extra);
        }
        return true;
    }

    private boolean applyBookSpecifics(Book book, String extra) {
        if (!extra.isEmpty() && !Validator.isValidISBN13(extra)) {
            Dialogs.warn("Invalid ISBN", "ISBN-13 must be 13 digits starting with 978 or 979.");
            return false;
        }
        book.setIsbn(extra);
        return true;
    }

    private boolean applyMagazineSpecifics(Magazine mag, String extra) {
        if (!extra.isEmpty() && !Validator.isValidISSN(extra)) {
            Dialogs.warn("Invalid ISSN", "ISSN must be in format xxxx-xxxx.");
            return false;
        }
        mag.setIssueNumber(0);
        return true;
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
        Integer value = promptInt("How many additional copies for " + selected.getTitle() + "? (use negative to remove)");
        if (value != null) {
            String itemId = selected.getItemId();
            int delta = value;
            runMutation(() -> {
                LibraryService.updateItemQuantityFromCallCenter(itemId, delta);
                // Adding copies frees stock, so advance the reservation queue
                // once per new copy — the same trigger the CLI admin console
                // runs after a restock. Without this, waiting members are
                // never promoted when an admin/call-center adds copies.
                if (delta > 0) {
                    ReservationService.fulfillFromQueue(
                            itemId, delta, SimulationClock.getCurrentDay());
                }
            }, "Quantity updated.");
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
        loadProgress.setVisible(true);
        BackgroundJobs.run(
                () -> LibraryService.getAllItems().stream()
                        .sorted(Comparator.comparing(LibraryItem::getItemId,
                                String.CASE_INSENSITIVE_ORDER))
                        .collect(java.util.stream.Collectors.toList()),
                items -> {
                    loadProgress.setVisible(false);
                    pagedTable.setItems(items != null ? items : new ArrayList<>());
                },
                error -> {
                    loadProgress.setVisible(false);
                    Dialogs.error("Could not load items", error);
                });
    }

    private record ItemParams(String type, String id, String title, String category, int year) {}

    private LibraryItem buildItem(ItemParams params) {
        switch (params.type()) {
            case STR_MAGAZINE: return new Magazine(params.id(), params.title(), params.category(), params.year());
            case STR_EBOOK: return new EBook(params.id(), params.title(), params.category(), params.year());
            case STR_AUDIOBOOK: return new AudioBook(params.id(), params.title(), params.category(), params.year());
            default: return new Book(params.id(), params.title(), params.category(), params.year());
        }
    }

    private LibraryItem selected() {
        LibraryItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            Dialogs.warn("No selection", "Please select an item in the table first.");
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
        List.of(idField, titleField, categoryField, yearField, copiesField, priceField, extraField)
                .forEach(TextField::clear);
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
