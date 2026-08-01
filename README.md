# Modular Monolith LMS (Library Management System)

A Java Library Management System built as a modular monolith.
Every microservice lives in its own package under `ir.ac.kntu` and communicates through plain
method calls. Data is stored in a **PostgreSQL** database (or **H2** in-memory during tests) — no file-based persistence.

> Full design docs live in [`docs/`](docs/README.md) and every test is explained in
> [`test_docs/`](test_docs/README.md). The cross-service contract is in
> [`docs/microservice_communications.md`](docs/microservice_communications.md).

## Phase 3 — JavaFX GUI

Phase 3 wraps the CLI backend in a JavaFX desktop GUI (`ir.ac.kntu.gui`). The
GUI is a **thin client**: it only calls existing backend services and adds no
new backend business logic (see the backend-unchanged proof in
[`docs/gui.md`](docs/gui.md)).

```bash
./gradlew run       # launch the JavaFX GUI
./gradlew runCli    # launch the original CLI
```

Highlights:

- **Two-factor login** — credentials, then a 2FA code delivered to a simulated
  inbox window; verified before the shell opens. (`.env` `MASTER_KEY=bid` and
  `MASTER_OTP=123` skip password / OTP for quick access.)
- **Password show/hide toggle** on login and registration.
- **Asynchronous sign-up** — registration collects email, first/last name, and
  phone. The account (email + password) is created on one thread so the user can
  log in immediately; the profile fields are wrapped in a disk-backed
  **message queue** and persisted by a separate worker thread. Full design and
  code walkthrough in [`docs/signup_queue.md`](docs/signup_queue.md).
- **Role-aware shell** — sidebar navigation, top bar with identity, theme
  toggle, Next-Day control, and sign-out. Tall panels scroll; the Notifications
  nav item shows a small red dot when unread mail is waiting.
- **Real-time library search** — debounced, paginated, off the FX thread.
- **Borrow / reserve / return** — every backend rule (limits, debt, walk-in
  availability, reservation queue) enforced through the existing services.
- **Card validation** — top-up cards are checked against strict PAN / CVV /
  expiry rules (`gui/util/CardValidator`) before any wallet charge.
- **Wallet, Fines, Analytics, Callcenter, Support, System Settings, Profile,
  Notifications** panels.
- **Light / Dark themes** — CSS classpath resources, persisted per user via
  `SsoService.changeTheme`.
- **Visual feedback** — short fade transitions on view/panel swaps, toast
  notifications, and progress indicators on longer waits (analytics load, HTML
  report generation, wallet actions). See the animations table in
  [`docs/gui.md`](docs/gui.md).
- **Responsive UI** — all DB / Streams / mail / report work runs on a shared
  background executor ([`docs/threads.md`](docs/threads.md)).

GUI-specific docs: [`docs/gui.md`](docs/gui.md) (walkthrough, theming,
animations, backend-unchanged proof), [`docs/threads.md`](docs/threads.md)
(background threading), [`docs/signup_queue.md`](docs/signup_queue.md)
(async sign-up message queue), [`docs/reports.md`](docs/reports.md) (report
tables and their backend sources).

### GUI tests

TestFX tests under `src/test/java/ir/ac/kntu/gui` run headed locally with
`./gradlew clean test`. They are excluded on the CI runner (which cannot run a
headed JavaFX display) via `-PskipGuiTests`:

```bash
./gradlew clean test -PskipGuiTests   # CI mode: backend + style, no GUI tests
./gradlew clean test                  # local: everything, including headed GUI E2E
```

`BorrowReserveReturnE2ETest` drives real borrow / reserve / return flows through
the GUI and asserts the backend state actually changed.

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
└── util      Database, DatabaseAccess, DatabaseException, ConsoleColor, ConsoleMenu, EnvConfig, Validator
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

## Database migration (PostgreSQL)

All XOR-encrypted file persistence has been replaced with a relational database.
The data layer lives in `ir.ac.kntu.util`:

| Class | Role |
|-------|------|
| `Database` | Connection management, schema initialisation (11 tables), helper methods (`withPs`, `queryAll`, `querySingle`, `queryPrepared`, `executeUpdate`) |
| `DatabaseAccess` | Every CRUD operation exposed as static methods — `insertPersona()`, `getAllLoans()`, `deleteLibraryItem()`, … |
| `DatabaseException` | Custom runtime exception wrapping `SQLException` |

### How Java connects to Docker Compose PostgreSQL

The Java code **does not hardcode** a database address. Instead it reads
`JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD` from environment variables.
When you run `docker-compose up`:

1. Docker Compose starts **PostgreSQL 16** in the `db` container.
2. Docker Compose starts the **app** container and sets:
   ```
   JDBC_URL=jdbc:postgresql://db:5432/lms
   JDBC_USER=lms
   JDBC_PASSWORD=lms
   ```
3. `Database.getConnection()` reads these env vars, connects to `db:5432`,
   and runs `CREATE TABLE IF NOT EXISTS …` for all 11 tables.
4. Services call `DatabaseAccess.insertPersona(…)` etc., which internally use
   `Database.withPs(…)` — all SQL goes to **PostgreSQL** at `db:5432`.

When **no env vars** are set (e.g. running `java -jar …` locally, or during
`gradle test`), the code defaults to an **H2 in-memory** database:
`jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`.

All upserts use **standard SQL `MERGE … USING … ON`** syntax compatible with
both PostgreSQL 15+ and H2 2.x — so the same code works against both engines
without change.

```
                         ┌─────────────────┐
  docker-compose.yml     │  JDBC_URL=…      │
  sets env vars ────────→│  JDBC_USER=…     │────→ Database.getConnection()
                         │  JDBC_PASSWORD=… │         │
                         └─────────────────┘         │
                                          PostgreSQL  │
                                          at db:5432  │
                                                      ▼
                                              initTables() → 11 tables
                                                      │
                                                      ▼
                                         Service code → DatabaseAccess.* → Database.withPs/queryAll
```

See [`docs/db.md`](docs/db.md) for the full schema, connection flow, and Docker
integration details.

## Configuration (`.env`)

```env
DEFAULT_ADMIN_USERNAME=admin
DEFAULT_ADMIN_PASSWORD=adminpass
DEFAULT_CALLCENTER_USERNAME=callcenter
DEFAULT_CALLCENTER_PASSWORD=ccpass
SIMULATED_2FA_EXPIRE_MINUTES=5
MAILBOX_MAX_MESSAGES=100
MAIL_SYSTEM_NAME=UniLibraryMail
# JDBC_URL / JDBC_USER / JDBC_PASSWORD — set for PostgreSQL in Docker Compose;
# defaults to H2 in-memory when unset.
```

Copy `.env.example` to `.env` and adjust values. Tests run against H2 in-memory automatically.

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

`./gradlew clean test` runs **100 tests**:

- **Functional JUnit tests** for every microservice and model (`src/test/java/ir/ac/kntu/...`).
- **CheckStyleTest** — enforces indentation and naming conventions on `src/main`.
- **CheckPMDTest** — enforces the project's PMD ruleset on `src/main`.

The functional suite was trimmed to the most important tests per microservice
plus the new features. A green build means all functional tests pass **and** the
production code is style-clean. See [`test_docs/`](test_docs/README.md) for an
explanation of every test.

## Documentation

- [`docs/`](docs/README.md) — what each microservice does.
- [`docs/gui.md`](docs/gui.md) — Phase 3 JavaFX GUI: walkthrough, theming, backend-unchanged proof.
- [`docs/signup_queue.md`](docs/signup_queue.md) — async sign-up: disk-backed producer/consumer message queue.
- [`docs/threads.md`](docs/threads.md) — GUI background-threading model.
- [`docs/reports.md`](docs/reports.md) — admin/callcenter report tables and their backend sources.
- [`docs/microservice_communications.md`](docs/microservice_communications.md) — how services talk, flow by flow.
- [`docs/mock_data.md`](docs/mock_data.md) — seeded suppliers, items, and default accounts.
- [`test_docs/`](test_docs/README.md) — every JUnit test explained.
