# Report & Analytics Tables (Admin / Callcenter)

Phase 3 surfaces the backend's financial and operational data through several
read-only tables and charts. Every one of these is a **viewer** over an existing
backend service — the GUI computes nothing the CLI didn't already expose. This
document lists each report table, what it shows, and its backend source.

## Fines — Indebted Users (`FinesPanel`)

Screen: **Admin / Callcenter → Fines**

A table of every user with outstanding debt, sorted by debt descending.

| Column            | Source                                            |
|-------------------|---------------------------------------------------|
| Email             | `PersonaRepository.getAllPersonas()`              |
| Member ID         | `Persona.getMemberId()`                           |
| Outstanding debt  | `FinanceService.getOutstandingDebt(memberId)`     |

Only users with `debt > 0` appear (Streams filter). The row list is built with
the Java Streams API on a background thread.

### Drill-downs (buttons above the table)

- **View transactions** — for the selected debtor, lists their transaction
  history via `FinanceService.getTransactionsForMember(memberId)` (type, amount,
  description).
- **Overdue loans** — lists all currently overdue loans via
  `LoanService.getOverdueLoans(SimulationClock.getCurrentDay())`. This delegates
  to the backend's canonical overdue check rather than re-implementing the
  day-comparison in the GUI.
- **Tax revenue** — total tax collected via
  `FinanceService.getTaxRevenueCollected()`.

## Analytics (`AnalyticsPanel`)

Screen: **Admin → Analytics**

Two Streams-derived charts plus an HTML report generator.

### Top 10 borrowed items (BarChart)

Groups loan history by item and shows the ten most-borrowed titles.

- Source: `LoanService.getLoans()` grouped by `Loan::getItemId` (counting collector)
- Item titles resolved via `LibraryService.getItemById(id)`

### Monthly fine revenue (LineChart)

Fine revenue over time, one point per month.

- Source: `FinanceService.getAllTransactions()` filtered to `type == "TAX"`,
  grouped by month (sorted `TreeMap`), summed per month.

### Generate HTML report

A button runs `ReportService.exportReport(path)` on a background thread and
writes the report to **`<project>/reports/lms_financial_report.html`** (the
project working directory, not the user's home folder, so the report ships with
the project). The backend `ReportService` is unchanged — only the caller picks
the output path.

## Wallet — Transaction History (`WalletPanel`)

Screen: **any signed-in user → Wallet**

The user's own transaction ledger.

| Column      | Source                                          |
|-------------|-------------------------------------------------|
| Date        | `Transaction.getSentDate()` / timestamp         |
| Type        | `Transaction.getType()`                         |
| Amount      | `Transaction.getAmount()`                       |
| Description | `Transaction.getDescription()`                  |

Plus stat cards for **Balance** and **Outstanding debt**, backed by
`FinanceService`.

## Support Inbox (`SupportInboxPanel`)

Screen: **Callcenter → Support Inbox**

A table of support tickets the agent can act on.

| Column   | Source                                    |
|----------|-------------------------------------------|
| Ticket   | `SupportTicket.getId()`                    |
| User     | `SupportTicket.getUserEmail()`            |
| Section  | `SupportTicket.getSection()`              |
| Title    | `SupportTicket.getTitle()`                |
| Priority | `SupportTicket.getPriority()`             |
| Status   | `SupportTicket.getStatus()`               |

Actions (Reply, Close ticket, Refresh) call the existing `SupportService`.

## Backend-unchanged guarantee

None of these tables introduce backend logic. Aggregations that are purely
presentational (grouping loans for a chart, filtering debtors for display) live
in the GUI because the backend has no equivalent method — but every number they
show comes from an existing backend service call. The two places where the GUI
originally *duplicated* backend logic (extension tax math, overdue-loan
filtering) were removed and now delegate to
`FinanceService.proccessExtentionPayment(...)` and
`LoanService.getOverdueLoans(...)` respectively. See `docs/gui.md` for the full
backend-unchanged proof.
