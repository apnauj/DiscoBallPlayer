package com.discoballplayer.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.discoballplayer.service.DemoPlayerService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the transport buttons and the mode selector by firing the real controls, so the
 * {@code onAction} wiring in the FXML is part of what is under test.
 *
 * <p>The disabled Previous button in arrival order is the visible proof that a queue cannot
 * go backwards, and it is graded, so it is asserted from three directions: it is off in
 * arrival order, on in shuffle, and it follows the service rather than a hard-coded mode
 * check.</p>
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class TransportControlsTest extends JavaFxTestBase {

    private static final int DEMO_LIBRARY_SIZE = 12;

    private MainController controller;
    private Parent root;

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
    }

    @BeforeEach
    void loadTheView() {
        onFxThread(() -> {
            URL view = Objects.requireNonNull(
                    MainController.class.getResource("/com/discoballplayer/fxml/main-view.fxml"),
                    "main-view.fxml is not on the test classpath");
            FXMLLoader loader = new FXMLLoader(view);
            try {
                root = loader.load();
            } catch (IOException e) {
                throw new IllegalStateException("main-view.fxml failed to load", e);
            }
            controller = loader.getController();
        });
    }

    @AfterEach
    void stopTheTicker() {
        if (controller.player() instanceof DemoPlayerService demo) {
            demo.shutdown();
        }
    }

    // ---- the graded moment -----------------------------------------------

    @Test
    void previousIsDisabledInArrivalOrder() {
        press("arrivalModeButton");

        assertTrue(button("previousButton").isDisabled(),
                "a queue cannot go backwards, and the button has to show it");
    }

    @Test
    void previousIsEnabledInShuffle() {
        press("arrivalModeButton");
        press("shuffleModeButton");

        assertFalse(button("previousButton").isDisabled());
    }

    @Test
    void previousStaysDisabledThroughoutAnArrivalRun() {
        press("arrivalModeButton");

        for (int played = 0; played < 5; played++) {
            press("nextButton");
            assertTrue(button("previousButton").isDisabled(),
                    "still disabled after " + (played + 1) + " songs");
        }
    }

    @Test
    void alphabeticalHasNoPreviousAtTheFirstSongAndOneAfterwards() {
        press("alphabeticalModeButton");

        press("nextButton");
        assertTrue(button("previousButton").isDisabled(), "nothing precedes the first title");

        press("nextButton");
        assertFalse(button("previousButton").isDisabled());
    }

    // ---- exhaustion ------------------------------------------------------

    @Test
    void arrivalOrderReportsAFinishedQueueInsteadOfThrowing() {
        press("arrivalModeButton");

        for (int played = 0; played < DEMO_LIBRARY_SIZE; played++) {
            press("nextButton");
        }
        assertNotEquals("Queue finished", status().getText(), "the last song is still playable");

        press("nextButton");

        assertEquals("Queue finished", status().getText());
    }

    @Test
    void shuffleNeverRunsOut() {
        press("shuffleModeButton");

        for (int played = 0; played < DEMO_LIBRARY_SIZE * 2; played++) {
            press("nextButton");
        }

        assertNotEquals("Queue finished", status().getText(), "the ring wraps forever");
    }

    // ---- play and pause --------------------------------------------------

    @Test
    void playPauseLabelFollowsTheService() {
        assertEquals("Play", button("playPauseButton").getText());

        press("playPauseButton");
        assertEquals("Pause", button("playPauseButton").getText());

        press("playPauseButton");
        assertEquals("Play", button("playPauseButton").getText());
    }

    @Test
    void playStartsTheFirstSongWhenNothingIsPlayingYet() {
        press("shuffleModeButton");
        assertEquals("Nothing playing", label("nowPlayingTitle").getText());

        press("playPauseButton");

        assertNotEquals("Nothing playing", label("nowPlayingTitle").getText());
    }

    // ---- navigation ------------------------------------------------------

    @Test
    void nextChangesTheSongOnShow() {
        press("shuffleModeButton");

        press("nextButton");
        String first = label("nowPlayingTitle").getText();
        press("nextButton");

        assertNotEquals(first, label("nowPlayingTitle").getText());
    }

    @Test
    void nextThenPreviousReturnsToTheSameSongInShuffle() {
        press("shuffleModeButton");
        press("nextButton");
        String first = label("nowPlayingTitle").getText();

        press("nextButton");
        press("previousButton");

        assertEquals(first, label("nowPlayingTitle").getText());
    }

    @Test
    void choosingAModeClearsWhateverTheLastModeWasShowing() {
        press("shuffleModeButton");
        press("nextButton");
        assertNotEquals("Nothing playing", label("nowPlayingTitle").getText());

        press("alphabeticalModeButton");

        assertEquals("Nothing playing", label("nowPlayingTitle").getText());
        assertEquals("No song loaded", status().getText());
    }

    @Test
    void alphabeticalVisitsTitlesInOrder() {
        press("alphabeticalModeButton");

        press("nextButton");
        String first = label("nowPlayingTitle").getText();
        press("nextButton");
        String second = label("nowPlayingTitle").getText();

        assertTrue(first.compareToIgnoreCase(second) < 0,
                first + " should sort before " + second);
    }

    // ---- helpers ---------------------------------------------------------

    private void press(String id) {
        onFxThread(() -> {
            javafx.scene.Node node = root.lookup("#" + id);
            if (node instanceof RadioButton radio) {
                radio.fire();
            } else {
                ((Button) node).fire();
            }
        });
        flushFxThread();
    }

    private Button button(String id) {
        return (Button) root.lookup("#" + id);
    }

    private Label label(String id) {
        return (Label) root.lookup("#" + id);
    }

    private Label status() {
        return label("statusLabel");
    }
}
