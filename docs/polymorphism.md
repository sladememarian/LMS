# Polymorphism Patterns

## In plain terms
Polymorphism means "many forms" — the same method call produces different
behavior depending on which object receives it. This project uses polymorphism
in five distinct patterns, each solving a different design problem.

---

## 1. LibraryItem hierarchy (Template Method)

The library catalog is an inheritance tree rooted at `LibraryItem`:

```
LibraryItem (abstract) — implements Borrowable, Searchable
├── PhysicalItem (abstract) — implements Reservable
│   ├── Book       borrowPeriod=14 days
│   └── Magazine   borrowPeriod=7 days, canReserve() overridden to false
└── DigitalItem (abstract)
    ├── EBook      borrowPeriod=21 days, actions include "Download"
    └── AudioBook  borrowPeriod=21 days, actions include "Stream"
```

**Abstract extension points** that each leaf implements differently:

| Method | Book | Magazine | EBook | AudioBook |
|--------|------|----------|-------|-----------|
| `borrowPeriod()` | 14 | 7 | 21 | 21 |
| `canReserve()` | true (from PhysicalItem) | **false** (overridden) | false (from DigitalItem) | false |
| `getItemType()` | "BOOK" | "MAGAZINE" | "EBOOK" | "AUDIOBOOK" |
| `displayInfo()` | "Author: ..." | "Issue #..." | "Pages: ..." | "Narrator: ... (X min)" |
| `availableActions()` | base | base | base+"Download" | base+"Stream" |

**Key pattern — cancel-inheritance:** `Magazine` extends `PhysicalItem`
(which sets `canReserve()=true`) but overrides it to `false`, opting out
of parent behavior. This lets the type hierarchy carry the general rule
(physical items are reservable) while allowing specific exceptions.

**Why it matters:** `LibraryPrinter.printDetails()` calls
`item.borrowPeriod()`, `item.canReserve()`, and `item.displayInfo()`
without knowing which concrete type it holds. Adding a new item type
(e.g. `DVD`) requires only creating a new leaf class — zero changes to
the printer or any service code.

---

## 2. Role Profiles (Strategy Pattern)

Instead of `switch(role)` blocks scattered through the UI, each role is
represented by a `UserProfile` subclass that encapsulates all
role-specific behavior:

```
UserProfile (abstract)
├── AdminProfile       canBorrow=false, canExtend=true,  isStaff=true
├── CallCenterProfile  canBorrow=false, canExtend=false, isStaff=true
├── TeacherProfile     canBorrow=true,  canExtend=true,  reservationLimit=10
├── StudentProfile     canBorrow=true,  canExtend=true,  reservationLimit=5
└── GuestProfile       canBorrow=true,  canExtend=false, reservationLimit=2
```

The factory `UserProfile.forRole(role)` selects the right profile. Console
code calls polymorphic methods:

```java
// This works for ANY role — no switch needed
UserProfile profile = user.getUserProfile();
if (profile.canExtend()) { ... }
profile.openLibraryConsole(scanner, user);  // routes to Admin or Member console
```

`Persona.getUserProfile()` is the bridge: it calls the factory with the
user's current role, so the same `Persona` object gets different behavior
as its role changes.

---

## 3. Interface Segregation

Small, focused interfaces let unrelated classes share behavior contracts:

| Interface | Contract | Implementors |
|-----------|----------|-------------|
| `Borrowable` | `canBorrow()` | `LibraryItem` (all types) |
| `Searchable` | `matchesQuery(String)` | `LibraryItem`, `SupportTicket` |
| `Reservable` | `canReserve()` | `PhysicalItem` (Book, Magazine) |
| `Displayable` | `toDisplayString()` | `SupportTicket`, `SupplierFinancials`, `OverdueLoanReport` |
| `RequestAssignable<T>` | `assign(T)`, `getAssignee()`, `isAssigned()` | `SupportTicket` |

**Polymorphic consumers:**
- `ConsoleMenu.printAll(List<? extends Displayable>)` renders any
  `Displayable` list without knowing the concrete type.
- `SearchEngine.search(List<T extends Searchable>)` searches any
  searchable collection generically.
- `PaginatedDisplay<T>` paginates any list, rendering via a
  `BiConsumer<T, Integer>` callback.

---

## 4. Exception Hierarchy (Catch-Grain Discrimination)

A 3-level tree lets callers catch at any granularity:

```
RuntimeException
└── BaseException
    ├── ValidationException
    │   ├── InvalidEmailFormatException
    │   ├── InvalidPasswordException
    │   ├── InsufficientFundsException
    │   └── ...
    ├── NotFoundException
    │   └── UserNotFoundException
    ├── AuthorizationException
    ├── ConflictException
    │   ├── InsufficientCopiesException
    │   └── DuplicateEmailException
    └── AccountDeactivatedException
```

- **Catch `BaseException`** → handles every application error uniformly
  (used by all UI controllers).
- **Catch `ValidationException`** → handles all input-validation failures
  with one message style.
- **Catch `InsufficientCopiesException`** → handles a specific business
  rule with a custom message.

No code is duplicated: each exception type exists only to enable finer-grained
handling when needed.

---

## 5. Persona Roles (Data-Driven Polymorphism)

`UserRole` is an enum carrying per-role data:

| Role | Max Borrow | ID Prefix | Can Reserve |
|------|-----------|-----------|-------------|
| GUEST | 2 | GST- | yes (limit 2) |
| STUDENT | 10 | STU- | yes (limit 5) |
| TEACHER | 15 | FAC- | yes (limit 10) |
| CALLCENTER | 0 | CC- | no |
| ADMIN | unlimited | ADM- | no |

`UserProfile.forRole()` maps the enum to a profile instance, and the
profile's polymorphic methods (`canBorrow()`, `canReserve()`,
`reservationLimit()`) replace what would otherwise be `if/else` chains.

---

## Summary

| Pattern | Where | What it replaces |
|---------|-------|------------------|
| Template Method | `LibraryItem` hierarchy | `if (type == "BOOK")` branches in printer/service code |
| Strategy | `UserProfile` subclasses | `switch(role)` blocks in every console |
| Interface Segregation | `Borrowable`, `Searchable`, `Displayable` | Type-checking `instanceof` chains |
| Exception Hierarchy | `BaseException` tree | Catch-all `catch (Exception)` with string matching |
| Data-Driven Enum | `UserRole` + `UserProfile.forRole()` | Scattered role constants and limit tables |
