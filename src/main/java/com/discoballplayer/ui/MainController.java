package com.discoballplayer.ui;

import com.discoballplayer.model.Song;
import com.discoballplayer.service.DemoPlayerService;
import com.discoballplayer.service.PlaybackListener;
import com.discoballplayer.service.PlayerService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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

    /** The single line that ticket {@code C-01} swaps for the real {@code Player}. */
    private final PlayerService player = new DemoPlayerService();

    @FXML
    private Label statusLabel;

    /**
     * Called by {@link javafx.fxml.FXMLLoader} once the widget tree is built.
     */
    @FXML
    private void initialize() {
        player.addListener(this);
        statusLabel.setText("No song loaded");
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
        // The library table listens to this from B2-02.
    }
}
