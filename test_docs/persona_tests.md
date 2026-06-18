# Persona Tests

## `PersonaServiceTest`
| Test | Explanation |
|------|-------------|
| `defaultStaffAccountsExist` | The seeded `admin` and `callcenter` accounts exist with ADMIN / CALLCENTER roles. |
| `registerAndValidate` | A registered persona is GUEST and validates only with the correct password. |
| `updateProfileAndTheme` | Profile fields and theme update and persist. |
| `updatePasswordChangesCredentials` | Password update succeeds for a known email and fails for an unknown one. |
| `walletOperations` | Wallet starts at 0 and `updateWalletBalance` adds funds. |
| `getProfileUnknownReturnsNull` | Unknown email returns `null`. |

## `PersonaInventoryTest` (new)
Covers the per-user inventory and role-promotion helpers added for the role-based
Library experience.

| Test | Explanation |
|------|-------------|
| `personaTracksBorrowedItems` | `addBorrowedItem` / `removeBorrowedItem` / `hasBorrowed` / `getBorrowCount` behave correctly on a Persona. |
| `blankBorrowIdsAreIgnored` | Null/empty item ids are not added (defensive). |
| `recordBorrowAndReturnPersist` | `PersonaService.recordBorrow` then `recordReturn` add/remove the owned item and persist; returning an unowned id returns `false`. |
| `promoteRoleChangesRoleAndPrefix` | `promoteRole` upgrades a GUEST to STUDENT (member id prefix becomes `STU-`); an unknown email returns `false`. |
