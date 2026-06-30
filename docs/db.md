# Database Layer

All XOR-encrypted file persistence has been replaced with a relational database.
The app uses **PostgreSQL** (production, via Docker Compose) and **H2** (in-memory,
for JUnit tests only — Gradle automatically injects the H2 URL during test runs).

---

## Architecture — Connecting Java to Docker Compose PostgreSQL

The Java code does **not** hardcode a PostgreSQL connection. Instead it reads
environment variables (`JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`) at runtime.
Docker Compose sets these variables on the `app` container, pointing at the
`db` container:

```
docker-compose up
  │
  ├─ db (postgres:16-alpine)
  │    POSTGRES_DB=lms, POSTGRES_USER=lms, POSTGRES_PASSWORD=lms
  │    port 5432
  │
  └─ app (java -jar app.jar)
       JDBC_URL=jdbc:postgresql://db:5432/lms  ← Docker's internal DNS
       JDBC_USER=lms
       JDBC_PASSWORD=lms
       │
       └─ Database.getConnection()
            reads JDBC_URL → jdbc:postgresql://db:5432/lms
            connects to PostgreSQL
            initTables() → CREATE TABLE IF NOT EXISTS … (11 tables)
```

When running **tests** (`./gradlew test`), Gradle injects the H2 URL automatically
via the `test { environment … }` block in `build.gradle` — no manual setup needed.

When running **locally outside Docker** (e.g. `java -jar app.jar`), you must point
the app at your Docker PostgreSQL by creating a `.env` file:
```
JDBC_URL=jdbc:postgresql://localhost:5432/lms
JDBC_USER=lms
JDBC_PASSWORD=lms
```
Without any configuration, the app throws: `JDBC_URL is not configured`.

Two classes in `ir.ac.kntu.util` implement the data layer:

```
┌─────────────────────────────────────────────────┐
│                  Service code                    │
│  (PersonaService, MailService, LoanService, …)   │
└──────────────┬──────────────────────────────────┘
               │  static methods
               ▼
┌─────────────────────────────────────────────────┐
│                 DatabaseAccess                   │
│  insertPersona, getAllLoans, deleteLibraryItem…  │
│  (all public static — the only import services   │
│   need)                                          │
└──────────────┬──────────────────────────────────┘
               │  calls Database.withPs, Database.queryAll, …
               │  (same package — package-private access)
               ▼
┌─────────────────────────────────────────────────┐
│                    Database                      │
│  getConnection / closeConnection / initTables    │
│  helpers: withPs, queryAll, querySingle,         │
│           queryPrepared, executeUpdate           │
│  interfaces: SqlRunner, RowMapper<T>             │
└─────────────────────────────────────────────────┘
               │
               ▼
    ┌─────────────────────┐
    │  PostgreSQL / H2     │
    └─────────────────────┘
```

Key design decisions:

- **All methods are static** — no instance management. Services call
  `DatabaseAccess.insertPersona(p)` directly.
- **Connection is lazy** — `Database.getConnection()` creates the connection on
  first call and caches it. `Database.closeConnection()` releases it (used in
  tests).
- **Schema is auto-created** — `initTables()` runs `CREATE TABLE IF NOT EXISTS …`
  once when the connection is first established.
- **Standard SQL `MERGE` for upserts** — Both PostgreSQL 15+ and H2 2.x support
  the SQL:2016 `MERGE` syntax (`MERGE INTO t USING s ON key WHEN MATCHED …
  WHEN NOT MATCHED …`). The code uses this portable form instead of
  PostgreSQL-specific `INSERT … ON CONFLICT` or H2-specific `MERGE … KEY(…)`.

---

## Connection Flow

```
getConnection()
  │
  ├─ connection already open? → return it
  │
  └─ resolve JDBC_URL, JDBC_USER, JDBC_PASSWORD:
       │
       ├─ System.getenv("JDBC_URL") set? → use it   (tests: Gradle injects H2 URL)
       ├─ .env file has JDBC_URL? → use it           (local dev: points to localhost)
       └─ neither set → throws DatabaseException: "JDBC_URL is not configured"
       │
       └─ DriverManager.getConnection(url, user, password)
            │
            └─ initTables() → CREATE TABLE IF NOT EXISTS … (11 tables)
```

### Environment Variables

| Variable | Default | When to set |
|----------|---------|-------------|
| `JDBC_URL` | none — throws an error if missing | Docker Compose: `jdbc:postgresql://db:5432/lms`; Tests (auto): `jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`; Local dev: set in `.env` |
| `JDBC_USER` | `sa` | Docker Compose sets this to `lms` |
| `JDBC_PASSWORD` | `` (empty) | Docker Compose sets this to `lms` |

The resolution order is: system environment → `.env` file → hard-coded default.

---

## Schema (11 Tables)

### `personas`
| Column | Type | Notes |
|--------|------|-------|
| `email` | `VARCHAR(255)` | PK |
| `username` | `VARCHAR(255)` | |
| `password` | `VARCHAR(255)` | NOT NULL |
| `role` | `VARCHAR(50)` | NOT NULL, DEFAULT `'GUEST'` |
| `member_id` | `VARCHAR(50)` | |
| `wallet_balance` | `INTEGER` | DEFAULT 0 |
| `first_name` | `VARCHAR(255)` | |
| `last_name` | `VARCHAR(255)` | |
| `phone` | `VARCHAR(50)` | |
| `theme` | `VARCHAR(50)` | DEFAULT `'LIGHT'` |

System accounts (admin, callcenter) are stored with `email = username + "@system.local"`.

### `borrowed_items`
| Column | Type | Notes |
|--------|------|-------|
| `email` | `VARCHAR(255)` | NOT NULL, part of composite PK |
| `item_id` | `VARCHAR(50)` | NOT NULL, part of composite PK |

### `mail_messages`
| Column | Type | Notes |
|--------|------|-------|
| `message_id` | `VARCHAR(50)` | PK |
| `recipient_email` | `VARCHAR(255)` | NOT NULL |
| `subject` | `VARCHAR(500)` | |
| `body` | `TEXT` | |
| `type` | `VARCHAR(50)` | |
| `sent_date` | `VARCHAR(100)` | |
| `is_read` | `BOOLEAN` | DEFAULT FALSE |

### `two_factor_codes`
| Column | Type | Notes |
|--------|------|-------|
| `email` | `VARCHAR(255)` | PK |
| `code` | `VARCHAR(10)` | NOT NULL |
| `issued_at` | `BIGINT` | NOT NULL |

### `transactions`
| Column | Type | Notes |
|--------|------|-------|
| `tx_id` | `VARCHAR(50)` | PK |
| `member_id` | `VARCHAR(50)` | |
| `amount` | `INTEGER` | NOT NULL |
| `type` | `VARCHAR(50)` | |
| `description` | `VARCHAR(500)` | |
| `timestamp` | `BIGINT` | NOT NULL |

### `loans`
| Column | Type | Notes |
|--------|------|-------|
| `member_id` | `VARCHAR(50)` | NOT NULL, part of composite PK |
| `item_id` | `VARCHAR(50)` | NOT NULL, part of composite PK |
| `borrow_day` | `INTEGER` | NOT NULL |
| `due_day` | `INTEGER` | NOT NULL |
| `last_charged_day` | `INTEGER` | NOT NULL |

### `clock_state`
| Column | Type | Notes |
|--------|------|-------|
| `id` | `INTEGER` | DEFAULT 1, PK |
| `current_day` | `INTEGER` | NOT NULL, DEFAULT 1 |
| `start_date` | `VARCHAR(20)` | NOT NULL |

### `suppliers`
| Column | Type | Notes |
|--------|------|-------|
| `company_id` | `VARCHAR(50)` | PK |
| `company_name` | `VARCHAR(255)` | NOT NULL |

### `library_items`
| Column | Type | Notes |
|--------|------|-------|
| `item_id` | `VARCHAR(50)` | PK |
| `type` | `VARCHAR(50)` | NOT NULL (`BOOK`, `EBOOK`, `MAGAZINE`, `AUDIOBOOK`) |
| `title` | `VARCHAR(500)` | NOT NULL |
| `category` | `VARCHAR(255)` | |
| `publish_year` | `INTEGER` | |
| `supplier_id` | `VARCHAR(50)` | |
| `total_copies` | `INTEGER` | DEFAULT 0 |
| `available_copies` | `INTEGER` | DEFAULT 0 |
| `unit_price` | `INTEGER` | DEFAULT 0 |

### `support_tickets`
| Column | Type | Notes |
|--------|------|-------|
| `ticket_id` | `VARCHAR(50)` | PK |
| `user_id` | `VARCHAR(50)` | |
| `title` | `VARCHAR(500)` | |
| `description` | `TEXT` | |
| `category` | `VARCHAR(100)` | |
| `priority` | `VARCHAR(50)` | |
| `status` | `VARCHAR(50)` | |
| `response` | `TEXT` | |

### `role_requests`
| Column | Type | Notes |
|--------|------|-------|
| `request_id` | `VARCHAR(50)` | PK |
| `requester_email` | `VARCHAR(255)` | |
| `requested_role` | `VARCHAR(50)` | |
| `message` | `TEXT` | |
| `status` | `VARCHAR(50)` | DEFAULT `'PENDING'` |

---

## Code Patterns

### Writing data — `withPs`

`Database.withPs` is the core write helper. It takes a SQL string (with `?`
placeholders) and a lambda that binds parameters and executes:

```java
Database.withPs("INSERT INTO mail_messages (message_id, recipient_email, subject, body, type, sent_date, is_read) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
    ps.setString(1, msg.getMessageId());
    ps.setString(2, msg.getRecipientEmail());
    ps.setString(3, msg.getSubject());
    ps.setString(4, msg.getBody());
    ps.setString(5, msg.getMessageType().getLabel());
    ps.setString(6, msg.getSentDate());
    ps.setBoolean(7, msg.isRead());
    ps.executeUpdate();
});
```

The helper opens a `PreparedStatement`, runs the lambda, and wraps any
`SQLException` in a `DatabaseException`.

### Reading data — `queryAll` / `querySingle` / `queryPrepared`

**`queryAll`** returns a list via a `RowMapper<T>`:

```java
public static List<Transaction> getAllTransactions() {
    return Database.queryAll("SELECT * FROM transactions", rs -> new Transaction(
            rs.getString("tx_id"), rs.getString("member_id"), rs.getInt("amount"),
            rs.getString("type"), rs.getString("description"), rs.getLong("timestamp")));
}
```

**`querySingle`** returns one result (or `null`):

```java
public static Map<String, Object> loadClock() {
    return Database.querySingle("SELECT current_day, start_date FROM clock_state WHERE id=1", rs -> {
        Map<String, Object> result = new HashMap<>();
        result.put("currentDay", rs.getInt("current_day"));
        result.put("startDate", LocalDate.parse(rs.getString("start_date")));
        return result;
    });
}
```

**`queryPrepared`** is for parameterised single-row queries:

```java
public static String getTwoFactorCode(String email) {
    return Database.queryPrepared("SELECT code FROM two_factor_codes WHERE email=?",
            ps -> ps.setString(1, email.toLowerCase()), rs -> rs.getString("code"));
}
```

### Upserts — Standard SQL `MERGE`

Both PostgreSQL 15+ and H2 2.x support the SQL:2016 `MERGE` syntax with a
`USING` clause. This is what the code uses (portable across both databases):

```java
Database.withPs("MERGE INTO loans USING (VALUES (?, ?, ?, ?, ?)) AS s(member_id, item_id, borrow_day, due_day, last_charged_day) ON loans.member_id = s.member_id AND loans.item_id = s.item_id WHEN MATCHED THEN UPDATE SET borrow_day = s.borrow_day, due_day = s.due_day, last_charged_day = s.last_charged_day WHEN NOT MATCHED THEN INSERT (member_id, item_id, borrow_day, due_day, last_charged_day) VALUES (s.member_id, s.item_id, s.borrow_day, s.due_day, s.last_charged_day)", ps -> {
    ps.setString(1, loan.getMemberId());
    ps.setString(2, loan.getItemId());
    ps.setInt(3, loan.getBorrowDay());
    ps.setInt(4, loan.getDueDay());
    ps.setInt(5, loan.getLastChargedDay());
    ps.executeUpdate();
});
```

The `VALUES (…)` clause builds a single-row derived table aliased as `s`.
The `ON` clause specifies the match condition (the primary key).
`WHEN MATCHED THEN UPDATE SET …` updates each column.
`WHEN NOT MATCHED THEN INSERT …` inserts a new row.

---

## How Services Use the Database

Every service that previously read/wrote XOR-encrypted files now calls
`DatabaseAccess.*` static methods. The change is purely in the data layer —
service logic (role checks, borrowing rules, debt calculation) is unchanged.

Example — `PersonaService` before and after:

| Aspect | Before (file-based) | After (database) |
|--------|-------------------|-------------------|
| Import | `import ir.ac.kntu.util.Database;` | `import ir.ac.kntu.util.DatabaseAccess;` |
| Read | `loadFromEncryptedFile()` → parse JSON | `DatabaseAccess.getAllPersonas()` |
| Write | `saveToEncryptedFile()` → serialise JSON | `DatabaseAccess.insertPersona(p)` |

The services still keep in-memory lists for caching and filtering, but they
reload from the database on every operation (ensuring cross-instance data sharing).

---

## Docker Compose Integration

`docker-compose.yml` defines two services:

### `db` — PostgreSQL 16 (Alpine)
```yaml
db:
  image: postgres:16-alpine
  container_name: lms-db
  environment:
    POSTGRES_DB: lms
    POSTGRES_USER: lms
    POSTGRES_PASSWORD: lms
  ports:
    - "5432:5432"
  volumes:
    - pgdata:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U lms -d lms"]
    interval: 5s
    timeout: 5s
    retries: 5
```

- Stores data in a named Docker volume (`pgdata`) so it survives container restarts.
- Health check ensures the app only starts after PostgreSQL is ready.

### `app` — the Java application
```yaml
app:
  build: .
  container_name: lms-app
  stdin_open: true
  tty: true
  depends_on:
    db:
      condition: service_healthy
  environment:
    JDBC_URL: jdbc:postgresql://db:5432/lms
    JDBC_USER: lms
    JDBC_PASSWORD: lms
  volumes:
    - .:/app-data
  entrypoint: >
    sh -c "java -jar /app/app.jar"
```

- `JDBC_URL` points at the `db` container via Docker's internal DNS (`db:5432`).
- Since `initTables()` runs `CREATE TABLE IF NOT EXISTS …`, the schema is
  auto-created on the first startup — no manual migration scripts needed.
- The project is built with `gradle build` into `build/libs/template-1.0-SNAPSHOT.jar`,
  copied into the Docker image by the Dockerfile.

### Dockerfile
```dockerfile
FROM gradle:8.2-jdk17 AS builder
WORKDIR /build
COPY build.gradle settings.gradle ./
RUN gradle --no-daemon dependencies 2>/dev/null || true
COPY src src
RUN gradle --no-daemon build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/template-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- Two-stage build: compiles in the `gradle` image, runs in a slim `jre` image.
- Tests are skipped (`-x test`) during the Docker build because they require H2
  (available as a test dependency) and would run against the PostgreSQL container
  anyway.

### How to run with Docker

```bash
# Build the image and start both db + app together
docker compose up --build

# Or run the app interactively (keeps stdin open for the menu)
docker compose run app

# The app connects to PostgreSQL at jdbc:postgresql://db:5432/lms
# Schema is auto-created on first connection.
```

> The Docker image builds its own JAR internally (see Dockerfile). Running
> `./gradlew build` beforehand is only needed if you want a local JAR — it is
> not required for Docker.

---

## Testing — H2 In-Memory

During `./gradlew test`, Gradle injects these environment variables into the test JVM
(defined in `build.gradle`), pointing at an H2 in-memory database in PostgreSQL
compatibility mode:

```
jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
```

Key points:

- **`MODE=PostgreSQL`** — H2 mimics PostgreSQL SQL syntax, including `CREATE TABLE IF NOT EXISTS` compatibility. The upsert code uses the SQL:2016 `MERGE` syntax (`MERGE … USING … ON …`) which works on both H2 2.x and PostgreSQL 15+.
- **`DB_CLOSE_DELAY=-1`** — Keeps the in-memory database alive as long as the JVM runs, even after the last connection closes. This allows multiple test classes to share the same database.
- **All tables are cleared before each test** — `DatabaseTest` calls `clear*()` methods in `@BeforeEach` to give every test a clean slate.
- **`Database.closeConnection()`** is called in tests to verify reconnection works, but in normal service code the connection stays open for the JVM lifetime.

### Test Database Dependencies (`build.gradle`)

```groovy
dependencies {
    implementation    'org.postgresql:postgresql:42.7.4'  // production
    testRuntimeOnly   'com.h2database:h2:2.2.224'         // tests only — not in production classpath
}
```

---

## Key Differences: File-Based vs Database

| Aspect | Old (XOR files) | New (Database) |
|--------|-----------------|----------------|
| Storage | JSON files encrypted with XOR | PostgreSQL (prod) / H2 (test) |
| Schema | Implicit (JSON structure) | Explicit (11 tables with PKs) |
| Queries | Parse entire file, filter in memory | SQL queries (`SELECT`, `MERGE … USING …`, `DELETE`) |
| Cross-instance sync | Re-read files on every access | Same — query DB on every access |
| Startup | Read files on first access | Auto-create tables on first connection |
| Test isolation | Separate test files | `@BeforeEach` clears all tables |
| External dependency | None | PostgreSQL + Docker (prod); H2 (test, zero setup) |
