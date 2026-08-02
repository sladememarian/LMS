package ir.ac.kntu.gui.view.dashboard;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ir.ac.kntu.finance.FinanceService;
import ir.ac.kntu.finance.Loan;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.Transaction;
import ir.ac.kntu.gui.component.StatCard;
import ir.ac.kntu.gui.concurrency.BackgroundJobs;
import ir.ac.kntu.gui.util.ChartFactory;
import ir.ac.kntu.gui.util.Dialogs;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.reservation.ReservationService;
import ir.ac.kntu.support.SupportService;
import ir.ac.kntu.support.SupportTicket;
import ir.ac.kntu.support.rolerequest.RoleRequest;
import ir.ac.kntu.support.rolerequest.RoleRequestService;
import ir.ac.kntu.util.PersonaRepository;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Role-aware dashboard. Every statistic is computed with the Java Streams API,
 * and the whole computation runs on a background {@link javafx.concurrent.Task}
 * (via {@link BackgroundJobs}) so the UI never blocks. A {@link ProgressIndicator}
 * is shown while stats load.
 */
public class DashboardPanel extends StackPane {

    private final Persona persona;

    public DashboardPanel(Persona persona) {
        this.persona = persona;
        getStyleClass().add("content-area");
        setPadding(new Insets(24));
        loadAsync();
    }

    private void loadAsync() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        getChildren().setAll(spinner);

        BackgroundJobs.run(
                this::computeContent,
                node -> getChildren().setAll(node),
                error -> {
                    getChildren().setAll(new Label("Could not load dashboard."));
                    Dialogs.error("Dashboard error", error);
                });
    }

    /** Runs OFF the FX thread. Only builds lightweight data + nodes here. */
    private Node computeContent() {
        UserRole role = persona.getRole();
        if (role == UserRole.ADMIN) {
            return buildAdminDashboard();
        } else if (role == UserRole.CALLCENTER) {
            return buildSupportDashboard();
        }
        return buildUserDashboard();
    }

    // ---------------------------------------------------------------- User ---

    private Node buildUserDashboard() {
        String memberId = persona.getMemberId();
        int borrowed = persona.getBorrowCount();
        int reservations = ReservationService.getActiveReservationCount(memberId);
        int debt = FinanceService.getOutstandingDebt(memberId);

        List<Transaction> txns = FinanceService.getTransactionsForMember(memberId);
        long recentCount = txns.stream().count();

        FlowPane cards = cardRow(
                new StatCard("Borrowed items", String.valueOf(borrowed), "currently on loan"),
                new StatCard("Active reservations", String.valueOf(reservations), "in queue / ready"),
                new StatCard("Outstanding debt", debt + " ", "unpaid fines"),
                new StatCard("Transactions", String.valueOf(recentCount), "lifetime"));

        return page("My Dashboard", cards);
    }

    // ------------------------------------------------------------- Support ---

    private Node buildSupportDashboard() {
        List<SupportTicket> tickets = SupportService.getAllTickets();

        long open = tickets.stream()
                .filter(t -> !isResolved(t.getStatus()))
                .count();
        long resolved = tickets.stream()
                .filter(t -> isResolved(t.getStatus()))
                .count();

        // Ticket breakdown by status (Streams grouping) — stands in for the
        // weekly trend where per-ticket dates are unavailable.
        Map<String, Long> byStatus = tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> normalise(t.getStatus()),
                        Collectors.counting()));

        FlowPane cards = cardRow(
                new StatCard("Open tickets", String.valueOf(open), "awaiting action"),
                new StatCard("Resolved", String.valueOf(resolved), "closed / resolved"),
                new StatCard("Total tickets", String.valueOf(tickets.size()), "all time"));

        BarChart<String, Number> chart = barChart("Tickets by status", "Status", "Count");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        byStatus.forEach((status, count) ->
                series.getData().add(new XYChart.Data<>(status, count)));
        chart.getData().add(series);

        return page("Support Dashboard", cards, chart);
    }

    // --------------------------------------------------------------- Admin ---

    private Node buildAdminDashboard() {
        long users = PersonaRepository.getAllPersonas().stream().count();
        long items = LibraryService.getAllItems().stream().count();
        int taxRevenue = FinanceService.getTaxRevenueCollected();
        int outstanding = FinanceService.getTotalOutstandingDebt();

        FlowPane cards = cardRow(
                new StatCard("Total users", String.valueOf(users), "registered"),
                new StatCard("Library items", String.valueOf(items), "in catalogue"),
                new StatCard("Fine revenue", taxRevenue + " ", "collected"),
                new StatCard("Outstanding debt", outstanding + " ", "unpaid across users"));

        // Top-10 most-borrowed items, extracted from loan history via Streams.
        Map<String, Long> borrowCounts = LoanService.getLoans().stream()
                .collect(Collectors.groupingBy(Loan::getItemId, Collectors.counting()));

        List<Map.Entry<String, Long>> top10 = borrowCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        BarChart<String, Number> chart = barChart("Top borrowed items", "Item", "Borrows");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        top10.forEach(entry -> {
            LibraryItem item = LibraryService.getItemById(entry.getKey());
            String label = item != null ? item.getTitle() : entry.getKey();
            series.getData().add(new XYChart.Data<>(ChartFactory.truncateLabel(label), entry.getValue()));
        });
        chart.getData().add(series);

        VBox requestsBox = buildRequestsBox();

        return page("Admin Dashboard", cards, requestsBox, chart);
    }

    private VBox buildRequestsBox() {
        VBox requestsBox = new VBox(8);
        requestsBox.setPadding(new Insets(12, 0, 0, 0));
        Label requestsHeading = new Label("Pending Role Requests");
        requestsHeading.getStyleClass().add("h3");
        List<RoleRequest> pending = RoleRequestService.getPending();
        if (pending.isEmpty()) {
            requestsBox.getChildren().add(new Label("No pending requests."));
        } else {
            pending.stream()
                    .map(DashboardPanel::roleRequestLabel)
                    .forEach(requestsBox.getChildren()::add);
        }
        return requestsBox;
    }

    /** Renders one pending role-request as a label, appending its optional note. */
    private static Label roleRequestLabel(RoleRequest req) {
        String text = req.getRequestId() + " | "
                + req.getRequesterEmail() + " -> " + req.getRequestedRole();
        if (req.getMessage() != null && !req.getMessage().isBlank()) {
            text = text + " : " + req.getMessage();
        }
        return new Label(text);
    }

    // -------------------------------------------------------------- Helpers --

    private FlowPane cardRow(Node... cards) {
        FlowPane pane = new FlowPane(16, 16, cards);
        pane.setPadding(new Insets(4, 0, 12, 0));
        return pane;
    }

    private BarChart<String, Number> barChart(String title, String xLabel, String yLabel) {
        return ChartFactory.barChart(title, xLabel, yLabel, 340);
    }

    private Node page(String title, Node... content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("h1");
        VBox box = new VBox(16, heading);
        box.getChildren().addAll(content);
        return box;
    }

    private static boolean isResolved(String status) {
        String normalized = normalise(status);
        return normalized.equals("resolved") || normalized.equals("closed") || normalized.equals("done");
    }

    private static String normalise(String status) {
        return status == null ? "unknown" : status.trim().toLowerCase();
    }
}
