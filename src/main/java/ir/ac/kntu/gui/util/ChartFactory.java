package ir.ac.kntu.gui.util;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;

// Shared builders for the dashboard/analytics charts, so the axis setup and
// label truncation live in one place. Presentation only — data comes from the
// existing backend services.
public final class ChartFactory {

    // Labels longer than this get shortened with an ellipsis.
    private static final int MAX_LABEL = 18;

    private ChartFactory() {
        // utility class
    }

    // Builds a legend-less bar chart with a rotated category axis, matching the
    // dashboards. Pass long labels through truncateLabel first.
    public static BarChart<String, Number> barChart(String title, String xLabel, String yLabel,
            double prefHeight) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xLabel);
        // Rotate ticks so long category labels stay inside the plot area instead
        // of overflowing and breaking the chart layout.
        xAxis.setTickLabelRotation(30);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setPrefHeight(prefHeight);
        return chart;
    }

    // Shortens long titles so chart labels don't overflow the layout.
    public static String truncateLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.length() <= MAX_LABEL ? label : label.substring(0, MAX_LABEL - 1) + "…";
    }
}
