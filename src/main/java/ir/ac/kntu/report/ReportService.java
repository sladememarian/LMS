package ir.ac.kntu.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ir.ac.kntu.finance.Loan;
import ir.ac.kntu.finance.LoanService;
import ir.ac.kntu.finance.SimulationClock;
import ir.ac.kntu.library.LibraryItem;
import ir.ac.kntu.library.LibraryService;
import ir.ac.kntu.library.SupplierCompany;
import ir.ac.kntu.mail.MailService;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.UserRole;
import ir.ac.kntu.util.SystemSettingsService;

public class ReportService {
    // generating HTML reports that nobody will read
    private static final String[] PALETTE = {
        "#2563eb", "#16a34a", "#f59e0b", "#db2777", "#7c3aed", "#0891b2"
    };
    private static final double DONUT_RADIUS = 80.0;
    private static final double DONUT_CIRCUMFERENCE = 2 * Math.PI * DONUT_RADIUS;
    private static final int BAR_WIDTH = 520;
    private static final String CARD_FMT =
        "<div class='card'><span class='card-label'>%s</span><span class='card-value'>%s</span></div>\n";
    private static final String BAR_FMT =
        "<div class='bar-row'><span class='bar-name'>%s</span>"
        + "<div class='bar-track'><div class='bar-fill' style='width:%dpx;background:%s'></div></div>"
        + "<span class='bar-val'>%s</span></div>\n";
    private static final String DONUT_SEG_FMT =
        "<circle cx='100' cy='100' r='80' fill='none' stroke='%s' stroke-width='34' "
        + "stroke-dasharray='%.2f %.2f' stroke-dashoffset='%.2f' transform='rotate(-90 100 100)'></circle>\n";
    private static final String LEGEND_FMT =
        "<div class='legend-item'><span class='dot' style='background:%s'></span>%s &mdash; %s cr (%.1f%%)</div>\n";
    private static final String ROW_FMT =
        "<tr><td>%s</td><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%s cr</td><td>%s cr</td></tr>\n";

    public static boolean isAuthorized() {
        Persona user = Persona.getCurrentUser();
        if (user == null) {
            return false;
        }
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.CALLCENTER;
    }

    public static List<SupplierFinancials> computeSupplierFinancials() {
        List<SupplierFinancials> rows = new ArrayList<>();
        for (SupplierCompany supplier : LibraryService.getAllSuppliers()) {
            SupplierFinancials row = new SupplierFinancials(supplier.getCompanyId(), supplier.getCompanyName());
            for (LibraryItem item : LibraryService.getAllItems()) {
                if (supplier.getCompanyId().equals(item.getSupplierId())) {
                    row.accumulate(item);
                }
            }
            rows.add(row);
        }
        return rows;
    }

    public static List<OverdueLoanReport> computeOverdueLoans() {
        int currentDay = SimulationClock.getCurrentDay();
        int fineRate = SystemSettingsService.getFineRate();
        List<OverdueLoanReport> rows = new ArrayList<>();
        for (Loan loan : LoanService.getOverdueLoans(currentDay)) {
            int daysOverdue = loan.daysOverdue(currentDay);
            rows.add(new OverdueLoanReport(loan.getMemberId(), loan.getItemId(),
                    loan.getDueDay(), daysOverdue, daysOverdue * fineRate));
        }
        return rows;
    }

    public static String exportReport(String path) {
        if (!isAuthorized()) {
            throw new IllegalStateException("Only ADMIN or CALLCENTER roles may generate financial reports");
        }
        String html = buildHtml(computeSupplierFinancials());
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(html.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write report: " + ex.getMessage(), ex);
        }
        return new File(path).getAbsolutePath();
    }

    public static String buildHtml(List<SupplierFinancials> data) {
        StringBuilder builder = new StringBuilder();
        builder.append(head());
        builder.append(header());
        builder.append(summarySection(data));
        builder.append(barSection(data));
        builder.append(donutSection(data));
        builder.append(tableSection(data));
        builder.append("</main></body></html>");
        return builder.toString();
    }

    private static String money(long value) {
        return String.format("%,d", value);
    }

    private static String header() {
        String generated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return "<header><h1>Supplier Financial Status Report</h1>"
            + "<p class='sub'>" + MailService.getSystemName()
            + " &middot; Library Management System &middot; Generated " + generated + "</p></header>\n<main>\n";
    }

    private static String summarySection(List<SupplierFinancials> data) {
        int companies = data.size();
        int items = 0;
        int copies = 0;
        int borrowed = 0;
        long inventory = 0;
        long circulation = 0;
        for (SupplierFinancials row : data) {
            items += row.getItemCount();
            copies += row.getTotalCopies();
            borrowed += row.getBorrowedCopies();
            inventory += row.getInventoryValue();
            circulation += row.getCirculationValue();
        }
        StringBuilder builder = new StringBuilder("<section class='cards'>\n");
        builder.append(String.format(CARD_FMT, "Companies", String.valueOf(companies)));
        builder.append(String.format(CARD_FMT, "Catalogued Items", String.valueOf(items)));
        builder.append(String.format(CARD_FMT, "Total Copies", String.valueOf(copies)));
        builder.append(String.format(CARD_FMT, "Copies With Users", String.valueOf(borrowed)));
        builder.append(String.format(CARD_FMT, "Inventory Value", money(inventory) + " cr"));
        builder.append(String.format(CARD_FMT, "Value In Circulation", money(circulation) + " cr"));
        builder.append("</section>\n");
        return builder.toString();
    }

    private static long maxInventory(List<SupplierFinancials> data) {
        long max = 1;
        for (SupplierFinancials row : data) {
            if (row.getInventoryValue() > max) {
                max = row.getInventoryValue();
            }
        }
        return max;
    }

    private static String barSection(List<SupplierFinancials> data) {
        long max = maxInventory(data);
        StringBuilder builder = new StringBuilder("<section class='panel'><h2>Inventory Value by Company</h2>\n");
        for (int i = 0; i < data.size(); i++) {
            SupplierFinancials row = data.get(i);
            int width = (int) (row.getInventoryValue() * BAR_WIDTH / max);
            String color = PALETTE[i % PALETTE.length];
            builder.append(String.format(BAR_FMT, row.getCompanyName(), width, color, money(row.getInventoryValue())));
        }
        builder.append("</section>\n");
        return builder.toString();
    }

    private static long totalCirculation(List<SupplierFinancials> data) {
        long total = 0;
        for (SupplierFinancials row : data) {
            total += row.getCirculationValue();
        }
        return total > 0 ? total : 1;
    }

    private static String donutSection(List<SupplierFinancials> data) {
        long total = totalCirculation(data);
        StringBuilder svg = new StringBuilder("<section class='panel'><h2>Value In Circulation Share</h2>"
            + "<div class='donut-wrap'><svg viewBox='0 0 200 200' class='donut'>\n");
        StringBuilder legend = new StringBuilder("<div class='legend'>\n");
        double offset = 0.0;
        for (int i = 0; i < data.size(); i++) {
            SupplierFinancials row = data.get(i);
            double fraction = (double) row.getCirculationValue() / total;
            double segLen = fraction * DONUT_CIRCUMFERENCE;
            String color = PALETTE[i % PALETTE.length];
            svg.append(String.format(DONUT_SEG_FMT, color, segLen, DONUT_CIRCUMFERENCE, -offset));
            legend.append(String.format(LEGEND_FMT, color, row.getCompanyName(),
                    money(row.getCirculationValue()), fraction * 100));
            offset += segLen;
        }
        svg.append("<circle cx='100' cy='100' r='55' fill='var(--bg)'></circle></svg>");
        legend.append("</div>");
        return svg.toString() + legend + "</div></section>\n";
    }

    private static String tableSection(List<SupplierFinancials> data) {
        StringBuilder builder = new StringBuilder("<section class='panel'><h2>Per-Company Breakdown</h2>"
            + "<table><thead><tr><th>Company</th><th>ID</th><th>Items</th><th>Total Copies</th>"
            + "<th>With Users</th><th>Inventory Value</th><th>In Circulation</th></tr></thead><tbody>\n");
        for (SupplierFinancials row : data) {
            builder.append(String.format(ROW_FMT, row.getCompanyName(), row.getCompanyId(),
                    row.getItemCount(), row.getTotalCopies(), row.getBorrowedCopies(),
                    money(row.getInventoryValue()), money(row.getCirculationValue())));
        }
        builder.append("</tbody></table></section>\n");
        return builder.toString();
    }

    private static String head() {
        return "<!DOCTYPE html>\n<html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
            + "<title>Supplier Financial Status</title><style>" + css() + "</style></head><body>\n";
    }

    private static String css() {
        return ":root{--bg:#0b1120;--card:#111c33;--line:#1e2a44;--text:#e5edff;--muted:#94a8cc;--accent:#2563eb}"
            + "*{box-sizing:border-box;margin:0;padding:0}"
            + "body{font-family:'Segoe UI',Arial,sans-serif;background:var(--bg);color:var(--text);padding:24px}"
            + "header h1{font-size:24px}.sub{color:var(--muted);margin-top:4px;font-size:13px}"
            + "main{max-width:980px;margin:18px auto}"
            + ".cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px;margin-bottom:22px}"
            + ".card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:16px;"
            + "display:flex;flex-direction:column;gap:6px}"
            + ".card-label{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.04em}"
            + ".card-value{font-size:20px;font-weight:700}"
            + ".panel{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:20px;margin-bottom:22px}"
            + ".panel h2{font-size:16px;margin-bottom:16px}"
            + ".bar-row{display:flex;align-items:center;gap:12px;margin:10px 0}"
            + ".bar-name{width:150px;font-size:13px;color:var(--muted)}"
            + ".bar-track{flex:1;background:#0b1120;border-radius:8px;height:20px;overflow:hidden}"
            + ".bar-fill{height:100%;border-radius:8px}"
            + ".bar-val{width:90px;text-align:right;font-variant-numeric:tabular-nums;font-size:13px}"
            + ".donut-wrap{display:flex;flex-wrap:wrap;align-items:center;gap:28px}"
            + ".donut{width:200px;height:200px}"
            + ".legend{display:flex;flex-direction:column;gap:8px;font-size:13px}"
            + ".legend-item{display:flex;align-items:center;gap:8px;color:var(--muted)}"
            + ".dot{width:12px;height:12px;border-radius:3px;display:inline-block}"
            + "table{width:100%;border-collapse:collapse;font-size:13px}"
            + "th,td{padding:10px 12px;text-align:left;border-bottom:1px solid var(--line)}"
            + "th{color:var(--muted);text-transform:uppercase;font-size:11px;letter-spacing:.04em}"
            + "tbody tr:hover{background:#0d1730}"
            + "@media(max-width:560px){.bar-name{width:90px}}";
    }
}