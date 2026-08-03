package ir.ac.kntu.gui.view.admin;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.Loan;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.Transaction;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.ChartFactory;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// Admin analytics screen. Two Streams-derived charts: top-10 most-borrowed items
// (BarChart) from loan history, and monthly fine revenue (LineChart) from TAX
// transactions. Both datasets are computed on a background thread.
public class AnalyticsPanel extends StackPane {

    public AnalyticsPanel() {
        getStyleClass().add("content-area");
        setPadding(new javafx.geometry.Insets(24));
        loadAsync();
    }

    private void loadAsync() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        getChildren().setAll(spinner);

        BackgroundJobs.run(
                this::buildContent,
                node -> getChildren().setAll(node),
                error -> {
                    getChildren().setAll(new Label("Could not load analytics."));
                    Dialogs.error("Analytics error", error);
                });
    }

    private Node buildContent() {
        Label heading = new Label("Analytics");
        heading.getStyleClass().add("h1");
        return new VBox(16, heading, buildReportBar(), topBorrowedChart(), monthlyRevenueChart());
    }

    // Generates an HTML financial report on a background thread with progress.
    private Node buildReportBar() {
        javafx.scene.control.Button generate = new javafx.scene.control.Button("Generate HTML report");
        generate.getStyleClass().add("primary");

        generate.setOnAction(event -> {
            generate.setDisable(true);
            // Show a large spinner centered over the whole analytics area while
            // the report is generated, then remove it when done (item 3).
            ProgressIndicator overlay = new ProgressIndicator();
            overlay.setMaxSize(64, 64);
            getChildren().add(overlay);
            // Reports live in the project source folder (./reports), not the
            // user's home directory, so they ship with the project.
            java.io.File reportsDir = new java.io.File(
                    System.getProperty("user.dir"), "reports");
            reportsDir.mkdirs();
            String path = new java.io.File(reportsDir, "lms_financial_report.html")
                    .getAbsolutePath();
            BackgroundJobs.run(
                    () -> ir.ac.kntu.report.ReportService.exportReport(path),
                    savedPath -> {
                        generate.setDisable(false);
                        getChildren().remove(overlay);
                        Dialogs.info("Report generated", "Saved to:\n" + savedPath);
                    },
                    error -> {
                        generate.setDisable(false);
                        getChildren().remove(overlay);
                        Dialogs.error("Report failed", error);
                    });
        });

        javafx.scene.layout.HBox bar = new javafx.scene.layout.HBox(10, generate);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return bar;
    }

    private BarChart<String, Number> topBorrowedChart() {
        BarChart<String, Number> chart = ChartFactory.barChart(
                "Top 10 borrowed items", "Item", "Borrows", 320);

        Map<String, Long> counts = LoanService.getLoans().stream()
                .collect(Collectors.groupingBy(Loan::getItemId, Collectors.counting()));

        List<Map.Entry<String, Long>> top10 = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        top10.forEach(entry -> {
            LibraryItem item = LibraryService.getItemById(entry.getKey());
            String label = item != null ? item.getTitle() : entry.getKey();
            series.getData().add(new XYChart.Data<>(ChartFactory.truncateLabel(label), entry.getValue()));
        });
        chart.getData().add(series);
        return chart;
    }

    private LineChart<String, Number> monthlyRevenueChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Revenue");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Monthly fine revenue");
        chart.setLegendVisible(false);
        chart.setPrefHeight(320);

        // Fine revenue = TAX transactions, grouped by month (sorted) via Streams.
        Map<String, Integer> byMonth = FinanceService.getAllTransactions().stream()
                .filter(tx -> "TAX".equals(tx.getType()))
                .collect(Collectors.groupingBy(
                        AnalyticsPanel::monthKey,
                        TreeMap::new,
                        Collectors.summingInt(Transaction::getAmount)));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        byMonth.forEach((month, total) ->
                series.getData().add(new XYChart.Data<>(month, total)));
        chart.getData().add(series);
        return chart;
    }

    private static String monthKey(Transaction tx) {
        long ts = tx.getTimestamp();
        if (ts <= 0) {
            return "unknown";
        }
        return YearMonth.from(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())).toString();
    }
}
