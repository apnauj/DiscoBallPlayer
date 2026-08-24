package com.discoballplayer.ui;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.model.Album;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.AlphabeticalMode;
import com.discoballplayer.playback.ArrivalMode;
import com.discoballplayer.playback.PlaybackMode;
import com.discoballplayer.playback.ShuffleMode;
import com.discoballplayer.service.DemoPlayerService;
import com.discoballplayer.service.PlaybackListener;
import com.discoballplayer.service.PlayerService;
import com.discoballplayer.util.TimeFormatter;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Controller for {@code main-view.fxml}.
 *
 * <p>It reads input, calls {@link PlayerService} and updates widgets. It never decides
 * anything about playback order and never imports {@code com.discoballplayer.structures}:
 * which structure is behind a mode is invisible from here, which is the whole point of the
 * service seam.</p>
 *
 * <p>Playback events arrive on a background timer thread, so every callback body below runs
 * inside {@link Platform#runLater}.</p>
 */
public class MainController implements PlaybackListener {

    /** Shown where a song carries no album. */
    private static final String ABSENT = "—";

    private static final String DEFAULT_COVER = "/com/discoballplayer/images/default-cover.png";

    private static final String NO_SONG_TITLE = "Nothing playing";
    private static final String NO_SONG_ARTIST = "Pick a song to begin";

    private static final String PLAY = "Play";
    private static final String PAUSE = "Pause";
    private static final String QUEUE_FINISHED = "Queue finished";
    private static final String NO_SONG_LOADED = "No song loaded";

    /** The single line that ticket {@code C-01} swaps for the real {@code Player}. */
    private final PlayerService player = new DemoPlayerService();

    /** Decoded once: the fallback is reached often and re-reading it per song would show. */
    private Image defaultCover;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Song> libraryTable;

    @FXML
    private TableColumn<Song, String> titleColumn;

    @FXML
    private TableColumn<Song, String> artistColumn;

    @FXML
    private TableColumn<Song, String> albumColumn;

    @FXML
    private TableColumn<Song, String> durationColumn;

    @FXML
    private TableColumn<Song, String> genreColumn;

    @FXML
    private TableColumn<Song, String> yearColumn;

    @FXML
    private TableColumn<Song, String> ratingColumn;

    @FXML
    private ImageView coverImage;

    @FXML
    private Label nowPlayingTitle;

    @FXML
    private Label nowPlayingArtist;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label elapsedLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Button previousButton;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button nextButton;

    @FXML
    private RadioButton shuffleModeButton;

    @FXML
    private RadioButton arrivalModeButton;

    @FXML
    private RadioButton alphabeticalModeButton;

    /**
     * Called by {@link javafx.fxml.FXMLLoader} once the widget tree is built.
     */
    @FXML
    private void initialize() {
        defaultCover = loadDefaultCover();
        configureColumns();
        searchField.textProperty().addListener((observable, previous, query) -> showMatches(query));
        player.addListener(this);
        showMatches(searchField.getText());
        clearNowPlaying();
        statusLabel.setText(NO_SONG_LOADED);
        shuffleModeButton.setSelected(true);
        selectMode(new ShuffleMode());
    }

    /**
     * The service this view talks to.
     *
     * <p>Package-private, and the only door into the controller's state: it lets a test drive
     * the library the way the rest of the application will, through {@link PlayerService},
     * rather than reaching for a field.</p>
     */
    PlayerService player() {
        return player;
    }

    // ---- library table ---------------------------------------------------

    /**
     * Binds each column to a rendered string.
     *
     * <p>Deliberately not {@code PropertyValueFactory}: that resolves the getter reflectively,
     * which would need {@code model} opened to {@code javafx.base} in a module descriptor that
     * was frozen in {@code F0-09}. A lambda reads the getter directly and fails at compile time
     * instead of at runtime.</p>
     */
    private void configureColumns() {
        titleColumn.setCellValueFactory(cell -> text(cell.getValue().getTitle()));
        artistColumn.setCellValueFactory(cell -> text(cell.getValue().getArtistsNames()));
        albumColumn.setCellValueFactory(cell -> text(albumTitle(cell.getValue())));
        durationColumn.setCellValueFactory(
                cell -> text(TimeFormatter.mmss(cell.getValue().getDurationSeconds())));
        genreColumn.setCellValueFactory(cell -> text(cell.getValue().getGenre().getDisplayName()));
        yearColumn.setCellValueFactory(cell -> text(String.valueOf(cell.getValue().getYear())));
        ratingColumn.setCellValueFactory(cell -> text(String.valueOf(cell.getValue().getRating())));
    }

    private static String albumTitle(Song song) {
        Album album = song.getAlbum();
        return (album == null) ? ABSENT : album.getTitle();
    }

    private static ObservableValue<String> text(String value) {
        return new ReadOnlyStringWrapper(value);
    }

    /**
     * Replaces the table's rows with the songs matching {@code query}.
     *
     * <p>A blank query is not a special case here: the service answers it with the whole
     * library, so filtering and the initial load are the same call.</p>
     */
    private void showMatches(String query) {
        List<Song> matches = player.search(query);
        libraryTable.setItems(FXCollections.observableArrayList(matches));
    }

    // ---- now playing -----------------------------------------------------

    /**
     * Puts the bar back into its idle state, cover included, so nothing from a previous song
     * lingers next to placeholder text.
     */
    private void clearNowPlaying() {
        nowPlayingTitle.setText(NO_SONG_TITLE);
        nowPlayingArtist.setText(NO_SONG_ARTIST);
        coverImage.setImage(defaultCover);
        renderProgress(0, 0);
    }

    private void renderNowPlaying(Song song) {
        if (song == null) {
            clearNowPlaying();
            return;
        }
        nowPlayingTitle.setText(song.getTitle());
        nowPlayingArtist.setText(song.getArtistsNames());
        coverImage.setImage(coverFor(song));
        renderProgress(0, song.getDurationSeconds());
    }

    private void renderProgress(int elapsedSeconds, int totalSeconds) {
        double fraction = (totalSeconds <= 0)
                ? 0
                : Math.min(1, Math.max(0, elapsedSeconds / (double) totalSeconds));
        progressBar.setProgress(fraction);
        elapsedLabel.setText(TimeFormatter.mmss(elapsedSeconds));
        totalLabel.setText(TimeFormatter.mmss(totalSeconds));
    }

    /**
     * Resolves a song's cover, falling back to the shipped placeholder.
     *
     * <p>A cover path is user-supplied and points outside the jar, so every way it can fail —
     * absent, not a file, not an image, not even a valid URL — has to land on the placeholder.
     * A blank box or a dialog in the middle of playback would both be worse than a default
     * picture.</p>
     */
    private Image coverFor(Song song) {
        String path = song.getCoverPath();
        if (path == null || path.isBlank()) {
            return defaultCover;
        }
        try {
            File file = new File(path);
            String source = file.isFile() ? file.toURI().toString() : path;
            Image cover = new Image(source, false);
            return cover.isError() ? defaultCover : cover;
        } catch (RuntimeException malformedPath) {
            return defaultCover;
        }
    }

    private Image loadDefaultCover() {
        URL resource = MainController.class.getResource(DEFAULT_COVER);
        return (resource == null) ? null : new Image(resource.toExternalForm(), false);
    }

    // ---- transport -------------------------------------------------------

    @FXML
    private void onPrevious() {
        player.previous();
        refreshTransport();
    }

    @FXML
    private void onNext() {
        try {
            player.next();
            statusLabel.setText("");
        } catch (EmptyStructureException exhausted) {
            // Arrival order consumes its queue, so running out is ordinary, not a defect.
            // Letting it reach the FX event loop would print a stack trace at the demo.
            statusLabel.setText(QUEUE_FINISHED);
        }
        refreshTransport();
    }

    @FXML
    private void onPlayPause() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        refreshTransport();
    }

    /**
     * Re-reads the service's own answer about what is navigable.
     *
     * <p>The Previous button is disabled from {@link PlayerService#hasPrevious()} rather than
     * from a check on which mode is selected, so a mode that cannot go back disables it by
     * saying so, and the UI never encodes a rule that belongs to the playback layer.</p>
     *
     * <p>Next is deliberately left enabled on an exhausted queue. Disabling it too would be
     * tidier, but it would also make the finished-queue message unreachable: the press that
     * runs off the end is the one that reports it.</p>
     */
    private void refreshTransport() {
        previousButton.setDisable(!player.hasPrevious());
    }

    // ---- mode selection --------------------------------------------------

    @FXML
    private void onShuffleMode() {
        selectMode(new ShuffleMode());
    }

    @FXML
    private void onArrivalMode() {
        selectMode(new ArrivalMode());
    }

    @FXML
    private void onAlphabeticalMode() {
        selectMode(new AlphabeticalMode());
    }

    private void selectMode(PlaybackMode mode) {
        player.setMode(mode);
        clearNowPlaying();
        statusLabel.setText(NO_SONG_LOADED);
        refreshTransport();
    }

    // ---- playback events -------------------------------------------------

    @Override
    public void onSongChanged(Song song) {
        Platform.runLater(() -> {
            renderNowPlaying(song);
            refreshTransport();
        });
    }

    /**
     * The button label follows the service, never a boolean kept here: anything that starts or
     * stops playback without going through the button still leaves the label truthful.
     */
    @Override
    public void onPlaybackStateChanged(boolean playing) {
        Platform.runLater(() -> playPauseButton.setText(playing ? PAUSE : PLAY));
    }

    @Override
    public void onProgress(int elapsedSeconds, int totalSeconds) {
        Platform.runLater(() -> renderProgress(elapsedSeconds, totalSeconds));
    }

    @Override
    public void onLibraryChanged() {
        Platform.runLater(() -> showMatches(searchField.getText()));
    }
}
