package com.discoballplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.discoballplayer.exception.PersistenceException;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.repository.JsonLibraryRepository;
import com.discoballplayer.repository.LibraryRepository;
import com.discoballplayer.service.Player;

/**
 * Covers the startup policy in {@link Main}: what the application opens on, given what is or
 * is not on disk. The FXML wiring around it needs a running toolkit and is verified by
 * launching the application.
 *
 * <p>Flat by convention: {@code @Nested} would make a method selector run zero tests.</p>
 */
class MainLibraryLoadingTest {

    private static MusicLibrary oneSongLibrary() {
        MusicLibrary library = new MusicLibrary();
        library.addSong(new Song("Stored Song", List.of(new Artist("Someone")), null,
                200, Genre.ROCK, 2001));
        return library;
    }

    @Test
    void anEmptyStoreIsSeededSoTheAppNeverOpensBlank() {
        MusicLibrary loaded = Main.loadLibrary(new StubRepository(new MusicLibrary()));

        assertEquals(20, loaded.size(), "an empty window makes every mode look broken");
    }

    @Test
    void aStoredLibraryIsUsedAsIsAndNotSeededOver() {
        MusicLibrary stored = oneSongLibrary();

        MusicLibrary loaded = Main.loadLibrary(new StubRepository(stored));

        assertSame(stored, loaded, "seeding over the user's catalogue would destroy it");
        assertEquals(1, loaded.size());
    }

    @Test
    void aCorruptStoreFallsBackToSampleDataInsteadOfRefusingToOpen() {
        MusicLibrary loaded = Main.loadLibrary(new FailingRepository());

        assertEquals(20, loaded.size(),
                "a working window on sample data beats a stack trace and no way in");
    }

    @Test
    void aCorruptFileIsLeftOnDiskForTheUserToRecover(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("library.json");
        Files.writeString(file, "{ not json");
        LibraryRepository repository = new JsonLibraryRepository(file);

        Main.loadLibrary(repository);

        assertEquals("{ not json", Files.readString(file),
                "falling back must not overwrite what the user might still recover");
    }

    @Test
    void aRealEmptyFileRoundTripsThroughSeeding(@TempDir Path directory) {
        LibraryRepository repository = new JsonLibraryRepository(directory.resolve("library.json"));

        MusicLibrary loaded = Main.loadLibrary(repository);
        repository.save(loaded);

        assertEquals(20, repository.load().size(), "what the app opened on is what it saves");
        assertFalse(loaded.isEmpty());
    }

    @Test
    void savedEditsSurviveAReload(@TempDir Path directory) {
        LibraryRepository repository = new JsonLibraryRepository(directory.resolve("library.json"));
        MusicLibrary library = Main.loadLibrary(repository);
        library.addSong(new Song("Added By User", List.of(new Artist("Someone")), null,
                150, Genre.POP, 2024));

        repository.save(library);

        MusicLibrary reopened = Main.loadLibrary(repository);
        assertEquals(21, reopened.size());
        assertTrue(reopened.search("Added By User").size() == 1,
                "this is the whole point of C-02");
    }

    // ---------- doubles ----------

    private record StubRepository(MusicLibrary stored) implements LibraryRepository {
        @Override
        public MusicLibrary load() {
            return stored;
        }

        @Override
        public void save(MusicLibrary library) {
        }
    }

    private static final class FailingRepository implements LibraryRepository {
        @Override
        public MusicLibrary load() {
            throw new PersistenceException("simulated corruption", new IllegalStateException());
        }

        @Override
        public void save(MusicLibrary library) {
            throw new PersistenceException("simulated disk failure", new IllegalStateException());
        }
    }

    // ---------- shutdown ----------

    @Test
    void shutdownWritesTheLibraryToDisk(@TempDir Path directory) {
        LibraryRepository repository = new JsonLibraryRepository(directory.resolve("library.json"));
        MusicLibrary library = oneSongLibrary();

        Main.shutdown(repository, library, null);

        assertEquals(1, repository.load().size(), "closing the window must persist the catalogue");
    }

    @Test
    void shutdownReleasesTheAudioEngine(@TempDir Path directory) {
        LibraryRepository repository = new JsonLibraryRepository(directory.resolve("library.json"));
        RecordingEngine engine = new RecordingEngine();
        Player player = new Player(new MusicLibrary(), engine);

        Main.shutdown(repository, new MusicLibrary(), player);

        assertTrue(engine.disposed, "the ticker thread would otherwise outlive the window");
    }

    @Test
    void aFailedSaveStillReleasesTheEngineAndDoesNotThrow() {
        RecordingEngine engine = new RecordingEngine();
        Player player = new Player(new MusicLibrary(), engine);

        Main.shutdown(new FailingRepository(), new MusicLibrary(), player);

        assertTrue(engine.disposed, "an application that refuses to exit helps nobody");
    }

    @Test
    void shutdownBeforeStartupCompletedIsHarmless() {
        Main.shutdown(null, null, null);
    }

    private static final class RecordingEngine
            implements com.discoballplayer.playback.audio.AudioEngine {
        boolean disposed;
        double volume = 1;

        /** Added by Track B when AudioEngine gained volume; records rather than plays. */
        @Override public void setVolume(double level) { volume = Math.min(1, Math.max(0, level)); }
        @Override public double getVolume() { return volume; }

        @Override public void load(Song song) { }
        @Override public void play() { }
        @Override public void pause() { }
        @Override public void stop() { }
        @Override public int elapsedSeconds() { return 0; }
        @Override public void setProgressCallback(java.util.function.IntConsumer callback) { }
        @Override public void setCompletionCallback(Runnable callback) { }
        @Override public void dispose() { disposed = true; }
    }
}
