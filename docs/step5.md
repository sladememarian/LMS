# Step 5 — Introduce Interfaces (Implementation)

## First — what even IS an interface? (plain terms)

Think of an interface as a **promise, not an implementation**. It's a list
of method names with no code inside them — just "any class that signs this
contract must provide these methods." The class itself still writes the
real code; the interface only guarantees *that a method with this name and
this shape will exist*.

Why bother? Because it lets you write code that says "give me anything that
can be displayed" instead of "give me specifically a `SupportTicket`." A
single `List<? extends Displayable>` can then hold tickets, financial
report rows, or anything else that promises a `toDisplayString()` method —
and the printing code never has to change or know which one it got.

This is different from the `abstract class` hierarchies from Steps 3 and 4:
- **Abstract class** (`UserProfile`, `LibraryItem`) = "these things are all
  fundamentally the *same kind of thing* and share real code/fields." A
  `StudentProfile` IS-A `UserProfile`.
- **Interface** (`Displayable`, `Borrowable`) = "these things might be
  totally unrelated, but they all *can do* the same one thing." A
  `SupportTicket` and a `SupplierFinancials` row have nothing to do with
  each other, but both CAN be displayed.

A class can implement as many interfaces as it wants (unlike extending a
class, where you only get one parent). That's exactly why `Book` ends up
with two labels — `Borrowable` (inherited from `LibraryItem`) and
`Reservable` (inherited from `PhysicalItem`).

---

## What was already there (unused scaffolding)

Four interface files already existed in `ir.ac.kntu.interfaces` from the
Phase 2 setup, but nothing in the whole project used them yet:
`Borrowable`, `Reservable`, `Displayable`, `Searchable`. Since nothing
implemented them, they were safe to reshape to fit what the project
actually needed.

`Borrowable` and `Reservable` originally had 3 methods each (`reserve()`,
`cancelReservation()`, `hasActiveReservation()`, etc.) describing a whole
reservation *system* that doesn't exist in this project (no waiting lists,
no expiry dates). They were simplified down to exactly the one question the
codebase already answers:

```java
public interface Borrowable {
    boolean canBorrow();
}

public interface Reservable {
    boolean canReserve();
}
```

`Displayable` (`String toDisplayString();`) and `Searchable`
(`boolean matchesQuery(String query);`) were already shaped correctly and
needed no changes.

---

## Who implements what, and why

| Class | Interfaces | Why |
|-------|-----------|-----|
| `LibraryItem` (and everything under it: Book, Magazine, EBook, AudioBook) | `Borrowable`, `Searchable` | Every item already had `canBorrow()` (Step 4) and search-by-title/category logic — this just puts an official label on abilities that already existed. |
| `PhysicalItem` (Book, Magazine) | `Reservable` | Only physical copies support a reservation queue — `PhysicalItem.canReserve()` already returned `true`/overridden by Magazine. `DigitalItem` (EBook, AudioBook) does NOT implement `Reservable` — matches the spec's table exactly, no code changes needed, just adding `implements Reservable` to a class that already had the method. |
| `SupportTicket` | `Displayable` (new) | Added `toDisplayString()`, which reproduces the exact line `TicketPrinter` used to print by hand. |
| `SupplierFinancials` | `Displayable` (new) | Added `toDisplayString()` — a new one-line summary (company, item count, copies, borrowed, inventory value). Nothing showed this on-screen before; it only existed inside the HTML report. |

**Note on Magazine**: `Magazine` still overrides `canReserve()` to return
`false` ("back issues aren't worth reserving" — see Step 4), even though it
implements `Reservable` (inherited from `PhysicalItem`). That's not a
contradiction: the interface says "this class is the *kind of thing* that
answers a reserve question," the boolean is just today's answer to that
question. A magazine could start supporting holds later without changing
its interface, only its logic.

---

## "Menus use interfaces instead of concrete classes"

Added one new generic helper to `ConsoleMenu`:

```java
public static void printAll(List<? extends Displayable> items) {
    if (items.isEmpty()) {
        System.out.println(ConsoleColor.gray("  (nothing to show)"));
        return;
    }
    for (Displayable item : items) {
        System.out.println(item.toDisplayString());
    }
}
```

This method has never seen `SupportTicket` or `SupplierFinancials` before —
it only knows about the `Displayable` promise. Two call sites now use it:

1. `TicketPrinter.printTickets()` used to hand-format each ticket line by
   line; it now just calls `ConsoleMenu.printAll(tickets)`. Output is
   byte-for-byte identical to before (verified — `toDisplayString()` builds
   the exact same string).
2. `LibraryAdminConsole` gained a brand-new menu option — **"11. View
   Supplier Financials"** — that calls
   `ConsoleMenu.printAll(ReportService.computeSupplierFinancials())`. This
   is genuinely new: before this, supplier financial numbers were only
   visible inside the generated HTML report, never on the console.

Both call sites go through the exact same `printAll()` method despite
printing two completely unrelated kinds of objects — that's the concrete
demonstration of "code depends on the interface, not the concrete class."

---

## Searchable wiring

`LibraryService.searchItems()` used to have its own inline
`.toLowerCase().contains()` logic checking title and category. That logic
moved onto `LibraryItem.matchesQuery(String query)`, and the service now
just asks each item:

```java
for (LibraryItem item : INVENTORY) {
    if (item.matchesQuery(keyword)) {
        results.add(item);
    }
}
```

Same behavior, single source of truth for "does this item match this
search term" (previously the answer only existed inline in one method; now
any future search/filter feature can just call `matchesQuery()` too).

---

## What was left alone (out of scope, on purpose)

- `RequestAssignable<T>` and `Reservation.java` — pre-existing unused
  scaffolding for a full reservation-queue feature (holds, expiry dates)
  that this project doesn't implement. Not part of this step's spec.
- `LibraryMemberConsole`'s interactive item picker — not touched, to avoid
  changing any user-facing flow beyond what was asked for.

---

## Verification

`./gradlew test` — all tests pass, no behavior changed for anything with
existing test coverage (`LibraryServiceTest`'s search-null/empty/keyword
cases still pass unchanged).
