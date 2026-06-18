# Library Tests

## `LibraryServiceTest`
| Test | Explanation |
|------|-------------|
| `inventorySeededAndSuppliersAligned` | Inventory is seeded, there are exactly **4** suppliers, and every item resolves to a non-empty supplier name. |
| `supplierNameLookup` | Known supplier id maps to its name; unknown id is returned as-is. |
| `searchMatchesTitleOrCategory` | Search matches by title/category (case-insensitive); blank/null returns empty. |
| `borrowAndReturnAdjustsAvailability` | Borrow decrements and return increments available copies. |
| `borrowUnknownItemFails` | Borrowing a non-existent item returns `false`. |
| `callCenterStockUpdateIncreasesCopies` | `updateItemQuantityFromCallCenter` raises total copies. |
| `pricingAndBorrowedDerivation` | Borrowed = total − available and price is non-negative for all items. |

## `ItemModelsTest`
| Test | Explanation |
|------|-------------|
| `bookIsPhysicalWithMetadata` | `Book` is a physical item exposing author/ISBN/shelf/condition. |
| `ebookIsDigital` | `EBook` is a digital item with page count and download metadata. |
| `magazineAndAudioBook` | `Magazine` (issue number) and `AudioBook` (narrator, duration) behave correctly. |
| `supplierCompanyGetters` | `SupplierCompany` returns its id and name. |

## `LibraryAdminOpsTest` (new)
Covers the catalog operations and expanded mock data added for the Admin/operator
dashboards.

| Test | Explanation |
|------|-------------|
| `getItemByIdFindsSeededItem` | `getItemById` finds a seeded item; unknown/null ids return `null`. |
| `expandedMockDataIsSeeded` | At least 10 items are seeded (the enriched mock data set). |
| `addAndDeleteItem` | `addItem` accepts a unique id and rejects duplicates; `deleteItem` removes it and then returns `false`. |
| `updatePrice` | `updateItemPrice` updates a valid price and rejects negative prices / missing items. |
