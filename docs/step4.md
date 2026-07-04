# Step 4 — Library Item Polymorphism (Implementation)

## What Changed

Replaced the `instanceof Book` / `instanceof AudioBook` checks in
`LibraryPrinter` with polymorphic methods declared on `LibraryItem` and
overridden by the leaf classes.

```
LibraryItem            (abstract)
├── PhysicalItem        (abstract)
│   ├── Book
│   └── Magazine
└── DigitalItem         (abstract)
    ├── EBook
    └── AudioBook
```

`PhysicalItem` and `DigitalItem` were promoted from concrete to `abstract`,
since they no longer implement every method on `LibraryItem` themselves —
some behavior (`displayInfo()`, `borrowPeriod()`, `availableActions()`) only
makes sense at the leaf level.

---

## Important Design Decision: Data vs. Behavior (same rule as Step 3)

Before adding a method, each candidate was checked against the question:
*"does this vary by instance data, or by type?"* — see `docs/step3.md` for
why that distinction matters.

### `canBorrow()` — NOT overridden (data, not behavior)

```java
// LibraryItem.java — one implementation, no subclass overrides it
public boolean canBorrow() {
    return availableCopies > 0;
}
```

Whether an item can be borrowed depends on how many copies are left *right
now* — that is per-instance stock data, identical in shape for a `Book` and
an `EBook`. Forcing every leaf class to override this would just re-type the
same `availableCopies > 0` check five times for no behavioral difference.
This mirrors the `borrowLimit()` lesson from Step 3.

### `canReserve()`, `borrowPeriod()`, `displayInfo()`, `availableActions()` — genuinely polymorphic

These differ *by type*, not by instance:

| Method | Book | Magazine | EBook | AudioBook | Why it's type-based |
|--------|------|----------|-------|-----------|----------------------|
| `canReserve()` | true (inherited) | **false** (override) | false (inherited) | false (inherited) | Physical items support a reservation queue; digital items have no scarcity to queue for. Magazines override back to `false` — back issues aren't worth reserving. |
| `borrowPeriod()` | 14 days | 7 days | 21 days | 21 days | Loan length is a property of the format, not of any one copy. |
| `displayInfo()` | `"Author: ..."` | `"Issue #..."` | `"Pages: ..."` | `"Narrator: ... (N min)"` | Completely different fields per type — this is what used to be the `instanceof` chain. |
| `availableActions()` | Borrow, Return, Reserve | Borrow, Return | Borrow, Return, Download | Borrow, Return, Stream | The extra action (`Reserve`/`Download`/`Stream`) only makes sense for that format. |

`availableActions()` is built from a shared `baseActions()` helper in
`LibraryItem` that already checks `canReserve()` — so "can this be reserved"
has exactly **one** source of truth (the `canReserve()` override), instead of
also being hardcoded a second time into a literal action list.

```java
protected List<String> baseActions() {
    List<String> actions = new ArrayList<>();
    actions.add("Borrow");
    actions.add("Return");
    if (canReserve()) {
        actions.add("Reserve");
    }
    return actions;
}
```

`PhysicalItem` defaults `canReserve()` to `true`, `DigitalItem` defaults it
to `false`; only `Magazine` needs to override it back to `false`.

---

## Before / After

**Before** (`LibraryPrinter.java`):
```java
private static void printAuthorLine(LibraryItem item) {
    if (item instanceof Book) {
        System.out.println(ConsoleColor.gray("     Author: " + ((Book) item).getAuthor()));
    } else if (item instanceof AudioBook) {
        System.out.println(ConsoleColor.gray("     Narrator: " + ((AudioBook) item).getNarrator()));
    }
}
```
Magazines and EBooks silently printed nothing extra — the check simply
didn't know about them, and every new item type would require editing this
method again.

**After**:
```java
private static void printDisplayInfo(LibraryItem item) {
    System.out.println(ConsoleColor.gray(INDENT + item.displayInfo()));
}
```
Every item type now prints its own detail line, including Magazine and
EBook, which the old code silently skipped. Adding a new item type requires
zero changes to `LibraryPrinter`.

`printDetails()` also now shows `item.borrowPeriod()` and
`item.availableActions()`, which didn't exist as user-facing information
before this step.

---

## Fixing a Latent Duplicate-Source-of-Truth Bug

While designing `borrowPeriod()`, `LoanService` was found to already compute
loan due dates using a single flat constant:

```java
private static final int LOAN_PERIOD_DAYS = 3;
...
int dueDay = currentDay + LOAN_PERIOD_DAYS;
```

Every item — Book, Magazine, EBook, AudioBook — got the same 3-day loan
regardless of format. Adding `LibraryItem.borrowPeriod()` without wiring it
in would have created exactly the duplicate-source-of-truth problem called
out in Step 3 (two competing answers to "how long is the loan?").

Instead, `LoanService.recordLoan` now takes the period as a parameter, and
`LibraryMemberConsole.doBorrow` supplies `item.borrowPeriod()`:

```java
public static void recordLoan(String memberId, String itemId, int currentDay, int loanPeriodDays) {
    ...
    int dueDay = currentDay + loanPeriodDays;
    ...
}
```

`LibraryItem.borrowPeriod()` is now the single, real source of truth for
loan length, and it actually drives due-date calculation instead of sitting
unused.

---

## Adding a New Item Type Now Requires

1. Extend `PhysicalItem` or `DigitalItem` (pick based on whether it's a
   physical or digital format).
2. Implement `getItemType()`, `borrowPeriod()`, `displayInfo()`, and
   `availableActions()`.
3. Override `canReserve()` only if it disagrees with the physical/digital
   default.

No changes to `LibraryPrinter`, `LoanService`, or any console class.

---

## Verification

`./gradlew test` — all tests pass, including the CheckStyle/PMD
quality-gate tests (PMD's `AvoidDuplicateLiterals` rule was fixed along the
way by extracting the repeated `"     "` indent into an `INDENT` constant in
`LibraryPrinter`).
