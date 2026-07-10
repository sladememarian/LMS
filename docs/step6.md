# Step 6 — Use Generics Properly (Implementation)

## What is a generic, in plain terms?

A generic is a class or method that says "I work on a box of *something*,
but I don't need to know what's inside the box to do my job." Instead of
writing `PagedList` for `LibraryItem`, then a near-identical copy-paste
`PagedList` for `SupportTicket`, you write it once with a placeholder
letter (`T`) for "whatever type goes in here," and Java fills in the real
type when you use it — `PagedList<LibraryItem>`, `PagedList<SupportTicket>`,
etc. One class, works for anything.

This project already had three unused generic helper classes sitting in
`ir.ac.kntu.generic` from the Phase 2 setup (`PagedList<T>`,
`SearchResult<T>`, `Menu<T extends Displayable>`) — nothing called them
yet. This step wires them into real code paths.

## Scope decision (confirmed with you first)

You explicitly said: **don't touch how things are stored** — books, users,
tickets stay in plain Java `List`s the way they already are; no rewrite
onto the pre-existing (also unused) `JsonRepository<T>` abstract class.
Only the smaller, safer generics got wired up. `JsonRepository<T>` remains
unused scaffolding, same as before this step — deliberately, not an
oversight.

---

## 1. `PagedList<T>` replaces hand-rolled pagination math

`LibraryPrinter.printListPaginated()` used to compute page boundaries by
hand every time:

```java
int pageSize = 10;
int totalPages = (items.size() + pageSize - 1) / pageSize;
int start = currentPage * pageSize;
int end = Math.min(start + pageSize, items.size());
```

That's arithmetic a generic class can just do once, correctly, for any list
of anything. Now:

```java
PagedList<LibraryItem> paged = new PagedList<>(items, currentPage, PAGE_SIZE);
// paged.getItems(), paged.getTotalPages(), paged.getTotalItems() — no manual math
```

Output is identical (same 10-per-page behavior, same page numbers) — this
is a pure "same result, less duplicated arithmetic" refactor.

---

## 2. `SearchEngine` — one search algorithm, any `Searchable` type

Previously, `LibraryService.searchItems()` had its own inline
`.contains()` loop. That loop doesn't actually care that it's searching
`LibraryItem`s specifically — it only needs "does this item say yes to
`matchesQuery()`?" (the `Searchable` interface from Step 5). So the loop
was pulled out into a small static utility:

```java
public static <T extends Searchable> List<T> search(List<T> items, String query) {
    List<T> results = new ArrayList<>();
    for (T item : items) {
        if (item.matchesQuery(query)) {
            results.add(item);
        }
    }
    return results;
}
```

`LibraryService.searchItems()` now just calls `SearchEngine.search(INVENTORY, keyword)`.
If a `SupportTicket` search feature is added later, it reuses the exact
same method — `SearchEngine.search(tickets, keyword)` — with zero new
searching code, as long as `SupportTicket` implements `Searchable` (it
doesn't yet; only `LibraryItem` does per the Step 5 assignment table).

`SearchEngine` itself is a stateless utility class (like `SystemClock`,
`Validator`) — it doesn't hold any data of its own, so there's nothing to
create an "instance" of; you just call `SearchEngine.search(...)` directly.

---

## 3. `SearchResult<T>` — pairing an item with *why* it matched

Search used to answer "yes/no, this item matched" but threw away *which*
field matched. `LibraryItem` got a small sibling method to `matchesQuery()`:

```java
public String matchedField(String query) {
    // returns "title", "category", or null if no match
}
```

And a new method, `LibraryService.searchItemsDetailed(String keyword)`,
returns `List<SearchResult<LibraryItem>>` — each result bundles the item
together with which field matched. This did **not** replace
`searchItems()` (existing callers/tests are untouched); it's a new,
additional method.

The Admin's "Search Item" screen now uses it to print a one-line summary
before the results list:

```
Found 3 item(s) - 2 matched by title, 1 matched by category
```

then shows the same paginated list as before. Nothing about the existing
search *results* changed, just an extra summary line.

---

## 4. `Menu<T extends Displayable>` — now actually used

`Menu<T>` requires `T` to be `Displayable` (Step 5's interface) — this is
a **bounded** generic: not "any type," but specifically "any type that
knows how to display itself." Since `SupplierFinancials` implements
`Displayable` (Step 5), the Admin's "View Supplier Financials" option now
uses `Menu<SupplierFinancials>` instead of the plainer `printAll()` helper
from Step 5:

```java
new Menu<>("Supplier Financials", ReportService.computeSupplierFinancials()).render();
```

This prints the same rows, now with numbering (since `Menu` is built for
selectable lists), and is the concrete proof that `Menu<T>` — sitting
unused since Phase 2 setup — now genuinely compiles and runs against a
real `Displayable` type.

---

## What was left alone (on purpose)

- **`JsonRepository<T>`** — per your explicit instruction, storage stays on
  plain Lists + `DatabaseAccess`. Still unused scaffolding.
- **`Response<T>` / `Result<T>`** — these wrapper types were mentioned in
  the original spec but don't exist as scaffolding in this project and
  nothing in the current architecture needs a generic
  success/failure-wrapper (methods already return plain booleans or throw
  exceptions — see the exception audit). Not added, to avoid inventing an
  unused abstraction with no real caller.
- **`RequestAssignable<T>`, `Reservation.java`** — still unrelated,
  pre-existing unused scaffolding, out of scope for this step (same as
  Step 5).

---

## A PMD gotcha along the way

The project's PMD rules require **every** class to have an explicit
constructor (`AtLeastOneConstructor`), but also forbid constructors that do
nothing (`UnnecessaryConstructor`) — those two rules together mean: a
class either needs real per-instance data to initialize in its
constructor, or it shouldn't be instantiated at all. `SearchEngine`
originally had neither (no fields, would've needed an empty constructor),
so it was made a `static` utility class instead — same pattern already
used by `SystemClock`/`Validator`: private constructor that throws
`UnsupportedOperationException`, and all real methods are `static`.

---

## Verification

`./gradlew test` — all 119 tests pass, including PMD/CheckStyle
quality-gate tests. No existing behavior changed for `searchItems()`,
pagination output, or supplier financials data — only new, additive
methods and one small cosmetic upgrade (Menu numbering) with no test
coverage on it.
