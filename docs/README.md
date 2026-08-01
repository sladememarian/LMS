# Documentation Index

This `docs/` folder explains what each microservice does and how the services
communicate for every flow of the application.

## Start here
- **[microservice_communications.md](microservice_communications.md)** — the
  full communication map and a flow-by-flow breakdown (sign-up, login, borrow,
  My Inventory, debt, role requests, tickets, reports, …).

## Per-microservice
| Doc | Microservice |
|-----|--------------|
| [iam.md](iam.md) | Identity & Access Management (registration, login, 2FA, back buttons) |
| [persona.md](persona.md) | Users, roles, wallet, **borrowed-item inventory** |
| [sso.md](sso.md) | Settings & session |
| [library.md](library.md) | Catalog + role-based dashboards (Admin/CallCenter/Guest/Student/Teacher) |
| [finance.md](finance.md) | Wallet, debts, tax, simulated payments, role dashboards |
| [support.md](support.md) | Tickets, role requests, notifications, inboxes (operations centre) |
| [mail.md](mail.md) | Simulated offline mail provider (also backs notifications) |
| [report.md](report.md) | Supplier financial HTML report |
| [util.md](util.md) | ConsoleColor, ConsoleMenu, EnvConfig, Validator |
| [db.md](db.md) | Database schema, connection flow, Docker integration |

## Phase 3 — JavaFX GUI
| Doc | Topic |
|-----|-------|
| [gui.md](gui.md) | GUI walkthrough, theming, and the backend-unchanged proof |
| [signup_queue.md](signup_queue.md) | Async sign-up: disk-backed producer/consumer message queue |
| [threads.md](threads.md) | Background-threading model (`BackgroundJobs`) |
| [reports.md](reports.md) | Admin/callcenter report tables and their backend sources |

## Data
- [mock_data.md](mock_data.md) — seeded suppliers, items, and default accounts.

## Tests
See the sibling **`test_docs/`** folder for an explanation of every JUnit test.
