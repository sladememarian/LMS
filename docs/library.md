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

Supporting UI classes: `LibraryPrinter` (role-aware rendering), `ItemEntry`
(shared "add book" prompt used by both Admin and CallCenter).

## Communications
- Operator/Admin catalog changes route through **Support**
  (`SupportService.addLibraryItemViaSupport`, `handleCallCenterStockUpdate`) to
  honour the *CallCenter → Support → Library* workflow.
- **Report** reads `getAllItems`/`getAllSuppliers` to build the HTML report.
- **Persona.InventoryConsole** reads `getItemById` to show owned items.

## Mock data
Eleven seeded items across 4 suppliers (programming, academic, periodical,
fiction, reference). See `docs/mock_data.md`.
