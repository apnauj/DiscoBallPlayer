package com.discoballplayer.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;

import com.discoballplayer.model.Album;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.audio.AudioMetadata;
import com.discoballplayer.util.TimeFormatter;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controller for {@code song-dialog.fxml}, used for both adding and editing.
 *
 * <p>{@link #setSong(Song)} with {@code null} means create; with a song it means edit, and the
 * result is that same instance mutated. {@code Song} carries a final generated id and the
 * library is keyed on it, so editing must never hand back a different object.</p>
 *
 * <p>Validation marks the offending fields and writes a message. It never throws: a dialog
 * that throws on a typo would take the whole event loop with it.</p>
 */
public class SongDialogController {

    private static final String ERROR_CLASS = "field-error";
    private static final String DERIVED_ABSENT = "derived-value-absent";
    private static final String UNKNOWN_DURATION = "Read from the audio file";
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

    /** Set only when Save accepted the form; {@code null} after Cancel or before Save. */
    private Song result;

    /** The song being edited, or {@code null} when the dialog is creating one. */
    private Song editing;

    /**
     * The duration in seconds, read from the audio file rather than typed.
     *
     * <p>Zero means unknown, which is only possible before an audio file has been chosen.</p>
     */
    private int durationSeconds;

    @FXML
    private VBox dialogRoot;

    @FXML
    private Label dialogTitle;

    @FXML
    private TextField titleField;

    @FXML
    private TextField artistField;

    @FXML
    private TextField albumField;

    @FXML
    private Label durationLabel;

    @FXML
    private ComboBox<Genre> genreCombo;

    @FXML
    private TextField yearField;

    @FXML
    private TextField ratingField;

    @FXML
    private TextField coverField;

    @FXML
    private TextField audioField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button saveButton;

    @FXML
    private void initialize() {
        genreCombo.setItems(FXCollections.observableArrayList(Genre.values()));
        genreCombo.setValue(Genre.OTHER);
        // So the row reads as "waiting for a file" rather than blank, even before setSong.
        showDuration(0);
    }

    // ---- the form's two doors --------------------------------------------

    /**
     * Fills the form. {@code null} puts the dialog into create mode.
     */
    public void setSong(Song song) {
        editing = song;
        result = null;
        clearErrors();

        if (song == null) {
            dialogTitle.setText("Add song");
            titleField.clear();
            artistField.clear();
            albumField.clear();
            showDuration(0);
            genreCombo.setValue(Genre.OTHER);
            yearField.clear();
            ratingField.setText("0");
            coverField.clear();
            audioField.clear();
            return;
        }

        dialogTitle.setText("Edit song");
        titleField.setText(song.getTitle());
        artistField.setText(song.getArtists().isEmpty() ? "" : song.getArtistsNames());
        albumField.setText(song.getAlbum() == null ? "" : song.getAlbum().getTitle());
        showDuration(song.getDurationSeconds());
        genreCombo.setValue(song.getGenre());
        yearField.setText(String.valueOf(song.getYear()));
        ratingField.setText(String.valueOf(song.getRating()));
        coverField.setText(song.getCoverPath() == null ? "" : song.getCoverPath());
        audioField.setText(song.getAudioPath() == null ? "" : song.getAudioPath());
    }

    /**
     * @return the saved song, or {@code null} if the dialog was cancelled or never accepted
     */
    public Song getResult() {
        return result;
    }

    // ---- save and cancel -------------------------------------------------

    @FXML
    private void onSave() {
        clearErrors();
        List<String> problems = new ArrayList<>();

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            problems.add("Title is required.");
            markInvalid(titleField);
        }

        if (durationSeconds <= 0) {
            problems.add("Choose an audio file — the duration is read from it.");
            markInvalid(audioField);
        }

        int year = parseInt(yearField.getText(), -1);
        if (year < MIN_YEAR || year > MAX_YEAR) {
            problems.add("Year must be between " + MIN_YEAR + " and " + MAX_YEAR + ".");
            markInvalid(yearField);
        }

        int rating = parseInt(ratingField.getText(), 0);
        if (rating < Song.MIN_RATING || rating > Song.MAX_RATING) {
            problems.add("Rating must be between " + Song.MIN_RATING + " and " + Song.MAX_RATING + ".");
            markInvalid(ratingField);
        }

        if (!problems.isEmpty()) {
            showErrors(problems);
            return;
        }

        result = apply(title, durationSeconds, year, rating);
        close();
    }

    @FXML
    private void onCancel() {
        result = null;
        close();
    }

    /**
     * Writes the accepted values, mutating the edited song rather than replacing it so its id
     * — the library's key — survives an edit.
     */
    private Song apply(String title, int duration, int year, int rating) {
        Genre genre = genreCombo.getValue() == null ? Genre.OTHER : genreCombo.getValue();
        String artistName = trimmed(artistField);
        String albumTitle = trimmed(albumField);

        Song song = editing;
        if (song == null) {
            List<Artist> artists = artistName.isEmpty() ? List.of() : List.of(new Artist(artistName));
            Album album = albumTitle.isEmpty() ? null : new Album(albumTitle);
            song = new Song(title, artists, album, duration, genre, year);
        } else {
            song.setTitle(title);
            song.setDurationSeconds(duration);
            song.setGenre(genre);
            song.setYear(year);
            replaceArtists(song, artistName);
            song.setAlbum(albumTitle.isEmpty() ? null : new Album(albumTitle));
        }

        song.setRating(rating);
        song.setCoverPath(blankToNull(trimmed(coverField)));
        song.setAudioPath(blankToNull(trimmed(audioField)));
        return song;
    }

    private static void replaceArtists(Song song, String artistName) {
        List.copyOf(song.getArtists()).forEach(song::removeArtist);
        if (!artistName.isEmpty()) {
            song.addArtist(new Artist(artistName));
        }
    }

    // ---- file pickers ----------------------------------------------------

    @FXML
    private void onBrowseCover() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a cover image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        storeAbsolutePath(chooser.showOpenDialog(window()), coverField);
    }

    @FXML
    private void onBrowseAudio() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an audio file");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.m4a", "*.aac"));
        File chosen = chooser.showOpenDialog(window());
        storeAbsolutePath(chosen, audioField);
        fillDurationFrom(chosen);
    }

    /**
     * Where a file's duration comes from.
     *
     * <p>Package-private and replaceable so a test can supply a known answer instead of
     * depending on the platform's decoder, which behaves differently per file and per OS.</p>
     */
    private Function<String, OptionalInt> durationSource = AudioMetadata::durationSeconds;

    void durationSource(Function<String, OptionalInt> source) {
        this.durationSource = Objects.requireNonNull(source);
    }

    /**
     * Fills the duration from the chosen file's own headers.
     *
     * <p>The file knows its length better than the person typing, so reading it removes the
     * commonest way for a catalogue entry to be wrong.</p>
     *
     * <p>When the file will not give one up — unreadable, not audio, or slow enough that the
     * reader gives up — the field is left exactly as it was, and it is never made read-only:
     * a song with no audio file can only get its duration by being typed, so a field that
     * fills itself sometimes must still accept typing always.</p>
     *
     * <p>Written as {@code m:ss} to match what {@link #setSong} writes and what
     * {@link #parseDuration} reads back.</p>
     */
    void fillDurationFrom(File chosen) {
        readDurationInBackground(chosen);
    }

    /**
     * Reads the duration off the FX thread and applies it back on it.
     *
     * <p>Not an optimisation. The reader blocks for up to three seconds on a file whose
     * duration it cannot determine, and this runs from a file-chooser handler on the FX
     * thread, so doing it inline freezes the whole window for those three seconds — on
     * exactly the files where nothing is gained by waiting.</p>
     *
     * @return the worker, so a test can wait for it; callers in the UI ignore it
     */
    Thread readDurationInBackground(File chosen) {
        if (chosen == null) {
            return null;
        }
        String path = chosen.getAbsolutePath();
        Thread worker = new Thread(() -> {
            OptionalInt seconds = durationSource.apply(path);
            if (seconds.isPresent()) {
                Platform.runLater(() -> applyDuration(seconds));
            }
        }, "duration-reader");
        worker.setDaemon(true);
        worker.start();
        return worker;
    }

    /**
     * Records a duration read from a file, leaving the previous one alone when there is none.
     *
     * <p>Keeping the old value matters when editing: a song already has a duration, and
     * replacing its audio file with one that cannot be measured must not erase it.</p>
     */
    void applyDuration(OptionalInt seconds) {
        if (seconds.isPresent()) {
            showDuration(seconds.getAsInt());
        }
    }

    private void showDuration(int seconds) {
        durationSeconds = Math.max(seconds, 0);
        boolean known = durationSeconds > 0;
        durationLabel.setText(known ? TimeFormatter.mmss(durationSeconds) : UNKNOWN_DURATION);
        durationLabel.getStyleClass().removeAll(DERIVED_ABSENT);
        if (!known) {
            durationLabel.getStyleClass().add(DERIVED_ABSENT);
        }
    }

    /** Visible for tests: the duration the form will save. */
    int durationSeconds() {
        return durationSeconds;
    }

    /**
     * Stores the chosen file as an absolute path.
     *
     * <p>Absolute on purpose, and nothing is copied into {@code resources}: the library points
     * at where the user's media already lives, so a rebuild cannot silently discard it.</p>
     *
     * <p>Package-private and separate from the chooser so the path handling is testable
     * without a native file dialog.</p>
     */
    void storeAbsolutePath(File chosen, TextField target) {
        if (chosen == null) {
            return;
        }
        target.setText(chosen.getAbsolutePath());
    }

    // ---- validation plumbing ---------------------------------------------

    private void showErrors(List<String> problems) {
        errorLabel.setText(String.join(" ", problems));
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearErrors() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        List.of(titleField, audioField, yearField, ratingField).forEach(
                field -> field.getStyleClass().remove(ERROR_CLASS));
    }

    private static void markInvalid(Control field) {
        if (!field.getStyleClass().contains(ERROR_CLASS)) {
            field.getStyleClass().add(ERROR_CLASS);
        }
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException | NullPointerException notANumber) {
            return fallback;
        }
    }

    private String trimmed(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static String blankToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private Window window() {
        return (dialogRoot.getScene() == null) ? null : dialogRoot.getScene().getWindow();
    }

    /**
     * Closes the hosting window when there is one. A dialog loaded in a test has no Stage, and
     * accepting the form must not depend on being on screen.
     */
    private void close() {
        if (window() instanceof Stage stage) {
            stage.close();
        }
    }
}
