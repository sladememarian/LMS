# interfaces Package (Phase 2)

## In plain terms
This package holds small "contracts" — interfaces that say "any class that
implements me promises to support this behavior." No implementations live
here yet; this is scaffolding for upcoming features. Nothing in the app
implements them yet (that comes in later steps).

## The contracts

| Interface | Promise it makes | Example future user |
|-----------|-------------------|----------------------|
| `Displayable` | "I can render myself as one line of text for a menu." (`toDisplayString()`) | Library items, search results shown in a `Menu<T>` |
| `Searchable` | "I can tell you if I match a search keyword." (`matchesQuery(String)`) | Library catalog search |
| `Borrowable` | "I can be borrowed and returned." (`isAvailable()`, `onBorrow()`, `onReturn()`) | `LibraryItem` subclasses |
| `Reservable` | "I can be reserved ahead of time and expire." (`reserve`, `cancelReservation`, `hasActiveReservation`) | Library items with a waitlist |
| `RequestAssignable<T>` | "I can be assigned to someone (of type T) and know if I've been assigned." (`assign`, `getAssignee`, `isAssigned`) | Support tickets, role requests |

## Why interfaces instead of just adding methods to existing classes?
Because it lets *different, unrelated* classes share the same behavior
contract without needing a common parent class. For example, a `Book` and a
`SupportTicket` are nothing alike, but both could implement
`RequestAssignable<Persona>` if tickets and role requests both need "assign
this to a staff member" logic — the console/menu code that handles assignment
can be written once, generically, against the interface.

This is the **Interfaces** grading criterion from the project spec: contracts
that get implemented across otherwise-unrelated classes.
