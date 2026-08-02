# Asynchronous Sign-Up: A Disk-Backed Message Queue

Phase 3 replaces the old "collect email + password, block until the row is
written" sign-up with a small **producer/consumer message queue**. The goal is a
sign-up that *feels* instant: the account (email + password) is created on one
thread so the user can log in right away, while the rest of the profile
(first name, last name, phone) is handed to a **durable queue** and written to
PostgreSQL by a **separate worker thread**.

Everything here lives in `ir.ac.kntu.gui.signup` and only ever calls **existing**
backend services (`PersonaService.registerPersona`, `PersonaService.updateProfile`).
No phase-1/2 code was changed — the backend already stored `first_name`,
`last_name`, `phone` on the `personas` table.

## The three-stage design

`SignupService.submit(...)` splits the interactive path into three concerns
(**A**, **B**, **C**) that run on independent threads so the user gets an
"account created" window the instant their credentials are accepted, and none of
the slower work blocks the UI:

```
RegisterView.handleRegister()
        │  (validate email, names, phone, password on the FX thread)
        ▼
SignupService.submit(...)
        │
        ├── Stage A  (BackgroundJobs pool) ── create the account
        │      PersonaService.registerPersona(email, password)   → account row (login now works)
        │
        │   …on success, back on the FX thread:
        │
        ├── Stage B  (FX thread) ── GUI trigger
        │      onAccountReady.run()                              → "Account created" window opens
        │
        └── Stage C  (scheduled independently) ── profile persistence
               producer:  SignupQueue.enqueue(envelope)          → append to disk + in-memory queue
               consumer:  (signup-profile-worker daemon)
                          loop: envelope = queue.take()          → blocks until work arrives
                                PersonaService.updateProfile(…)  → writes name/phone onto the row
                                queue.markProcessed(envelope)    → drop the line from the spool
```

- **Stage A** creates the account with just email + password. This is the only
  thing the user must wait for before they can sign in.
- **Stage B** fires the moment Stage A reports the credentials are accepted: the
  success window opens *without* waiting for the profile to be persisted.
- **Stage C** enqueues the profile envelope independently of Stage B. Enqueuing
  writes the envelope to a disk spool first (durability) and then to an in-memory
  `BlockingQueue`, which the single long-lived `signup-profile-worker` daemon
  drains via the existing `updateProfile`.

Because Stage C is scheduled independently of the GUI trigger, a queue/disk
hiccup can never surface as a "registration failed" error once the account
already exists — the enqueue failure is logged and the durable spool still
carries the envelope. Splitting the fast path (create account) from the slow
path (persist the full profile) is what keeps the interactive sign-up snappy —
the user is sent to the login screen as soon as the account exists.

## The envelope (`SignupEnvelope`)

An immutable holder for the deferred fields, serialised as one JSON object per
line so the queue can be spooled to disk:

```java
public String toJsonLine() {
    return "{"
            + "\"email\":\"" + escape(email) + "\","
            + "\"firstName\":\"" + escape(firstName) + "\","
            + "\"lastName\":\"" + escape(lastName) + "\","
            + "\"phoneNumber\":\"" + escape(phoneNumber) + "\""
            + "}";
}
```

`fromJsonLine(...)` parses it back. Escaping handles quotes/backslashes/newlines
so names never corrupt the spool.

## The durable queue (`SignupQueue`)

A singleton with two faces: an in-memory `LinkedBlockingQueue` for the worker to
block on, and an on-disk spool (`<project>/signup_queue/pending.jsonl`) so queued
work survives a crash or restart.

```java
public void enqueue(SignupEnvelope envelope) {
    appendToSpool(envelope);   // durable first — one JSON line appended to pending.jsonl
    memory.add(envelope);      // then visible to the worker
}

public SignupEnvelope take() throws InterruptedException {
    return memory.take();      // consumer blocks here until work arrives
}
```

- **Crash recovery**: the constructor calls `recoverFromDisk()`, replaying any
  un-processed lines from `pending.jsonl` back into the in-memory queue, so a
  restart resumes exactly where it left off.
- **Completion**: `markProcessed(envelope)` rewrites the spool without that line
  once the worker has persisted it, so the spool always reflects *outstanding*
  work only.
- All disk access is guarded by a lock and is best-effort — if the disk write
  fails, the in-memory envelope still processes this session.

## The worker (`SignupWorker`)

One daemon thread, started once (idempotent) the first time a sign-up happens:

```java
private void process(SignupEnvelope envelope) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
        try {
            PersonaService.updateProfile(
                    envelope.getEmail(), envelope.getFirstName(),
                    envelope.getLastName(), envelope.getPhoneNumber());
            queue.markProcessed(envelope);
            return;
        } catch (RuntimeException e) {
            if (attempt == MAX_ATTEMPTS) { throw e; }
            sleepQuietly();            // account row not visible yet → back off & retry
        }
    }
}
```

Two robustness properties matter here:

1. **Ordering safety.** `updateProfile` throws `UserNotFoundException` if the
   account row is not visible yet (Thread A might still be mid-flight). The worker
   retries a bounded number of times with a short back-off, so the profile lands
   as soon as the account exists — without the two threads needing to coordinate
   directly.
2. **Fault isolation.** A single bad envelope is logged and dropped
   (`markProcessed`) rather than being allowed to kill the consumer loop.

## Why enqueue *after* register succeeds

`SignupService.submit` runs Stage A (register) first and only reaches Stage C
(enqueue) on success. This deliberately avoids a corruption path: if the email
already exists, `registerPersona` throws, Stage A's error callback fires, and
**no** envelope is queued — so the worker can never overwrite an existing user's
profile with a rejected sign-up's data. Conversely, once the account *does*
exist, a Stage C enqueue failure is logged rather than reported to the user: the
account is real and login works, so surfacing a queue error would be misleading.

## Testing many users at once — the concurrency test

`SignupQueueConcurrencyTest` (`src/test/java/ir/ac/kntu/signup/`) is the
"many sign-up windows at the same instant" stress test. It reproduces the
producer/consumer flow **without a JavaFX stage**, so it runs headless and is
*not* excluded by `-PskipGuiTests` (it lives in `ir.ac.kntu.signup`, not
`ir.ac.kntu.gui`).

### How to run it

```bash
# Just this test, with its terminal logs visible:
./gradlew test --tests ir.ac.kntu.signup.SignupQueueConcurrencyTest -i

# Headless: skip the TestFX GUI tests, run everything else (incl. this one):
./gradlew test -PskipGuiTests
```

On Windows use `gradlew.bat` if you are not in Git Bash. The `-i` (info) flag
surfaces the `[test]`, `[SignupQueue]`, and `[SignupWorker]` `System.out` lines
so you can watch the queue fill and drain.

### What it exercises, in code

Constants at the top set the load: **`SIGNUP_COUNT = 60`** envelopes fired across
**`PRODUCER_THREADS = 12`** threads.

1. **Clean slate** (`@BeforeEach`): `PersonaService.reset()` clears the account
   store and the on-disk spool `signup_queue/pending.jsonl` is deleted, so
   recovered envelopes from an earlier run can't inflate the counts.
2. **Start the consumer**: `SignupWorker.start()` spins up the single
   `signup-profile-worker` daemon; `processedCount()` is snapshotted as
   `processedBefore`.
3. **Line the producers up on a gate**: 60 tasks are submitted to a 12-thread
   pool, each blocked on a `startGate` latch. A second latch, `producedGate`,
   counts down once per task so the test knows when every producer has finished.
4. **Release them all at once**: `startGate.countDown()` unblocks all 60
   simultaneously. Each producer runs the same two steps as Stage A + Stage C of
   `SignupService.submit`, minus the FX layer:
   `PersonaService.registerPersona(email, password)` then
   `queue.enqueue(new SignupEnvelope(...))`, incrementing an `enqueued` counter
   (and an `AtomicInteger` of failures for the assertion).
5. **Wait for the burst to be enqueued**: `producedGate.await(30s)` — a timeout
   here fails the test. Then it asserts zero producer failures.
6. **Wait for the single consumer to drain**: it polls up to 400×50 ms (≈20 s)
   until `processedCount() - processedBefore >= expected` **and**
   `pendingCount() == 0`.

### What it proves (the logic)

- **Completeness** — every one of the 60 envelopes is processed
  (`drained` is true) and, crucially, every profile is actually persisted: the
  test loops over all 60 emails and asserts `getFirstName()`/`getLastName()`
  match what was enqueued. This is the real guarantee — not just that the queue
  emptied, but that the worker wrote each profile onto the right account row.
- **Performance / no O(N²) regression** — draining the whole burst must finish in
  **under 20 s** (`assertTrue(totalMillis < 20_000)`). Before the fix,
  `markProcessed` rewrote the *entire* spool on every call under the same lock
  that serialised producers' appends — O(N²) disk work that made bursts crawl.
  The [deferred compaction](#the-durable-queue-signupqueue) change makes the hot
  path O(1) amortised, and this bound is what guards it from creeping back.
- **Correctness under contention** — 12 threads register and enqueue at the same
  instant while one worker drains concurrently. The test passing means the
  shared `SignupQueue` singleton (its `BlockingQueue`, disk lock, and
  `processedBuffer`) is thread-safe under real contention.

The `startGate`/`producedGate` latch pair is the key trick: rather than creating
threads in a loop (which drift apart in start time), all 60 producers park on the
gate and are released in one `countDown()`, so the burst genuinely hits the queue
concurrently instead of trickling in.

## Where to see it run

- Register a new member in the GUI with all fields → the success dialog appears
  immediately (account ready), and moments later the profile row carries the
  name/phone.
- `signup_queue/pending.jsonl` is created under the project directory while an
  envelope is in flight and is emptied once the worker finishes.

## Files

| File | Role |
|------|------|
| `gui/signup/SignupEnvelope.java` | The message: deferred profile fields + JSON (de)serialisation |
| `gui/signup/SignupQueue.java`    | Durable producer/consumer queue (in-memory + disk spool, recovery) |
| `gui/signup/SignupWorker.java`   | The consumer daemon thread (retry + fault isolation) |
| `gui/signup/SignupService.java`  | Orchestrates the three stages: create (A), GUI trigger (B), enqueue (C) |
| `gui/view/RegisterView.java`     | Collects the fields and calls `SignupService.submit` |
| `test/.../signup/SignupQueueConcurrencyTest.java` | Headless burst test: 60 concurrent sign-ups drain completely and quickly |
| `test/.../gui/SignupFlowTest.java` | TestFX end-to-end: register in the GUI, account is instant, profile lands shortly after |

See `docs/threads.md` for the wider GUI threading model this builds on.
