# JavaFX + Multithreading Migration Plan — LMS Phase 3

This document is the **master roadmap** for converting the existing CLI-based
Library Management System into a JavaFX desktop app with proper multithreading
and Java Streams. It is written so you (or another AI) can **resume at any step**.

Each step below is delivered as its own cumulative ZIP (`step-N-*.zip`). Each ZIP
contains the *entire* project at that point, so you always build/run the latest one.

> The existing business logic (services in `iam`, `library`, `finance`,
> `reservation`, `support`, `persona`) is **reused as-is**. The GUI is a new
> presentation layer in package `ir.ac.kntu.gui` that calls those services.
> No existing feature is removed — we only add a GUI on top of the CLI.

---

## Architecture decisions

- **New package:** `ir.ac.kntu.gui` holds all JavaFX code. CLI `Main` stays intact.
- **No FXML** (optional): views are built in Java for clarity, but FXML module is
  enabled so you can add it later.
- **Navigation:** a single `Navigator` owns the primary `Stage` and swaps root
  views (multi-scene). Each screen is a `View` (root node + title).
- **Threading:** ALL heavy work (login, DB access, search, report gen, stats)
  runs off the JavaFX Application Thread via `BackgroundJobs` (a shared
  `ExecutorService`) using JavaFX `Task`. Results are marshaled back with the
  Task success/fail callbacks (which fire on the FX thread) or `Platform.runLater`.
- **Streams:** all filtering/sorting/grouping/aggregation uses the Streams API.
- **Theming:** light/dark CSS, persisted per user via existing `SsoService.changeTheme`.

---

## Step-by-step roadmap

| Step | Title | Deliverable ZIP | Status |
|------|-------|-----------------|--------|
| 1 | **Foundation & threading infra** — Gradle JavaFX config, `gui` package, `App`, `GuiLauncher`, `Navigator`, `BackgroundJobs`, `Dialogs`, base `View`, CSS themes, working **Login + Register** screens wired to real services | `step-1-foundation.zip` | ✅ done |
| 2 | **App shell + role-based navigation** — post-login shell with sidebar/tabs per role (User/Support/Admin), routing to placeholder scenes, logout, theme toggle | `step-2-shell.zip` | ✅ done |
| 3 | **Dashboards (Streams)** — User/Support/Admin dashboards with info cards + charts; all stats computed via Streams on a background `Task` with `ProgressIndicator` | `step-3-dashboards.zip` | ✅ done |
| 4 | **Library / Search** — `TableView` of items, **real-time search** on a background thread (debounced), pagination controls | `step-4-library-search.zip` | ✅ done |
| 5 | **Item Management** — add / edit / delete items & quantities (Admin + CallCenter) with validation + `Alert` errors | `step-5-item-mgmt.zip` | ⏳ next |
| 6 | **Loans & Reservations** — active loans, history, reservations; borrow/return/extend actions | `step-6-loans-reservations.zip` | pending |
| 7 | **Wallet & Transactions** — balance, top-up, pay debt, transaction history table | `step-7-wallet.zip` | pending |
| 8 | **Support** — ticket creation + tracking (user), inbox + reply/close (CallCenter) | `step-8-support.zip` | pending |
| 9 | **Admin management screens** — User Management, Support-Staff Management, System Settings, Fines (sorted debtor `TableView`, Streams filtering) | `step-9-admin-mgmt.zip` | pending |
| 10 | **Notification system** — inbox screen; background check (Task) at Login and on "Next Day" for reservation-active + due-soon (<3 days) warnings | `step-10-notifications.zip` | pending |
| 11 | **Analytics charts** — Admin `BarChart` top-10 borrowed items, `LineChart` monthly fine revenue (Streams over history) | `step-11-analytics.zip` | pending |
| 12 | **Streams refactor sweep** — replace remaining `for`/`while` aggregations across services with Streams | `step-12-streams-refactor.zip` | pending |
| 13 | **Bonus** — scene/notification animations, HTML report generation (background Task + progress), theme persistence polish, JDBC/JSON persistence if missing | `step-13-bonus.zip` | pending |

---

## How to build & run (once JavaFX is added — Step 1)

```bash
# from project root
./gradlew run          # launches the JavaFX GUI (mainClass = ir.ac.kntu.gui.GuiLauncher)
./gradlew runCli       # still runs the original CLI (ir.ac.kntu.Main)
```

The JavaFX Gradle plugin (`org.openjfx.javafxplugin`) downloads the correct
JavaFX modules automatically, so you do **not** need to hand-set `--module-path`
VM args. If you run from an IDE without the plugin, use:

```
--module-path "<javafx-sdk>/lib" --add-modules=javafx.controls,javafx.fxml
```

Default seeded logins (from existing bootstrap):
- Admin: `admin@system.local` / `adminpass`
- CallCenter: `callcenter@system.local` / `ccpass`

---

## Threading rules (enforce in every step)

1. Never call a service that touches the DB or does heavy Stream work directly in
   an event handler. Wrap it in `BackgroundJobs.run(...)`.
2. Update UI only inside Task `setOnSucceeded` / `setOnFailed` or `Platform.runLater`.
3. Show a `ProgressIndicator` / disable the button while a Task runs.
4. Exceptions from services → caught in `setOnFailed` → shown via `Dialogs.error(...)`.

---

## Package layout being introduced

```
ir/ac/kntu/gui/
├── GuiLauncher.java        # plain main() -> launches App (module workaround)
├── App.java                # extends javafx.application.Application
├── Navigator.java          # owns primary Stage; switchTo(View), theme handling
├── View.java               # interface: Parent getRoot(); String title()
├── concurrency/
│   └── BackgroundJobs.java # shared ExecutorService + Task helpers
├── util/
│   ├── Dialogs.java        # error/info/confirm Alerts (FX-thread safe)
│   └── UiTheme.java        # light/dark stylesheet switching + persistence
└── view/
    ├── LoginView.java
    └── RegisterView.java
resources/ir/ac/kntu/gui/css/
├── light.css
└── dark.css
```

Later steps add `view/shell/`, `view/dashboard/`, `view/library/`, etc.
