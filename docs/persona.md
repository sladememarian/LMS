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
| Method | Throws | Description |
|--------|--------|-------------|
| `updateProfile(email, first, last, phone)` | `UserNotFoundException` | Updates name and phone; persists. |
| `updatePassword(email, newPassword)` | `UserNotFoundException` | Changes password; persists. |
| `updateTheme(email, theme)` | `UserNotFoundException` | Sets LIGHT/DARK theme; persists. |
| `updateWalletBalance(email, amount)` | `UserNotFoundException` | Adjusts balance and syncs current session. |
| `recordBorrow(email, itemId)` | `UserNotFoundException` | Adds an owned item and persists. |
| `recordReturn(email, itemId)` | `UserNotFoundException` | Removes an owned item and persists. |
| `promoteRole(email, role)` | `UserNotFoundException` | Applies an approved role change and persists. |
| `getWalletBalance(email)` | `UserNotFoundException` | Returns current balance. |
| `getProfileByMemberId(memberId)` | — | Looks up a user by member id (returns `null` if missing). |

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

## Cross-process awareness (updated)

`validateCredentials`, `getProfileByMemberId`, and `getProfileByUsername` now
always reload `persona_secure.json` before searching, so staff login and member
lookup work correctly across simultaneously running instances. `promoteRole` also
reloads before promoting so the target user (registered in a separate instance)
is found. Write operations (`updateWalletBalance`, `recordBorrow`, `recordReturn`,
`updateProfile`, etc.) use the existing in-memory reference and then sync the
`currentUser` display object — preserving wallet balance display correctness.
