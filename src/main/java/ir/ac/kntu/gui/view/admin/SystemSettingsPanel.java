package ir.ac.kntu.gui.view.admin;

import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.util.SystemSettingsService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

// Admin System Settings: edit core parameters (borrow days, fine rate,
// reservation days, max reservations). Loads and saves off the FX thread.
public class SystemSettingsPanel extends VBox {

    @SuppressWarnings("PMD.UnusedPrivateField")
    private final Persona actor;
    private final TextField borrowDays = new TextField();
    private final TextField fineRate = new TextField();
    private final TextField reservationDays = new TextField();
    private final TextField maxReservations = new TextField();

    public SystemSettingsPanel(Persona actor) {
        super(16);
        this.actor = actor;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        Label heading = new Label("System Settings");
        heading.getStyleClass().add("h1");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.getStyleClass().add("card");
        grid.setPadding(new Insets(18));

        addRow(grid, 0, new RowConfig("Borrow days", borrowDays,
                v -> SystemSettingsService.updateBorrowDays(actor, v)));
        addRow(grid, 1, new RowConfig("Fine rate", fineRate,
                v -> SystemSettingsService.updateFineRate(actor, v)));
        addRow(grid, 2, new RowConfig("Reservation days", reservationDays,
                v -> SystemSettingsService.updateReservationDays(actor, v)));
        addRow(grid, 3, new RowConfig("Max reservations", maxReservations,
                v -> SystemSettingsService.updateMaxReservations(actor, v)));

        borrowDays.setId("borrowDays");
        fineRate.setId("fineRate");
        reservationDays.setId("reservationDays");
        maxReservations.setId("maxReservations");

        getChildren().addAll(heading, grid);
        load();
    }

    private interface IntSetter {
        void apply(int value);
    }

    private record RowConfig(String label, TextField field, IntSetter setter) {}

    private void addRow(GridPane grid, int row, RowConfig config) {
        config.field().getStyleClass().add("field");
        config.field().setPrefWidth(120);
        Button save = new Button("Save");
        save.getStyleClass().add("ghost");
        save.setOnAction(event -> {
            int value;
            try {
                value = Integer.parseInt(config.field().getText() == null ? "" : config.field().getText().trim());
            } catch (NumberFormatException ex) {
                Dialogs.warn("Invalid value", config.label() + " must be a whole number.");
                return;
            }
            BackgroundJobs.runAction(
                    () -> config.setter().apply(value),
                    () -> {
                        Dialogs.info("Saved", config.label() + " updated.");
                        load();
                    },
                    error -> Dialogs.error("Save failed", error));
        });
        grid.add(new Label(config.label()), 0, row);
        grid.add(config.field(), 1, row);
        grid.add(save, 2, row);
    }

    private void load() {
        BackgroundJobs.run(
                () -> new int[]{
                        SystemSettingsService.getBorrowDays(),
                        SystemSettingsService.getFineRate(),
                        SystemSettingsService.getReservationDays(),
                        SystemSettingsService.getMaxReservations()
                },
                values -> {
                    borrowDays.setText(String.valueOf(values[0]));
                    fineRate.setText(String.valueOf(values[1]));
                    reservationDays.setText(String.valueOf(values[2]));
                    maxReservations.setText(String.valueOf(values[3]));
                },
                error -> Dialogs.error("Could not load settings", error));
    }
}
