package com.discoballplayer.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.discoballplayer.service.DemoPlayerService;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the theme toggle and the keyboard shortcuts.
 *
 * <p>Two of these assert on the stylesheets as text. That is deliberate: the claim that
 * swapping the theme cannot relayout the window is a claim about what {@code dark.css} is
 * allowed to contain, and a rule slipped in later would break it silently. Checking the file
 * catches that; checking the rendering would not.</p>
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class ThemeAndShortcutsTest extends JavaFxTestBase {

    private static final String APP_CSS = "/com/discoballplayer/css/app.css";
    private static final String DARK_CSS = "/com/discoballplayer/css/dark.css";

    private MainController controller;
    private Parent root;
    private Scene scene;

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
        // This class measures where things sit, and an entrance animation moves them on
        // purpose. getBoundsInParent includes a node's translation, so a transition still in
        // flight when the theme is toggled reads as the theme having moved something. Holding
        // the floor still is what makes the layout claim about the theme and nothing else.
        AnimationManager.setEnabled(false);
    }

    @AfterAll
    static void letTheFloorDanceAgain() {
        AnimationManager.resetToDefault();
    }

    @BeforeEach
    void loadTheViewIntoAScene() {
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
            scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(resource(APP_CSS));
        });
        // The controller defers its first theme application by one pulse, exactly as it does
        // under Main, so the stylesheet lands after the one the scene's owner added.
        flushFxThread();
        flushFxThread();
    }

    @AfterEach
    void stopTheTicker() {
        if (controller.player() instanceof DemoPlayerService demo) {
            demo.shutdown();
        }
    }

    // ---- theme -----------------------------------------------------------

    @Test
    void opensInTheDarkThemeOfferingTheLightOne() {
        assertTrue(controller.isDarkTheme());
        assertEquals("Light", themeToggle().getText());
    }

    @Test
    void theOverrideIsAppliedAfterTheBaseSheetSoItActuallyWins() {
        List<String> sheets = scene.getStylesheets();
        assertEquals(2, sheets.size());
        assertTrue(sheets.indexOf(resource(DARK_CSS)) > sheets.indexOf(resource(APP_CSS)),
                "a stylesheet added before the base sheet loses to it");
    }

    @Test
    void togglingSwapsTheThemeBothWays() {
        press();
        assertFalse(controller.isDarkTheme());
        assertEquals("Dark", themeToggle().getText());

        press();
        assertTrue(controller.isDarkTheme());
        assertEquals("Light", themeToggle().getText());
    }

    @Test
    void togglingChangesColourWithoutMovingAnything() {
        layOut();
        Bounds tableBefore = boundsOf("libraryTable");
        Bounds barBefore = boundsOf("nowPlayingBar");
        Bounds sidebarBefore = boundsOf("sidebar");
        assertNotEquals(0.0, tableBefore.getWidth(), "nothing was laid out, so nothing is proved");

        press();
        layOut();

        assertEquals(tableBefore, boundsOf("libraryTable"));
        assertEquals(barBefore, boundsOf("nowPlayingBar"));
        assertEquals(sidebarBefore, boundsOf("sidebar"));
    }

    private void layOut() {
        onFxThread(() -> {
            root.applyCss();
            root.layout();
        });
    }

    @Test
    void theDarkSheetOverridesTokensAndNothingElse() {
        String dark = read(DARK_CSS);

        List<String> declarations = declarationsIn(dark);
        assertFalse(declarations.isEmpty(), "dark.css declares nothing");
        declarations.forEach(property -> assertTrue(property.startsWith("-db-"),
                property + " is not a token: dark.css may only override the token block, "
                        + "or swapping the theme could relayout the window"));
    }

    @Test
    void theBaseSheetUsesRawColourOnlyInsideTheTokenBlock() {
        String app = read(APP_CSS);
        int endOfTokenBlock = app.indexOf('}', app.indexOf(".root"));
        String rules = app.substring(endOfTokenBlock);

        Matcher literal = Pattern.compile("#[0-9a-fA-F]{3,8}\\b").matcher(rules);
        assertFalse(literal.find(),
                "raw colour " + (literal.hitEnd() ? "" : literal.group())
                        + " outside the token block: dark.css could not override it");
    }

    // ---- keyboard --------------------------------------------------------

    @Test
    void spaceTogglesPlayback() {
        // The view opens on a mode that is already playing.
        assertEquals("Pause", button("playPauseButton").getText());

        pressKey(KeyCode.SPACE);
        assertEquals("Play", button("playPauseButton").getText());

        pressKey(KeyCode.SPACE);
        assertEquals("Pause", button("playPauseButton").getText());
    }

    @Test
    void rightArrowAdvancesToTheNextSong() {
        pressKey(KeyCode.RIGHT);
        String first = label("nowPlayingTitle").getText();

        pressKey(KeyCode.RIGHT);

        assertNotEquals(first, label("nowPlayingTitle").getText());
    }

    @Test
    void leftArrowGoesBack() {
        pressKey(KeyCode.RIGHT);
        String first = label("nowPlayingTitle").getText();
        pressKey(KeyCode.RIGHT);

        pressKey(KeyCode.LEFT);

        assertEquals(first, label("nowPlayingTitle").getText());
    }

    @Test
    void typingASpaceIntoTheSearchBoxDoesNotStartPlayback() {
        onFxThread(() -> searchField().requestFocus());
        flushFxThread();

        String before = button("playPauseButton").getText();
        pressKey(KeyCode.SPACE);

        assertEquals(before, button("playPauseButton").getText(),
                "a space typed into a search box is a space, not a transport command");
    }

    @Test
    void arrowKeysInTheSearchBoxMoveTheCaretRatherThanTheQueue() {
        onFxThread(() -> searchField().requestFocus());
        flushFxThread();
        String playing = label("nowPlayingTitle").getText();

        pressKey(KeyCode.RIGHT);

        assertEquals(playing, label("nowPlayingTitle").getText(),
                "an arrow key in a text field moves the caret, not the queue");
    }

    // ---- helpers ---------------------------------------------------------

    /** Property names declared anywhere in a stylesheet. */
    private static List<String> declarationsIn(String css) {
        String withoutComments = css.replaceAll("(?s)/\\*.*?\\*/", "");
        Matcher matcher = Pattern.compile("([-a-zA-Z0-9]+)\\s*:").matcher(withoutComments);
        return matcher.results().map(result -> result.group(1)).toList();
    }

    private static String read(String resource) {
        try (var stream = Objects.requireNonNull(
                ThemeAndShortcutsTest.class.getResourceAsStream(resource), resource + " is missing")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("could not read " + resource, e);
        }
    }

    private static String resource(String path) {
        return Objects.requireNonNull(MainController.class.getResource(path), path).toExternalForm();
    }

    private void press() {
        onFxThread(() -> themeToggle().fire());
        flushFxThread();
    }

    private void pressKey(KeyCode code) {
        onFxThread(() -> Event.fireEvent(scene, new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false)));
        flushFxThread();
    }

    private Bounds boundsOf(String id) {
        return root.lookup("#" + id).getBoundsInParent();
    }

    private ToggleButton themeToggle() {
        return (ToggleButton) root.lookup("#themeToggle");
    }

    private Button button(String id) {
        return (Button) root.lookup("#" + id);
    }

    private Label label(String id) {
        return (Label) root.lookup("#" + id);
    }

    private TextField searchField() {
        return (TextField) root.lookup("#searchField");
    }
}
