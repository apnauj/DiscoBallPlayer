package com.discoballplayer.ui;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.Song;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the main view end to end: the real {@code main-view.fxml} is loaded through
 * {@code FXMLLoader}, so a missing {@code fx:id} or an unbound column fails here rather than
 * at the demo.
 *
 * <p>Flat by design. Under {@code @Nested} the outer class matches {@code -Dtest=Class#method}
 * and runs nothing, which reports success while testing nothing.</p>
 */
class MainControllerTest extends JavaFxTestBase {

    private static final int DEMO_LIBRARY_SIZE = 12;

    private MainController controller;
    private Parent root;

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
    }

    @BeforeEach
    void loadTheView() {
        FXMLLoader loader = loadView("/com/discoballplayer/fxml/main-view.fxml");
        root = loader.getRoot();
        controller = loader.getController();
    }

    // ---- library table ---------------------------------------------------

    @Test
    void loadsEveryDemoSongIntoTheTable() {
        assertEquals(DEMO_LIBRARY_SIZE, table().getItems().size());
    }

    @Test
    void bindsAllSevenColumns() {
        List<String> headers = table().getColumns().stream().map(TableColumn::getText).toList();
        assertEquals(
                List.of("Title", "Artist", "Album", "Duration", "Genre", "Year", "Rating"),
                headers);
        table().getColumns().forEach(column ->
                assertNotNull(column.getCellValueFactory(), column.getText() + " has no cell value factory"));
    }

    @Test
    void rendersDurationAsMinutesAndSeconds() {
        Song song = songTitled("One More Time");
        assertEquals(320, song.getDurationSeconds());
        assertEquals("5:20", cellText("Duration", song));
    }

    @Test
    void showsAnEmDashWhenTheSongHasNoAlbum() {
        Song albumless = songTitled("La Rebelion");
        assertNull(albumless.getAlbum());
        assertEquals("—", cellText("Album", albumless));
    }

    @Test
    void searchLeavesOnlyMatchingRows() {
        onFxThread(() -> searchField().setText("gorillaz"));

        List<String> artists = table().getItems().stream().map(Song::getArtistsNames).distinct().toList();
        assertEquals(List.of("Gorillaz"), artists);
        assertEquals(3, table().getItems().size());
    }

    @Test
    void clearingTheSearchRestoresEveryRow() {
        onFxThread(() -> searchField().setText("gorillaz"));
        assertEquals(3, table().getItems().size());

        onFxThread(() -> searchField().setText(""));
        assertEquals(DEMO_LIBRARY_SIZE, table().getItems().size());
    }

    // ---- now playing metadata --------------------------------------------

    @Test
    void songChangeRendersTitleAndArtist() {
        controller.onSongChanged(songTitled("Feel Good Inc."));
        flushFxThread();

        assertEquals("Feel Good Inc.", label("nowPlayingTitle").getText());
        assertEquals("Gorillaz", label("nowPlayingArtist").getText());
    }

    @Test
    void aSongWithNoCoverFallsBackToTheDefaultImage() {
        Image before = cover().getImage();
        assertNotNull(before, "the bar starts on the default cover, never on an empty box");

        controller.onSongChanged(songTitled("La Rebelion"));
        flushFxThread();

        assertSame(before, cover().getImage());
        assertFalse(cover().getImage().isError());
    }

    @Test
    void anUnreadableCoverPathFallsBackInsteadOfThrowing() {
        Image fallback = cover().getImage();
        Song song = songTitled("Dare");
        song.setCoverPath("/no/such/directory/cover.png");

        controller.onSongChanged(song);
        flushFxThread();

        assertSame(fallback, cover().getImage());
    }

    @Test
    void aCoverPathThatIsNotEvenAUrlFallsBackInsteadOfThrowing() {
        Image fallback = cover().getImage();
        Song song = songTitled("Dare");
        song.setCoverPath("::not a url::");

        controller.onSongChanged(song);
        flushFxThread();

        assertSame(fallback, cover().getImage());
    }

    @Test
    void aFileThatIsNotAnImageFallsBackInsteadOfShowingABrokenBox(@TempDir Path folder)
            throws Exception {
        // A real, readable file that no decoder will accept: this is the branch a bad path
        // never reaches, because a bad path fails earlier as a malformed URL.
        Path notAnImage = folder.resolve("cover.png");
        Files.writeString(notAnImage, "this is text, not a PNG");

        Image fallback = cover().getImage();
        Song song = songTitled("Dare");
        song.setCoverPath(notAnImage.toAbsolutePath().toString());

        controller.onSongChanged(song);
        flushFxThread();

        assertSame(fallback, cover().getImage());
    }

    @Test
    void aReadableCoverPathIsUsedInsteadOfTheDefault(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("cover.png");
        Files.copy(
                Objects.requireNonNull(MainController.class.getResourceAsStream(
                        "/com/discoballplayer/images/default-cover.png")),
                file);

        Image fallback = cover().getImage();
        Song song = songTitled("Dare");
        song.setCoverPath(file.toAbsolutePath().toString());

        controller.onSongChanged(song);
        flushFxThread();

        assertNotSame(fallback, cover().getImage(), "a readable cover must win over the placeholder");
        assertFalse(cover().getImage().isError());
    }

    @Test
    void aNullSongReturnsTheBarToItsIdleState() {
        controller.onSongChanged(songTitled("Dare"));
        flushFxThread();

        controller.onSongChanged(null);
        flushFxThread();

        assertEquals("Nothing playing", label("nowPlayingTitle").getText());
        assertEquals("Pick a song to begin", label("nowPlayingArtist").getText());
    }

    // ---- progress --------------------------------------------------------

    @Test
    void progressEventDrivesTheBarAndBothTimeLabels() {
        controller.onProgress(30, 120);
        flushFxThread();

        assertEquals(0.25, progressBar().getProgress(), 1e-9);
        assertEquals("0:30", label("elapsedLabel").getText());
        assertEquals("2:00", label("totalLabel").getText());
    }

    @Test
    void songChangeResetsProgressToZeroAndShowsTheNewTotal() {
        controller.onProgress(90, 120);
        flushFxThread();
        assertEquals(0.75, progressBar().getProgress(), 1e-9);

        controller.onSongChanged(songTitled("Aerodynamic"));
        flushFxThread();

        assertEquals(0, progressBar().getProgress(), 1e-9);
        assertEquals("0:00", label("elapsedLabel").getText());
        assertEquals("3:32", label("totalLabel").getText());
    }

    @Test
    void aZeroLengthTrackDoesNotDivideByZero() {
        controller.onProgress(5, 0);
        flushFxThread();

        assertEquals(0, progressBar().getProgress(), 1e-9);
    }

    @Test
    void progressBeyondTheEndStaysAtAFullBar() {
        controller.onProgress(500, 120);
        flushFxThread();

        assertEquals(1, progressBar().getProgress(), 1e-9);
    }

    // ---- library events --------------------------------------------------

    @Test
    void aLibraryChangeRefreshesTheTableWithoutLosingTheSearch() {
        onFxThread(() -> searchField().setText("gorillaz"));
        assertEquals(3, table().getItems().size());

        controller.onLibraryChanged();
        flushFxThread();

        assertEquals(3, table().getItems().size(), "the active query must survive a refresh");
    }

    @Test
    void anAddedSongAppearsInTheTable() {
        Song extra = new Song("Zzz Test Track", List.of(new Artist("Test Artist")), null,
                100, Genre.JAZZ, 2020);

        onFxThread(() -> controller.player().addSong(extra));
        flushFxThread();

        assertEquals(DEMO_LIBRARY_SIZE + 1, table().getItems().size());
        assertTrue(table().getItems().contains(extra));
    }

    // ---- helpers ---------------------------------------------------------

    @SuppressWarnings("unchecked")
    private TableView<Song> table() {
        return (TableView<Song>) root.lookup("#libraryTable");
    }

    private TextField searchField() {
        return (TextField) root.lookup("#searchField");
    }

    private Label label(String id) {
        return (Label) root.lookup("#" + id);
    }

    private ImageView cover() {
        return (ImageView) root.lookup("#coverImage");
    }

    private ProgressBar progressBar() {
        return (ProgressBar) root.lookup("#progressBar");
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
