package com.discoballplayer.playback.audio;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import com.discoballplayer.model.Song;

/**
 * Drives the progress bar from a timer rather than from real audio.
 *
 * <p>This is the engine the application ships with. Real playback needs an audio file the user
 * may not have supplied, so the demo must not depend on one — a simulated clock advances at
 * exactly one second per second and stops at the song's duration.</p>
 *
 * <p>The ticking task is injectable so tests can advance time by hand instead of sleeping for
 * the length of a song.</p>
 */
public class SimulatedAudioEngine implements AudioEngine {

    /** Schedules the once-a-second tick. Swapped in tests for a manual clock. */
    public interface Ticker {
        /** Starts calling {@code tick} once a second; replaces any previous schedule. */
        void start(Runnable tick);

        void stop();

        default void dispose() {
            stop();
        }
    }

    private static final class RealTicker implements Ticker {
        private final ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "audio-progress-ticker");
                    thread.setDaemon(true);
                    return thread;
                });
        private ScheduledFuture<?> scheduled;

        @Override
        public void start(Runnable tick) {
            stop();
            scheduled = scheduler.scheduleAtFixedRate(tick, 1, 1, TimeUnit.SECONDS);
        }

        @Override
        public void stop() {
            if (scheduled != null) {
                scheduled.cancel(false);
                scheduled = null;
            }
        }

        @Override
        public void dispose() {
            stop();
            scheduler.shutdownNow();
        }
    }

    private final Ticker ticker;

    private Song song;
    private int elapsed;
    private boolean playing;
    private IntConsumer progressCallback = elapsedSeconds -> { };
    private Runnable completionCallback = () -> { };

    public SimulatedAudioEngine() {
        this(new RealTicker());
    }

    public SimulatedAudioEngine(Ticker ticker) {
        this.ticker = ticker;
    }

    @Override
    public void load(Song song) {
        this.song = song;
        this.elapsed = 0;
        this.playing = false;
        ticker.stop();
        progressCallback.accept(0);
    }

    @Override
    public void play() {
        if (song == null || playing) {
            return;
        }
        playing = true;
        ticker.start(this::tick);
    }

    @Override
    public void pause() {
        if (!playing) {
            return;
        }
        playing = false;
        ticker.stop();
    }

    @Override
    public void stop() {
        playing = false;
        elapsed = 0;
        ticker.stop();
        progressCallback.accept(0);
    }

    /**
     * Advances one second.
     *
     * <p>Completion is announced <em>after</em> the final progress update, so a listener that
     * paints the bar sees it reach the end before the next song replaces it. Playback is
     * stopped before the callback runs, so a callback that starts the next song is not fighting
     * a still-running ticker.</p>
     */
    private void tick() {
        if (!playing || song == null) {
            return;
        }
        int total = song.getDurationSeconds();
        elapsed = Math.min(elapsed + 1, total);
        progressCallback.accept(elapsed);

        if (elapsed >= total) {
            playing = false;
            ticker.stop();
            completionCallback.run();
        }
    }

    @Override
    public int elapsedSeconds() {
        return elapsed;
    }

    @Override
    public void setProgressCallback(IntConsumer callback) {
        this.progressCallback = callback == null ? elapsedSeconds -> { } : callback;
    }

    @Override
    public void setCompletionCallback(Runnable callback) {
        this.completionCallback = callback == null ? () -> { } : callback;
    }

    @Override
    public void dispose() {
        ticker.dispose();
    }
}
