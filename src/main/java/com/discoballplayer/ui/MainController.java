package com.discoballplayer.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    private static final String SONG_DIALOG = "/com/discoballplayer/fxml/song-dialog.fxml";
    private static final String DARK_THEME = "/com/discoballplayer/css/dark.css";

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

    /** True while the slider is being set from a song, so the sync does not re-rate it. */
    private boolean syncingRating;

    /**
     * The song the bar is currently showing.
     *
     * <p>Held rather than re-read from the service on demand: the rating slider sits next to
     * this song's title, so it has to rate the song the user is looking at even if the service
     * has since moved on.</p>
     */
    private Song displayedSong;

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

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Slider ratingSlider;

    @FXML
    private Label ratingValueLabel;

    @FXML
    private ToggleButton themeToggle;

    @FXML
    private javafx.scene.layout.BorderPane rootPane;

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
        configureSelectionBinding();
        configureRatingSlider();
        // The scene does not exist while the controller is being initialised, so the theme and
        // the shortcuts are installed the moment the view is attached to one.
        rootPane.sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) {
                return;
            }
            installShortcuts(scene);
            // Deferred one pulse on purpose. This listener fires while the Scene is being
            // constructed, before its owner has added app.css, and a stylesheet added first
            // loses to one added after it. Waiting until the scene is assembled puts the
            // theme override last, where it has to be to win.
            Platform.runLater(() -> applyTheme(true));
        });
    }

    /**
     * Edit and Delete act on the selected row, so they are bound to the selection rather than
     * enabled and then guarded against a null.
     */
    private void configureSelectionBinding() {
        var noSelection = libraryTable.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
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
        Song selected = libraryTable.getSelectionModel().getSelectedItem();
        List<Song> matches = player.search(query);
        libraryTable.setItems(FXCollections.observableArrayList(matches));
        // Replacing the items clears the selection, which would disable Edit and Delete every
        // time a rating change refreshed the table. Put the user's row back.
        if (selected != null && matches.contains(selected)) {
            libraryTable.getSelectionModel().select(selected);
        }
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
        syncRating(null);
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
        syncRating(song);
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

    // ---- library editing -------------------------------------------------

    @FXML
    private void onAddSong() {
        Song created = showSongDialog(null);
        if (created != null) {
            player.addSong(created);
        }
    }

    @FXML
    private void onEditSong() {
        Song selected = libraryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Song edited = showSongDialog(selected);
        if (edited != null) {
            player.updateSong(edited);
        }
    }

    @FXML
    private void onDeleteSong() {
        Song selected = libraryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        player.removeSong(selected);
    }

    /**
     * Opens the add/edit form modally and returns what the user accepted.
     *
     * @param song the song to edit, or {@code null} to create one
     * @return the saved song, or {@code null} if the dialog was cancelled
     */
    private Song showSongDialog(Song song) {
        try {
            URL view = Objects.requireNonNull(
                    MainController.class.getResource(SONG_DIALOG), "song-dialog.fxml is missing");
            FXMLLoader loader = new FXMLLoader(view);
            Parent form = loader.load();
            SongDialogController dialog = loader.getController();
            dialog.setSong(song);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(libraryTable.getScene().getWindow());
            stage.setTitle(song == null ? "Add song" : "Edit song");
            Scene scene = new Scene(form);
            stylesheetOf(libraryTable).ifPresent(scene.getStylesheets()::add);
            stage.setScene(scene);
            stage.showAndWait();

            return dialog.getResult();
        } catch (IOException cannotLoad) {
            statusLabel.setText("The song form could not be opened");
            return null;
        }
    }

    private static java.util.Optional<String> stylesheetOf(javafx.scene.Node node) {
        Scene scene = node.getScene();
        if (scene == null || scene.getStylesheets().isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(scene.getStylesheets().get(0));
    }

    // ---- rating ----------------------------------------------------------

    private void configureRatingSlider() {
        ratingSlider.valueProperty().addListener((observable, previous, value) -> {
            int rating = (int) Math.round(value.doubleValue());
            ratingValueLabel.setText("Rating " + rating);
            if (syncingRating || displayedSong == null) {
                return;
            }
            player.rate(displayedSong, rating);
        });
    }

    /**
     * Points the slider at the current song without treating the move as a user rating.
     */
    private void syncRating(Song song) {
        displayedSong = song;
        syncingRating = true;
        try {
            ratingSlider.setDisable(song == null);
            ratingSlider.setValue(song == null ? 0 : song.getRating());
            ratingValueLabel.setText("Rating " + (song == null ? 0 : song.getRating()));
        } finally {
            syncingRating = false;
        }
    }

    // ---- theme -----------------------------------------------------------

    @FXML
    private void onToggleTheme() {
        applyTheme(!themeToggle.isSelected());
    }

    /**
     * Adds or removes {@code dark.css}, which carries token overrides and nothing else.
     *
     * <p>Because no size, padding or radius lives in that file, swapping it changes colours
     * only and the scene graph is never re-measured.</p>
     */
    private void applyTheme(boolean dark) {
        Scene scene = rootPane.getScene();
        if (scene == null) {
            return;
        }
        URL theme = MainController.class.getResource(DARK_THEME);
        if (theme == null) {
            return;
        }
        String stylesheet = theme.toExternalForm();
        scene.getStylesheets().remove(stylesheet);
        if (dark) {
            scene.getStylesheets().add(stylesheet);
        }
        themeToggle.setSelected(!dark);
        themeToggle.setText(dark ? "Light" : "Dark");
    }

    /** Visible for tests: which theme the scene is currently wearing. */
    boolean isDarkTheme() {
        Scene scene = rootPane.getScene();
        URL theme = MainController.class.getResource(DARK_THEME);
        return scene != null && theme != null
                && scene.getStylesheets().contains(theme.toExternalForm());
    }

    // ---- keyboard --------------------------------------------------------

    /**
     * Registers the shortcuts on the scene rather than on individual buttons, so they work
     * wherever the focus happens to be.
     *
     * <p>Space and the arrow keys are filtered rather than bound as accelerators: both already
     * mean something inside a text field and inside the table, and stealing them there would
     * make the search box unusable. The filter stands down whenever a text input has focus.</p>
     */
    private void installShortcuts(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                () -> searchField.requestFocus());
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                this::onAddSong);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTransportKey);
    }

    void handleTransportKey(KeyEvent event) {
        if (scene() != null && scene().getFocusOwner() instanceof TextInputControl) {
            return;
        }
        switch (event.getCode()) {
            case SPACE -> {
                onPlayPause();
                event.consume();
            }
            case LEFT -> {
                if (!previousButton.isDisabled()) {
                    onPrevious();
                }
                event.consume();
            }
            case RIGHT -> {
                onNext();
                event.consume();
            }
            default -> {
                // every other key belongs to whatever has focus
            }
        }
    }

    private Scene scene() {
        return rootPane.getScene();
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
