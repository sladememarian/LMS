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

## The two-thread design

```
RegisterView.handleRegister()
        │  (validate email, names, phone, password on the FX thread)
        ▼
SignupService.submit(...)
        │
        ├── Thread A  (BackgroundJobs pool) ── PRODUCER
        │      1. PersonaService.registerPersona(email, password)   → account row (login now works)
        │      2. SignupQueue.enqueue(envelope)                     → append to disk + in-memory queue
        │
        └── Thread B  (signup-profile-worker) ── CONSUMER
               loop: envelope = queue.take()                        → blocks until work arrives
                     PersonaService.updateProfile(email, …)         → writes name/phone onto the row
                     queue.markProcessed(envelope)                  → drop the line from the spool
```

- **Thread A** creates the account *and* enqueues the profile envelope. Enqueuing
  writes the envelope to a disk spool first (durability) and then to an in-memory
  `BlockingQueue`.
- **Thread B** is a single long-lived daemon that blocks on the queue and applies
  each envelope to the account via the existing `updateProfile`.

Splitting the fast path (create account) from the slow path (persist the full
profile) is what makes the interactive sign-up snappy — the user is sent to the
login screen as soon as the account exists, without waiting on the profile write.

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

`SignupService.submit` registers first and enqueues the envelope in the same
background action, on success. This deliberately avoids a corruption path: if the
email already exists, `registerPersona` throws and **no** envelope is queued — so
the worker can never overwrite an existing user's profile with a rejected
sign-up's data.

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
| `gui/signup/SignupService.java`  | Orchestrates register (fast) + enqueue (deferred) |
| `gui/view/RegisterView.java`     | Collects the fields and calls `SignupService.submit` |

See `docs/threads.md` for the wider GUI threading model this builds on.
