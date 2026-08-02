package ir.ac.kntu.gui.signup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Durable producer/consumer queue for sign-up profile envelopes.
 *
 * <p>This is the message-queue at the heart of the async sign-up design. It has
 * two faces:</p>
 * <ul>
 *   <li>an in-memory {@link BlockingQueue} that lets the single {@link
 *       SignupWorker} consumer block in {@link #take()} until work arrives; and</li>
 *   <li>an on-disk <em>spool</em> ({@code signup_queue/pending.jsonl}, one
 *       envelope per line) so queued-but-unprocessed work survives a crash or
 *       restart. On construction the spool is replayed back into the in-memory
 *       queue; once the worker finishes an envelope it calls {@link
 *       #markProcessed(SignupEnvelope)} to drop that line from the spool.</li>
 * </ul>
 *
 * <p><b>Performance:</b> the previous implementation rewrote the <em>entire</em>
 * spool file on every {@link #markProcessed} call, all under {@code diskLock} —
 * which also serialised producers appending in {@link #enqueue}. With a burst of
 * <var>N</var> concurrent sign-ups that is O(N&sup2;) disk work, so opening many
 * sign-up windows at once made the whole flow crawl. It now uses <em>deferred
 * compaction</em>: processed envelopes are buffered in memory and the spool is
 * rewritten only when the buffer crosses a threshold or the queue drains, turning
 * the hot path back into O(1) amortised. Progress is logged to the terminal so the
 * queue/worker state is observable.</p>
 *
 * <p>Everything here lives in the GUI layer and only ever calls existing backend
 * services from the worker — no phase-1/2 code is modified.</p>
 */
public final class SignupQueue {

    private static final SignupQueue INSTANCE = new SignupQueue();

    /** Rewrite the spool after this many processed envelopes accumulate. */
    private static final int COMPACT_THRESHOLD = 50;

    private final BlockingQueue<SignupEnvelope> memory = new LinkedBlockingQueue<>();
    private final Path spoolFile;
    private final Object diskLock = new Object();

    /** Lines already handled by the worker, buffered until the next compaction. */
    private final Set<String> processedBuffer = new HashSet<>();

    // Lifetime counters for observability (logged to the terminal).
    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong processed = new AtomicLong();

    private SignupQueue() {
        this.spoolFile = Paths.get(System.getProperty("user.dir"), "signup_queue", "pending.jsonl");
        recoverFromDisk();
    }

    public static SignupQueue getInstance() {
        return INSTANCE;
    }

    /**
     * Producer side. Appends the envelope to the disk spool first (so it is
     * durable before we acknowledge it), then hands it to the in-memory queue
     * for the worker to pick up.
     */
    public void enqueue(SignupEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        appendToSpool(envelope);
        SignupLog.step(SignupLog.THREAD_C, "envelope made for " + envelope.getEmail());
        memory.add(envelope);
        long total = enqueued.incrementAndGet();
        SignupLog.step(SignupLog.THREAD_C, "envelope injected in queue (pending="
                + memory.size() + ", totalEnqueued=" + total + ")");
    }

    /** Consumer side. Blocks until an envelope is available. */
    public SignupEnvelope take() throws InterruptedException {
        return memory.take();
    }

    /**
     * Marks an envelope as done. Instead of rewriting the whole spool on every
     * call, the line is buffered and the spool is compacted only when the buffer
     * crosses {@link #COMPACT_THRESHOLD} or the in-memory queue has drained. This
     * keeps the worker's hot path O(1) amortised even under a burst of sign-ups.
     */
    public void markProcessed(SignupEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        long total = processed.incrementAndGet();
        synchronized (diskLock) {
            processedBuffer.add(envelope.toJsonLine());
            boolean drained = memory.isEmpty();
            if (processedBuffer.size() >= COMPACT_THRESHOLD || drained) {
                compact();
            }
        }
        SignupLog.step(SignupLog.THREAD_C, "envelope removed from queue for "
                + envelope.getEmail() + " (pending=" + memory.size()
                + ", totalProcessed=" + total + ")");
    }

    /** Number of envelopes still waiting in memory (diagnostics/tests). */
    public int pendingCount() {
        return memory.size();
    }

    /** Total envelopes handed to the worker so far (diagnostics/tests). */
    public long processedCount() {
        return processed.get();
    }

    // --- disk helpers -----------------------------------------------------

    private void recoverFromDisk() {
        synchronized (diskLock) {
            readSpoolLines().stream()
                    .map(SignupEnvelope::fromJsonLine)
                    .filter(java.util.Objects::nonNull)
                    .forEach(memory::add);
        }
    }

    private void appendToSpool(SignupEnvelope envelope) {
        synchronized (diskLock) {
            try {
                Files.createDirectories(spoolFile.getParent());
                Files.write(spoolFile,
                        (envelope.toJsonLine() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                // Durability is best-effort; the in-memory queue still carries the
                // envelope so the worker can process it this session.
                System.err.println("SignupQueue: could not append to spool: " + e.getMessage());
            }
        }
    }

    /** Rewrites the spool without any buffered-processed lines. Caller holds diskLock. */
    private void compact() {
        if (processedBuffer.isEmpty()) {
            return;
        }
        List<String> remaining = readSpoolLines().stream()
                .filter(line -> !processedBuffer.contains(line))
                .collect(java.util.stream.Collectors.toList());
        writeSpoolLines(remaining);
        SignupLog.step(SignupLog.THREAD_C, "spool compacted (removed="
                + processedBuffer.size() + ", remainingOnDisk=" + remaining.size() + ")");
        processedBuffer.clear();
    }

    private List<String> readSpoolLines() {
        if (!Files.exists(spoolFile)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(spoolFile, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        } catch (IOException e) {
            System.err.println("SignupQueue: could not read spool: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void writeSpoolLines(List<String> lines) {
        try {
            Files.createDirectories(spoolFile.getParent());
            String content = lines.stream()
                    .collect(java.util.stream.Collectors.joining(System.lineSeparator(), "",
                            lines.isEmpty() ? "" : System.lineSeparator()));
            Files.write(spoolFile, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("SignupQueue: could not rewrite spool: " + e.getMessage());
        }
    }
}
