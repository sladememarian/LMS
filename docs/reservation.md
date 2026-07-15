# Reservation System & Algorithm

## In plain terms
A reservation is a way to say "I want this item — hold it for me." If a copy is
free right now, you get it held immediately with a pickup deadline. If every copy
is out, you join a **waiting line** with no deadline, and you're promoted to the
front (with a fresh deadline) the moment a copy frees up. This doc explains the
data, the states, and the exact algorithm.

Files: `reservation/` package (`Reservation`, `ReservationQueue`,
`ReservationStatus`, `ReservationService`) plus `util/ReservationRepository` for
persistence.

---

## The two states that matter most: WAITING vs ACTIVE

| Status | Meaning | Has a pickup deadline? |
|--------|---------|------------------------|
| `WAITING` | In line; no copy available yet | No (`expiresOnDay = -1`) |
| `ACTIVE` | A copy is held for you; ready to pick up | Yes (`expiresOnDay` set) |
| `COMPLETED` | You borrowed the held copy | — |
| `EXPIRED` | You didn't pick up in time | — |
| `CANCELLED` | You cancelled it | — |

The last three are "terminal" — done, no longer in play. `isTerminal()` returns
true for those.

Key detail: **only ACTIVE reservations can expire.** A WAITING reservation has
`expiresOnDay = -1` and never times out — you can't lose your place in line by
waiting. The deadline clock only starts when you're promoted to ACTIVE.

---

## The reservation record
```java
String reservationId;   // "RES-XXXXXXXX"
String memberId;        // who reserved
String itemId;          // what they reserved
int    reservedOnDay;   // simulated day it was placed (final)
int    expiresOnDay;    // pickup deadline (or -1 while WAITING)
ReservationStatus status;
```

`isExpired(currentDay)` is `true` only when `status == ACTIVE && currentDay >
expiresOnDay` — i.e. an ACTIVE reservation whose deadline is strictly in the past.

---

## The per-item waiting line (`ReservationQueue`)
Each item has its own queue — a plain **FIFO list** (first in, first out). New
reservations are appended. When counting or picking, the queue looks at **only
the WAITING entries**, in insertion order:
- `peekFirst()` → the first WAITING reservation (skips non-waiting ones).
- `getPosition(r)` → 1-based rank among WAITING entries (position #1 = next up).

Entries are never removed when their status changes; they just stop being counted
once they leave WAITING.

---

## Placing a reservation — `reserve(memberId, itemId, currentDay)`

**First, four guard checks** (`validateReservationRequest`):
1. Item exists → else `NotFoundException`.
2. Item type is reservable (`item.canReserve()`) → else `ValidationException`.
   (Books & magazines: yes. E-books & audiobooks: no.)
3. Member has no existing non-terminal reservation for this item → else
   `ConflictException` ("You already have a reservation for this item").
4. Member is under their reservation limit → else `ConflictException`.

**Then, the branch that decides WAITING vs ACTIVE:**

```java
if (item.getAvailableCopies() > 0) {
    // a copy is free -> hold it immediately
    status   = ACTIVE;
    expiresOnDay = currentDay + reservationDays;   // deadline starts now
} else {
    // nothing free -> join the line, no deadline
    status   = WAITING;
    expiresOnDay = -1;
}
```

`reservationDays` comes from system settings (default 7). See
`docs/system-settings-and-time.md`.

---

## The heart of the algorithm: promotion — `processReturn(itemId, currentDay)`
This is what moves the line forward. It promotes **exactly one** waiter:

```java
Reservation first = queue.peekFirst();   // first WAITING
if (first == null) return false;         // nobody waiting
first.setStatus(ACTIVE);
first.setExpiresOnDay(currentDay + reservationDays);  // FRESH deadline
// ... persist + print "Activated reservation ... Pick up by day X"
return true;
```

The promoted member gets a **brand-new** pickup window starting the day the copy
frees up — not the (possibly already-elapsed) day they joined the queue. Returns
`true` if it promoted someone, `false` if nobody was waiting.

**When does promotion run?** Whenever a copy becomes free:

| Trigger | Where |
|---------|-------|
| A borrower returns the item | `LibraryMemberConsole.doReturn` → `processReturn` |
| An ACTIVE reservation is cancelled (freeing its held copy) | `cancel()` |
| An ACTIVE reservation expires (freeing its held copy) | `expireReservations()` |
| Stock is restocked (admin or callcenter) | `fulfillFromQueue()` |

### Multi-copy restock — `fulfillFromQueue(itemId, copiesFreed, currentDay)`
If several copies free up at once, promote several waiters — one per copy,
stopping early if the line runs out:

```java
for (int i = 0; i < copiesFreed; i++) {
    if (!processReturn(itemId, currentDay)) break;
}
```

---

## Ending a reservation

- **Cancel** (`cancel(reservationId)`): marks a non-terminal reservation
  `CANCELLED`. If it was **ACTIVE** (holding a copy), that copy is freed and
  `processReturn` hands it to the next waiter. Throws `NotFoundException` if the
  id isn't found or is already terminal.
- **Expire** (`expireReservations(currentDay)`): scans all reservations; any
  ACTIVE one past its deadline becomes `EXPIRED`, and its held copy cascades to
  the next waiter. Runs when the simulated day advances (see below).
- **Complete** (`completeReservation(memberId, itemId)`): when the member
  actually borrows their held copy, their ACTIVE reservation becomes `COMPLETED`.
  Called from the borrow flow. Silent no-op if there's no matching ACTIVE
  reservation.

---

## "Held copies" vs "walk-in available copies"
The library's `availableCopies` counter still includes copies that are physically
being **held** for someone's ACTIVE reservation. So a naive walk-in borrower must
not be allowed to grab a copy reserved for someone else.

- `getHeldCopiesCount(itemId)` = number of ACTIVE reservations = copies earmarked
  for pickup.
- `getWalkInAvailableCopies(itemId, memberId, availableCopies)`:

  ```java
  int held = getHeldCopiesCount(itemId);
  if (hasReadyReservation(memberId, itemId)) held -= 1;  // your own held copy counts for YOU
  return availableCopies - held;
  ```

  In plain terms: copies free for a walk-in = total available minus copies held
  for **other** people. If you have your own ACTIVE reservation, one held copy is
  credited back to you so you can pick it up.

The borrow flow (`LibraryMemberConsole.doBorrow`) uses this: if walk-in available
is `<= 0`, it blocks the borrow and tells you how many copies are held for
reservations.

---

## Reservation limit per member
```java
int roleLimit = persona.getUserProfile().reservationLimit();
return Math.min(roleLimit, SystemSettingsService.getMaxReservations());
```

The effective limit is the **smaller** of the role's limit and the system-wide
`maxReservations` ceiling (default 10). Role limits: Teacher 10, Student 5,
Guest 2, Admin 0, CallCenter 0.

> Naming caution for maintainers: `hasActiveReservation`,
> `getActiveReservationCount`, and `getMemberReservations` actually count
> **non-terminal** (WAITING + ACTIVE) reservations despite "active" in the names.
> Only `getHeldCopiesCount`, `hasReadyReservation`, and
> `getActiveReservationHolders` filter strictly on ACTIVE.

---

## How the member sees it (UI)
The reserve options appear only if the member's role allows reserving
(`profile.canReserve()`):
- **Reserve Item**: if the result is WAITING, shows your queue position and how
  many active holders are ahead; if ACTIVE, shows "Pick up by day X".
- **Cancel Reservation**: lists your non-terminal reservations, cancels by id.
- **My Reservations**: WAITING → "Waiting (no deadline) | Queue #pos (N ahead)";
  ACTIVE → "Expires: day X | Ready for pickup".

---

## Persistence
Every state change is saved immediately. `ReservationService.syncReservation`
calls `ReservationRepository.insertReservation`, an H2 `MERGE` (upsert) on the
`reservations` table. On startup, `loadFromDatabase()` reloads all reservations
and rebuilds each item's queue in memory.
