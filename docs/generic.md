# generic Package

## In plain terms
"Generic" in Java means a class or method that works with *any* type, chosen by
the caller later, instead of being hard-wired to one type. You write it once
with a placeholder `<T>`, and it serves `Book`, `Persona`, `SupportTicket`, or
anything else. This package holds **six** reusable building blocks.

> Correction vs. an older draft: there is **no** `JsonRepository` class. The
> real members are listed below.

## The six building blocks

| Member | Kind | Type parameter | What it does |
|--------|------|----------------|--------------|
| `SearchResult<T>` | class | `<T>` | Wraps a found item plus which field matched |
| `PagedList<T>` | class | `<T>` | Slices a big list into one page |
| `Menu<T extends Displayable>` | class | bounded `<T>` | Prints a numbered menu of displayable items |
| `SearchEngine` | final utility | generic *method* | Filters any `Searchable` list by keyword |
| `Repository<T, ID>` | interface | `<T, ID>` | A generic CRUD contract (find/save/delete) |
| `PaginatedDisplay<T>` | class | `<T>` | Interactive paged browser for any list |

## Member by member

### `SearchResult<T>` — a "found it, and here's why" wrapper
Holds two things: the item that matched (`T getItem()`) and the name of the
field that matched (`String getMatchedField()`, e.g. `"title"` or `"author"`).
This lets a search UI show *why* a result matched, not just *what* matched.
Used by `LibraryService.searchItemsDetailed(...)`, which returns
`List<SearchResult<LibraryItem>>`.

### `PagedList<T>` — one page of a long list
Given the full list, a page number, and a page size, it slices out just that
page and answers questions about it:

| Method | Answer |
|--------|--------|
| `getItems()` | the items on this page |
| `getPage()` / `getPageSize()` / `getTotalItems()` | the paging numbers |
| `getTotalPages()` | how many pages in total |
| `hasNext()` / `hasPrevious()` | is there a page after / before this one |

It is the engine `PaginatedDisplay` uses under the hood.

### `Menu<T extends Displayable>` — a generic console menu
The `<T extends Displayable>` bound means: "T can be anything, *as long as* it
knows how to display itself." That guarantee lets the menu call
`option.toDisplayString()` on every item. `render()` prints a numbered list
plus a "0. Back"; `select(scanner)` reads the user's number and returns the
chosen item (or `null` if out of range). Used to list overdue-loan reports and
supplier financials.

```java
new Menu<>("Overdue Loans", ReportService.computeOverdueLoans()).render();
```

### `SearchEngine` — a generic search *method*
A `final` utility class (can't be instantiated — the private constructor throws).
Instead of a class-level `<T>`, the *method* is generic:

```java
public static <T extends Searchable> List<T> search(List<T> items, String query)
```

It streams the list and keeps the items where `item.matchesQuery(query)` is true.
Because `T` is bounded by `Searchable`, it works for any searchable type.
`LibraryService.searchItems` calls it with the library inventory.

### `Repository<T, ID>` — a generic storage contract
An **interface** with two placeholders: `T` (what's stored) and `ID` (the type of
its key). Four methods:

```java
List<T> findAll();
Optional<T> findById(ID id);
void save(T item);
void deleteById(ID id);
```

One class implements it: `SupplierRepository implements Repository<SupplierCompany, String>`.
It exists mostly to demonstrate the generic contract; a test
(`RepositoryContractTest`) exercises save/find/delete purely through the
`Repository` interface type, proving the contract works polymorphically.

### `PaginatedDisplay<T>` — an interactive paged browser
Takes a title, a list of any `T`, and a `renderer` callback
(`BiConsumer<T, Integer>` — given an item and its index, print it). Two modes:

- `showPaginated(scanner)` — an interactive loop with `[N]ext / [P]revious /
  jump-to-page / [Q]uit`, showing 10 per page (uses `PagedList<T>` internally).
- `showAll()` — dump every item at once, no paging.

Used to browse library items (`LibraryPrinter`) and the full user list
(`AdminUserManagement`).

## Why generics matter here
Without `<T>`, you'd need a separate `BookMenu`, `TicketMenu`, `LoanSearchResult`,
`TransactionPagedList`, and so on — one class per domain type. Generics let a
single implementation serve every current and future type. That is the
**Generics** grading criterion: reusable, type-safe building blocks that work for
any type the caller supplies.
