# Admin & Owner Hierarchy

## In plain terms
Not all admins are equal. There is one special admin — the **Owner** — who can
manage everyone. Every other admin can only manage the accounts *they themselves
created*. This stops one admin from deleting or resetting another admin they had
nothing to do with. This doc explains how that rule is built and enforced.

For the plain field/method reference see `docs/persona.md`; this doc focuses on
the *hierarchy logic*.

---

## The three ideas that make the hierarchy work

### 1. There is no "Owner" role — the Owner is just an admin with a flag
`UserRole` has no `OWNER` value. Instead, a `Persona` carries a boolean:

```java
private boolean owner = false;   // getter isOwner(), setter setOwner(...)
```

The Owner is simply the one `ADMIN` whose `owner` flag is `true`. Only two places
ever set it to `true`, both at startup:
- `bootstrapDefaultAdmin()` — when the database has no admin at all, it creates
  `admin@system.local` and marks it the Owner.
- `promoteOldestAdminToOwner()` — for older data with admins but no Owner, the
  first admin that has no creator (`createdBy == null`) becomes the Owner.

### 2. Every admin remembers who created it (`createdBy`)
When an admin creates another admin, the new account is stamped with the
creator's email:

```java
// inside createAdmin(creator, email, password)
newAdmin.updateRole(UserRole.ADMIN);
newAdmin.setCreatedBy(creator.getEmail());   // <-- the stamp
```

`createdBy` is the whole basis of "you may only manage admins you created." The
bootstrapped Owner has `createdBy == null` (nobody created it).

> Note: `createCallCenter(...)` does **not** stamp `createdBy` — only admins get
> the creator stamp, because only admin-on-admin management uses it.

### 3. One permission check reads the flag and the stamp
`requireCanManageAdmin(manager, target)` is the gatekeeper for admin-on-admin
actions:

```java
private static void requireCanManageAdmin(Persona manager, Persona target) {
    if (target.isOwner()) {
        throw new AuthorizationException("The Owner cannot be managed by another Admin.");
    }
    boolean isOwner = manager.isOwner();
    boolean createdTarget = manager.getEmail() != null
            && manager.getEmail().equalsIgnoreCase(target.getCreatedBy());
    if (!isOwner && !createdTarget) {
        throw new AuthorizationException("You are not authorized to manage this admin.");
    }
}
```

In plain terms, an admin may manage a *target admin* only if **either**:
- the manager is the Owner, **or**
- the manager's email matches the target's `createdBy` stamp (case-insensitive).

And nobody — not even another Owner-less path — can manage the Owner.

---

## Two kinds of targets: admins vs. everyone else

Managing a plain member (student/teacher/guest/callcenter) is looser than
managing an admin. `requireCanManageTarget` routes to the right rule:

```java
private static void requireCanManageTarget(Persona actor, Persona target, String action) {
    if (target.getRole() == UserRole.ADMIN) {
        requireCanManageAdmin(actor, target);        // strict: owner-or-creator
    } else if (actor.getRole() != UserRole.ADMIN) {
        throw new AuthorizationException("Only Admins can " + action + ".");
    }
}
```

- **Target is an admin** → the strict owner-or-creator rule above.
- **Target is a normal user** → any admin will do.

---

## What each operation allows

| Operation | Method | Who may do it | Owner protection |
|-----------|--------|---------------|------------------|
| Create admin | `createAdmin` | any admin | — |
| Create callcenter | `createCallCenter` | any admin | — |
| Delete admin | `deleteAdmin` | Owner or the admin's creator | Owner can't be deleted |
| Change role (promote/demote) | `promoteAdmin` | any admin for members; owner/creator for admins | Owner can't be changed |
| Demote admin | `demoteAdmin` | Owner or creator (target must be admin) | Owner can't be demoted |
| Reset password | `resetPassword` | any admin for members; owner/creator for admins | Owner protected |
| Activate / deactivate | `toggleActive` | any admin for members; owner/creator for admins | Owner can't be deactivated (explicit block) |
| Delete user | `deleteUser` | any admin; owner/creator if target is admin | Owner can't be deleted (explicit block) |

Two operations add an **explicit** Owner guard on top of the shared check —
`toggleActive` ("The Owner account cannot be deactivated.") and `deleteUser`
("The Owner account cannot be deleted.") — so the Owner is safe even for
non-admin-typed code paths.

> Naming quirk: `promoteAdmin` is the general "change this user's role" entry
> point used by the UI's *Promote / Demote User* option — it handles both
> directions. A separate `demoteAdmin` method exists but the inbox UI drives
> role changes through `promoteAdmin`.

---

## Member IDs change with the role
When a role changes, `updateRole(...)` regenerates the member id using the new
role's prefix (`ADM-`, `CC-`, `FAC-`, `STU-`, `GST-`) plus a random 6-digit
number. So promoting a student to admin gives them a fresh `ADM-######` id.

---

## Where you drive it in the UI
`AdminInbox` (the admin support console) is the entry point:
- **Option 10 "Manage Admins/Callcenters"** → create admin, create callcenter,
  delete admin, reset password.
- **Option 11 "User Management"** → `AdminUserManagement`: view/search users,
  edit profile, promote/demote, reset password, activate/deactivate, delete user.
- **Option 12 "Assign CallCenter Support Sections"** → see
  `docs/callcenter-support-sections.md`.

---

## Startup seeding recap
On first run (`PersonaService` static load):
1. No admin exists → create `admin@system.local` (password from
   `DEFAULT_ADMIN_PASSWORD`, default `adminpass`) and mark it **Owner**.
2. Admins exist but none is Owner → promote the oldest creator-less admin.
3. No callcenter exists → create `callcenter@system.local` (password from
   `DEFAULT_CALLCENTER_PASSWORD`, default `ccpass`) with no assigned sections.
