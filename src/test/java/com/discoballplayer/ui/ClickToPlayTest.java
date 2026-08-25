package com.discoballplayer.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.Song;
import com.discoballplayer.service.DemoPlayerService;
import com.discoballplayer.service.PlayerService;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers click-to-play: what it does when the mode allows it, and what the window does instead
 * when the mode refuses.
 *
 * <p>The refusals matter more than the success here. Arrival order will not reposition —
 * honouring a jump would mean discarding everything queued ahead of the target, which is no
 * longer FIFO — and both refusals are ordinary answers rather than defects. An exception
 * reaching the FX event loop would break the window, so each one has to land on a message.</p>
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class ClickToPlayTest extends JavaFxTestBase {

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

    // ---- playing ---------------------------------------------------------

    @Test
    void playingASongFromTheTableShowsItInTheBar() {
        press("shuffleModeButton");
        Song target = songTitled("Ojitos Lindos");

        play(target);

        assertEquals("Ojitos Lindos", label("nowPlayingTitle").getText());
        assertSame(target, controller.player().current());
    }

    @Test
    void playingASongStartsPlayback() {
        press("shuffleModeButton");
        press("playPauseButton");
        assertEquals("Play", button("playPauseButton").getText());

        play(songTitled("Dare"));

        assertTrue(controller.player().isPlaying());
        assertEquals("Pause", button("playPauseButton").getText());
    }

    @Test
    void playingASongClearsAStaleStatusMessage() {
        press("arrivalModeButton");
        play(songTitled("Dare"));
        assertNotEquals("", status().getText(), "arrival order refused, so it said so");

        press("shuffleModeButton");
        play(songTitled("Dare"));

        assertEquals("", status().getText());
    }

    @Test
    void aSongAddedWhileAModeIsPlayingCanBePlayedImmediately() {
        // Used to be impossible: the mode built its structure at load() and never heard about
        // a later addition, so the song sat in the table and refused to play.
        press("shuffleModeButton");
        Song latecomer = new Song("Added While Playing", java.util.List.of(new Artist("Nobody")),
                null, 100, Genre.JAZZ, 2024);
        onFxThread(() -> controller.player().addSong(latecomer));
        flushFxThread();

        play(latecomer);

        assertEquals("Added While Playing", label("nowPlayingTitle").getText());
        assertEquals("", status().getText());
    }

    @Test
    void addingASongDoesNotDisturbWhatIsPlaying() {
        press("alphabeticalModeButton");
        String playing = label("nowPlayingTitle").getText();

        onFxThread(() -> controller.player().addSong(
                new Song("Aaa First Alphabetically", java.util.List.of(new Artist("Nobody")),
                        null, 100, Genre.JAZZ, 2024)));
        flushFxThread();

        assertEquals(playing, label("nowPlayingTitle").getText(),
                "an insertion must not move the cursor off the song being played");
    }

    @Test
    void playingWorksInAlphabeticalOrderToo() {
        press("alphabeticalModeButton");

        play(songTitled("Tania"));

        assertEquals("Tania", label("nowPlayingTitle").getText());
    }

    @Test
    void aRealDoubleClickOnARowPlaysThatSong() {
        Scene scene = showTable();
        TableRow<Song> row = firstRenderedRow();
        Song expected = row.getItem();

        onFxThread(() -> Event.fireEvent(row, doubleClick()));
        flushFxThread();

        assertEquals(expected.getTitle(), label("nowPlayingTitle").getText(),
                "the row factory's handler is what makes double click work at all");
        assertNotNull(scene);
    }

    @Test
    void aSingleClickPlaysNothing() {
        showTable();
        TableRow<Song> row = firstRenderedRow();
        String playing = label("nowPlayingTitle").getText();

        onFxThread(() -> Event.fireEvent(row, singleClick()));
        flushFxThread();

        assertEquals(playing, label("nowPlayingTitle").getText(),
                "selecting a row is not asking to hear it");
    }

    @Test
    void aDoubleClickBelowTheLastSongPlaysNothing() {
        showTable();
        TableRow<Song> empty = renderedRows().stream()
                .filter(TableRow::isEmpty)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the table rendered no empty rows"));

        String playing = label("nowPlayingTitle").getText();

        onFxThread(() -> Event.fireEvent(empty, doubleClick()));
        flushFxThread();

        assertEquals(playing, label("nowPlayingTitle").getText(),
                "there is no song below the last row to play");
    }

    // ---- the mode that refuses -------------------------------------------

    @Test
    void arrivalOrderRefusesTheJumpAndSaysWhy() {
        press("arrivalModeButton");
        String before = label("nowPlayingTitle").getText();
        assertNotEquals("Nothing playing", before, "the mode started playing");

        play(songTitled("Tania"));

        assertEquals("Arrival order plays in the order songs arrived", status().getText());
        assertEquals(before, label("nowPlayingTitle").getText(), "nothing should have moved");
    }

    @Test
    void arrivalOrderIsAskedRatherThanNamed() {
        press("arrivalModeButton");
        assertEquals(false, controller.player().canPlaySong(),
                "the mode reports it cannot reposition; the UI must not hard-code the mode");

        press("shuffleModeButton");
        assertEquals(true, controller.player().canPlaySong());
    }

    @Test
    void theTableCursorFollowsWhetherAJumpIsPossible() {
        press("shuffleModeButton");
        assertEquals(Cursor.HAND, table().getCursor());

        press("arrivalModeButton");
        assertEquals(Cursor.DEFAULT, table().getCursor());
    }

    @Test
    void aSongTheModeHasNeverSeenIsReportedNotThrown() {
        press("shuffleModeButton");
        // Never added to the library at all, so no mode can have it.
        Song stranger = new Song("Never In The Library", java.util.List.of(new Artist("Nobody")),
                null, 100, Genre.JAZZ, 2024);

        play(stranger);

        assertEquals("That song is not in the current queue", status().getText());
    }

    @Test
    void aRefusalLeavesTheWindowUsable() {
        press("arrivalModeButton");
        String before = label("nowPlayingTitle").getText();
        play(songTitled("Tania"));

        // If the exception had escaped, the handlers below would be running on a broken window.
        press("nextButton");

        assertNotEquals(before, label("nowPlayingTitle").getText());
    }

    // ---- the guard and the catch, separately -----------------------------

    @Test
    void aModeThatCannotRepositionIsNeverEvenAsked() {
        // The guard and the catch are each redundant given the other: remove one and the
        // message still appears. This asserts the guard's own effect -- that playSong is not
        // called at all -- so exception-driven control flow cannot creep back in unnoticed.
        SpyService spy = new SpyService(false, false);
        MainController injected = loadWith(spy);

        onFxThread(() -> injected.playFromLibrary(anySong()));
        flushFxThread();

        assertEquals(0, spy.playSongCalls, "a mode that says no should not be asked anyway");
    }

    @Test
    void aServiceThatRefusesAfterSayingYesIsStillHandled() {
        // canPlaySong() is true and playSong() throws anyway. The guard cannot help here, so
        // this is the catch on its own.
        SpyService spy = new SpyService(true, true);
        MainController injected = loadWith(spy);

        onFxThread(() -> injected.playFromLibrary(anySong()));
        flushFxThread();

        assertEquals(1, spy.playSongCalls);
        assertEquals("Arrival order plays in the order songs arrived",
                ((Label) injectedRoot.lookup("#statusLabel")).getText());
    }

    private Parent injectedRoot;

    private MainController loadWith(PlayerService service) {
        MainController[] built = new MainController[1];
        onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    MainController.class.getResource("/com/discoballplayer/fxml/main-view.fxml")));
            loader.setControllerFactory(type -> new MainController(service));
            try {
                injectedRoot = loader.load();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            built[0] = loader.getController();
        });
        flushFxThread();
        return built[0];
    }

    private Song anySong() {
        return new Song("Anything", java.util.List.of(new Artist("Nobody")), null,
                100, Genre.POP, 2020);
    }

    /** A PlayerService that records whether it was asked to jump, and can refuse after saying yes. */
    private static final class SpyService extends DemoPlayerService {

        private final boolean canPlay;
        private final boolean throwOnPlay;
        private int playSongCalls;

        private SpyService(boolean canPlay, boolean throwOnPlay) {
            this.canPlay = canPlay;
            this.throwOnPlay = throwOnPlay;
        }

        @Override
        public boolean canPlaySong() {
            return canPlay;
        }

        @Override
        public Song playSong(Song song) {
            playSongCalls++;
            if (throwOnPlay) {
                throw new UnsupportedOperationException("arrival order cannot reposition");
            }
            return super.playSong(song);
        }
    }

    // ---- helpers ---------------------------------------------------------

    /** Calls what the row's handler calls. The real double click is covered separately. */
    private void play(Song song) {
        onFxThread(() -> controller.playFromLibrary(song));
        flushFxThread();
    }

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

    @SuppressWarnings("unchecked")
    private TableView<Song> table() {
        return (TableView<Song>) root.lookup("#libraryTable");
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

    /** Puts the view in a laid-out scene so the table actually renders its rows. */
    private Scene showTable() {
        Scene[] scene = new Scene[1];
        onFxThread(() -> {
            scene[0] = new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
        });
        flushFxThread();
        return scene[0];
    }

    @SuppressWarnings("unchecked")
    private java.util.List<TableRow<Song>> renderedRows() {
        return table().lookupAll(".table-row-cell").stream()
                .filter(node -> node instanceof TableRow)
                .map(node -> (TableRow<Song>) node)
                .toList();
    }

    private TableRow<Song> firstRenderedRow() {
        return renderedRows().stream()
                .filter(row -> !row.isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the table rendered no rows"));
    }

    private static MouseEvent doubleClick() {
        return mouseClick(2);
    }

    private static MouseEvent singleClick() {
        return mouseClick(1);
    }

    private static MouseEvent mouseClick(int clicks) {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, clicks,
                false, false, false, false, true, false, false, false, false, false, null);
    }

    private Song songTitled(String title) {
        return table().getItems().stream()
                .filter(song -> song.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no demo song titled " + title));
    }
}
