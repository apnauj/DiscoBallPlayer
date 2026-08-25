package com.discoballplayer.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the disco retro theme: the palette it is supposed to wear, and the one class of
 * mistake that a stylesheet can make invisible to every other test.
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class DiscoThemeTest extends JavaFxTestBase {

    private static final String APP_CSS = "/com/discoballplayer/css/app.css";
    private static final String DARK_CSS = "/com/discoballplayer/css/dark.css";

    /**
     * How far a node may legitimately paint outside its own layout box. Glows do exactly that,
     * and the widest one in the sheet has a radius of 30, which spreads roughly that far on
     * each side. Anything past this is not light -- it is geometry gone wrong.
     */
    private static final double GLOW_ALLOWANCE = 80;

    private Parent root;

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
    }

    @BeforeEach
    void loadTheView() {
        FXMLLoader loader = loadView("/com/discoballplayer/fxml/main-view.fxml");
        root = loader.getRoot();
        onFxThread(() -> {
            Scene scene = new Scene(root, 1180, 720);
            scene.getStylesheets().add(resource(APP_CSS));
            scene.getStylesheets().add(resource(DARK_CSS));
            root.applyCss();
            root.layout();
        });
        flushFxThread();
    }

    /**
     * The regression this file exists for.
     *
     * <p>A corner radius far larger than the box it rounds does not simply clamp: JavaFX
     * computes a border shape from it, and on a slider track six pixels tall a "pill" radius of
     * 999 produced bounds over two thousand pixels wide. It rendered as a dark line straight
     * across the window, through the artist name and both sliders. Nothing else caught it --
     * the layout was correct, every control was where it belonged, and the suite stayed green.
     * Only the painted result was wrong, so the assertion has to be about painted extent.</p>
     */
    @Test
    void noNodeSpillsFarBeyondItsLayoutBox() {
        List<String> spilling = new ArrayList<>();
        onFxThread(() -> collectSpills(root, spilling));

        assertTrue(spilling.isEmpty(),
                "a node paints far outside its layout box, which means a radius or an effect is "
                        + "wrong rather than merely bright: " + String.join("; ", spilling));
    }

    private void collectSpills(Node node, List<String> spilling) {
        Bounds painted = node.getBoundsInLocal();
        Bounds box = node.getLayoutBounds();
        double horizontal = painted.getWidth() - box.getWidth();
        double vertical = painted.getHeight() - box.getHeight();
        if (horizontal > GLOW_ALLOWANCE || vertical > GLOW_ALLOWANCE) {
            spilling.add(describe(node) + " paints "
                    + Math.round(painted.getWidth()) + "x" + Math.round(painted.getHeight())
                    + " for a box of "
                    + Math.round(box.getWidth()) + "x" + Math.round(box.getHeight()));
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> collectSpills(child, spilling));
        }
    }

    private String describe(Node node) {
        String id = node.getId() == null ? "" : "#" + node.getId();
        return node.getClass().getSimpleName() + id + node.getStyleClass();
    }

    /**
     * The dance floor is the theme the window opens wearing, so the palette that was specified
     * has to be the one in dark.css. Checking the sheet rather than a rendered pixel keeps this
     * readable as a list of decisions, which is what it is.
     */
    @Test
    void theDanceFloorWearsTheSpecifiedPalette() {
        String dark = read(DARK_CSS).toLowerCase(Locale.ROOT);

        assertTrue(dark.contains("#12071f"), "base background missing");
        assertTrue(dark.contains("#1e0b33"), "panel surface missing");
        assertTrue(dark.contains("#2d1b4e"), "mid purple missing");
        assertTrue(dark.contains("#7b2ff7"), "vibrant purple missing");
        assertTrue(dark.contains("#9d4edd"), "bright lilac missing");
        assertTrue(dark.contains("#e040fb"), "neon magenta missing");
        assertTrue(dark.contains("#ffd166"), "amber gold missing");
        assertTrue(dark.contains("#4cc9f0"), "electric cyan missing");
        assertTrue(dark.contains("#f3e8ff"), "primary text missing");
        assertTrue(dark.contains("#b79cd9"), "secondary text missing");
    }

    /**
     * Cyan is the only cool colour on the floor, which is what makes it read as an accent. Used
     * freely it stops being one, so the brief said to use it sparingly and this counts.
     */
    @Test
    void cyanStaysRare() {
        long uses = read(APP_CSS).lines().filter(line -> line.contains("-db-cyan")).count();
        assertTrue(uses <= 3, "cyan is used " + uses + " times: it is the accent, not a colour");
    }

    /** Every glow names a token, so the dark sheet can dim it without restating the rule. */
    @Test
    void glowsAreBuiltFromTokensRatherThanLiterals() {
        read(APP_CSS).lines()
                .filter(line -> line.contains("-fx-effect:") && line.contains("dropshadow("))
                .forEach(line -> assertTrue(line.contains("-db-"),
                        "glow without a token cannot be re-themed: " + line.trim()));
    }

    /** A flat fill is the one thing the brief ruled out for the shell. */
    @Test
    void theShellIsPaintedWithGradientsRatherThanFlatColour() {
        String app = read(APP_CSS);
        int start = app.indexOf(".root-pane {");
        assertTrue(start > 0, ".root-pane is missing");
        String rule = app.substring(start, app.indexOf('}', start));

        assertTrue(rule.contains("linear-gradient"), ".root-pane has no linear gradient");
        assertEquals(2, rule.split("radial-gradient", -1).length - 1,
                ".root-pane should carry both light washes");
    }

    private static String resource(String path) {
        return Objects.requireNonNull(DiscoThemeTest.class.getResource(path), path).toExternalForm();
    }

    private static String read(String path) {
        try {
            URL url = Objects.requireNonNull(DiscoThemeTest.class.getResource(path), path);
            return Files.readString(Path.of(url.toURI()), StandardCharsets.UTF_8);
        } catch (IOException | java.net.URISyntaxException e) {
            throw new IllegalStateException("could not read " + path, e);
        }
    }
}
