package com.discoballplayer.service;

import com.discoballplayer.model.Song;

/**
 * Receives playback and library events from a {@link PlayerService}.
 *
 * <p>Deliberately free of any JavaFX type: the backend must not depend on the UI toolkit.
 * Callbacks may arrive on a background timer thread, so a JavaFX implementation wraps every
 * handler body in {@code Platform.runLater}.</p>
 */
public interface PlaybackListener {

    void onSongChanged(Song song);

    void onPlaybackStateChanged(boolean playing);

    void onProgress(int elapsedSeconds, int totalSeconds);

    void onLibraryChanged();
}
