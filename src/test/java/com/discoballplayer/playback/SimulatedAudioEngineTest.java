package com.discoballplayer.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.audio.SimulatedAudioEngine;

/**
 * Time is advanced by hand through a manual {@code Ticker}. Sleeping for the length of a song
 * would make the suite take minutes and would still be flaky on a loaded machine.
 *
 * <p>Flat by convention: {@code @Nested} would make a method selector run zero tests.</p>
 */
class SimulatedAudioEngineTest {

    /** Runs the scheduled tick only when a test says so. */
    private static final class ManualTicker implements SimulatedAudioEngine.Ticker {
        private Runnable tick;
        boolean running;

        @Override
        public void start(Runnable tick) {
            this.tick = tick;
            this.running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        /** Fires the tick {@code times} times, stopping early if the engine cancelled it. */
        void advance(int times) {
            for (int i = 0; i < times && running; i++) {
                tick.run();
            }
        }
    }

    private static Song song(int durationSeconds) {
        return new Song("Track", List.of(new Artist("Tester")), null,
                durationSeconds, Genre.OTHER, 2020);
    }

    @Test
    void emitsOneTickPerSecond() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        List<Integer> progress = new ArrayList<>();
        engine.setProgressCallback(progress::add);

        engine.load(song(10));
        engine.play();
        ticker.advance(3);

        assertEquals(List.of(0, 1, 2, 3), progress, "load reports 0, then one value per second");
        assertEquals(3, engine.elapsedSeconds());
    }

    @Test
    void ticksFreezeWhilePaused() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        engine.load(song(10));
        engine.play();
        ticker.advance(2);

        engine.pause();
        ticker.advance(5);

        assertEquals(2, engine.elapsedSeconds(), "a paused engine must not advance");
        assertFalse(ticker.running);
    }

    @Test
    void resumingContinuesFromWhereItPaused() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        engine.load(song(10));
        engine.play();
        ticker.advance(2);
        engine.pause();

        engine.play();
        ticker.advance(2);

        assertEquals(4, engine.elapsedSeconds());
    }

    @Test
    void stopResetsElapsedTimeToZero() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        List<Integer> progress = new ArrayList<>();
        engine.load(song(10));
        engine.play();
        ticker.advance(4);
        engine.setProgressCallback(progress::add);

        engine.stop();

        assertEquals(0, engine.elapsedSeconds());
        assertEquals(List.of(0), progress, "stop tells the UI to reset the bar");
    }

    @Test
    void playbackStopsAtTheSongDuration() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        engine.load(song(3));
        engine.play();

        ticker.advance(10);

        assertEquals(3, engine.elapsedSeconds(), "elapsed must never exceed the duration");
        assertFalse(ticker.running, "the ticker is cancelled once the song ends");
    }

    @Test
    void completionFiresOnceWhenTheSongEnds() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        int[] completions = {0};
        engine.setCompletionCallback(() -> completions[0]++);
        engine.load(song(2));
        engine.play();

        ticker.advance(10);

        assertEquals(1, completions[0], "the end of a song is announced exactly once");
    }

    @Test
    void completionFiresAfterTheFinalProgressUpdate() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        List<String> order = new ArrayList<>();
        engine.setProgressCallback(elapsed -> order.add("progress:" + elapsed));
        engine.setCompletionCallback(() -> order.add("complete"));
        engine.load(song(2));
        engine.play();

        ticker.advance(5);

        assertEquals(List.of("progress:0", "progress:1", "progress:2", "complete"), order,
                "the bar must reach the end before the next song replaces it");
    }

    @Test
    void loadResetsElapsedTimeForTheNewSong() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        engine.load(song(10));
        engine.play();
        ticker.advance(5);

        engine.load(song(10));

        assertEquals(0, engine.elapsedSeconds());
        assertFalse(ticker.running, "loading a new song must not leave the old one playing");
    }

    @Test
    void playWithoutALoadedSongDoesNothing() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);

        engine.play();

        assertFalse(ticker.running);
        assertEquals(0, engine.elapsedSeconds());
    }

    @Test
    void repeatedPlayDoesNotRestartTheClock() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        engine.load(song(10));
        engine.play();
        ticker.advance(3);

        engine.play();

        assertEquals(3, engine.elapsedSeconds());
        assertTrue(ticker.running);
    }

    @Test
    void aCompletionCallbackThatLoadsTheNextSongIsNotFightingTheTicker() {
        ManualTicker ticker = new ManualTicker();
        SimulatedAudioEngine engine = new SimulatedAudioEngine(ticker);
        engine.setCompletionCallback(() -> {
            engine.load(song(5));
            engine.play();
        });
        engine.load(song(2));
        engine.play();

        ticker.advance(2);
        assertEquals(0, engine.elapsedSeconds(),
                "the next song starts from zero, not from the previous song's clock");

        ticker.advance(1);
        assertEquals(1, engine.elapsedSeconds(),
                "and its clock runs, so the restarted ticker was not left cancelled");
    }
}
