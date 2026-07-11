# Step 3 — Role Polymorphism (Implementation)

## What Changed

Replaced scattered `if(user.getRole() == UserRole.X)` **behavioral** checks in
`LibraryMemberConsole`, `FinanceMemberConsole`, and `SupportMemberConsole` with
a `UserProfile` class hierarchy.

```
UserProfile  (abstract)
├── GuestProfile
├── StudentProfile
├── TeacherProfile
├── CallCenterProfile
└── AdminProfile
```

`Persona.getUserProfile()` is the entry point — it returns the correct
subclass instance via `UserProfile.forRole(this.role)`.

---

## Important Design Decision: Data vs. Behavior

Not every `if(role == X)` in the codebase needed a class hierarchy. There are
two categories, and confusing them leads to duplicated data:

### Category 1 — Pure data lookup (already solved by the enum)

```java
// UserRole.java — already correct before Step 3
GUEST(2, "GST-"),
STUDENT(10, "STU-"),
...
```

`UserRole.STUDENT.getMaxBorrowLimit()` returning `10` is not a "missing
polymorphism" problem — the enum constant already carries its own data. This
**is** a form of polymorphism (each enum constant is technically its own
singleton instance), so `UserProfile` does not re-implement it.

`UserProfile.borrowLimit()` and `UserProfile.dashboardLabel()` are declared
`final` in the abstract base class and simply delegate to the wrapped
`UserRole`:

```java
public final int borrowLimit() {
    return role.getMaxBorrowLimit();
}

public final String dashboardLabel() {
    return role.name();
}
```

**Why this matters**: an earlier draft of this refactor mistakenly
re-hardcoded these numbers/strings inside each `GuestProfile`/`StudentProfile`/etc.
subclass. That created two sources of truth — if someone later changed
`UserRole.STUDENT`'s limit from `10` to `12`, the enum and the profile
subclass would silently disagree. Delegating to the enum keeps a single
source of truth.

### Category 2 — Behavioral branching (what Step 3 actually fixes)

These differ *qualitatively* by role — not just a stored number — and were
previously copy-pasted as `if(role==X)` across three unrelated console
classes:

| Method | What varies | Old location(s) of the duplicated check |
|--------|-------------|------------------------------------------|
| `canBorrow()` | CallCenter cannot borrow at all | (new — was implicit via limit `0`) |
| `canExtend()` | Guests cannot extend loans | `LibraryMemberConsole`, `FinanceMemberConsole` |
| `canRequestRoleUpgrade()` | Only Guests can request Student/Teacher | `SupportMemberConsole` |
| `isStaff()` | Admin/CallCenter vs. regular members | (new — used for future staff-only gating) |
| `supportMenuExtras()` | Which extra menu options a role sees | `SupportMemberConsole.printMenu` |

These stay `abstract` and are overridden per subclass because the logic is
genuinely different behavior, not a table lookup.

---

## Before / After

**Before** (`LibraryMemberConsole.printMenu`):
```java
private static void printMenu(UserRole role) {
    ConsoleMenu.banner("LIBRARY DASHBOARD (" + role.name() + ")");
    ...
    if (role != UserRole.GUEST) {
        ConsoleMenu.option("6", "Extend Return Date");
    }
    ...
}
```

**After**:
```java
private static void printMenu(UserProfile profile) {
    ConsoleMenu.banner("LIBRARY DASHBOARD (" + profile.dashboardLabel() + ")");
    ...
    if (profile.canExtend()) {
        ConsoleMenu.option("6", "Extend Return Date");
    }
    ...
}
```

The console no longer needs to import `UserRole` or know which specific role
is a Guest — it just asks the profile a yes/no behavioral question.

Same transformation applied to:
- `FinanceMemberConsole.printMenu` / `doExtend` — `canExtend()`
- `SupportMemberConsole.printMenu` / `requestRole` — `canRequestRoleUpgrade()`
- `LibraryMemberConsole.canBorrow` — `canBorrow()` + `borrowLimit()`

---

## Adding a New Role Now Requires

1. Add the constant to `UserRole` (with its borrow limit and prefix — data).
2. Create one new `UserProfile` subclass answering the behavioral questions.
3. Add one line to `UserProfile.forRole()`.

No changes to any console class. Previously, adding a role meant finding and
updating `if(role==X)` in at least three separate files.

---

## Verification

`./gradlew test` — all 121 tests pass (including the 3 CheckStyle/PMD
quality-gate tests), confirming the refactor is behavior-preserving.
