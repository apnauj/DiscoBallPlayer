package com.discoballplayer.playback.audio;

import java.io.File;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.media.Media;
import javafx.util.Duration;

/**
 * Reads what an audio file can tell us about itself.
 *
 * <p>Asking a user to type a song's duration is asking them to copy a number the file already
 * knows, and to get it wrong. The dialog should fill the field from the file and let the user
 * override it only when there is no file.</p>
 */
public final class AudioMetadata {

    private static final Logger LOG = Logger.getLogger(AudioMetadata.class.getName());

    /** Metadata is read asynchronously; beyond this the file is treated as unreadable. */
    private static final long TIMEOUT_MILLIS = 3000;

    private AudioMetadata() {
    }

    /**
     * Reads a file's duration in seconds.
     *
     * <p>{@link Media} parses its headers on a background thread, so this blocks briefly and
     * then gives up. A file picker that hangs on a malformed file is worse than one that
     * leaves the field for the user to fill.</p>
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

        try {
            Media media = new Media(file.toURI().toString());
            CountDownLatch ready = new CountDownLatch(1);
            media.getMetadata().addListener(
                    (javafx.collections.MapChangeListener<String, Object>) change -> ready.countDown());

            Duration immediate = media.getDuration();
            if (isUsable(immediate)) {
                return OptionalInt.of((int) Math.round(immediate.toSeconds()));
            }

            if (!ready.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                LOG.log(Level.INFO, "Timed out reading the duration of {0}.", path);
                return OptionalInt.empty();
            }
            Duration parsed = media.getDuration();
            return isUsable(parsed)
                    ? OptionalInt.of((int) Math.round(parsed.toSeconds()))
                    : OptionalInt.empty();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return OptionalInt.empty();
        } catch (RuntimeException unreadable) {
            LOG.log(Level.INFO, "Could not read the duration of " + path, unreadable);
            return OptionalInt.empty();
        }
    }

    private static boolean isUsable(Duration duration) {
        return duration != null && !duration.isUnknown() && duration.toSeconds() >= 1;
    }
}
