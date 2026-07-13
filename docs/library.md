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
All mutating methods now throw specific exceptions on failure instead of
returning a boolean. Callers catch `BaseException` to handle every case
uniformly.

| Method | Throws | Description |
|--------|--------|-------------|
| `searchItems(keyword)` | — | Keyword search; returns empty list for null/blank input. |
| `executeBorrow(itemId)` | `InsufficientCopiesException` (zero copies left), `NotFoundException` (bad id) | Decrements available copies and saves. |
| `executeReturn(itemId)` | `ConflictException` (already at max), `NotFoundException` (bad id) | Increments available copies and saves. |
| `updateItemQuantityFromCallCenter(itemId, qty)` | `NotFoundException` (bad id) | CallCenter adjusts stock through Support → Library. |
| `getAllItems()` | — | Full inventory snapshot. |
| `getAllSuppliers()` | — | Supplier list. |
| `getSupplierName(id)` | — | Supplier lookup by id. |
| `getItemById(id)` | — | Single-item lookup (returns `null` if missing). |
| `addItem(item)` | `ValidationException` (null item), `ConflictException` (duplicate id) | Adds a new catalog item and persists. |
| `deleteItem(id)` | `NotFoundException` (bad id) | Removes item and persists. |
| `updateItemPrice(id, price)` | `NotFoundException` (bad id), `ValidationException` (negative price) | Updates unit price and persists. |

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
