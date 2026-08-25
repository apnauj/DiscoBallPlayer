package com.discoballplayer.playback.audio;

import java.io.File;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.discoballplayer.model.Song;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * Plays real MP3/WAV files, falling back to the simulated clock when it cannot.
 *
 * <p>A song may have no audio path at all — the seeded catalogue has none, and a user can add
 * metadata without a file. Those songs must still play: the progress bar advances on the
 * simulated clock and the transport controls behave identically. Silence is acceptable;
 * a dead Play button is not.</p>
 *
 * <p>Every callback is forwarded through this class rather than handed to the delegate
 * directly, so {@code Player} never learns which engine is actually running.</p>
 */
public class JavaFxAudioEngine implements AudioEngine {

    private static final Logger LOG = Logger.getLogger(JavaFxAudioEngine.class.getName());

    private final SimulatedAudioEngine fallback;

    private MediaPlayer mediaPlayer;
    private Song song;
    private double volume = 1;
    private IntConsumer progressCallback = elapsed -> { };
    private Runnable completionCallback = () -> { };

    public JavaFxAudioEngine() {
        this(new SimulatedAudioEngine());
    }

    public JavaFxAudioEngine(SimulatedAudioEngine fallback) {
        this.fallback = fallback;
        fallback.setProgressCallback(elapsed -> progressCallback.accept(elapsed));
        fallback.setCompletionCallback(() -> completionCallback.run());
    }

    /** True while a real file is loaded; false when the simulated clock is standing in. */
    private boolean playingRealAudio() {
        return mediaPlayer != null;
    }

    @Override
    public void load(Song song) {
        releaseMediaPlayer();
        this.song = song;
        fallback.load(song);

        MediaPlayer opened = openMedia(song);
        if (opened == null) {
            return;
        }
        // A new player starts at full volume; carry the user's setting across the song change.
        opened.setVolume(volume);
        mediaPlayer = opened;
        mediaPlayer.currentTimeProperty().addListener((observable, before, now) ->
                progressCallback.accept((int) now.toSeconds()));
        mediaPlayer.setOnEndOfMedia(() -> completionCallback.run());
        progressCallback.accept(0);
    }

    /**
     * Opens the song's audio file, or returns null when there is nothing playable.
     *
     * <p>Overridable so the fallback logic can be tested without a JavaFX toolkit — building a
     * {@link Media} requires one, and the decision this method drives is the part worth
     * testing.</p>
     */
    protected MediaPlayer openMedia(Song song) {
        String path = song == null ? null : song.getAudioPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) {
            LOG.log(Level.INFO, "No readable audio at {0}; using the simulated clock.", path);
            return null;
        }
        try {
            return new MediaPlayer(new Media(file.toURI().toString()));
        } catch (RuntimeException unplayable) {
            LOG.log(Level.WARNING,
                    "Could not open " + path + "; using the simulated clock.", unplayable);
            return null;
        }
    }

    /**
     * Applies the level to whichever engine is actually running, and remembers it.
     *
     * <p>Both are kept in step: the real player is what the user hears now, and the fallback
     * holds the level for the next song that turns out to have no readable file.</p>
     */
    @Override
    public void setVolume(double volume) {
        this.volume = SimulatedAudioEngine.clampVolume(volume);
        fallback.setVolume(this.volume);
        if (playingRealAudio()) {
            mediaPlayer.setVolume(this.volume);
        }
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public void play() {
        if (playingRealAudio()) {
            mediaPlayer.play();
        } else {
            fallback.play();
        }
    }

    @Override
    public void pause() {
        if (playingRealAudio()) {
            mediaPlayer.pause();
        } else {
            fallback.pause();
        }
    }

    @Override
    public void stop() {
        if (playingRealAudio()) {
            mediaPlayer.stop();
            progressCallback.accept(0);
        } else {
            fallback.stop();
        }
    }

    @Override
    public int elapsedSeconds() {
        return playingRealAudio()
                ? (int) mediaPlayer.getCurrentTime().toSeconds()
                : fallback.elapsedSeconds();
    }

    @Override
    public void setProgressCallback(IntConsumer callback) {
        this.progressCallback = callback == null ? elapsed -> { } : callback;
    }

    @Override
    public void setCompletionCallback(Runnable callback) {
        this.completionCallback = callback == null ? () -> { } : callback;
    }

    @Override
    public void dispose() {
        releaseMediaPlayer();
        fallback.dispose();
    }

    /**
     * Native media resources are not reclaimed by garbage collection alone, so every player is
     * disposed before it is replaced. Skipping this leaks a decoder per song played.
     */
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    /**
     * @return the duration the file reports, or 0 when it cannot be read
     */
    static int durationOf(MediaPlayer player) {
        Duration total = player.getTotalDuration();
        return total == null || total.isUnknown() ? 0 : (int) total.toSeconds();
    }
}
