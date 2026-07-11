# Step 2 — Role-Based Access Control (RBAC) Before Polymorphism

## What This Step Covers

This document describes the **before** state of role-based access control in the project — how user roles are defined and how the system currently routes behaviour based on role. Step 3 refactors this into a true polymorphic design.

---

## The `UserRole` Enum

`src/main/java/ir/ac/kntu/persona/UserRole.java`

```java
public enum UserRole {
    ADMIN(Integer.MAX_VALUE, "ADM-"),
    CALLCENTER(0, "CC-"),
    TEACHER(15, "FAC-"),
    STUDENT(10, "STU-"),
    GUEST(2, "GST-");

    private final int maxBorrowLimit;
    private final String prefix;
}
```

Each role constant carries:

| Role | Max Borrows | Member ID Prefix | Can Extend? |
|------|-------------|-----------------|-------------|
| ADMIN | unlimited | `ADM-` | N/A (staff) |
| CALLCENTER | 0 (cannot borrow) | `CC-` | N/A (staff) |
| TEACHER | 15 | `FAC-` | Yes |
| STUDENT | 10 | `STU-` | Yes |
| GUEST | 2 | `GST-` | **No** |

---

## The `Persona` Class

`src/main/java/ir/ac/kntu/persona/Persona.java`

Stores a `UserRole role` field. Role is changed with:

```java
public void updateRole(UserRole newRole) {
    this.role = newRole;
    this.memberId = generateMemberId(newRole.getPrefix());
}
```

Role change also regenerates the member ID with the new prefix. This is called by `PersonaService.promoteRole()` when an Admin approves a guest's role-upgrade request.

---

## How Role Routing Currently Works

The system checks `user.getRole()` at the console routing layer, using `switch`:

### Library (`LibraryConsole.java`)
```java
switch (user.getRole()) {
    case ADMIN:    LibraryAdminConsole.open(scanner);       break;
    case CALLCENTER: LibraryOperatorConsole.open(scanner); break;
    default:       LibraryMemberConsole.open(scanner, user); break;
}
```

### Finance (`FinanceConsole.java`)
```java
switch (user.getRole()) {
    case ADMIN:    FinanceAdminConsole.open(scanner);         break;
    case CALLCENTER: FinanceOperatorConsole.open(scanner);   break;
    default:       FinanceMemberConsole.open(scanner, user); break;
}
```

### Support (`SupportConsole.java`)
```java
switch (user.getRole()) {
    case ADMIN:    AdminInbox.open(scanner, user);         break;
    case CALLCENTER: CallCenterInbox.open(scanner, user); break;
    default:       SupportMemberConsole.open(scanner, user); break;
}
```

---

## Role-Gated Menu Items (Inline `if` Checks)

Beyond routing, individual menu items are conditionally shown using `if(role == X)`:

### `LibraryMemberConsole.printMenu()`
```java
if (role != UserRole.GUEST) {
    ConsoleMenu.option("6", "Extend Return Date");
}
```
Guests do not see the "Extend" option.

### `FinanceMemberConsole.printMenu()`
```java
if (role != UserRole.GUEST) {
    ConsoleMenu.option("3", "Extend Return Date");
}
```

### `FinanceMemberConsole.doExtend()`
```java
if (user.getRole() == UserRole.GUEST) {
    ConsoleColor.printError("Guests cannot extend return dates.");
    return;
}
```

### `SupportMemberConsole.printMenu()`
```java
if (role == UserRole.GUEST) {
    ConsoleMenu.option("1", "Request Student Role");
    ConsoleMenu.option("2", "Request Teacher Role");
}
```
Guests see role-upgrade options; Students and Teachers do not.

---

## Borrow Limit Check

In `LibraryMemberConsole.canBorrow()`:
```java
if (user.getBorrowCount() >= user.getRole().getMaxBorrowLimit()) {
    ConsoleColor.printError("Borrow limit reached for role " + user.getRole().name());
    return false;
}
```

This reads `maxBorrowLimit` from the `UserRole` enum. The logic is centralized in one place but the limit itself is baked into the enum constant.

---

## Permission Checks in Services

In `SupportService`:
```java
// CALLCENTER-only stock update
if (current.getRole() == UserRole.CALLCENTER) {
    LibraryService.updateItemQuantityFromCallCenter(itemId, quantity);
}

// Both ADMIN and CALLCENTER can add items
boolean allowed = current.getRole() == UserRole.CALLCENTER
    || current.getRole() == UserRole.ADMIN;
```

In `ReportService`:
```java
return user.getRole() == UserRole.ADMIN
    || user.getRole() == UserRole.CALLCENTER;
```

---

## Problems with This Approach

| Issue | Example |
|-------|---------|
| **Scattered logic** | Guest check for extend is in `LibraryMemberConsole`, `FinanceMemberConsole`, AND `SupportMemberConsole` |
| **Tight coupling** | Consoles import `UserRole` and compare constants directly |
| **Fragile extension** | Adding a new role means finding and updating every `if(role==X)` and every `switch` |
| **Data in wrong place** | "Can this role extend?" is behaviour, but it lives as a missing piece in the enum constant |

---

## What Step 3 Changes

Step 3 extracts all role-specific behaviour into a `UserProfile` class hierarchy:

```
UserProfile  (abstract)
├── GuestProfile
├── StudentProfile
├── TeacherProfile
├── CallCenterProfile
└── AdminProfile
```

Each subclass overrides methods like `canExtend()`, `borrowLimit()`, `canBorrow()`, `dashboardLabel()`.

The `Persona` class gets a `getUserProfile()` factory method that returns the right subclass based on the stored `UserRole`. The consoles then call `user.getUserProfile().canExtend()` instead of `user.getRole() == UserRole.GUEST`.

This means adding a new role only requires:
1. A new `UserProfile` subclass
2. One entry in `getUserProfile()`

No console code changes needed.
