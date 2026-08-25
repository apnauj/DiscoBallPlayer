package com.discoballplayer.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.exception.SongNotFoundException;
import com.discoballplayer.model.Album;
import com.discoballplayer.model.Genre;
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
import javafx.css.PseudoClass;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;
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
    private static final String NO_JUMPING = "Arrival order plays in the order songs arrived";
    private static final String NOT_IN_MODE = "That song is not in the current queue";

    /** Shown in a filter combo to mean "do not filter on this". */
    private static final String ANY = "All";

    private static final String PLAY_ICON = "M 5,3 L 16,10 L 5,17 Z";
    private static final String PAUSE_ICON = "M 5,3 L 8,3 L 8,17 L 5,17 Z M 12,3 L 15,3 L 15,17 L 12,17 Z";

    /**
     * The seam. Injected, never constructed here.
     *
     * <p>Building a {@code Player} inside this class would give the controller its own
     * {@link com.discoballplayer.model.MusicLibrary} while {@code Main} loads a different one
     * from disk. Nothing would throw — the app would simply show one catalogue, save another,
     * and lose every edit on restart. Composition belongs in {@code Main}.</p>
     */
    private final PlayerService player;

    /**
     * Runs the view on the in-memory stub.
     *
     * <p>This is the constructor {@code FXMLLoader} calls when nobody supplies a controller
     * factory, which keeps {@code song-dialog.fxml} and {@code main-view.fxml} loadable
     * standalone — in Scene Builder, and in the UI tests.</p>
     */
    public MainController() {
        this(new DemoPlayerService());
    }

    /**
     * Runs the view on the service it is given.
     *
     * <p>{@code Main} passes the real {@code Player} through
     * {@code FXMLLoader.setControllerFactory}.</p>
     */
    public MainController(PlayerService player) {
        this.player = Objects.requireNonNull(player, "A view without a service has nothing to show.");
    }

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
    /**
     * Marks the row whose song is on the bar. A pseudo-class rather than a style class: a class
     * has to be added and removed by hand, and a row that is recycled twice ends up carrying it
     * twice. JavaFX tracks a pseudo-class as a boolean, so it cannot drift out of step.
     */
    private static final PseudoClass PLAYING = PseudoClass.getPseudoClass("playing");

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

    @FXML
    private ComboBox<String> genreFilter;

    @FXML
    private ComboBox<String> artistFilter;

    @FXML
    private Slider ratingFilter;

    @FXML
    private Label ratingFilterLabel;

    @FXML
    private Label filterSummary;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Label volumeLabel;

    @FXML
    private SVGPath playPauseIcon;

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
        showPlaying(false);
        shuffleModeButton.setSelected(true);
        selectMode(new ShuffleMode());
        configureSelectionBinding();
        configureClickToPlay();
        configureRatingSlider();
        configureFilters();
        configureVolume();
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
     * Double-clicking a row plays that song.
     *
     * <p>The handler is installed on the row rather than on the table so a double click on the
     * empty area below the last song does nothing, and so the row that was hit is known
     * without consulting the selection model.</p>
     */
    private void configureClickToPlay() {
        libraryTable.setRowFactory(table -> {
            TableRow<Song> row = new TableRow<>() {
                @Override
                protected void updateItem(Song song, boolean empty) {
                    super.updateItem(song, empty);
                    pseudoClassStateChanged(PLAYING,
                            !empty && song != null && song.equals(displayedSong));
                }
            };
            row.getStyleClass().add("track-row");
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    playFromLibrary(row.getItem());
                }
            });
            return row;
        });
    }

    /**
     * Plays a song picked out of the table.
     *
     * <p>Both refusals are ordinary answers, not defects: arrival order will not reposition
     * because honouring a jump would mean discarding everything queued ahead of the target,
     * and a song can be absent from a mode that was loaded before it was added. Either one
     * reaching the FX event loop would break the window, so both become a status message.</p>
     */
    void playFromLibrary(Song song) {
        if (!player.canPlaySong()) {
            statusLabel.setText(NO_JUMPING);
            return;
        }
        try {
            player.playSong(song);
            statusLabel.setText("");
        } catch (UnsupportedOperationException cannotReposition) {
            statusLabel.setText(NO_JUMPING);
        } catch (SongNotFoundException notLoaded) {
            statusLabel.setText(NOT_IN_MODE);
        }
        refreshTransport();
    }

    /**
     * Edit and Delete act on the selection, so they are bound to it rather than enabled and
     * then guarded against a null. Click-to-play follows the mode's own answer the same way
     * the Previous button follows {@code hasPrevious()}.
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
        List<Song> matches = player.search(query).stream()
                .filter(this::passesFilters)
                .toList();
        describeFilters(matches.size(), player.listAll().size());
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
        libraryTable.setCursor(player.canPlaySong() ? Cursor.HAND : Cursor.DEFAULT);
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

    /**
     * Switches mode and starts playing its first song.
     *
     * <p>Choosing a mode is a request to hear it. Leaving the bar on "Nothing playing" made
     * the selection look like it had failed, and every mode was one extra click from doing
     * anything.</p>
     */
    private void selectMode(PlaybackMode mode) {
        player.setMode(mode);
        clearNowPlaying();
        statusLabel.setText("");
        try {
            if (player.hasNext()) {
                player.next();
                player.play();
            } else {
                statusLabel.setText(NO_SONG_LOADED);
            }
        } catch (EmptyStructureException empty) {
            // An empty library is not an error here; there is simply nothing to start.
            statusLabel.setText(NO_SONG_LOADED);
        }
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
            scene.getStylesheets().addAll(stylesheetsOf(libraryTable));
            stage.setScene(scene);
            stage.showAndWait();

            return dialog.getResult();
        } catch (IOException cannotLoad) {
            statusLabel.setText("The song form could not be opened");
            return null;
        }
    }

    /**
     * Every stylesheet the main window is wearing, in order.
     *
     * <p>All of them, not just the first: the theme override is a second sheet, and copying
     * only the base one opened the dialog in the light theme while the window behind it stayed
     * dark.</p>
     */
    private static List<String> stylesheetsOf(javafx.scene.Node node) {
        Scene scene = node.getScene();
        return (scene == null) ? List.of() : List.copyOf(scene.getStylesheets());
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
        boolean moved = !Objects.equals(displayedSong, song);
        displayedSong = song;
        if (moved) {
            // Rows decide their own "playing" state in updateItem, and nothing re-runs it on
            // its own: the row's item did not change, only the song it is compared against.
            libraryTable.refresh();
        }
        syncingRating = true;
        try {
            ratingSlider.setDisable(song == null);
            ratingSlider.setValue(song == null ? 0 : song.getRating());
            ratingValueLabel.setText("Rating " + (song == null ? 0 : song.getRating()));
        } finally {
            syncingRating = false;
        }
    }

    // ---- filters ---------------------------------------------------------

    private void configureFilters() {
        genreFilter.getItems().add(ANY);
        for (Genre genre : Genre.values()) {
            genreFilter.getItems().add(genre.getDisplayName());
        }
        genreFilter.setValue(ANY);

        refreshArtistChoices();

        ratingFilter.valueProperty().addListener((observable, previous, value) -> {
            ratingFilterLabel.setText("Rating " + (int) Math.round(value.doubleValue()) + "+");
            showMatches(searchField.getText());
        });
    }

    /**
     * Rebuilds the artist list from the library, keeping the current choice when it survives.
     *
     * <p>The list is derived rather than stored: an artist exists as a filter option exactly
     * as long as a song credits them, so deleting the last song by someone removes them
     * without anything having to remember to.</p>
     */
    private void refreshArtistChoices() {
        String chosen = artistFilter.getValue();
        List<String> artists = player.listAll().stream()
                .map(Song::getArtistsNames)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        artistFilter.getItems().setAll(ANY);
        artistFilter.getItems().addAll(artists);
        artistFilter.setValue(artistFilter.getItems().contains(chosen) ? chosen : ANY);
    }

    @FXML
    private void onFilterChanged() {
        showMatches(searchField.getText());
    }

    @FXML
    private void onClearFilters() {
        genreFilter.setValue(ANY);
        artistFilter.setValue(ANY);
        ratingFilter.setValue(0);
        searchField.clear();
        showMatches("");
    }

    /**
     * @return whether the song passes every active filter
     */
    private boolean passesFilters(Song song) {
        String genre = genreFilter.getValue();
        if (genre != null && !ANY.equals(genre) && !song.getGenre().getDisplayName().equals(genre)) {
            return false;
        }
        String artist = artistFilter.getValue();
        if (artist != null && !ANY.equals(artist) && !song.getArtistsNames().equals(artist)) {
            return false;
        }
        return song.getRating() >= (int) Math.round(ratingFilter.getValue());
    }

    private void describeFilters(int shown, int total) {
        filterSummary.setText(shown == total
                ? total + " songs"
                : shown + " of " + total + " songs");
    }

    // ---- volume ----------------------------------------------------------

    private void configureVolume() {
        volumeSlider.setValue(player.getVolume() * 100);
        showVolume(player.getVolume());
        volumeSlider.valueProperty().addListener((observable, previous, value) -> {
            double level = value.doubleValue() / 100;
            player.setVolume(level);
            showVolume(level);
        });
    }

    private void showVolume(double level) {
        volumeLabel.setText("Volume " + (int) Math.round(level * 100) + "%");
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
        Platform.runLater(() -> showPlaying(playing));
    }

    /**
     * The button is an icon now, so the state shows as a shape. The text is still set, because
     * it is what a screen reader announces and what the tooltip shows.
     */
    private void showPlaying(boolean playing) {
        playPauseButton.setText(playing ? PAUSE : PLAY);
        playPauseIcon.setContent(playing ? PAUSE_ICON : PLAY_ICON);
    }

    @Override
    public void onProgress(int elapsedSeconds, int totalSeconds) {
        Platform.runLater(() -> renderProgress(elapsedSeconds, totalSeconds));
    }

    @Override
    public void onLibraryChanged() {
        Platform.runLater(() -> {
            refreshArtistChoices();
            showMatches(searchField.getText());
        });
    }
}
