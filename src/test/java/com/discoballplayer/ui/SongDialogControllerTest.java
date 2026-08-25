package com.discoballplayer.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    // ---- editing ---------------------------------------------------------

    @Test
    void editModeFillsTheFormFromTheSong() {
        onFxThread(() -> dialog.setSong(existingSong()));

        assertEquals("Edit song", label("dialogTitle").getText());
        assertEquals("Tania", field("titleField").getText());
        assertEquals("Joe Arroyo", field("artistField").getText());
        assertEquals("4:48", label("durationLabel").getText());
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
    void theDurationIsNotSomethingTheUserCanType() {
        assertNull(root.lookup("#durationField"),
                "the file already knows its length; asking again only invites a wrong answer");
        assertNotNull(root.lookup("#durationLabel"));
        assertInstanceOf(Label.class, root.lookup("#durationLabel"));
    }

    @Test
    void anEmptyFormSaysWhereTheDurationWillComeFrom() {
        assertEquals("Read from the audio file", label("durationLabel").getText());
        assertEquals(0, dialog.durationSeconds());
    }

    @Test
    void choosingAnAudioFileFillsTheDurationFromWhatTheFileReports(@TempDir Path folder)
            throws Exception {
        File audio = existingFile(folder, "track.mp3");
        onFxThread(() -> dialog.durationSource(path -> OptionalInt.of(212)));

        readDuration(audio);

        assertEquals("3:32", label("durationLabel").getText());
        assertEquals(212, dialog.durationSeconds());
    }

    @Test
    void aFileSuppliedDurationIsWhatGetsSaved(@TempDir Path folder) throws Exception {
        fillFormExceptDuration();
        File audio = existingFile(folder, "track.mp3");
        onFxThread(() -> dialog.durationSource(path -> OptionalInt.of(212)));
        readDuration(audio);

        save();

        assertEquals(212, dialog.getResult().getDurationSeconds());
    }

    @Test
    void savingWithoutAReadableDurationIsRefusedAndPointsAtTheAudioField() {
        fillFormExceptDuration();

        save();

        assertNull(dialog.getResult());
        assertTrue(label("errorLabel").getText().contains("audio file"),
                label("errorLabel").getText());
        assertTrue(field("audioField").getStyleClass().contains("field-error"),
                "the message has to point at the field that fixes it");
    }

    @Test
    void aFileWithNoReadableDurationLeavesTheKnownOneAlone(@TempDir Path folder) throws Exception {
        onFxThread(() -> dialog.setSong(existingSong()));
        assertEquals(288, dialog.durationSeconds());

        File audio = existingFile(folder, "replacement.mp3");
        onFxThread(() -> dialog.durationSource(path -> OptionalInt.empty()));
        readDuration(audio);

        assertEquals(288, dialog.durationSeconds(),
                "replacing the audio with an unmeasurable file must not erase a known duration");
    }

    @Test
    void aRealFileThatIsNotAudioLeavesTheDurationUnknown(@TempDir Path folder) throws Exception {
        // Against the real reader, not a stub: this is the path a user hits by mis-picking.
        Path text = folder.resolve("not-audio.mp3");
        Files.writeString(text, "this is text, not audio");

        readDuration(text.toFile());

        assertEquals(0, dialog.durationSeconds());
        assertEquals("Read from the audio file", label("durationLabel").getText());
    }

    @Test
    void aRealAudioFileIsMeasuredByTheRealReader(@TempDir Path folder) throws Exception {
        // No stub. This is the bug the user reported: the field stayed empty for every file
        // without metadata tags, because the reader waited on the tag map instead of on a
        // MediaPlayer becoming ready. A generated WAV has no tags at all.
        File wav = threeSecondWav(folder);

        readDuration(wav);

        assertEquals(3, dialog.durationSeconds(),
                "a file with no tags still knows how long it is");
        assertEquals("0:03", label("durationLabel").getText());
    }

    /** A real, decodable, completely untagged three-second WAV. */
    private static File threeSecondWav(Path folder) throws Exception {
        int rate = 8000;
        int seconds = 3;
        byte[] samples = new byte[rate * seconds * 2];
        for (int i = 0; i < rate * seconds; i++) {
            short value = (short) (Math.sin(i * 2 * Math.PI * 440 / rate) * 8000);
            samples[i * 2] = (byte) (value & 0xff);
            samples[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }
        AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
        File file = folder.resolve("untagged.wav").toFile();
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(samples), format, samples.length / format.getFrameSize())) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
        }
        return file;
    }

    @Test
    void aCancelledAudioChooserReadsNothingAtAll() {
        assertNull(dialog.readDurationInBackground(null));
        assertEquals(0, dialog.durationSeconds());
    }

    @Test
    void theDurationIsNeverReadOnTheFxThread(@TempDir Path folder) throws Exception {
        // The reader blocks until the decoder is ready. Inline, that is a frozen window from a
        // file-chooser handler, on exactly the files where waiting gains nothing.
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
    void editingShowsTheStoredDurationWithoutAnAudioFile() {
        onFxThread(() -> dialog.setSong(existingSong()));

        assertEquals("4:48", label("durationLabel").getText());
        assertEquals(288, dialog.durationSeconds());
    }

    @Test
    void editingASongWithNoAudioFileStillSaves() {
        Song stored = existingSong();
        onFxThread(() -> dialog.setSong(stored));

        set("titleField", "Tania (remaster)");
        save();

        assertSame(stored, dialog.getResult());
        assertEquals(288, dialog.getResult().getDurationSeconds(),
                "a catalogue entry that already has a duration keeps it");
    }

    /** Starts the background read from the FX thread, as a file-chooser handler would. */
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

    /** Everything a save needs except the duration, which only an audio file can supply. */
    private void fillFormExceptDuration() {
        set("titleField", "Blue Monday");
        set("artistField", "New Order");
        set("albumField", "Power, Corruption & Lies");
        set("yearField", "1983");
        set("ratingField", "75");
        onFxThread(() -> genreCombo().setValue(Genre.ELECTRONIC));
    }

    private void fillValidForm() {
        fillFormExceptDuration();
        onFxThread(() -> dialog.applyDuration(OptionalInt.of(448)));
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
