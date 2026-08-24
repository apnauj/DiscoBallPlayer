package com.discoballplayer.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.audio.AudioMetadata;
import com.discoballplayer.playback.audio.JavaFxAudioEngine;
import com.discoballplayer.playback.audio.SimulatedAudioEngine;

/**
 * Covers the fallback decision, not real decoding.
 *
 * <p>Building a {@code Media} needs a running JavaFX toolkit and an actual audio file, neither
 * of which belongs in a unit suite. What matters here is that a song with no playable file
 * still plays on the simulated clock — the case the whole seeded catalogue is in.</p>
 *
 * <p>Flat by convention: {@code @Nested} would make a method selector run zero tests.</p>
 */
class JavaFxAudioEngineTest {

    /** Never opens real media, so every song takes the fallback path. */
    private static final class AlwaysFallingBack extends JavaFxAudioEngine {
        AlwaysFallingBack(SimulatedAudioEngine fallback) {
            super(fallback);
        }

        @Override
        protected javafx.scene.media.MediaPlayer openMedia(Song song) {
            return null;
        }
    }

    private static final class ManualTicker implements SimulatedAudioEngine.Ticker {
        private Runnable tick;
        boolean running;

        @Override public void start(Runnable tick) { this.tick = tick; this.running = true; }
        @Override public void stop() { running = false; }

        void advance(int times) {
            for (int i = 0; i < times && running; i++) {
                tick.run();
            }
        }
    }

    private static Song song(String title, String audioPath) {
        Song song = new Song(title, List.of(new Artist("Tester")), null, 10, Genre.OTHER, 2020);
        song.setAudioPath(audioPath);
        return song;
    }

    @Test
    void aSongWithoutAudioStillReportsProgress() {
        ManualTicker ticker = new ManualTicker();
        JavaFxAudioEngine engine = new AlwaysFallingBack(new SimulatedAudioEngine(ticker));
        List<Integer> progress = new ArrayList<>();
        engine.setProgressCallback(progress::add);

        engine.load(song("No File", null));
        engine.play();
        ticker.advance(3);

        assertEquals(List.of(0, 1, 2, 3), progress,
                "the seeded catalogue has no audio paths; silence is fine, a dead bar is not");
    }

    @Test
    void aSongWithoutAudioStillCompletes() {
        ManualTicker ticker = new ManualTicker();
        JavaFxAudioEngine engine = new AlwaysFallingBack(new SimulatedAudioEngine(ticker));
        int[] completions = {0};
        engine.setCompletionCallback(() -> completions[0]++);

        engine.load(song("No File", null));
        engine.play();
        ticker.advance(20);

        assertEquals(1, completions[0], "chaining to the next song must work without audio");
    }

    @Test
    void pauseAndElapsedFollowTheFallbackClock() {
        ManualTicker ticker = new ManualTicker();
        JavaFxAudioEngine engine = new AlwaysFallingBack(new SimulatedAudioEngine(ticker));

        engine.load(song("No File", null));
        engine.play();
        ticker.advance(2);
        engine.pause();
        ticker.advance(5);

        assertEquals(2, engine.elapsedSeconds());
    }

    @Test
    void callbacksSetAfterConstructionStillReachTheFallback() {
        ManualTicker ticker = new ManualTicker();
        JavaFxAudioEngine engine = new AlwaysFallingBack(new SimulatedAudioEngine(ticker));
        engine.load(song("No File", null));
        List<Integer> late = new ArrayList<>();
        engine.setProgressCallback(late::add);

        engine.play();
        ticker.advance(1);

        assertEquals(List.of(1), late,
                "the delegate must forward through this class, not hold a stale callback");
    }

    // ---------- duration probing ----------

    @Test
    void durationOfABlankPathIsEmpty() {
        assertEquals(OptionalInt.empty(), AudioMetadata.durationSeconds(null));
        assertEquals(OptionalInt.empty(), AudioMetadata.durationSeconds("   "));
    }

    @Test
    void durationOfAMissingFileIsEmpty(@TempDir Path directory) {
        assertEquals(OptionalInt.empty(),
                AudioMetadata.durationSeconds(directory.resolve("nope.mp3").toString()));
    }

    @Test
    void durationOfAFileThatIsNotAudioIsEmpty(@TempDir Path directory) throws IOException {
        Path notAudio = directory.resolve("cover.png");
        Files.writeString(notAudio, "not an audio file");

        assertTrue(AudioMetadata.durationSeconds(notAudio.toString()).isEmpty(),
                "an unreadable file must leave the field for the user, not hang the dialog");
    }
}
