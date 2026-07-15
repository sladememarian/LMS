# Polymorphism Patterns

## In plain terms
Polymorphism means "many forms" — the same method call does different things
depending on which object receives it. Instead of asking "what type is this?"
and branching with `if`/`switch`, you call one method and let each type answer
in its own way. This project uses polymorphism in five distinct patterns, each
solving a different design problem.

---

## 1. LibraryItem hierarchy (Template Method)

The library catalog is an inheritance tree rooted at `LibraryItem`:

```
LibraryItem (abstract) — implements Borrowable, Searchable
├── PhysicalItem (abstract) — implements Reservable, canReserve() = true
│   ├── Book       borrowPeriod = 14 days
│   └── Magazine   borrowPeriod = 7 days
└── DigitalItem (abstract) — canReserve() = false
    ├── EBook      borrowPeriod = 21 days, actions include "Download"
    └── AudioBook  borrowPeriod = 21 days, actions include "Stream"
```

**Abstract extension points** — each leaf answers differently:

| Method | Book | Magazine | EBook | AudioBook |
|--------|------|----------|-------|-----------|
| `borrowPeriod()` | 14 | 7 | 21 | 21 |
| `canReserve()` | true | true | false | false |
| `getItemType()` | "BOOK" | "MAGAZINE" | "EBOOK" | "AUDIOBOOK" |
| `displayInfo()` | "Author: …" | "Issue #…" | "Pages: …" | "Narrator: … (X min)" |
| `availableActions()` | base | base | base + "Download" | base + "Stream" |

**Where `canReserve()` comes from:** `PhysicalItem` implements the `Reservable`
interface and returns `true`, so both `Book` and `Magazine` are reservable.
`DigitalItem` returns `false`, so `EBook` and `AudioBook` are not. (Book and
Magazine simply inherit `true` from `PhysicalItem`; neither overrides it.)

**Why it matters:** `LibraryPrinter.printDetails()` calls `item.borrowPeriod()`,
`item.canReserve()`, and `item.displayInfo()` without knowing the concrete type.
Adding a new item type (say `DVD`) means writing one new leaf class — zero
changes to the printer or any service.

---

## 2. Role Profiles (Strategy Pattern)

Instead of `switch(role)` blocks scattered through the UI, each role is a
`UserProfile` subclass that encapsulates all role-specific behavior:

```
UserProfile (abstract)
├── AdminProfile       canBorrow=true,  canExtend=true,  isStaff=true,  reservationLimit=0
├── CallCenterProfile  canBorrow=false, canExtend=false, isStaff=true,  reservationLimit=0
├── TeacherProfile     canBorrow=true,  canExtend=true,  isStaff=false, reservationLimit=10
├── StudentProfile     canBorrow=true,  canExtend=true,  isStaff=false, reservationLimit=5
└── GuestProfile       canBorrow=true,  canExtend=false, isStaff=false, reservationLimit=2
```

Note: `canBorrow()` (may this role borrow at all?) is separate from
`canReserve()` (may this role place reservations). Only `AdminProfile` and
`CallCenterProfile` return `canReserve()=false`; the three member roles return
`true`.

The factory `UserProfile.forRole(role)` selects the right profile. Console code
just calls polymorphic methods:

```java
UserProfile profile = user.getUserProfile();  // works for ANY role, no switch
if (profile.canExtend()) { ... }
profile.openLibraryConsole(scanner, user);     // routes to Admin/Operator/Member console
```

`Persona.getUserProfile()` is the bridge: it calls the factory with the user's
current role, so the same `Persona` gets different behavior as its role changes.
Staff profiles also override the `open*Console` methods to route to their own
dashboards (e.g. `AdminProfile` opens `LibraryAdminConsole`).

---

## 3. Interface Segregation

Small, focused interfaces let unrelated classes share behavior contracts:

| Interface | Contract | Real implementors |
|-----------|----------|-------------------|
| `Borrowable` | `canBorrow()` | `LibraryItem` (all item types) |
| `Searchable` | `matchesQuery(String)` | `LibraryItem` (all item types) |
| `Reservable` | `canReserve()` | `PhysicalItem` (Book, Magazine) |
| `Displayable` | `toDisplayString()` | `SupportTicket`, `SupplierFinancials`, `OverdueLoanReport` |
| `RequestAssignable<T>` | `assign(T)`, `getAssignee()`, `isAssigned()` | *none — declared but unused* |

**Polymorphic consumers:**
- `ConsoleMenu.printAll(List<? extends Displayable>)` renders any `Displayable`
  list without knowing the concrete type.
- `SearchEngine.search(List<T extends Searchable>, String)` searches any
  searchable collection generically.
- `PaginatedDisplay<T>` paginates any list, rendering via a
  `BiConsumer<T, Integer>` callback.

See `docs/interfaces.md` for the full contract-by-contract breakdown.

---

## 4. Exception Hierarchy (Catch-Grain Discrimination)

A tree of exception types lets callers catch at any level of granularity:

```
RuntimeException
└── BaseException
    ├── ValidationException
    │   ├── InvalidEmailFormatException
    │   ├── InvalidPasswordException
    │   ├── InvalidPhoneFormatException
    │   ├── InvalidThemeException
    │   ├── InvalidVerificationCodeException
    │   └── InsufficientFundsException
    ├── NotFoundException
    │   └── UserNotFoundException
    ├── AuthorizationException
    │   └── InvalidCredentialsException
    ├── ConflictException
    │   ├── InsufficientCopiesException
    │   └── DuplicateEmailException
    └── AccountDeactivatedException
```

- **Catch `BaseException`** → handle every application error uniformly (what all
  UI controllers do).
- **Catch `ValidationException`** → handle all input-validation failures with one
  message style.
- **Catch `InsufficientCopiesException`** → handle one specific business rule
  with a custom message.

No code is duplicated: each type exists only to enable finer-grained handling
when a caller wants it.

---

## 5. Persona Roles (Data-Driven Polymorphism)

`UserRole` is an enum that carries per-role *data* (the numbers), while
`UserProfile` carries per-role *behavior* (the yes/no decisions). The enum is the
single source of truth for the data:

| Role | Max Borrow | ID Prefix | Reservation Limit |
|------|-----------|-----------|-------------------|
| GUEST | 2 | GST- | 2 |
| STUDENT | 10 | STU- | 5 |
| TEACHER | 15 | FAC- | 10 |
| CALLCENTER | 0 | CC- | 0 |
| ADMIN | unlimited (`Integer.MAX_VALUE`) | ADM- | 0 |

`UserProfile.borrowLimit()` and `dashboardLabel()` just read from the enum (no
per-subclass override), so the borrow limits and prefixes live in exactly one
place. The behavioral methods (`canBorrow()`, `canReserve()`,
`reservationLimit()`) are the ones each subclass overrides. This split replaces
what would otherwise be scattered role constants plus `if/else` chains.

---

## Summary

| Pattern | Where | What it replaces |
|---------|-------|------------------|
| Template Method | `LibraryItem` hierarchy | `if (type == "BOOK")` branches in printer/service code |
| Strategy | `UserProfile` subclasses | `switch(role)` blocks in every console |
| Interface Segregation | `Borrowable`, `Searchable`, `Displayable`, … | `instanceof` type-checking chains |
| Exception Hierarchy | `BaseException` tree | Catch-all `catch (Exception)` with string matching |
| Data-Driven Enum | `UserRole` + `UserProfile.forRole()` | Scattered role constants and limit tables |
