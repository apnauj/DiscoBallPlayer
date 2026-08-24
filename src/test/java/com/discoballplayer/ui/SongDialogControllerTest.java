package com.discoballplayer.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    // ---- duration read from the audio file -------------------------------

    @Test
    void choosingAnAudioFileFillsTheDurationFromWhatTheFileReports(@TempDir Path folder)
            throws Exception {
        File audio = existingFile(folder, "track.mp3");
        onFxThread(() -> dialog.durationSource(path -> OptionalInt.of(212)));

        readDuration(audio);

        assertEquals("3:32", field("durationField").getText(),
                "the file knows its own length better than the person typing");
    }

    @Test
    void aFilledDurationSurvivesARoundTripThroughTheForm(@TempDir Path folder) throws Exception {
        File audio = existingFile(folder, "track.mp3");
        onFxThread(() -> dialog.durationSource(path -> OptionalInt.of(212)));
        readDuration(audio);

        fillValidForm();
        set("durationField", "3:32");
        save();

        assertEquals(212, dialog.getResult().getDurationSeconds(),
                "what the picker writes has to be readable by parseDuration");
    }

    @Test
    void aFileWithNoReadableDurationLeavesTheFieldAlone(@TempDir Path folder) throws Exception {
        File audio = existingFile(folder, "track.mp3");
        set("durationField", "4:20");
        onFxThread(() -> dialog.durationSource(path -> OptionalInt.empty()));

        readDuration(audio);

        assertEquals("4:20", field("durationField").getText(),
                "an unreadable file must not wipe what the user already typed");
    }

    @Test
    void aRealFileThatIsNotAudioLeavesTheFieldAlone(@TempDir Path folder) throws Exception {
        // Against the real reader, not a stub: this is the path a user hits by mis-picking.
        Path text = folder.resolve("not-audio.mp3");
        Files.writeString(text, "this is text, not audio");
        set("durationField", "4:20");

        readDuration(text.toFile());

        assertEquals("4:20", field("durationField").getText());
    }

    @Test
    void aCancelledAudioChooserReadsNothingAtAll() {
        set("durationField", "4:20");

        assertNull(dialog.readDurationInBackground(null));
        assertEquals("4:20", field("durationField").getText());
    }

    @Test
    void theDurationIsNeverReadOnTheFxThread(@TempDir Path folder) throws Exception {
        // The reader blocks for up to three seconds on a file it cannot measure. Inline, that
        // is a three-second freeze of the whole window from a file-chooser handler.
        File audio = existingFile(folder, "track.mp3");
        AtomicReference<String> readerThread = new AtomicReference<>();
        onFxThread(() -> dialog.durationSource(path -> {
            readerThread.set(Thread.currentThread().getName());
            return OptionalInt.of(60);
        }));

        readDuration(audio);

        assertNotNull(readerThread.get());
        assertNotEquals("JavaFX Application Thread", readerThread.get(),
                "reading the duration inline freezes the dialog");
    }

    @Test
    void theDurationFieldStaysTypeableAfterBeingFilled(@TempDir Path folder) throws Exception {
        File audio = existingFile(folder, "track.mp3");
        onFxThread(() -> dialog.durationSource(path -> OptionalInt.of(60)));
        readDuration(audio);
        assertEquals("1:00", field("durationField").getText());

        assertTrue(field("durationField").isEditable(),
                "a song with no audio file can only get its duration by being typed");
        assertFalse(field("durationField").isDisabled());

        set("durationField", "2:30");
        assertEquals("2:30", field("durationField").getText());
    }

    @Test
    void applyingAnAbsentDurationWritesNothing() {
        set("durationField", "4:20");

        onFxThread(() -> dialog.applyDuration(OptionalInt.empty()));

        assertEquals("4:20", field("durationField").getText());
    }

    /**
     * Starts the read the way production does — from the FX thread, as a file-chooser handler
     * would — then waits for both it and the FX update to finish.
     *
     * <p>Starting it from the test thread instead would let an inline, blocking implementation
     * pass {@code theDurationIsNeverReadOnTheFxThread}. A deliberate mutation proved exactly
     * that before this helper was corrected.</p>
     */
    private void readDuration(File file) throws InterruptedException {
        AtomicReference<Thread> worker = new AtomicReference<>();
        onFxThread(() -> worker.set(dialog.readDurationInBackground(file)));
        if (worker.get() != null) {
            worker.get().join(10_000);
        }
        flushFxThread();
    }

    private static File existingFile(Path folder, String name) throws IOException {
        Path file = folder.resolve(name);
        Files.writeString(file, "placeholder");
        return file.toFile();
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
