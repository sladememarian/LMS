package ir.ac.kntu.signup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ir.ac.kntu.gui.signup.SignupEnvelope;
import ir.ac.kntu.gui.signup.SignupQueue;
import ir.ac.kntu.gui.signup.SignupWorker;
import ir.ac.kntu.persona.Persona;
import ir.ac.kntu.persona.PersonaService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency + performance test for the async sign-up message queue (item 11).
 *
 * <p>Many "sign-up windows" register at the same instant: this fires a burst of
 * concurrent producers, each creating an account and enqueuing a profile
 * envelope, exactly as {@code SignupService.submit} does. It then proves the
 * single {@link SignupWorker} consumer drains the whole burst quickly and every
 * profile is persisted, guarding against the old O(N&sup2;) spool rewrite that
 * made bursts crawl.</p>
 *
 * <p>This is a headless backend test (no JavaFX stage) — the queue and worker
 * live in the GUI layer but have no UI dependency, so it lives outside the
 * {@code ir.ac.kntu.gui} package to still run under {@code -PskipGuiTests}. Its
 * terminal logs show the queue/worker state as required.</p>
 */
public class SignupQueueConcurrencyTest {

    private static final int SIGNUP_COUNT = 60;
    private static final int PRODUCER_THREADS = 12;

    @BeforeEach
    void resetDb() throws Exception {
        PersonaService.reset();
        Persona.setCurrentUser(null);
        // Drop any spool left over from a previous run so the worker starts this
        // test with an empty on-disk queue (otherwise recovered envelopes inflate
        // the pending/processed counts).
        Path spool = Paths.get(System.getProperty("user.dir"), "signup_queue", "pending.jsonl");
        Files.deleteIfExists(spool);
    }

    @Test
    public void concurrentSignupsAreProcessedQuicklyAndCompletely() throws InterruptedException {
        SignupWorker.start();
        SignupQueue queue = SignupQueue.getInstance();
        long processedBefore = queue.processedCount();

        System.out.println("[test] firing " + SIGNUP_COUNT + " concurrent sign-ups across "
                + PRODUCER_THREADS + " producer threads");

        ExecutorService producers = Executors.newFixedThreadPool(PRODUCER_THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch producedGate = new CountDownLatch(SIGNUP_COUNT);
        AtomicInteger enqueued = new AtomicInteger();
        AtomicInteger producerFailures = new AtomicInteger();
        long start = System.nanoTime();

        for (int i = 0; i < SIGNUP_COUNT; i++) {
            final int index = i;
            producers.submit(() -> {
                try {
                    startGate.await();
                    String email = "burst" + index + "@test.local";
                    // Same two stages as SignupService.submit, minus the FX layer:
                    // create the account fast, then enqueue the profile envelope.
                    PersonaService.registerPersona(email, "Secure@123");
                    queue.enqueue(new SignupEnvelope(
                            email, "First" + index, "Last" + index, "09123456789"));
                    enqueued.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    producerFailures.incrementAndGet();
                    System.err.println("[test] producer " + index + " failed: " + e);
                } finally {
                    producedGate.countDown();
                }
            });
        }

        startGate.countDown(); // release all producers at once
        assertTrue(producedGate.await(30, TimeUnit.SECONDS),
                "producers did not finish enqueuing in time");
        assertEquals(0, producerFailures.get(), "some producers failed to register/enqueue");
        int expected = enqueued.get();
        long enqueueMillis = (System.nanoTime() - start) / 1_000_000L;
        System.out.println("[test] all " + expected + " envelopes enqueued in "
                + enqueueMillis + "ms; pending=" + queue.pendingCount());

        // Wait for the single consumer to drain the whole burst.
        boolean drained = false;
        for (int attempt = 0; attempt < 400 && !drained; attempt++) {
            if (queue.processedCount() - processedBefore >= expected
                    && queue.pendingCount() == 0) {
                drained = true;
            } else {
                Thread.sleep(50);
            }
        }
        long totalMillis = (System.nanoTime() - start) / 1_000_000L;
        producers.shutdownNow();

        System.out.println("[test] worker drained the queue in " + totalMillis
                + "ms (processed=" + (queue.processedCount() - processedBefore) + ")");

        assertEquals(SIGNUP_COUNT, expected, "not every producer enqueued an envelope");
        assertTrue(drained, "worker did not drain " + expected
                + " envelopes; pending=" + queue.pendingCount());
        // A generous bound: 60 profile writes on a single worker with amortised
        // O(1) spool maintenance finish comfortably within this budget even on a
        // cold CI machine (observed per-write cost is ~1-3ms).
        assertTrue(totalMillis < 20_000,
                "draining the burst took too long: " + totalMillis + "ms");

        // Every profile must actually be persisted.
        for (int i = 0; i < SIGNUP_COUNT; i++) {
            String email = "burst" + i + "@test.local";
            Persona profile = PersonaService.getProfile(email);
            assertNotNull(profile, "missing account for " + email);
            assertEquals("First" + i, profile.getFirstName(), "first name not persisted for " + email);
            assertEquals("Last" + i, profile.getLastName(), "last name not persisted for " + email);
        }
    }
}
