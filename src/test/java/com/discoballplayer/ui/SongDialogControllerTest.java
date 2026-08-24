package com.discoballplayer.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.discoballplayer.model.Album;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.Song;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the add/edit form: what it reads out of a song, what it builds back, and what it
 * refuses. Save and Cancel are fired as real buttons so the FXML wiring is under test.
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class SongDialogControllerTest extends JavaFxTestBase {

    private SongDialogController dialog;
    private Parent root;

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
    }

    @BeforeEach
    void loadTheDialog() {
        onFxThread(() -> {
            URL view = Objects.requireNonNull(
                    SongDialogController.class.getResource("/com/discoballplayer/fxml/song-dialog.fxml"),
                    "song-dialog.fxml is not on the test classpath");
            FXMLLoader loader = new FXMLLoader(view);
            try {
                root = loader.load();
            } catch (IOException e) {
                throw new IllegalStateException("song-dialog.fxml failed to load", e);
            }
            dialog = loader.getController();
            dialog.setSong(null);
        });
    }

    // ---- creating --------------------------------------------------------

    @Test
    void createModeOpensOnAnEmptyForm() {
        assertEquals("Add song", label("dialogTitle").getText());
        assertEquals("", field("titleField").getText());
        assertEquals(Genre.OTHER, genreCombo().getValue());
    }

    @Test
    void buildsASongFromAValidForm() {
        fillValidForm();
        save();

        Song built = dialog.getResult();
        assertNotNull(built);
        assertEquals("Blue Monday", built.getTitle());
        assertEquals("New Order", built.getArtistsNames());
        assertEquals("Power, Corruption & Lies", built.getAlbum().getTitle());
        assertEquals(448, built.getDurationSeconds());
        assertEquals(Genre.ELECTRONIC, built.getGenre());
        assertEquals(1983, built.getYear());
        assertEquals(75, built.getRating());
    }

    @Test
    void aBlankAlbumMeansNoAlbumRatherThanAnEmptyOne() {
        fillValidForm();
        set("albumField", "   ");
        save();

        assertNull(dialog.getResult().getAlbum());
    }

    @Test
    void aBlankArtistLeavesTheSongWithoutOne() {
        fillValidForm();
        set("artistField", "");
        save();

        assertTrue(dialog.getResult().getArtists().isEmpty());
        assertEquals("Unknown artist", dialog.getResult().getArtistsNames());
    }

    @Test
    void blankPathsAreStoredAsNullNotAsEmptyText() {
        fillValidForm();
        save();

        assertNull(dialog.getResult().getAudioPath());
    }

    // ---- validation ------------------------------------------------------

    @Test
    void aBlankTitleIsRejectedWithAVisibleMessage() {
        fillValidForm();
        set("titleField", "   ");
        save();

        assertNull(dialog.getResult(), "a rejected form must not produce a song");
        assertTrue(label("errorLabel").isVisible());
        assertTrue(label("errorLabel").getText().contains("Title"));
    }

    @Test
    void aRejectedFieldIsMarkedRatherThanThrowing() {
        fillValidForm();
        set("titleField", "");
        save();

        assertTrue(field("titleField").getStyleClass().contains("field-error"));
    }

    @Test
    void aYearOutsideTheAllowedRangeIsRejected() {
        fillValidForm();
        set("yearField", "1850");
        save();

        assertNull(dialog.getResult());
        assertTrue(field("yearField").getStyleClass().contains("field-error"));
    }

    @Test
    void aRatingAboveTheMaximumIsRejected() {
        fillValidForm();
        set("ratingField", "140");
        save();

        assertNull(dialog.getResult());
        assertTrue(field("ratingField").getStyleClass().contains("field-error"));
    }

    @Test
    void aDurationThatIsNotANumberIsRejected() {
        fillValidForm();
        set("durationField", "three minutes");
        save();

        assertNull(dialog.getResult());
        assertTrue(field("durationField").getStyleClass().contains("field-error"));
    }

    @Test
    void everyProblemIsReportedAtOnceNotOneAtATime() {
        fillValidForm();
        set("titleField", "");
        set("yearField", "3000");
        save();

        String message = label("errorLabel").getText();
        assertTrue(message.contains("Title"), message);
        assertTrue(message.contains("Year"), message);
    }

    @Test
    void marksAreClearedOnceTheFormIsCorrected() {
        fillValidForm();
        set("titleField", "");
        save();
        assertTrue(field("titleField").getStyleClass().contains("field-error"));

        set("titleField", "Blue Monday");
        save();

        assertNotNull(dialog.getResult());
        assertFalse(field("titleField").getStyleClass().contains("field-error"));
        assertFalse(label("errorLabel").isVisible());
    }

    // ---- duration parsing ------------------------------------------------

    @Test
    void readsDurationAsMinutesAndSeconds() {
        assertEquals(320, SongDialogController.parseDuration("5:20"));
        assertEquals(9, SongDialogController.parseDuration("0:09"));
    }

    @Test
    void readsDurationAsAPlainNumberOfSeconds() {
        assertEquals(212, SongDialogController.parseDuration("212"));
    }

    @Test
    void refusesADurationWithMoreThanFiftyNineSeconds() {
        assertEquals(-1, SongDialogController.parseDuration("3:75"));
    }

    @Test
    void refusesABlankOrNonNumericDuration() {
        assertEquals(-1, SongDialogController.parseDuration(""));
        assertEquals(-1, SongDialogController.parseDuration(null));
        assertEquals(-1, SongDialogController.parseDuration("abc"));
    }

    // ---- editing ---------------------------------------------------------

    @Test
    void editModeFillsTheFormFromTheSong() {
        onFxThread(() -> dialog.setSong(existingSong()));

        assertEquals("Edit song", label("dialogTitle").getText());
        assertEquals("Tania", field("titleField").getText());
        assertEquals("Joe Arroyo", field("artistField").getText());
        assertEquals("4:48", field("durationField").getText());
        assertEquals("87", field("ratingField").getText());
    }

    @Test
    void editingReturnsTheSameInstanceSoTheIdSurvives() {
        Song original = existingSong();
        String id = original.getId();
        onFxThread(() -> dialog.setSong(original));

        set("titleField", "Tania (remaster)");
        save();

        assertSame(original, dialog.getResult(), "the library is keyed on the id, not on equality of fields");
        assertEquals(id, dialog.getResult().getId());
        assertEquals("Tania (remaster)", dialog.getResult().getTitle());
    }

    @Test
    void editingReplacesTheArtistRatherThanAppendingToIt() {
        Song original = existingSong();
        onFxThread(() -> dialog.setSong(original));

        set("artistField", "Joe Arroyo y La Verdad");
        save();

        assertEquals(1, dialog.getResult().getArtists().size());
        assertEquals("Joe Arroyo y La Verdad", dialog.getResult().getArtistsNames());
    }

    // ---- cancelling ------------------------------------------------------

    @Test
    void cancelProducesNoResultEvenWithAValidForm() {
        fillValidForm();
        onFxThread(() -> ((Button) root.lookup("#cancelButton")).fire());

        assertNull(dialog.getResult());
    }

    // ---- file pickers ----------------------------------------------------

    @Test
    void aChosenFileIsStoredAsAnAbsolutePath(@TempDir Path folder) throws Exception {
        File relative = new File(folder.toFile(), "art.png");
        assertTrue(relative.createNewFile());

        onFxThread(() -> dialog.storeAbsolutePath(relative, field("coverField")));

        String stored = field("coverField").getText();
        assertTrue(new File(stored).isAbsolute(), stored + " should be absolute");
        assertEquals(relative.getAbsolutePath(), stored);
    }

    @Test
    void aCancelledChooserLeavesTheFieldAlone() {
        set("coverField", "/existing/path.png");

        onFxThread(() -> dialog.storeAbsolutePath(null, field("coverField")));

        assertEquals("/existing/path.png", field("coverField").getText());
    }

    @Test
    void aChosenAudioFileIsNeverCopiedIntoResources(@TempDir Path folder) throws Exception {
        File audio = new File(folder.toFile(), "track.mp3");
        assertTrue(audio.createNewFile());

        onFxThread(() -> dialog.storeAbsolutePath(audio, field("audioField")));
        fillValidForm();
        set("audioField", audio.getAbsolutePath());
        save();

        assertEquals(audio.getAbsolutePath(), dialog.getResult().getAudioPath(),
                "the library points at where the media already lives");
    }

    // ---- helpers ---------------------------------------------------------

    private void fillValidForm() {
        set("titleField", "Blue Monday");
        set("artistField", "New Order");
        set("albumField", "Power, Corruption & Lies");
        set("durationField", "7:28");
        set("yearField", "1983");
        set("ratingField", "75");
        onFxThread(() -> genreCombo().setValue(Genre.ELECTRONIC));
    }

    private static Song existingSong() {
        Song song = new Song("Tania", List.of(new Artist("Joe Arroyo")), new Album("Cruzando El Milenio"),
                288, Genre.SALSA, 1984);
        song.setRating(87);
        return song;
    }

    private void save() {
        onFxThread(() -> ((Button) root.lookup("#saveButton")).fire());
    }

    private void set(String id, String value) {
        onFxThread(() -> field(id).setText(value));
    }

    private TextField field(String id) {
        return (TextField) root.lookup("#" + id);
    }

    private Label label(String id) {
        return (Label) root.lookup("#" + id);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Genre> genreCombo() {
        return (ComboBox<Genre>) root.lookup("#genreCombo");
    }
}
