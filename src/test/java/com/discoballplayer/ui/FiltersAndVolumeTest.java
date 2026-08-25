package com.discoballplayer.ui;

import java.util.List;

import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.Song;
import com.discoballplayer.service.DemoPlayerService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.shape.SVGPath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the sidebar filters, the volume control and the icon transport.
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class FiltersAndVolumeTest extends JavaFxTestBase {

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

    @AfterEach
    void stopTheTicker() {
        if (controller.player() instanceof DemoPlayerService demo) {
            demo.shutdown();
        }
    }

    // ---- filters ---------------------------------------------------------

    @Test
    void theGenreFilterOffersEveryGenrePlusAnAllOption() {
        assertEquals("All", combo("genreFilter").getValue());
        assertEquals(Genre.values().length + 1, combo("genreFilter").getItems().size());
    }

    @Test
    void filteringByGenreLeavesOnlyThatGenre() {
        choose("genreFilter", "Salsa");

        assertEquals(3, table().getItems().size());
        assertTrue(table().getItems().stream().allMatch(song -> song.getGenre() == Genre.SALSA));
    }

    @Test
    void filteringByArtistLeavesOnlyThatArtist() {
        choose("artistFilter", "Daft Punk");

        assertEquals(3, table().getItems().size());
        assertTrue(table().getItems().stream()
                .allMatch(song -> song.getArtistsNames().equals("Daft Punk")));
    }

    @Test
    void theRatingFilterKeepsOnlySongsAtOrAboveIt() {
        onFxThread(() -> slider("ratingFilter").setValue(90));
        flushFxThread();

        assertFalse(table().getItems().isEmpty());
        assertTrue(table().getItems().stream().allMatch(song -> song.getRating() >= 90));
    }

    @Test
    void filtersCombineRatherThanReplaceEachOther() {
        choose("genreFilter", "Indie");
        onFxThread(() -> slider("ratingFilter").setValue(90));
        flushFxThread();

        assertTrue(table().getItems().stream()
                .allMatch(song -> song.getGenre() == Genre.INDIE && song.getRating() >= 90));
        assertEquals(1, table().getItems().size());
    }

    @Test
    void aFilterCombinesWithTheSearchBoxToo() {
        choose("genreFilter", "Reggaeton");
        onFxThread(() -> searchField().setText("moscow"));
        flushFxThread();

        assertEquals(1, table().getItems().size());
        assertEquals("Moscow Mule", table().getItems().get(0).getTitle());
    }

    @Test
    void clearingFiltersRestoresTheWholeLibrary() {
        choose("genreFilter", "Salsa");
        onFxThread(() -> slider("ratingFilter").setValue(95));
        onFxThread(() -> searchField().setText("joe"));
        flushFxThread();
        assertNotEquals(DEMO_LIBRARY_SIZE, table().getItems().size());

        press("clearFiltersButton");

        assertEquals(DEMO_LIBRARY_SIZE, table().getItems().size());
        assertEquals("All", combo("genreFilter").getValue());
        assertEquals("All", combo("artistFilter").getValue());
        assertEquals(0, slider("ratingFilter").getValue(), 1e-9);
        assertEquals("", searchField().getText());
    }

    @Test
    void theSummaryCountsWhatIsShownAgainstTheWholeLibrary() {
        assertEquals("12 songs", label("filterSummary").getText());

        choose("genreFilter", "Salsa");

        assertEquals("3 of 12 songs", label("filterSummary").getText());
    }

    @Test
    void theArtistListIsDerivedFromTheLibraryNotStored() {
        assertFalse(combo("artistFilter").getItems().contains("Nobody At All"));

        onFxThread(() -> controller.player().addSong(new Song("New Track",
                List.of(new Artist("Nobody At All")), null, 100, Genre.JAZZ, 2024)));
        flushFxThread();

        assertTrue(combo("artistFilter").getItems().contains("Nobody At All"),
                "an artist exists as an option exactly as long as a song credits them");
    }

    @Test
    void aFilteredOutSongIsStillInTheLibrary() {
        choose("genreFilter", "Salsa");

        assertEquals(3, table().getItems().size());
        assertEquals(DEMO_LIBRARY_SIZE, controller.player().listAll().size(),
                "a filter hides rows; it does not delete songs");
    }

    // ---- volume ----------------------------------------------------------

    @Test
    void theVolumeStartsAtFull() {
        assertEquals(100, slider("volumeSlider").getValue(), 1e-9);
        assertEquals("Volume 100%", label("volumeLabel").getText());
        assertEquals(1.0, controller.player().getVolume(), 1e-9);
    }

    @Test
    void movingTheVolumeSliderReachesTheService() {
        onFxThread(() -> slider("volumeSlider").setValue(40));
        flushFxThread();

        assertEquals(0.4, controller.player().getVolume(), 1e-9);
        assertEquals("Volume 40%", label("volumeLabel").getText());
    }

    @Test
    void theVolumeCanBeTakenToSilence() {
        onFxThread(() -> slider("volumeSlider").setValue(0));
        flushFxThread();

        assertEquals(0, controller.player().getVolume(), 1e-9);
        assertEquals("Volume 0%", label("volumeLabel").getText());
    }

    @Test
    void theVolumeSurvivesASongChange() {
        onFxThread(() -> slider("volumeSlider").setValue(25));
        flushFxThread();

        press("nextButton");

        assertEquals(0.25, controller.player().getVolume(), 1e-9,
                "a level the user chose must not reset with every track");
    }

    // ---- icon transport --------------------------------------------------

    @Test
    void theTransportButtonsAreIconsWithAccessibleNames() {
        for (String id : List.of("previousButton", "playPauseButton", "nextButton")) {
            Button button = button(id);
            assertTrue(button.getGraphic() instanceof SVGPath, id + " has no icon");
            assertFalse(button.getText() == null || button.getText().isBlank(),
                    id + " has no accessible name behind the icon");
            assertFalse(button.getTooltip() == null, id + " has no tooltip");
        }
    }

    @Test
    void thePlayIconBecomesAPauseIconWhilePlaying() {
        // The view opens on a mode that is already playing.
        String whilePlaying = icon("playPauseButton").getContent();

        press("playPauseButton");
        String whilePaused = icon("playPauseButton").getContent();

        assertNotEquals(whilePlaying, whilePaused, "the icon has to show the state, not just sit there");

        press("playPauseButton");
        assertEquals(whilePlaying, icon("playPauseButton").getContent());
    }

    @Test
    void theIconFollowsTheServiceRatherThanTheClick() {
        // Stated, not assumed. The test only means anything if the view really opened playing,
        // and saying so here makes a broken premise fail with the reason instead of the symptom.
        assertTrue(controller.player().isPlaying(), "the view is expected to open playing");
        String whilePlaying = icon("playPauseButton").getContent();

        // Nothing touched the button; the service was told directly.
        onFxThread(() -> controller.player().pause());
        flushFxThread();

        assertNotEquals(whilePlaying, icon("playPauseButton").getContent());
    }

    // ---- helpers ---------------------------------------------------------

    private void choose(String id, String value) {
        onFxThread(() -> {
            combo(id).setValue(value);
            combo(id).fireEvent(new javafx.event.ActionEvent());
        });
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

    @SuppressWarnings("unchecked")
    private ComboBox<String> combo(String id) {
        return (ComboBox<String>) root.lookup("#" + id);
    }

    private Slider slider(String id) {
        return (Slider) root.lookup("#" + id);
    }

    private Button button(String id) {
        return (Button) root.lookup("#" + id);
    }

    private Label label(String id) {
        return (Label) root.lookup("#" + id);
    }

    /**
     * The icon is the button's graphic, not a node {@code lookup} can reach: a graphic only
     * joins the scene graph once the button's skin is built, which needs a layout pass.
     */
    private SVGPath icon(String buttonId) {
        return (SVGPath) button(buttonId).getGraphic();
    }

    private TextField searchField() {
        return (TextField) root.lookup("#searchField");
    }
}
