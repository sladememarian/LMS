# Support Microservice

## Purpose
The communication centre of the application — an **operations centre**, not just
a ticket box. It manages role requests, support tickets, notifications, the
Admin inbox and the CallCenter inbox, and bridges users to operators. Support
does **not** own users, items or money; it manages requests and communication.

## Internal modules
Following the recommended split, Support is organised into submodules:

```
support
├── SupportService / SupportTicket / TicketPrinter   (ticket module)
├── inbox
│   ├── AdminInbox
│   └── CallCenterInbox
├── notification
│   └── NotificationService        (delegates to Mail — single message store)
└── rolerequest
    ├── RoleRequest
    └── RoleRequestService          (delegates role change to Persona)
```

## Role-based experience
`SupportConsole` routes by role:

| Role | Console | Capabilities |
|------|---------|--------------|
| Guest | `SupportMemberConsole` | request Student/Teacher role, create tickets, view tickets/notifications |
| Student/Teacher | `SupportMemberConsole` | create tickets, view tickets/notifications |
| CallCenter | `CallCenterInbox` | view technical/book tickets, **respond with a message** (marks IN_PROGRESS + notifies the member), close, add library item (via Support), notifications |
| Admin | `AdminInbox` | role requests (approve/reject), user tickets, CallCenter activity, notifications, encrypted DB, debug, **Advance Simulated Day** |

## Key functions
All mutating methods now throw specific exceptions on failure instead of
returning a boolean.

| Method | Throws | Description |
|--------|--------|-------------|
| `createTicket(userId, section, title, desc)` | `ValidationException` (empty title or description) | Validates input, auto-assigns priority, persists. |
| `getAllTickets()` | — | Reloads and returns all tickets. |
| `validateCallCenterLogin(email, password)` | — | Returns `true` if credentials are valid AND role is CALLCENTER. |
| `updateTicketStatus(id, status)` | `NotFoundException` (bad id) | Changes ticket status and persists. |
| `respondToTicket(id, message)` | `NotFoundException` (bad id) | Stores response, marks IN_PROGRESS, notifies creator. |
| `handleCallCenterStockUpdate(itemId, qty)` | `AuthorizationException` (not CALLCENTER) | CallCenter adjusts stock through Support → Library. |
| `addLibraryItemViaSupport(item)` | `AuthorizationException` (not CALLCENTER/ADMIN) | Adds a catalog item through Support → Library. |
| `RoleRequestService.submit/getPending` | — | Guest role-upgrade workflow, persisted. |
| `RoleRequestService.approve(id)` | `NotFoundException` (bad id), `ConflictException` (already processed) | Approves role request, promotes persona, persists. |
| `RoleRequestService.reject(id)` | `NotFoundException` (bad id), `ConflictException` (already processed) | Rejects role request and persists. |
| `NotificationService.notify/notifyAddress/showNotifications` | — | Notification centre backed by Mail. |

## Staff console wiring
`Main` option **5 (Support)** now genuinely uses the Support microservice:
a logged-in member opens `SupportMemberConsole`; staff authenticate and open
`AdminInbox` / `CallCenterInbox`. The old "just print a report" behaviour was
removed (reports now live in the Admin Library/Finance dashboards).

## Ticket model
`ticketId, userId, title, description, category` (TECHNICAL / BOOK_REQUEST),
`priority` (LOW/HIGH/CRITICAL), `status` (OPEN/IN_PROGRESS/RESOLVED/CLOSED), and
`response` (the latest CallCenter reply, shown under the ticket in `TicketPrinter`).

## Communications
Support → Persona (`promoteRole`, member lookup), Support → Library (`addItem`,
`updateItemQuantityFromCallCenter`), Support → Mail (notifications), and
Support → Finance (the Admin *Advance Simulated Day* button drives
`SimulationClock`/`LoanService`). Tickets are stored XOR-encrypted in
`support_tickets.json`. **Role requests are now persisted** XOR-encrypted to
`role_requests.json` and reloaded on every operation, so a request raised in a
Guest instance is immediately visible to a separate Admin instance (no restart
needed); previously they lived only in an in-memory session registry, which is
why a second running instance could not see them. Notifications remain stored
by the Mail microservice.

## Cross-process ticket visibility (updated)

`SupportService` now **always reloads** `support_tickets.json` at the start of
every operation (`createTicket`, `updateTicketStatus`, `respondToTicket`,
`getAllTickets`). A ticket raised by a user in one running instance is
immediately visible to the CallCenter or Admin in a separate instance — no
restart required. This mirrors the reload-on-read behaviour already implemented
for `RoleRequestService`.

`AdminInbox.inspectDatabase` ("View Encrypted Database") now always includes
all 8 encrypted stores in the `merged_decrypted_export.json`. Stores that have
not yet been created on disk appear as `null` rather than being silently omitted.
