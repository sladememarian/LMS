# util Package

Cross-cutting helpers shared by every microservice.

| Class | Responsibility |
|-------|----------------|
| `ConsoleColor` | ANSI colour constants and `gray/printError/printSuccess` helpers. |
| `ConsoleMenu` | **New.** Shared dashboard plumbing: `readLine`, `readInt`, `banner`, `option`, `back`, `pause`. Centralises prompt/menu rendering so the Library/Finance/Support consoles do not duplicate it. |
| `EnvConfig` | Reads configuration from `.env` (XOR key, default accounts, 2FA settings). |
| `Validator` | Regex validation for email, phone, password, member id, item id, ISBN-13, ISSN, publish year, download URL. |

`ConsoleMenu` is the only addition here; it exists to keep the new role-based
consoles small enough to satisfy the project's CheckStyle/PMD limits
(method length, cyclomatic complexity, duplicate literals).
