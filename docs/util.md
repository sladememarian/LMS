# util Package

Cross-cutting helpers shared by every microservice.

| Class | Responsibility |
|-------|----------------|
| `Database` | PostgreSQL/H2 connection management, schema initialisation (11 tables), package-private helper methods (`withPs`, `queryAll`, `querySingle`, `queryPrepared`, `executeUpdate`). |
| `DatabaseAccess` | All public CRUD operations (`insertPersona`, `getAllLoans`, `deleteLibraryItem`, …) — the only class services import. |
| `DatabaseException` | Custom `RuntimeException` that wraps `SQLException` with a user-friendly message. |
| `ConsoleColor` | ANSI colour constants and `gray/printError/printSuccess` helpers. |
| `ConsoleMenu` | Shared dashboard plumbing: `readLine`, `readInt`, `banner`, `option`, `back`, `pause`. |
| `EnvConfig` | Reads configuration from `.env` (default accounts, 2FA settings). |
| `Validator` | Regex validation for email, phone, password, member id, item id, ISBN-13, ISSN, publish year, download URL. |

The old XOR-encrypted file persistence (`persona_secure.json`, `finance_secure.json`,
`mail.enc`, `library.enc`, …) has been fully replaced by `Database` + `DatabaseAccess`.
See [`db.md`](db.md) for the full schema and Docker integration.
