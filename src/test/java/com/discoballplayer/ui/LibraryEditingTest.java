package com.discoballplayer.ui;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.discoballplayer.model.Song;
import com.discoballplayer.service.DemoPlayerService;
import com.discoballplayer.service.PlaybackListener;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the Add / Edit / Delete toolbar and the rating slider.
 *
 * <p>Add and Edit open a modal window through {@code showAndWait}, which cannot run in a
 * headless suite, so what is asserted here is everything around that call: when the buttons
 * are live, what Delete does, and that the slider rates the current song and syncs without
 * re-rating it. The dialog's own behaviour is covered in {@link SongDialogControllerTest}.</p>
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class LibraryEditingTest extends JavaFxTestBase {

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

    // ---- toolbar ---------------------------------------------------------

    @Test
    void editAndDeleteAreOffUntilARowIsSelected() {
        assertTrue(button("editButton").isDisabled());
        assertTrue(button("deleteButton").isDisabled());
        assertFalse(button("addButton").isDisabled(), "adding never needs a selection");
    }

    @Test
    void selectingARowEnablesEditAndDelete() {
        selectRow(0);

        assertFalse(button("editButton").isDisabled());
        assertFalse(button("deleteButton").isDisabled());
    }

    @Test
    void clearingTheSelectionDisablesThemAgain() {
        selectRow(0);
        onFxThread(() -> table().getSelectionModel().clearSelection());

        assertTrue(button("editButton").isDisabled());
        assertTrue(button("deleteButton").isDisabled());
    }

    @Test
    void deleteRemovesTheSelectedSongFromTheTable() {
        selectRow(0);
        Song doomed = table().getItems().get(0);

        press("deleteButton");

        assertEquals(DEMO_LIBRARY_SIZE - 1, table().getItems().size());
        assertFalse(table().getItems().contains(doomed));
    }

    @Test
    void aRefreshKeepsTheSelectedRowSoEditStaysLive() {
        selectRow(3);
        Song selected = table().getItems().get(3);

        controller.onLibraryChanged();
        flushFxThread();

        assertEquals(selected, table().getSelectionModel().getSelectedItem());
        assertFalse(button("editButton").isDisabled());
    }

    // ---- rating slider ---------------------------------------------------

    @Test
    void theSliderIsOffWhenTheBarHasNoSong() {
        // The view opens on a playing mode now, so the idle state has to be asked for.
        controller.onSongChanged(null);
        flushFxThread();

        assertTrue(slider().isDisabled());
        assertEquals(0, slider().getValue(), 1e-9);
    }

    @Test
    void theSliderIsLiveWhileASongIsPlaying() {
        assertFalse(slider().isDisabled(), "the view opens on a mode that is already playing");
    }

    @Test
    void aSongChangeSyncsTheSliderToItsRating() {
        Song song = songTitled("La Rebelion");
        assertEquals(99, song.getRating());

        controller.onSongChanged(song);
        flushFxThread();

        assertFalse(slider().isDisabled());
        assertEquals(99, slider().getValue(), 1e-9);
        assertEquals("Rating 99", label("ratingValueLabel").getText());
    }

    @Test
    void syncingTheSliderDoesNotRateTheSong() {
        // Asserting the rating is unchanged proves nothing: a spurious rate() would write back
        // the value it just read. What has to be absent is the call itself, so count the
        // library-changed events it would raise.
        CountingListener events = new CountingListener();
        controller.player().addListener(events);

        controller.onSongChanged(songTitled("Dare"));
        flushFxThread();

        assertEquals(0, events.libraryChanges,
                "pointing the slider at a song must not count as the user rating it");
    }

    @Test
    void movingTheSliderDoesRaiseALibraryChange() {
        controller.onSongChanged(songTitled("Dare"));
        flushFxThread();

        CountingListener events = new CountingListener();
        controller.player().addListener(events);

        onFxThread(() -> slider().setValue(35));
        flushFxThread();

        assertEquals(1, events.libraryChanges, "a real rating is a library change");
    }

    @Test
    void movingTheSliderRatesTheCurrentSong() {
        Song song = songTitled("Dare");
        controller.onSongChanged(song);
        flushFxThread();

        onFxThread(() -> slider().setValue(45));
        flushFxThread();

        assertEquals(45, song.getRating());
    }

    @Test
    void theTablesRatingCellFollowsTheSlider() {
        Song song = songTitled("Dare");
        controller.onSongChanged(song);
        flushFxThread();

        onFxThread(() -> slider().setValue(20));
        flushFxThread();

        assertEquals("20", cellText("Rating", songTitled("Dare")));
    }

    @Test
    void theSliderSnapsToStepsOfFive() {
        assertTrue(slider().isSnapToTicks());
        assertEquals(5, slider().getMajorTickUnit(), 1e-9);
        assertEquals(0, slider().getMin(), 1e-9);
        assertEquals(100, slider().getMax(), 1e-9);
    }

    // ---- helpers ---------------------------------------------------------

    /** Records how many times the service reported the library as changed. */
    private static final class CountingListener implements PlaybackListener {

        private int libraryChanges;

        @Override
        public void onSongChanged(Song song) {
        }

        @Override
        public void onPlaybackStateChanged(boolean playing) {
        }

        @Override
        public void onProgress(int elapsedSeconds, int totalSeconds) {
        }

        @Override
        public void onLibraryChanged() {
            libraryChanges++;
        }
    }

    private void selectRow(int index) {
        onFxThread(() -> table().getSelectionModel().select(index));
        flushFxThread();
    }

    private void press(String id) {
        onFxThread(() -> button(id).fire());
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

    private Slider slider() {
        return (Slider) root.lookup("#ratingSlider");
    }

    private Song songTitled(String title) {
        return table().getItems().stream()
                .filter(song -> song.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no demo song titled " + title));
    }

    @SuppressWarnings("unchecked")
    private String cellText(String columnHeader, Song song) {
        TableColumn<Song, String> column = (TableColumn<Song, String>) table().getColumns().stream()
                .filter(c -> c.getText().equals(columnHeader))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no column headed " + columnHeader));
        return column.getCellObservableValue(song).getValue();
    }
}
