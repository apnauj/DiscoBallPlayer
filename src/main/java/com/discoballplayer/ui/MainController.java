package com.discoballplayer.ui;

import java.util.List;

import com.discoballplayer.model.Album;
import com.discoballplayer.model.Song;
import com.discoballplayer.service.DemoPlayerService;
import com.discoballplayer.service.PlaybackListener;
import com.discoballplayer.service.PlayerService;
import com.discoballplayer.util.TimeFormatter;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

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

    /** The single line that ticket {@code C-01} swaps for the real {@code Player}. */
    private final PlayerService player = new DemoPlayerService();

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

    /**
     * Called by {@link javafx.fxml.FXMLLoader} once the widget tree is built.
     */
    @FXML
    private void initialize() {
        configureColumns();
        searchField.textProperty().addListener((observable, previous, query) -> showMatches(query));
        player.addListener(this);
        showMatches(searchField.getText());
        statusLabel.setText("No song loaded");
    }

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

    @Override
    public void onSongChanged(Song song) {
        // Metadata and cover rendering arrive in B3-02.
    }

    @Override
    public void onPlaybackStateChanged(boolean playing) {
        // The Play/Pause label follows this event from B4-02.
    }

    @Override
    public void onProgress(int elapsedSeconds, int totalSeconds) {
        // The progress bar and time labels follow this event from B3-04.
    }

    @Override
    public void onLibraryChanged() {
        Platform.runLater(() -> showMatches(searchField.getText()));
    }
}
