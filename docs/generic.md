# generic Package (Phase 2)

## In plain terms
"Generic" in Java means a class that works with *any* type, decided later,
instead of being locked to one specific type. This package holds 4 reusable
building blocks that any future feature can plug its own type into (`<T>`).

## The building blocks

### `JsonRepository<T>` — a generic "storage box"
An abstract base class for an in-memory list of any type `T`, with the usual
storage operations already built in: `save`, `findById`, `getAll`, `delete`,
`size`. A future concrete class (e.g. `ReservationRepository extends
JsonRepository<Reservation>`) only needs to say *how* to get an item's ID and
how to persist/remove it in the database — the rest (searching, storing,
removing from the list) is free.

```java
public class ReservationRepository extends JsonRepository<Reservation> {
    public String getId(Reservation r) { return r.getReservationId(); }
    protected void persist(Reservation r) { /* save to DB */ }
    protected void removeById(String id) { /* delete from DB */ }
}
```

### `Menu<T extends Displayable>` — a generic console menu
Renders any list of items as a numbered menu (as long as each item knows how
to display itself via `Displayable`), and reads back the user's numeric
choice. Meant to replace hand-written "print numbered list, read number"
boilerplate that's currently duplicated across many `*Console` classes.

### `SearchResult<T>` — a generic "found it" wrapper
Wraps a found item together with *which field matched* the search (e.g.
"title" or "author"), so search UIs can show *why* something matched, not
just *what* matched.

### `PagedList<T>` — a generic "page" of results
Given a full list, a page number, and a page size, it slices out just that
page and tells you `getTotalPages()`, `hasNext()`, `hasPrevious()`. Useful
once the library catalog or transaction history gets long enough that
printing everything to the console isn't practical.

## Why generics matter here
Without `<T>`, we'd need a separate `Menu`, `SearchResult`, and `PagedList`
class for every domain type (`BookMenu`, `TicketMenu`, `LoanSearchResult`,
`TransactionPagedList`, …). Generics let one implementation serve every
current and future domain type — this is the **Generics** grading criterion.
