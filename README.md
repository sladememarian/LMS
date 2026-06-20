# Modular Monolith LMS (Library Management System)

A Java, fully **offline / self-contained** Library Management System built as a modular monolith.
Every microservice lives in its own package under `ir.ac.kntu` and communicates through plain
method calls. Data is persisted to local **XOR-encrypted** files — no external server or database is required.

> Full design docs live in [`docs/`](docs/README.md) and every test is explained in
> [`test_docs/`](test_docs/README.md). The cross-service contract is in
> [`docs/microservice_communications.md`](docs/microservice_communications.md).

## Architecture

```
FrontPanel (Main)  ── orchestrates everything
│
├── IAM       Authentication, registration, 2FA, change-password   → Persona, Mail
├── Persona   Users, roles, wallet, theme, borrowed-item inventory  → Mail
│   └── InventoryConsole  "My Inventory" (reads Library item details)
├── Finance   Wallet, debts, 10% tax, simulated payments, reports   → Persona, Mail
│   └── role consoles: Member / Operator / Admin
├── Library   Typed catalog + suppliers, role-based dashboards       → Support, Report
│   └── role consoles: Member / Operator / Admin  (+ LibraryPrinter, ItemEntry)
├── Support   Operations centre (tickets, role requests, inboxes)    → Persona, Library, Mail
│   ├── inbox        AdminInbox / CallCenterInbox
│   ├── ticket       SupportService / SupportTicket / TicketPrinter
│   ├── notification NotificationService (reuses Mail)
│   └── rolerequest  RoleRequest / RoleRequestService (→ Persona)
├── SSO       Settings & Session                                     → IAM, Persona
├── Mail      Simulated offline mail provider (2FA, welcome, notifications)
├── Report    Supplier financial HTML report (Admin)                 → Library
└── util      ConsoleColor, ConsoleMenu, EnvConfig, Validator
```

Communication rules (kept deliberately strict to avoid a spaghetti architecture):

```
IAM      -> Persona, Mail
Persona  -> Mail            (InventoryConsole -> Library, read-only)
Finance  -> Persona, Mail
Library  -> Support, Report
Support  -> Persona, Library, Mail
SSO      -> IAM, Persona
Report   -> Library
Mail     -> nobody
FrontPanel orchestrates everything
```

## Main menu

```
1. Sign Up        Registration, welcome mail   (Back option with 0)
2. Login          Credentials + 2FA via mail   (Back option with 0)
3. Settings       SSO: profile, password, theme
4. Mail Inbox     Read simulated messages
5. Support        Member tickets/roles, or staff inbox (Admin/CallCenter)
6. Library        Role-based catalog & borrowing
7. Finance        Wallet, debts, payments
8. Exit
```

## Role-based experiences

Each app routes the logged-in user to a dashboard for their role:

- **Library** — Guest/Student/Teacher (search, borrow, return, extend, My Inventory),
  CallCenter operator (add/edit via Support, quantities, suppliers),
  Admin (full CRUD, report, borrow stats, encrypted DB, debug).
- **Finance** — Guest/Student/Teacher (charge, pay debt, extend, history),
  CallCenter (view-only debt stats/alerts),
  Admin (wallets, debts, tax revenue, admin wallet, reports, encrypted DB).
- **Support** — Guest (role requests + tickets), Student/Teacher (tickets),
  CallCenter inbox (reply to tickets with a message + close, add items), Admin
  inbox (approve roles, monitor, **Advance Simulated Day**).

Borrowing enforces each role's limit (Guest 2, Student 10, Teacher 15) and is blocked while a
user has outstanding Finance debt. "My Inventory" lives in Persona; the Library only links to it.

## Recent feature changes

- **Item-search pagination** — results paginate 10 per page with `[N]ext`,
  `[P]revious`, jump-to-page-number, and a working `[Q]uit` (`LibraryPrinter`).
- **Full cross-instance data sharing** — Tickets, debts, library catalog, notifications, and
  role requests are now persisted and **reloaded on every operation** so any data change
  made in one running instance (e.g. a user terminal) is immediately visible to a second
  running instance (e.g. the CallCenter or Admin terminal). No restart required.
  *(Fixes: SupportService, FinanceService, LibraryService, MailService, PersonaService.)*
- **Cross-instance role requests** — role-upgrade requests are persisted to an
  XOR-encrypted `role_requests.json` and reloaded on every read, so a request
  raised in a Guest instance is visible to a separate Admin instance with no
  restart (`RoleRequestService`).
- **CallCenter ticket replies** — "Respond To Ticket" takes a message, stores it
  on the ticket, marks it `IN_PROGRESS`, and notifies the creator.
- **Time-ordered Finance history** — transactions carry a timestamp and history
  is shown oldest → newest.
- **Date simulation (Admin = god of time)** — the Admin's *Advance Simulated
  Day* button moves a persisted simulated real-calendar clock forward (shown as
  `M/d/yyyy` in the main menu for every role); borrowed items are due **3 simulated
  days** after borrowing and accrue a daily overdue fine (reusing
  `FinanceService.recordDebt`). The admin's "View Encrypted Database" export now
  includes all 8 stores (existing ones as JSON, not-yet-created ones as `null`).

## Configuration (`.env`)

```env
DEFAULT_ADMIN_USERNAME=admin
DEFAULT_ADMIN_PASSWORD=adminpass
DEFAULT_CALLCENTER_USERNAME=callcenter
DEFAULT_CALLCENTER_PASSWORD=ccpass
MASTER_ADMIN_DATABASE_PASSWORD=supersecureXORkey
SIMULATED_2FA_EXPIRE_MINUTES=5
SIMULATED_2FA_CODE=135790        # leave empty to auto-generate a 6-digit code
MAILBOX_MAX_MESSAGES=100
MAIL_SYSTEM_NAME=UniLibraryMail
```

Copy `.env.example` to `.env` and adjust values. The whole system stays 100% offline.

## Commands

- Run the full test suite (JUnit + CheckStyle + PMD):

  ```
  ./gradlew clean test
  ```

- Build the project:

  ```
  ./gradlew build
  ```

- Run the application (after building):

  ```
  java -cp "build/classes/java/main" ir.ac.kntu.Main
  ```

## Testing

`./gradlew clean test` runs **36 tests** (consolidated from an earlier 92):

- **Functional JUnit tests** for every microservice and model (`src/test/java/ir/ac/kntu/...`).
- **CheckStyleTest** — enforces indentation and naming conventions on `src/main`.
- **CheckPMDTest** — enforces the project's PMD ruleset on `src/main`.

The functional suite was trimmed to the most important tests per microservice
plus the new features. A green build means all functional tests pass **and** the
production code is style-clean. See [`test_docs/`](test_docs/README.md) for an
explanation of every test.

## Documentation

- [`docs/`](docs/README.md) — what each microservice does.
- [`docs/microservice_communications.md`](docs/microservice_communications.md) — how services talk, flow by flow.
- [`docs/mock_data.md`](docs/mock_data.md) — seeded suppliers, items, and default accounts.
- [`test_docs/`](test_docs/README.md) — every JUnit test explained.
