package ir.ac.kntu.gui.signup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 * <p>Everything here lives in the GUI layer and only ever calls existing backend
 * services from the worker — no phase-1/2 code is modified.</p>
 */
public final class SignupQueue {

    private static final SignupQueue INSTANCE = new SignupQueue();

    private final BlockingQueue<SignupEnvelope> memory = new LinkedBlockingQueue<>();
    private final Path spoolFile;
    private final Object diskLock = new Object();

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
        memory.add(envelope);
    }

    /** Consumer side. Blocks until an envelope is available. */
    public SignupEnvelope take() throws InterruptedException {
        return memory.take();
    }

    /**
     * Marks an envelope as done by rewriting the spool without the first line
     * that matches it. Called by the worker after the profile has been persisted.
     */
    public void markProcessed(SignupEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        synchronized (diskLock) {
            List<String> remaining = readSpoolLines();
            String target = envelope.toJsonLine();
            remaining.remove(target);
            writeSpoolLines(remaining);
        }
    }

    /** Number of envelopes still waiting in memory (diagnostics/tests). */
    public int pendingCount() {
        return memory.size();
    }

    // --- disk helpers -----------------------------------------------------

    private void recoverFromDisk() {
        synchronized (diskLock) {
            for (String line : readSpoolLines()) {
                SignupEnvelope env = SignupEnvelope.fromJsonLine(line);
                if (env != null) {
                    memory.add(env);
                }
            }
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

    private List<String> readSpoolLines() {
        if (!Files.exists(spoolFile)) {
            return new ArrayList<>();
        }
        try {
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(spoolFile, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            System.err.println("SignupQueue: could not read spool: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void writeSpoolLines(List<String> lines) {
        try {
            Files.createDirectories(spoolFile.getParent());
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line).append(System.lineSeparator());
            }
            Files.write(spoolFile, sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("SignupQueue: could not rewrite spool: " + e.getMessage());
        }
    }
}
