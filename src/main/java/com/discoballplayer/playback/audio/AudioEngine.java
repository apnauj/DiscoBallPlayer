package com.discoballplayer.playback.audio;

import java.util.function.IntConsumer;

import com.discoballplayer.model.Song;

/**
 * Plays a song and reports how far into it playback has got.
 *
 * <p>Exists so {@code Player} does not care whether audio is real. The simulated engine drives
 * the progress bar from a timer and always works; a real {@code javafx.media} engine can be
 * dropped in without the service layer noticing.</p>
 *
 * <p>Implementations tick on a background thread. A JavaFX listener therefore wraps its handler
 * body in {@code Platform.runLater}.</p>
 */
public interface AudioEngine {

    /**
     * Loads a song and resets elapsed time to zero, without starting playback.
     */
    void load(Song song);

    void play();

    void pause();

    /**
     * Stops playback and resets elapsed time to zero.
     */
    void stop();

    int elapsedSeconds();

    /**
     * Called once per elapsed second with the new elapsed time.
     */
    void setProgressCallback(IntConsumer callback);

    /**
     * Called when playback reaches the end of the loaded song, so {@code Player} can advance.
     */
    void setCompletionCallback(Runnable callback);

    /**
     * Releases the engine's threads or native resources. Called once, at shutdown.
     */
    void dispose();
}
