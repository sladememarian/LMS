# exception Package (Phase 2)

## In plain terms
This package is the app's "family tree" of error types. Instead of throwing
generic `IllegalArgumentException` everywhere (which tells you *that*
something went wrong but not *what kind* of thing), every error now has its
own specific, descriptively-named class.

Think of it like a hospital's triage categories instead of one big "sick"
label — `ValidationException` (bad input), `NotFoundException` (record
doesn't exist), `ConflictException` (already exists / already done),
`AuthorizationException` (not allowed to do that).

## The hierarchy
```
RuntimeException
  └─ BaseException                     (root of every custom exception in the app)
       ├─ ValidationException          (input is malformed / doesn't meet rules)
       │    ├─ InvalidPasswordException
       │    ├─ InvalidVerificationCodeException
       │    ├─ InvalidEmailFormatException
       │    ├─ InvalidPhoneFormatException
       │    ├─ InvalidThemeException
       │    └─ InsufficientFundsException   (wallet has no cover — Finance)
       ├─ AuthorizationException       (wrong credentials / not permitted)
       │    └─ InvalidCredentialsException
       ├─ NotFoundException            (looked for something that doesn't exist)
       │    └─ UserNotFoundException   (email/member-id has no matching persona)
       └─ ConflictException            (action conflicts with existing state)
            ├─ DuplicateEmailException (email already registered — Persona)
            └─ InsufficientCopiesException  (zero copies left — Library)
```

> **Note:** `DatabaseException` (in `ir.ac.kntu.util`) extends `RuntimeException`
> directly — it is *not* part of the `BaseException` tree and is not caught by
> the single catch block described below. It is reserved for unrecoverable
> database-level failures.

Every exception has two constructors, matching the existing `DatabaseException`
style already used in the project:
- `new SomeException("message")`
- `new SomeException("message", causeThrowable)`

## Why this matters for the console UI
Because every specific exception (e.g. `UserNotFoundException`) extends one of
the 4 categories, and all 4 extend `BaseException`, every console layer can
catch everything with **one single catch block**:

```java
try {
    IamService.registerUser(credentials);
} catch (BaseException ex) {
    ConsoleColor.printError(ex.getMessage());
}
```

No matter which specific exception was thrown deep inside a service, the
console always knows how to handle it the same way: print the message, don't
crash, keep the menu running. This is **polymorphism** in action — the catch
block treats many different exception types uniformly through their shared
parent type.

This pattern is used across **all** console layers: `LibraryMemberConsole`,
`LibraryAdminConsole`, `LibraryOperatorConsole`, `FinanceAdminConsole`,
`FinanceMemberConsole`, `AdminInbox`, `CallCenterInbox`, and
`InventoryConsole`.

## Where the concrete exceptions live

| Module | Exception | Extends | Thrown by |
|--------|-----------|---------|-----------|
| **IAM** | `InvalidCredentialsException` | `AuthorizationException` | `IamService` |
| **Persona** | `DuplicateEmailException` | `ConflictException` | `PersonaService` |
| **Persona** | `UserNotFoundException` | `NotFoundException` | `PersonaService` |
| **SSO** | `InvalidPasswordException` | `ValidationException` | `SsoService` |
| **SSO** | `InvalidPhoneFormatException` | `ValidationException` | `SsoService` |
| **SSO** | `InvalidThemeException` | `ValidationException` | `SsoService` |
| **SSO** | `InvalidEmailFormatException` | `ValidationException` | `IamService` |
| **SSO** | `InvalidVerificationCodeException` | `ValidationException` | `IamService` |
| **Library** | `InsufficientCopiesException` | `ConflictException` | `LibraryService` |
| **Library** | `NotFoundException` | `BaseException` | `LibraryService` |
| **Library** | `ConflictException` | `BaseException` | `LibraryService` |
| **Library** | `ValidationException` | `BaseException` | `LibraryService` |
| **Finance** | `InsufficientFundsException` | `ValidationException` | `FinanceService` |
| **Report** | `AuthorizationException` | `BaseException` | `ReportService` |
| **Support** | `ValidationException` | `BaseException` | `SupportService` |
| **Support** | `NotFoundException` | `BaseException` | `SupportService`, `RoleRequestService` |
| **Support** | `AuthorizationException` | `BaseException` | `SupportService` |
| **Support** | `ConflictException` | `BaseException` | `RoleRequestService` |
| **Reservation** | `NotFoundException` | `BaseException` | `ReservationService` |
| **Reservation** | `ConflictException` | `BaseException` | `ReservationService` |
| **Reservation** | `ValidationException` | `BaseException` | `ReservationService` |

See the module-specific docs (`iam.md`, `persona.md`, `sso.md`, `library.md`,
`finance.md`, `support.md`, `report.md`) for the full behaviour of each
service and exactly when each exception fires.
