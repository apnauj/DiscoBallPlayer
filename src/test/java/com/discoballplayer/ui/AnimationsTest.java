package com.discoballplayer.ui;

import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.animation.Timeline;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the dance floor: the single switch, what starts it, and the two ways an animation can
 * quietly wreck a window that is otherwise laid out correctly.
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class AnimationsTest extends JavaFxTestBase {

    private MainController controller;
    private Parent root;

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
    }

    @AfterAll
    static void restoreTheDefault() {
        AnimationManager.resetToDefault();
    }

    @BeforeEach
    void loadTheView() {
        AnimationManager.resetToDefault();
        FXMLLoader loader = loadView("/com/discoballplayer/fxml/main-view.fxml");
        root = loader.getRoot();
        controller = loader.getController();
    }

    @AfterEach
    void stopTheFloor() {
        onFxThread(() -> controller.stopAnimations());
        AnimationManager.resetToDefault();
    }

    // ---- the switch ------------------------------------------------------

    /**
     * An endless animation on a tree nobody is looking at costs a slice of every pulse for the
     * life of the process. A view is built far more often in tests than it is shown, so the
     * floor waits for a scene rather than starting on construction.
     */
    @Test
    void nothingDancesUntilTheViewIsShown() {
        assertEquals(0, controller.animations().runningCount(),
                "a view that was never shown should not be animating anything");
    }

    @Test
    void showingTheViewStartsTheFloor() {
        attachAScene();
        assertTrue(controller.animations().anyPlaying(), "nothing started");
    }

    /** The one call the brief asked for: everything off from a single point. */
    @Test
    void theSingleSwitchHoldsTheFloorStill() {
        AnimationManager.setEnabled(false);
        attachAScene();

        assertTrue(controller.animations().runningCount() > 0,
                "the animations should still be registered, merely not playing");
        assertFalse(controller.animations().anyPlaying(),
                "the switch is off and something is still moving");
    }

    @Test
    void stoppingLeavesNothingBehind() {
        attachAScene();
        onFxThread(() -> controller.stopAnimations());

        assertEquals(0, controller.animations().runningCount(), "an animation outlived the view");
    }

    // ---- the two faults an animation can hide ----------------------------

    /**
     * The mirror ball must not be part of the layout.
     *
     * <p>A Group is sized by its contents, and this one's contents are a glow and a mount rod
     * that sticks out above the sphere. Laid out normally, the sidebar's minimum height came to
     * depend on a decoration and settled only on the second CSS pass -- the window grew eleven
     * pixels every time the theme was toggled.</p>
     */
    @Test
    void theBallIsDecorationRatherThanLayout() {
        attachAScene();
        Node ball = onFxThreadGet(() -> ((StackPane) root.lookup("#discoBallSlot"))
                .getChildren().get(0));

        assertFalse(ball.isManaged(), "the ball is laid out, so its glow can resize the window");
    }

    /** Drawn, not shipped: it takes its colours from the theme, which a PNG could not. */
    @Test
    void theBallIsDrawnRatherThanShipped() {
        Node ball = onFxThreadGet(() -> ((StackPane) root.lookup("#discoBallSlot"))
                .getChildren().get(0));

        assertTrue(ball instanceof DiscoBall, "the ball is not the drawn one");
        assertFalse(ball.lookupAll("*").stream().anyMatch(node -> node instanceof ImageView),
                "the ball ships an image");
    }

    /**
     * The breathing glow must not move anything.
     *
     * <p>A drop shadow's bounds come from its radius, and a node's bounds feed the layout around
     * it. Animating the radius on the transport glyph grew the button, then the bar, then pushed
     * the table -- the window breathing along with the button. Spread changes how solid the
     * light is inside bounds that never move.</p>
     */
    @Test
    void theBreathingGlowLeavesTheLayoutAlone() {
        attachAScene();
        Node glyph = onFxThreadGet(() -> root.lookup("#playPauseIcon"));
        DropShadow glow = (DropShadow) onFxThreadGet(glyph::getEffect);
        assertTrue(glow != null, "the transport glyph has no glow to breathe");

        Bounds atRest = onFxThreadGet(() -> { glow.setSpread(0); return glyph.getLayoutBounds(); });
        Bounds atFull = onFxThreadGet(() -> { glow.setSpread(1); return glyph.getLayoutBounds(); });
        assertEquals(atRest, atFull, "spread should not resize the glyph");

        // The assertion that actually holds the line. Comparing bounds while driving the
        // spread proves spread is safe; it says nothing about what the timeline moves. Naming
        // the property does, and the property is the whole difference between a glow that
        // breathes and a window that does.
        boolean touchesRadius = controller.animations().registered().stream()
                .filter(animation -> animation instanceof Timeline)
                .map(animation -> (Timeline) animation)
                .flatMap(timeline -> timeline.getKeyFrames().stream())
                .flatMap(frame -> frame.getValues().stream())
                .anyMatch(value -> value.getTarget() == glow.radiusProperty());

        assertFalse(touchesRadius,
                "the glow breathes its radius, which resizes the node and relayouts the window");
    }

    // ---- helpers ---------------------------------------------------------

    private void attachAScene() {
        onFxThread(() -> {
            Scene scene = new Scene(root, 1180, 720);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/discoballplayer/css/app.css")).toExternalForm());
            root.applyCss();
            root.layout();
        });
        flushFxThread();
    }

    private static <T> T onFxThreadGet(java.util.function.Supplier<T> supplier) {
        java.util.List<T> box = new java.util.ArrayList<>(1);
        onFxThread(() -> box.add(supplier.get()));
        return box.get(0);
    }
}
