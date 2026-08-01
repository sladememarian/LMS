# GUI Documentation: Library Management System

This document describes the JavaFX GUI (Phase 3) added on top of the Phase 1/2
CLI backend, how to operate it, how theming works, and proof that the GUI does
not change the previous-phase backend logic.

## Architecture Overview

The GUI lives entirely in the `ir.ac.kntu.gui` package. It follows a multi-scene
architecture where the `Navigator` owns the primary `Stage` and swaps `View`
implementations in and out.

### Key Components

- **`GuiLauncher`** — entry point for the GUI application.
- **`App`** — extends `javafx.application.Application`, builds the initial scene.
- **`Navigator`** — owns the main `Stage`; switches views with a 220ms fade
  transition and holds the current `UiTheme`.
- **`View`** — interface every screen implements (`Parent getRoot()`, `String title()`).
- **`shell/AppShell`** — the post-login shell: sidebar navigation + top bar
  (identity, theme toggle, Next-Day control, sign-out) + content area.
- **`concurrency/BackgroundJobs`** — shared daemon executor for running heavy
  work off the FX thread (see `docs/threads.md`).
- **`util/Dialogs`** — error/info/warning alerts and toasts, thread-safe.
- **`util/UiTheme`** — Light/Dark stylesheet handling.
- **`component/PasswordBox`** — reusable password field with a show/hide eye toggle.
- **`notification/NotificationChecker`** — computes due-soon / ready-reservation
  notifications at login and on Next-Day.

## Running the Application

```bash
# Launch the JavaFX GUI
./gradlew run

# Launch the original CLI
./gradlew runCli
```

For IDE execution, ensure the JavaFX modules (`javafx.controls`, `javafx.fxml`)
are on the module path.

## GUI Walkthrough

### 1. Login & Two-Factor Authentication

The login screen (`LoginView`) has an email field, a password field with a
show/hide eye toggle (`PasswordBox`), a "Sign in" button, a "Create a new
account" link, and a theme toggle. Element IDs `#emailField`, `#passwordField`,
and `#loginButton` are preserved for TestFX.

Sign-in is a two-step flow, both steps off the FX thread:

1. **Credentials** — `PersonaService.validateCredentials(email, password)`.
   The `.env` master key (`MASTER_KEY=bid`) lets any existing account sign in.
2. **Two-factor** — on success, `MailService.deliver2FACode(email)` delivers a
   code to the user's simulated inbox and an **Inbox window** (`InboxWindow`)
   opens so the user can read it. A dialog then asks for the code, verified via
   `MailService.verifyCode(email, code)`. The master OTP (`MASTER_OTP=123`)
   always passes, so testers can skip reading the inbox.

Only after verification does `SessionManager.createSession(persona)` run and the
`AppShell` appear. A background `NotificationChecker` check then surfaces any
due-soon loans or ready reservations as a toast.

### 2. The Shell

After login, `AppShell` shows a role-aware sidebar. Nav items depend on the
user's role:

- **Member/student**: Dashboard, Library / Search, Loans & Reservations, Wallet,
  Support.
- **Admin**: Dashboard, Library / Search, Item Management, User Management,
  Callcenter, Fines, Analytics, System Settings.
- **Callcenter**: Dashboard, Support Inbox, Item Management, Fines.
- **Every role** additionally gets **Notifications** and **Profile**.

The top bar shows `email · ROLE`, a theme toggle, a Next-Day control (advances
the simulation clock), and Sign out.

### 3. Library & Search

Real-time, debounced search (300ms) over the catalog, paginated 10 rows per
page, running on a background thread. Two actions:

- **Borrow selected** — enforces every backend borrow rule (role can borrow,
  borrow limit, already-borrowed guard, walk-in availability, finance
  permission) then calls the backend borrow chain.
- **Reserve selected** — `ReservationService.reserve(...)`; reports ACTIVE
  (ready now) vs WAITING (queued, with queue position).

### 4. Loans & Reservations

Lists the user's active loans and reservations. Supports return, extend (with
the extension fee + tax read from `FinanceService`), and cancel-reservation.
Bulk actions operate on multiple selected rows.

### 5. Wallet

Balance and outstanding-debt stat cards plus a transaction-history table.
Top-up and pay-debt actions go through `FinanceService`.

### 6. Profile

View email / role / wallet (email is read-only — see below), edit first/last
name + phone (`SsoService.editProfile`), change password
(`SsoService.changePassword`), and toggle the theme (`SsoService.changeTheme`).

### 7. Admin & Callcenter screens

Item Management, User Management, Fines, Analytics, Callcenter agent management,
System Settings, and the Support Inbox. See `docs/reports.md` for the report
tables and their backend sources.

## Theming (Light / Dark)

Theme handling is in `ir.ac.kntu.gui.util.UiTheme`, an enum with two members:

```java
LIGHT("/ir/ac/kntu/gui/css/light.css")
DARK("/ir/ac/kntu/gui/css/dark.css")
```

### How it works

- **Stylesheets are classpath resources** under
  `src/main/resources/ir/ac/kntu/gui/css/`. Loading them via
  `UiTheme.class.getResource(path)` means they are found on the classpath both
  when running from Gradle and from the packaged jar — no absolute file paths.
- **Applying a theme** (`UiTheme.applyTo(scene)`) removes any known theme
  stylesheet from the scene, then adds the selected one. Switching themes is
  therefore just swapping which CSS file is attached to the live `Scene`.
- **Persistence per user**: toggling the theme calls
  `SsoService.changeTheme(email, key)` (`key` is `"light"` / `"dark"`), which
  saves the choice against the user's profile. On next login,
  `LoginView.onAuthenticated` reads it back via `UiTheme.from(persona.getTheme())`
  and applies it, so each user's preferred theme follows them across sessions.
- The theme toggle is available in three places: the login screen, the shell top
  bar, and the Profile panel — all routing through the same `Navigator.toggleTheme()`
  + `SsoService.changeTheme` path.

### What the CSS covers

Both `light.css` and `dark.css` style the custom component classes (`.card`,
`.h1`, `.muted`, `.primary`, `.ghost`, `.sidebar`, `.toast`, …) **and** type
selectors for standard controls (`.table-view`, `.table-row-cell`, `.button`,
`.combo-box`, `.text-field`, `.text-area`, `.pagination`, `.check-box`) so that
un-classed tables and buttons also get the modern look in both themes.

## Backend-Unchanged Proof (CRITICAL rule)

The task's CRITICAL rule: **no additional code or changes in the previous-phase
backend — the GUI must only connect to existing backend methods.** Here is how
this codebase honors that.

### Only `gui/**` was added

All new application code lives under `src/main/java/ir/ac/kntu/gui/**` and its
resources under `src/main/resources/ir/ac/kntu/gui/**`. The GUI panels call
existing static backend services (`PersonaService`, `LibraryService`,
`LoanService`, `ReservationService`, `FinanceService`, `MailService`,
`SsoService`, `SupportService`, `ReportService`, `SystemSettingsService`) — it
adds no backend behaviour.

### The two non-GUI changes, and why they are not new business logic

Running `git diff --stat HEAD` over non-GUI files shows exactly two:

1. **`build.gradle`** — build configuration only (UTF-8 compile encoding, the
   `run`/`runCli` tasks, TestFX dependencies, and the `-PskipGuiTests` CI
   filter). No product code.
2. **`FinanceService.java`** — a **behaviour-preserving Streams refactor** kept
   from the university's Phase-3 reference (see `STREAMS_REFACTOR.md`). The
   for-loops were rewritten as equivalent Stream pipelines (same inputs → same
   outputs) and a private `signedDebt(tx)` helper was extracted. No public
   signature, fee, constraint, or result changed — the finance tests still pass
   unchanged.

### GUI duplication was removed, not added

Where the GUI originally re-implemented backend logic, it was changed to
delegate:

- **Extension tax/total math** (`LoansReservationsPanel`) now delegates the whole
  fee + tax + funds check to `FinanceService.proccessExtentionPayment` (and its
  `InsufficientFundsException`) instead of re-computing the tax and duplicating
  the wallet pre-check in the GUI.
- **Overdue-loan filtering** (`FinesPanel`) now calls
  `LoanService.getOverdueLoans(today)` instead of looping over loans in the GUI.

Legitimate presentation-only aggregations (grouping loans for a chart, filtering
debtors for a table) remain in the GUI because the backend exposes no equivalent
method — but every value they display comes from an existing backend call.

## Features Intentionally Omitted (constraint-driven)

- **Profile photo** and **email change** — the backend has no photo field and
  `email` is immutable (`editProfile` only changes name/phone). Adding either
  would require new backend code, violating the CRITICAL rule. The Profile panel
  notes this in the UI.
- **Callcenter enum rename** — the nav/panel labels read "Callcenter", but the
  backend enum stays `UserRole.CALLCENTER`; renaming it would ripple through
  Phase 1/2 code. Only GUI-visible strings changed.

## Animations & progress feedback

Where the GUI uses JavaFX animations today (all short, functional — no
decorative motion):

| Where | Effect | Location |
|-------|--------|----------|
| View switch (login ↔ shell) | 220 ms fade-in on the new root | `gui/Navigator.java:38` |
| Panel swap inside the shell | 180 ms fade-in on the incoming panel | `gui/shell/AppShell.java` (`showContent`) |
| Toast notifications | fade-in → hold → fade-out (`SequentialTransition`) | `gui/util/Dialogs.java:83-90` |
| Library search | 300 ms `PauseTransition` debounce (timer, not visual) before firing the query | `gui/view/library/LibrarySearchPanel.java:55` |

Progress feedback for real waits (kept deliberately sparse):

- **HTML report generation** — an inline `ProgressIndicator` beside the "Generate
  HTML report" button, visible while `ReportService.exportReport` runs on a
  background thread (`gui/view/admin/AnalyticsPanel.java`).
- **Analytics load** — a large centred `ProgressIndicator` while the charts are
  computed off the FX thread.
- **Wallet top-up / pay-debt** — a small inline `ProgressIndicator` next to the
  action buttons, which are disabled during the background finance call
  (`gui/view/wallet/WalletPanel.java`).
- **Login** and **Register** already show a spinner while their background jobs
  run; **Library search** shows one during a query. These were left as-is.

## Testing

TestFX GUI tests live under `src/test/java/ir/ac/kntu/gui`. They are excluded on
the CI runner via `-PskipGuiTests` (the shared GitLab runner cannot run headed
JavaFX) but run locally with `./gradlew clean test`. `BorrowReserveReturnE2ETest`
drives real borrow / reserve / return flows and asserts the backend state
actually changed. See `docs/threads.md` for the threading model the tests rely on.
