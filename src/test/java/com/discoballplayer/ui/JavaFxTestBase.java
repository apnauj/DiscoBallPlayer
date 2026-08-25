package com.discoballplayer.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Starts the JavaFX toolkit once for the whole test run and hands subclasses the two
 * primitives every UI test needs: run something on the FX thread and wait for it, and wait
 * for callbacks that were posted there.
 *
 * <p>No window is ever shown. The controller under test is exercised through a scene graph
 * built by {@code FXMLLoader}, which is what makes these tests catch a missing {@code fx:id}
 * or a binding that was never attached.</p>
 */
abstract class JavaFxTestBase {

    private static final int TIMEOUT_SECONDS = 15;

    private static boolean started;

    static synchronized void startToolkit() {
        if (started) {
            return;
        }
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            // Another test class in the same JVM got here first.
            ready.countDown();
        }
        await(ready, "JavaFX toolkit startup");
        Platform.setImplicitExit(false);
        started = true;
    }

    /**
     * Loads an FXML view on the FX thread and drains the pulse that follows.
     *
     * <p>The flush is the point. A controller's {@code initialize} runs inside {@code load()},
     * and every {@code PlaybackListener} callback it triggers is required to defer its body
     * with {@code Platform.runLater} — so the widgets those callbacks write are still one
     * pulse behind when {@code load()} returns. A test that read them straight away raced the
     * FX thread and passed or failed depending on who got there first.</p>
     *
     * @param resourcePath classpath location of the FXML document
     * @return the loader, so the caller can take both the root and the controller from it
     */
    static FXMLLoader loadView(String resourcePath) {
        URL view = Objects.requireNonNull(
                JavaFxTestBase.class.getResource(resourcePath),
                resourcePath + " is not on the test classpath");
        FXMLLoader loader = new FXMLLoader(view);
        onFxThread(() -> {
            try {
                loader.load();
            } catch (IOException e) {
                throw new IllegalStateException(resourcePath + " failed to load", e);
            }
        });
        flushFxThread();
        return loader;
    }

    /**
     * Runs {@code action} on the FX thread and returns only once it has finished, rethrowing
     * whatever it threw so a failure inside the FX thread fails the test instead of vanishing
     * into an uncaught-exception handler.
     */
    static void onFxThread(Runnable action) {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                thrown.set(t);
            } finally {
                done.countDown();
            }
        });
        await(done, "FX thread action");
        if (thrown.get() != null) {
            fail("action on the FX thread threw", thrown.get());
        }
    }

    /**
     * Waits until everything already queued on the FX thread has run.
     *
     * <p>{@code Platform.runLater} is ordered, so a task enqueued now runs after the callbacks
     * the production code enqueued a moment ago.</p>
     */
    static void flushFxThread() {
        onFxThread(() -> { });
    }

    private static void await(CountDownLatch latch, String what) {
        try {
            assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    what + " did not finish within " + TIMEOUT_SECONDS + " seconds");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("interrupted while waiting for " + what, e);
        }
    }
}
