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
| CallCenter | `CallCenterInbox` | view technical/book tickets, respond, close, add library item (via Support), notifications |
| Admin | `AdminInbox` | role requests (approve/reject), user tickets, CallCenter activity, notifications, encrypted DB, debug |

## Key functions
Existing (reused): `createTicket`, `getAllTickets`, `validateCallCenterLogin`,
`handleCallCenterStockUpdate`, `submitLibraryItemPlaceholder`.

Added:
| Method | Description |
|--------|-------------|
| `SupportService.updateTicketStatus(id, status)` | Used by respond (IN_PROGRESS) and close (CLOSED). |
| `SupportService.addLibraryItemViaSupport(item)` | CallCenter/Admin add a catalog item through Support → Library. |
| `RoleRequestService.submit/getPending/approve/reject` | Guest role-upgrade workflow. |
| `NotificationService.notify/notifyAddress/showNotifications` | Notification centre backed by Mail. |

## Staff console wiring
`Main` option **5 (Support)** now genuinely uses the Support microservice:
a logged-in member opens `SupportMemberConsole`; staff authenticate and open
`AdminInbox` / `CallCenterInbox`. The old "just print a report" behaviour was
removed (reports now live in the Admin Library/Finance dashboards).

## Ticket model
`ticketId, userId, title, description, category` (TECHNICAL / BOOK_REQUEST),
`priority` (LOW/HIGH/CRITICAL), `status` (OPEN/IN_PROGRESS/RESOLVED/CLOSED).

## Communications
Support → Persona (`promoteRole`), Support → Library (`addItem`,
`updateItemQuantityFromCallCenter`), Support → Mail (notifications). Tickets are
stored XOR-encrypted in `support_tickets.json`; role requests are an in-memory
session registry and notifications are stored by the Mail microservice.
