package com.discoballplayer.playback.audio;

import java.io.File;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * Reads what an audio file can tell us about itself.
 *
 * <p>Asking a user to type a song's duration is asking them to copy a number the file already
 * knows, and to get it wrong.</p>
 */
public final class AudioMetadata {

    private static final Logger LOG = Logger.getLogger(AudioMetadata.class.getName());

    /** Headers are parsed asynchronously; beyond this the file is treated as unreadable. */
    private static final long TIMEOUT_MILLIS = 5000;

    private AudioMetadata() {
    }

    /**
     * Reads a file's duration in seconds.
     *
     * <p>The duration is taken from a {@link MediaPlayer} reaching {@code READY}, not from the
     * {@code Media} metadata map. That map holds tags — artist, title, album art — and a file
     * without tags never populates it, while {@link Media#getDuration()} stays
     * {@code UNKNOWN} until a player has prepared the stream. Waiting on the map therefore
     * timed out on every untagged file, which is most of them, and answered empty for a file
     * whose length was sitting right there.</p>
     *
     * <p>Blocks until the player is ready, fails, or the timeout elapses, so callers on the FX
     * thread must move it off. The player is always disposed: each one holds a native decoder,
     * and leaking one per file picked would be a slow leak in a long session.</p>
     *
     * @return the duration, or empty when the path is blank, unreadable, unparseable, or slow
     */
    public static OptionalInt durationSeconds(String path) {
        if (path == null || path.isBlank()) {
            return OptionalInt.empty();
        }
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) {
            return OptionalInt.empty();
        }

        MediaPlayer player = null;
        try {
            Media media = new Media(file.toURI().toString());

            Duration immediate = media.getDuration();
            if (isUsable(immediate)) {
                return OptionalInt.of(seconds(immediate));
            }

            player = new MediaPlayer(media);
            CountDownLatch settled = new CountDownLatch(1);
            player.setOnReady(settled::countDown);
            // Both failure paths must release the latch, or an undecodable file costs the
            // caller the whole timeout instead of failing as soon as the decoder gives up.
            player.setOnError(settled::countDown);
            media.setOnError(settled::countDown);

            if (!settled.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                LOG.log(Level.INFO, "Timed out reading the duration of {0}.", path);
                return OptionalInt.empty();
            }

            Duration ready = media.getDuration();
            return isUsable(ready) ? OptionalInt.of(seconds(ready)) : OptionalInt.empty();

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return OptionalInt.empty();
        } catch (RuntimeException unreadable) {
            LOG.log(Level.INFO, "Could not read the duration of " + path, unreadable);
            return OptionalInt.empty();
        } finally {
            if (player != null) {
                player.dispose();
            }
        }
    }

    private static int seconds(Duration duration) {
        return Math.max(1, (int) Math.round(duration.toSeconds()));
    }

    private static boolean isUsable(Duration duration) {
        return duration != null && !duration.isUnknown() && duration.toSeconds() >= 0.5;
    }
}
