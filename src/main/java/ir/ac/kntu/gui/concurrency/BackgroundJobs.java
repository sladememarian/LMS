package ir.ac.kntu.gui.concurrency;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.concurrent.Task;

// Central place for running work OFF the JavaFX Application Thread. No slow /
// DB / heavy-Stream operation may run on the FX thread or the UI freezes, so
// each one is wrapped in a JavaFX Task on a shared daemon ExecutorService. The
// success and failure callbacks are always delivered back on the FX thread, so
// callers can safely touch the scene graph inside them.
public final class BackgroundJobs {

    private static final ThreadFactory DAEMON_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "lms-bg-worker");
        thread.setDaemon(true);
        return thread;
    };

    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(4, DAEMON_FACTORY);

    private BackgroundJobs() {
        // utility class
    }

    // Runs work in the background. On success onSuccess runs on the FX thread
    // with the result; if it throws, onError runs on the FX thread with the
    // exception. Returns the running Task (handy for binding a ProgressIndicator).
    public static <T> Task<T> run(Supplier<T> work,
                                  Consumer<T> onSuccess,
                                  Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(event -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (onError != null) {
                onError.accept(task.getException());
            }
        });
        EXECUTOR.submit(task);
        return task;
    }

    // Convenience overload for work that returns nothing.
    public static Task<Void> runAction(Runnable work,
                                       Runnable onSuccess,
                                       Consumer<Throwable> onError) {
        return run(
                () -> {
                    work.run();
                    return null;
                },
                ignored -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                },
                onError);
    }

    // Marshals a snippet onto the FX thread (thin wrapper over Platform.runLater).
    public static void onFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    // Call once on application shutdown to stop the worker pool cleanly.
    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
