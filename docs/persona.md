# Persona Microservice

## Purpose
Owns user data: email/username, password, `UserRole`, member id, **wallet
balance**, profile, theme and — newly — the list of **borrowed item ids**
(the user's inventory). Persisted XOR-encrypted to `persona_secure.json`.
Two default staff accounts are seeded: `admin` (ADMIN) and `callcenter`
(CALLCENTER).

## Roles & borrow limits (`UserRole`)
| Role | Max borrow | Prefix |
|------|-----------|--------|
| ADMIN | unlimited | ADM- |
| CALLCENTER | 0 | CC- |
| TEACHER | 15 | FAC- |
| STUDENT | 10 | STU- |
| GUEST | 2 | GST- |

## Inventory (added)
`Persona` now tracks borrowed items:
`getBorrowedItemIds`, `getBorrowCount`, `hasBorrowed`, `addBorrowedItem`,
`removeBorrowedItem`. The list is persisted as a pipe-separated `borrowed` field.

`PersonaService` additions:
| Method | Description |
|--------|-------------|
| `recordBorrow(email, itemId)` | Adds an owned item and persists. |
| `recordReturn(email, itemId)` | Removes an owned item and persists. |
| `promoteRole(email, role)` | Applies an approved role change and persists. |

Existing wallet functions reused by Finance: `getWalletBalance`,
`updateWalletBalance`, `transferToAdmin`.

## My Inventory (`InventoryConsole`)
The Persona-owned "My Inventory" view. Library exposes a shortcut to it, but the
data belongs to Persona; it reads `LibraryService.getItemById` only to render
the owned items' details. This keeps discovery (Library) and ownership (Persona)
separated.

## Communications
Persona → Mail (notifications). Persona.InventoryConsole → Library (read-only).
Finance and Support call into Persona for wallet and role changes.
