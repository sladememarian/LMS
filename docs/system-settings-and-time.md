# System Settings & Time

## In plain terms
Two related ideas:
1. **System settings** — a handful of numbers an admin can change at runtime that
   control how the library behaves (how long you can borrow, the daily fine, how
   long reservations last, how many reservations you can hold).
2. **Simulated time** — the app runs on a fake calendar measured in "days." An
   admin advances the clock one day at a time, which triggers overdue fines and
   reservation expirations.

They live together in this doc because the settings tune the rules and the clock
is what makes those rules fire.

---

# Part 1 — System Settings

## The four settings
| Key | Default | What it controls |
|-----|---------|------------------|
| `borrowDays` | 21 | Max days an item can be borrowed (a ceiling) |
| `fineRate` | 10,000 | Daily overdue fine (currency units) |
| `reservationDays` | 7 | How long a reservation's pickup window lasts |
| `maxReservations` | 10 | Max reservations a member may hold (a ceiling) |

The defaults are **ceilings, not targets** — e.g. a Book's own 14-day borrow
period is used if it's shorter than `borrowDays`, and a Student's own limit of 5
reservations wins over the system's 10.

## Two classes: the snapshot and the live holder
- `SystemSettings` — an **immutable** value object: four `final int` fields plus
  getters and a `defaults()` factory. Think of it as a read-only snapshot.
- `SystemSettingsService` — the **live, mutable** holder. Static fields start at
  the defaults, then `load()` overwrites them from the database at class load.
  This is what the rest of the app reads and what admins update.

## Reading a setting
Anywhere in the app just calls the static getter:

```java
SystemSettingsService.getBorrowDays();       // 21 by default
SystemSettingsService.getReservationDays();  // 7 by default
```

Who reads what:
- `reservationDays` → `ReservationService` (pickup deadlines).
- `maxReservations` → the reservation limit check.
- `borrowDays` → the borrow flow (`LibraryMemberConsole.doBorrow`).
- `fineRate` → overdue fine accrual (via `LoanService`).

## Changing a setting (admin only)
Each setter runs the same three steps — authorize, validate, persist:

```java
public static void updateReservationDays(Persona actor, int value) {
    requireOwnerOrAdmin(actor);   // must be an ADMIN, else AuthorizationException
    requirePositive("Reservation days", value);  // > 0, else ValidationException
    reservationDays = value;      // update the live field
    SystemSettingsRepository.save(RESERVATION_DAYS_KEY, String.valueOf(value));  // persist
}
```

The other three (`updateBorrowDays`, `updateFineRate`, `updateMaxReservations`)
are identical in shape. So: **only an admin can change settings, and every value
must be positive.**

**In the UI:** `AdminInbox` option **13 "System Settings"** prints the current
values and offers 1–4 to edit Borrow Days / Fine Rate / Reservation Days / Max
Reservations. The choice reads a new integer and calls the matching update method
with the admin as `actor`, so the authorization check applies.

## Persistence
Settings are stored as string key/value rows in the `system_settings` table.
`SystemSettingsRepository.save(key, value)` is a `MERGE` (upsert);
`getAll()` returns them as a map, which `load()` parses back to ints (falling
back to the default if a value is missing or non-numeric).

---

# Part 2 — Simulated Time

## The clock is measured in "days"
There's no real-time passage. The app tracks an integer `currentDay` starting at
`1`, anchored to a real calendar date (`startDate`) so it can display a human
date too. Time only moves when an admin advances it.

## Two classes: a facade over the real clock
- `SimulationClock` (in the `finance` package) — the actual clock. Holds
  `currentDay` and `startDate`, persists to the `clock_state` table.
- `SystemClock` (in the `time` package) — a thin **facade** in front of it, so
  app-wide code can ask "what day is it?" without importing `finance`:

```java
public final class SystemClock {
    public static int getCurrentDay()   { return SimulationClock.getCurrentDay(); }
    public static void advanceOneDay()  { SimulationClock.advanceDay(); }
}
```

(See `docs/time.md` for more on why the facade exists.)

`SimulationClock` key methods:
- `getCurrentDay()` → the current day number.
- `getCurrentDate()` → `startDate.plusDays(currentDay - 1)` (the real date for
  today's simulated day).
- `formatCurrentDate()` → that date formatted `M/d/yyyy`.
- `advanceDay()` → increments `currentDay`, saves to `clock_state`, returns the
  new day.

## What "Advance Simulated Day" actually does
`AdminInbox` option **9 "Advance Simulated Day"** is the real entry point. It
chains three side effects, in order:

```java
int newDay = SimulationClock.advanceDay();               // 1. move the clock + persist
List<String> charges = LoanService.accrueOverdueDebts(newDay);  // 2. accrue overdue fines
ReservationService.expireReservations(newDay);           // 3. expire stale reservations
// then print the new day + date + any overdue charges
```

So advancing one day:
1. **Moves time forward** and saves it.
2. **Charges overdue fines** on loans past due (at `fineRate` per day).
3. **Expires reservations** whose pickup window has passed — and each freed copy
   cascades to the next person in that item's queue (see
   `docs/reservation.md`).

It does **not** touch system settings.

> Maintainer note: only `AdminInbox.advanceSimulatedDay` chains the fine/expiry
> side effects. The bare `SystemClock.advanceOneDay()` wrapper just moves the day
> (it's used mainly by tests) — calling it alone would advance time *without*
> accruing fines or expiring reservations.

## Persistence
The clock is a single row in the `clock_state` table (`id=1`, `current_day`,
`start_date`). `advanceDay()` upserts it via `MERGE`. On startup the clock loads
from that row, or seeds day 1 / today's date if the row doesn't exist yet.
