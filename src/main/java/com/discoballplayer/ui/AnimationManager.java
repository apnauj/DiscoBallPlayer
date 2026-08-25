package com.discoballplayer.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;

/**
 * The single switch for every animation in the window.
 *
 * <p>Turning the floor off is one call: {@code AnimationManager.setEnabled(false)}. Nothing
 * else in the UI starts an animation directly, so there is no second place to look, and a
 * manager built while animations are off hands back animations that are already stopped rather
 * than making every caller check a flag first.</p>
 *
 * <p>Every animation an instance creates is remembered, because an infinite one never ends on
 * its own. A view that is thrown away without {@link #stopAll()} leaves timelines running on
 * the FX thread for the life of the process -- invisible, and paid for on every pulse.</p>
 */
final class AnimationManager {

    /** Whether a freshly started application dances. The single point the brief asked for. */
    private static final boolean ENABLED_BY_DEFAULT = true;

    private static boolean enabled = ENABLED_BY_DEFAULT;

    private final List<Animation> running = new ArrayList<>();

    /** Turns every animation in the application on or off. Already-running ones are stopped. */
    static void setEnabled(boolean value) {
        enabled = value;
    }

    static boolean isEnabled() {
        return enabled;
    }

    /** Restores the built-in setting, so a test cannot leak its choice into the next one. */
    static void resetToDefault() {
        enabled = ENABLED_BY_DEFAULT;
    }

    /**
     * The mirror ball: one turn every {@code seconds}, for as long as the window is open.
     *
     * @param node    the ball to spin
     * @param seconds how long a full turn takes
     */
    Animation spin(Node node, double seconds) {
        RotateTransition spin = new RotateTransition(Duration.seconds(seconds), node);
        spin.setByAngle(360);
        spin.setCycleCount(Animation.INDEFINITE);
        spin.setInterpolator(Interpolator.LINEAR);
        return start(spin);
    }

    /**
     * A light that swells and fades on its own clock.
     *
     * <p>The phase offset matters: two lights pulsing in step read as one flashing panel, not
     * as a room with lights in it.</p>
     */
    Animation pulse(Node node, double from, double to, double seconds, double delaySeconds) {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), from)),
                new KeyFrame(Duration.seconds(seconds), new KeyValue(node.opacityProperty(), to)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setDelay(Duration.seconds(delaySeconds));
        return start(pulse);
    }

    /**
     * A glow that breathes.
     *
     * <p>The effect is created here and handed back rather than read off the node, because a
     * stylesheet rule owns {@code -fx-effect} on anything it matches: an effect set from Java on
     * such a node survives until the first hover and is then replaced. Callers apply this to
     * nodes no rule touches -- the transport glyph, the now-playing title.</p>
     *
     * <p><strong>The radius is fixed and the spread is what moves.</strong> A drop shadow's
     * bounds are computed from its radius, and a node's bounds feed the layout of everything
     * around it: animating the radius on the transport glyph grew the button, then the bar,
     * then pushed the table 22 pixels every time the glow swelled -- the whole window breathing
     * along with it. Spread changes how solid the light is inside bounds that never move, which
     * looks the same and costs no layout at all.</p>
     *
     * @return the glow to hang on the node, already breathing
     */
    DropShadow breathingGlow(Node node, javafx.scene.paint.Color colour, double radius,
                             double fromSpread, double toSpread, double seconds) {
        DropShadow glow = new DropShadow();
        glow.setColor(colour);
        glow.setRadius(radius);
        glow.setSpread(fromSpread);
        node.setEffect(glow);

        Timeline breath = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.spreadProperty(), fromSpread)),
                new KeyFrame(Duration.seconds(seconds), new KeyValue(glow.spreadProperty(), toSpread)));
        breath.setAutoReverse(true);
        breath.setCycleCount(Animation.INDEFINITE);
        start(breath);
        return glow;
    }

    /**
     * Grows a node slightly while the pointer is over it.
     *
     * <p>Not registered for {@link #stopAll()}: these are finite, they only run on a hover, and
     * holding a reference to one per button would keep every button alive in this list.</p>
     */
    void hoverLift(Node node, double scale) {
        node.setOnMouseEntered(event -> scaleTo(node, scale));
        node.setOnMouseExited(event -> scaleTo(node, 1));
    }

    private void scaleTo(Node node, double scale) {
        if (!enabled) {
            return;
        }
        ScaleTransition lift = new ScaleTransition(Duration.millis(120), node);
        lift.setToX(scale);
        lift.setToY(scale);
        lift.play();
    }

    /**
     * The entrance: a node fades up while it rises the last few pixels into place.
     *
     * <p>Finite, so it is not registered -- it ends on its own and holding it would pin the node
     * it moved.</p>
     */
    void enter(Node node, double fromBelow, double seconds) {
        if (!enabled) {
            node.setOpacity(1);
            node.setTranslateY(0);
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.seconds(seconds), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition rise = new TranslateTransition(Duration.seconds(seconds), node);
        rise.setFromY(fromBelow);
        rise.setToY(0);

        ParallelTransition entrance = new ParallelTransition(fade, rise);
        entrance.setInterpolator(Interpolator.EASE_OUT);
        entrance.play();
    }

    /** Stops everything this manager started and forgets it. */
    void stopAll() {
        running.forEach(Animation::stop);
        running.clear();
    }

    /** Visible for tests: how many endless animations this manager is keeping alive. */
    int runningCount() {
        return running.size();
    }

    /** Visible for tests: whether anything is actually moving, rather than merely registered. */
    boolean anyPlaying() {
        return running.stream().anyMatch(a -> a.getStatus() == Animation.Status.RUNNING);
    }

    /** Visible for tests: the endless animations, so a test can inspect what they animate. */
    List<Animation> registered() {
        return List.copyOf(running);
    }

    private Animation start(Animation animation) {
        Objects.requireNonNull(animation, "there is no animation to start");
        running.add(animation);
        if (enabled) {
            animation.play();
        }
        return animation;
    }
}
