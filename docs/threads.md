# Background Threading in the GUI

## Overview

Phase 3's JavaFX GUI must never freeze during heavy operations — searches, database queries, Stream aggregations, or mail delivery. Every such operation runs **off the JavaFX Application Thread** using a shared background worker pool, and callbacks that touch the scene graph are marshalled back onto the FX thread automatically.

## BackgroundJobs: The Central Pattern

All background work goes through `ir.ac.kntu.gui.concurrency.BackgroundJobs`, which wraps JavaFX `Task` and a daemon `ExecutorService`:

```java
BackgroundJobs.run(
    () -> LibraryService.searchItems(query),  // runs on worker thread
    results -> updateTable(results),           // runs on FX thread
    error -> Dialogs.error("Search failed", error) // runs on FX thread
);
```

**Key guarantees:**
- The work `Supplier` runs on a background thread from a fixed pool (4 daemon threads).
- On success, `onSuccess` is invoked on the FX thread with the result.
- On failure, `onError` is invoked on the FX thread with the exception.
- The UI stays responsive — no spinner freeze, no unresponsive window.

## Where It's Used

### Login & 2FA (LoginView)

Login credential validation and 2FA code delivery both run on background threads:
- `PersonaService.validateCredentials(email, password)` — DB lookup
- `MailService.deliver2FACode(email)` — mail write + inbox delivery
- `MailService.verifyCode(email, code)` — verification + expiry check

Each step uses `BackgroundJobs.run` so the login button doesn't freeze while waiting for DB or mail.

### Library Search (LibrarySearchPanel)

The search field is debounced (300ms), then:
- `LibraryService.searchItems(query)` runs on a background thread
- Results are paginated and rendered on the FX thread once the job completes
- A spinner shows while the search runs

This pattern keeps the UI fluid even when searching thousands of catalog items.

### Loan & Reservation Actions

Borrow, reserve, return, and extend operations all involve:
- Backend service calls (DB writes, Stream checks)
- Finance permission checks
- Reservation activation logic

Every action button in `LibrarySearchPanel`, `LoansReservationsPanel`, and `WalletPanel` wraps its handler in `BackgroundJobs.runAction(...)` so the button click returns immediately and the UI updates only when the operation completes.

### Admin Reports & Analytics (AnalyticsPanel)

Generating HTML reports via `ReportService.exportReport(path)` can take time for large datasets. The export runs on a background thread, and the "Export" button re-enables only when the file is written.

Similarly, the analytics charts load their data (overdue loans, revenue stats, borrow trends) via background jobs, and the panel shows a loading indicator until all stats arrive.

### Notifications (NotificationChecker)

At login and after "Next Day", `NotificationChecker.checkAndNotify(persona)` computes due-soon loans and ready reservations via Streams. The computation runs on a background thread, and if any warnings are found, a toast appears on the FX thread. This happens asynchronously — login proceeds immediately, and the notification pops when the check finishes.

## The Pattern in Practice

A typical button handler:
1. Validate inputs (on FX thread, lightweight)
2. Disable the button and show a spinner
3. Call `BackgroundJobs.run(() -> ...service call...)` with:
   - Work lambda: calls backend service (DB, Streams, file I/O)
   - Success callback: updates UI, shows success dialog, re-enables button
   - Error callback: shows error dialog, re-enables button

This keeps the pattern consistent: **no backend call ever blocks the FX thread**.

## Why Not a Permanent Daemon?

The plan notes that notifications are triggered **at login and on "Next Day"** — not by a permanently running background thread. This matches the backend's CLI design: the simulation clock advances explicitly, not via a wall-clock daemon. A permanent polling thread would:
- Complicate shutdown (requires explicit `ExecutorService.shutdown()`)
- Wake the CPU when idle
- Contradict the discrete-time simulation model

Instead, background jobs are **ephemeral**: launched on-demand, they complete and disappear. The shared executor pool stays alive for the session but only runs tasks when explicitly submitted.

## Summary

Phase 3's threading model is simple and uniform:
- **FX thread**: UI rendering, event dispatch, lightweight validation
- **Background threads** (via `BackgroundJobs`): DB, Streams, file I/O, backend service calls
- **Marshalling**: all UI updates from background callbacks are automatically delivered on the FX thread via `Platform.runLater`

This keeps the GUI responsive under load and matches the backend's synchronous, explicit-clock design.
