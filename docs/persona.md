# Persona Microservice

## Purpose
Owns user data: email/username, password, `UserRole`, member id, **wallet
balance**, profile, theme, **active status**, and the list of **borrowed item ids**
(the user's inventory). Persisted to the `personas` database table.
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

## Persona fields
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `email` | String | — | Primary key, unique |
| `username` | String | null | Optional username (system accounts) |
| `password` | String | — | Plain-text password |
| `role` | UserRole | GUEST | Current role |
| `memberId` | String | auto | Auto-generated with role prefix |
| `walletBalance` | int | 0 | Wallet in project currency units |
| `firstName` / `lastName` | String | null | Display name |
| `phoneNumber` | String | null | Contact phone |
| `theme` | String | "LIGHT" | UI theme (LIGHT / DARK) |
| `createdBy` | String | null | Email of the admin who created this account |
| `owner` | boolean | false | True for the Owner admin |
| **`active`** | **boolean** | **true** | **False = account deactivated, cannot log in** |
| `borrowedItemIds` | List\<String\> | [] | Currently borrowed item ids |
| `assignedSupportSections` | Set\<SupportSection\> | {} | CallCenter agent's assigned sections |

## Account activation/deactivation
Admins can deactivate any non-Admin user account. A deactivated account:
- Cannot log in (login throws `AccountDeactivatedException` after credential check)
- Can still be reactivated by an admin
- Admin and Owner accounts cannot be deactivated

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

## Admin management (`AdminManagementService`)
Split out of `PersonaService` for Owner/Admin hierarchy operations.

| Method | Throws | Description |
|--------|--------|-------------|
| `createAdmin(creator, email, password)` | `AuthorizationException` | Creates a new Admin; sets `createdBy` |
| `createCallCenter(creator, email, password)` | `AuthorizationException` | Creates a CallCenter agent |
| `deleteAdmin(deleter, email)` | `UserNotFoundException`, `AuthorizationException` | Deletes an admin (requires hierarchy permission) |
| `promoteAdmin(actor, email, newRole)` | `UserNotFoundException`, `AuthorizationException` | Changes a user's role |
| `resetPassword(actor, email, newPass)` | `UserNotFoundException`, `AuthorizationException` | Resets a user's password |
| `assignSupportSections(actor, email, sections)` | `UserNotFoundException`, `AuthorizationException` | Assigns ticket sections to a CallCenter agent |
| `toggleActive(actor, email)` | `UserNotFoundException`, `AuthorizationException` | Activates/deactivates an account; returns new state |
| `listAllUsers()` | — | Returns all personas |
| `searchUsers(keyword)` | — | Searches by name, email, id, or role |
| `editUserProfile(email, first, last, phone)` | `UserNotFoundException` | Updates profile fields |

**Hierarchy rule:** Only the Owner, or the Admin who personally created the
target admin, may manage (delete/promote/demote/reset) that admin. Nobody may
manage the Owner. Non-admin users can be managed by any Admin.

## My Inventory (`InventoryConsole`)
The Persona-owned "My Inventory" view. Library exposes a shortcut to it, but the
data belongs to Persona; it reads `LibraryService.getItemById` only to render
the owned items' details. This keeps discovery (Library) and ownership (Persona)
separated.

## Communications
Persona → Mail (notifications). Persona.InventoryConsole → Library (read-only).
Finance and Support call into Persona for wallet and role changes.
