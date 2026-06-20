# Library Microservice

## Purpose
Owns the catalog: typed items (`Book`, `EBook`, `Magazine`, `AudioBook`) supplied
by four `SupplierCompany` records, persisted XOR-encrypted to `library.enc`.
Library handles **discovery** (search/filter/details) and inventory counts;
**ownership** of borrowed copies lives in Persona ("My Inventory").

## Role-based experience
The UI layer (`LibraryConsole`) routes by `UserRole`:

| Role | Console | Capabilities |
|------|---------|--------------|
| Guest | `LibraryMemberConsole` | search, filter, details, borrow, return, My Inventory (limit 2) |
| Student | `LibraryMemberConsole` | member + extend return date (limit 10) |
| Teacher | `LibraryMemberConsole` | member + extend return date (limit 15) |
| CallCenter | `LibraryOperatorConsole` | search, add/edit, update quantities (via Support), suppliers, item statuses |
| Admin | `LibraryAdminConsole` | full CRUD, manage quantities, companies, HTML report, borrow stats, encrypted DB, debug |

Guests/Students/Teachers never see supplier companies, borrow statistics, or the
encrypted database — `LibraryPrinter` hides those fields for member views.

## Key service functions (`LibraryService`)
Existing (reused, **not** duplicated): `searchItems`, `executeBorrow`,
`executeReturn`, `updateItemQuantityFromCallCenter`, `getAllItems`,
`getAllSuppliers`, `getSupplierName`.

Added for the role dashboards:
| Method | Description |
|--------|-------------|
| `getItemById(id)` | Looks up a single item (used everywhere details are needed). |
| `addItem(LibraryItem)` | Adds a new catalog item if the id is unique; saves. |
| `deleteItem(id)` | Removes an item; saves; returns success. |
| `updateItemPrice(id, price)` | Edits unit price; saves. |

Supporting UI classes: `LibraryPrinter` (role-aware rendering, including
**paginated** result lists), `ItemEntry` (shared "add book" prompt used by both
Admin and CallCenter).

### Search-result pagination (`LibraryPrinter.printListPaginated`)
Search/browse/filter results are shown **10 per page**. Navigation accepts
`[N]ext`, `[P]revious`, a **page number to jump** (e.g. `1` jumps to page 1),
and `[Q]uit` to return to the menu. Navigation is bounded (no overflow past the
first/last page) and `Q` reliably exits.

## Communications
- Operator/Admin catalog changes route through **Support**
  (`SupportService.addLibraryItemViaSupport`, `handleCallCenterStockUpdate`) to
  honour the *CallCenter → Support → Library* workflow.
- **Report** reads `getAllItems`/`getAllSuppliers` to build the HTML report.
- **Persona.InventoryConsole** reads `getItemById` to show owned items.
- **Finance** is called on borrow/return to record/clear a `Loan`
  (`LoanService.recordLoan` / `clearLoan`) for the date-simulation overdue-fine
  feature.

## Mock data
Eleven seeded items across 4 suppliers (programming, academic, periodical,
fiction, reference). See `docs/mock_data.md`.

## Cross-process catalog visibility (updated)

`LibraryService` key operations (`getAllItems`, `searchItems`, `getItemById`,
`executeBorrow`, `executeReturn`, `addItem`) now call
`loadLibraryDatabaseEncrypted()` before accessing the catalog. An item added or
stock updated by the CallCenter / Admin in one instance is immediately visible
in a second instance.
