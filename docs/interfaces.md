# interfaces Package

## In plain terms
This package holds small "contracts." An interface is a promise: "any class
that implements me guarantees it has these methods." The class picking up the
contract decides *how* the methods work; the interface only says *what* must
exist. This lets completely unrelated classes (a `Book` and a `SupportTicket`,
say) share the same behavior contract without sharing a parent class.

Unlike an earlier draft of this doc, these interfaces are **not** empty
scaffolding — four of the five are actively implemented across the app. Only
`RequestAssignable` is currently unused.

## The five contracts

| Interface | The promise (method) | Who actually implements it |
|-----------|----------------------|-----------------------------|
| `Displayable` | "I can turn myself into one line of text." `String toDisplayString()` | `SupportTicket`, `OverdueLoanReport`, `SupplierFinancials` |
| `Searchable` | "I can say whether I match a search word." `boolean matchesQuery(String)` | `LibraryItem` (so every Book / Magazine / EBook / AudioBook) |
| `Borrowable` | "I can tell you if I'm available to borrow." `boolean canBorrow()` | `LibraryItem` (whole item hierarchy) |
| `Reservable` | "I can tell you if I'm allowed to be reserved." `boolean canReserve()` | `PhysicalItem` (so Book and Magazine) |
| `RequestAssignable<T>` | "I can be assigned to someone of type T." `assign(T)`, `getAssignee()`, `isAssigned()` | **Nobody yet** — declared but unused |

## Contract by contract

### `Displayable` — "render me as a menu line"
One method, `toDisplayString()`. Any class that can summarize itself in a single
line implements it. Three do: `SupportTicket`, `OverdueLoanReport`, and
`SupplierFinancials`. This is what lets the generic `Menu<T extends Displayable>`
and `ConsoleMenu.printAll(List<? extends Displayable>)` print *any* list of
these objects without knowing their concrete type.

### `Searchable` — "do I match this keyword?"
One method, `boolean matchesQuery(String query)`. `LibraryItem` (the abstract
root of the catalog) implements it, so every concrete item type inherits it.
The generic helper `SearchEngine.search(...)` uses this contract to filter *any*
searchable list.

### `Borrowable` — "am I available to borrow?"
One method, `boolean canBorrow()`. Implemented by `LibraryItem` as
`return availableCopies > 0`.

> Heads-up on a naming coincidence: the persona/role classes
> (`GuestProfile`, `StudentProfile`, …) also have methods called `canBorrow()`
> and `canReserve()`, but they do **not** implement these interfaces — they just
> happen to use the same names for a different purpose (whether a *user's role*
> may borrow). Only the library-item hierarchy implements `Borrowable` /
> `Reservable`.

### `Reservable` — "am I allowed to be reserved?"
One method, `boolean canReserve()`. Implemented by `PhysicalItem` (returns
`true`), so `Book` and `Magazine` are reservable. `DigitalItem` (`EBook`,
`AudioBook`) has its own `canReserve()` returning `false`, but note it does
**not** implement the `Reservable` interface — it only defines the method.

### `RequestAssignable<T>` — the unused generic contract
A generic contract for "this thing can be handed to an assignee of type `T`":
`assign(T)`, `getAssignee()`, `isAssigned()`. It is fully declared but **nothing
in the codebase implements or references it** — it is scaffolding kept for a
possible future feature (e.g. assigning tickets or role requests to a staff
member). If you are auditing dead code, this is the one interface that is
currently unused.

## Why use interfaces here?
Because they let *different, unrelated* classes share one behavior contract
without a common parent. `SupportTicket`, `OverdueLoanReport`, and
`SupplierFinancials` have nothing else in common, yet all three are
`Displayable`, so one generic menu can print all of them. Code written against
the interface (`List<? extends Displayable>`) is written once and works for
every present and future implementor.

This is the **Interfaces** grading criterion: small contracts implemented across
otherwise-unrelated classes and consumed polymorphically.
