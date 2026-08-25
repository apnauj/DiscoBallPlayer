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
    /**
     * Sets the output level, {@code 0.0} silent to {@code 1.0} full.
     *
     * <p>Values outside that range are clamped rather than rejected: a slider that cannot go
     * out of range is the caller's job, and refusing here would turn a UI rounding error into
     * an exception mid-playback.</p>
     */
    void setVolume(double volume);

    /**
     * @return the current output level between {@code 0.0} and {@code 1.0}
     */
    double getVolume();

    void dispose();
}
