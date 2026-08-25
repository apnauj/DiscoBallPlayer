package com.discoballplayer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.exception.SongNotFoundException;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.PlaybackMode;
import com.discoballplayer.playback.audio.AudioEngine;

/**
 * Player is exercised against a stub {@link PlaybackMode}, not the real modes.
 *
 * <p>Its job is delegation and event dispatch; using a stub isolates that from whether shuffle
 * or FIFO happens to be correct, and lets a test force cases a real mode makes awkward to
 * reach, such as a mode that throws on {@code next()}.</p>
 *
 * <p>Flat by convention: {@code @Nested} would make a method selector run zero tests.</p>
 */
class PlayerTest {

    // ---------- doubles ----------

    /** Records what it was told, and can be steered into failure states. */
    private static final class StubMode implements PlaybackMode {
        int loadCount;
        MusicLibrary loadedFrom;
        Song currentSong;
        boolean nextThrows;
        boolean previousUnsupported;
        boolean exhausted;
        boolean jumpSupported = true;
        final List<Song> added = new ArrayList<>();

        @Override
        public void load(MusicLibrary library) {
            loadCount++;
            loadedFrom = library;
            currentSong = null;
            added.clear();
        }

        /** Added by Track B when PlaybackMode gained add(); records rather than reloads. */
        @Override
        public void add(Song song) {
            added.add(song);
        }

        @Override
        public Song next() {
            if (nextThrows) {
                throw new EmptyStructureException("stub exhausted");
            }
            currentSong = song("next-" + loadCount);
            return currentSong;
        }

        @Override
        public Song previous() {
            if (previousUnsupported) {
                throw new UnsupportedOperationException("stub is FIFO");
            }
            currentSong = song("previous-" + loadCount);
            return currentSong;
        }

        @Override
        public boolean hasNext() {
            return !exhausted;
        }

        @Override
        public boolean hasPrevious() {
            return !previousUnsupported;
        }

        @Override
        public Song current() {
            return currentSong;
        }

        @Override
        public Song jumpTo(Song target) {
            if (!jumpSupported) {
                throw new UnsupportedOperationException("stub cannot jump");
            }
            currentSong = target;
            return target;
        }

        @Override
        public boolean canJumpTo() {
            return jumpSupported;
        }

        @Override
        public String displayName() {
            return "Stub";
        }
    }

    /** Records every callback so a test can assert on order and count, not just occurrence. */
    private static final class RecordingListener implements PlaybackListener {
        final List<String> events = new ArrayList<>();

        @Override
        public void onSongChanged(Song song) {
            events.add("song:" + song.getTitle());
        }

        @Override
        public void onPlaybackStateChanged(boolean playing) {
            events.add("playing:" + playing);
        }

        @Override
        public void onProgress(int elapsedSeconds, int totalSeconds) {
            events.add("progress:" + elapsedSeconds + "/" + totalSeconds);
        }

        @Override
        public void onLibraryChanged() {
            events.add("library");
        }
    }


    /** Records engine calls and lets a test fire the engine's callbacks by hand. */
    private static final class FakeAudio implements AudioEngine {

        double volume = 1;

        /** Added by Track B when AudioEngine gained volume; records rather than plays. */
        @Override
        public void setVolume(double level) {
            volume = Math.min(1, Math.max(0, level));
        }

        @Override
        public double getVolume() {
            return volume;
        }
        final List<String> calls = new ArrayList<>();
        java.util.function.IntConsumer progress = elapsed -> { };
        Runnable completion = () -> { };
        Song loaded;

        @Override public void load(Song song) { loaded = song; calls.add("load:" + song.getTitle()); }
        @Override public void play() { calls.add("play"); }
        @Override public void pause() { calls.add("pause"); }
        @Override public void stop() { calls.add("stop"); }
        @Override public int elapsedSeconds() { return 0; }
        @Override public void setProgressCallback(java.util.function.IntConsumer c) { progress = c; }
        @Override public void setCompletionCallback(Runnable c) { completion = c; }
        @Override public void dispose() { calls.add("dispose"); }
    }

    private final FakeAudio audio = new FakeAudio();

    private Player newPlayer(MusicLibrary library) {
        return new Player(library, audio);
    }

    // ---------- fixtures ----------

    private static Song song(String title) {
        return new Song(title, List.of(new Artist("Tester")), null, 180, Genre.OTHER, 2020);
    }

    private static MusicLibrary libraryOf(String... titles) {
        MusicLibrary library = new MusicLibrary();
        for (String title : titles) {
            library.addSong(song(title));
        }
        return library;
    }

    // ---------- mode delegation (A3-01) ----------

    @Test
    void setModeReloadsFromLibrary() {
        MusicLibrary library = libraryOf("A", "B");
        Player player = newPlayer(library);
        StubMode mode = new StubMode();

        player.setMode(mode);

        assertEquals(1, mode.loadCount);
        assertSame(library, mode.loadedFrom);
        assertSame(mode, player.getMode());
    }

    @Test
    void navigationDelegatesToTheMode() {
        Player player = newPlayer(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);

        assertEquals("next-1", player.next().getTitle());
        assertEquals("previous-1", player.previous().getTitle());
        assertEquals("previous-1", player.current().getTitle());
    }

    @Test
    void hasNextAndHasPreviousDelegateToTheMode() {
        Player player = newPlayer(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);

        assertTrue(player.hasNext());
        mode.exhausted = true;
        assertFalse(player.hasNext());

        mode.previousUnsupported = true;
        assertFalse(player.hasPrevious(), "the UI reads this to disable its Previous button");
    }

    @Test
    void navigationWithoutAModeFailsLoudly() {
        Player player = newPlayer(libraryOf("A"));

        assertThrows(IllegalStateException.class, player::next);
        assertThrows(IllegalStateException.class, player::previous);
    }

    @Test
    void queriesWithoutAModeAreSafe() {
        Player player = newPlayer(libraryOf("A"));

        assertNull(player.current());
        assertFalse(player.hasNext());
        assertFalse(player.hasPrevious());
        assertNull(player.getMode());
    }

    @Test
    void modeExceptionsReachTheCallerUnchanged() {
        Player player = newPlayer(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);
        mode.nextThrows = true;
        mode.previousUnsupported = true;

        assertThrows(EmptyStructureException.class, player::next);
        assertThrows(UnsupportedOperationException.class, player::previous,
                "arrival mode's refusal must surface, not be swallowed");
    }

    @Test
    void setModeRejectsNull() {
        assertThrows(NullPointerException.class, () -> newPlayer(libraryOf("A")).setMode(null));
    }

    // ---------- library CRUD (A3-02) ----------

    @Test
    void addSongPutsItInTheLibrary() {
        MusicLibrary library = libraryOf("A");
        Player player = newPlayer(library);

        player.addSong(song("B"));

        assertEquals(2, player.listAll().size());
    }

    @Test
    void removeSongRejectsAnUnknownSong() {
        Player player = newPlayer(libraryOf("A"));

        assertThrows(SongNotFoundException.class, () -> player.removeSong(song("Ghost")));
    }

    @Test
    void removeSongTakesItOutOfTheLibrary() {
        MusicLibrary library = libraryOf("A", "B");
        Player player = newPlayer(library);
        Song target = library.getAllSongs().get(0);

        player.removeSong(target);

        assertEquals(1, player.listAll().size());
    }

    @Test
    void updateSongRejectsAnUnknownSong() {
        Player player = newPlayer(libraryOf("A"));

        assertThrows(SongNotFoundException.class, () -> player.updateSong(song("Ghost")));
    }

    @Test
    void searchDelegatesToTheLibrary() {
        Player player = newPlayer(libraryOf("Bailando", "Tania"));

        assertEquals(1, player.search("bail").size());
        assertEquals(2, player.search("").size(), "a blank query returns everything");
    }

    @Test
    void rateRejectsOutOfRangeValues() {
        MusicLibrary library = libraryOf("A");
        Player player = newPlayer(library);
        Song target = library.getAllSongs().get(0);

        assertThrows(IllegalArgumentException.class, () -> player.rate(target, -1));
        assertThrows(IllegalArgumentException.class, () -> player.rate(target, 101));
    }

    @Test
    void rateAcceptsTheInclusiveBounds() {
        MusicLibrary library = libraryOf("A");
        Player player = newPlayer(library);
        Song target = library.getAllSongs().get(0);

        player.rate(target, 0);
        assertEquals(0, target.getRating());
        player.rate(target, 100);
        assertEquals(100, target.getRating());
    }

    @Test
    void rateRejectsASongOutsideTheLibrary() {
        Player player = newPlayer(libraryOf("A"));

        assertThrows(SongNotFoundException.class, () -> player.rate(song("Ghost"), 50));
    }

    // ---------- listeners (A3-03) ----------

    @Test
    void notifiesListenersOnSongChange() {
        Player player = newPlayer(libraryOf("A"));
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);
        player.setMode(new StubMode());

        player.next();

        assertTrue(listener.events.contains("song:next-1"));
    }

    @Test
    void notifiesListenersOnLibraryChange() {
        MusicLibrary library = libraryOf("A");
        Player player = newPlayer(library);
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        player.addSong(song("B"));
        player.rate(library.getAllSongs().get(0), 50);
        player.removeSong(library.getAllSongs().get(0));

        assertEquals(List.of("library", "library", "library"), listener.events);
    }

    @Test
    void notifiesListenersOnPlaybackStateChange() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        player.play();
        player.pause();

        assertEquals(List.of("song:next-1", "playing:true", "playing:false"), listener.events);
    }

    @Test
    void repeatedPlayDoesNotRefireTheStateEvent() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        player.play();
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        player.play();
        player.play();

        assertTrue(listener.events.isEmpty(), "the state did not change, so nothing to announce");
    }

    @Test
    void playStartsTheFirstSongWhenNothingIsPlaying() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());

        player.play();

        assertEquals("next-1", player.current().getTitle());
        assertTrue(player.isPlaying());
    }

    @Test
    void playOnAnExhaustedModeDoesNothingRatherThanClaimingToPlay() {
        Player player = newPlayer(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);
        mode.exhausted = true;
        audio.calls.clear();

        player.play();

        assertNull(player.current());
        assertFalse(player.isPlaying(),
                "reporting playback with nothing loaded would show a Pause button over silence");
        assertTrue(audio.calls.isEmpty());
    }

    @Test
    void aThrowingListenerDoesNotStopTheOthers() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        player.addListener(new PlaybackListener() {
            @Override public void onSongChanged(Song song) { throw new IllegalStateException("boom"); }
            @Override public void onPlaybackStateChanged(boolean playing) { }
            @Override public void onProgress(int elapsed, int total) { }
            @Override public void onLibraryChanged() { }
        });
        RecordingListener survivor = new RecordingListener();
        player.addListener(survivor);

        player.next();

        assertEquals(List.of("song:next-1"), survivor.events,
                "one broken UI component must not leave the rest half-updated");
    }

    @Test
    void removedListenersStopReceivingEvents() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);
        player.removeListener(listener);

        player.next();

        assertTrue(listener.events.isEmpty());
    }

    @Test
    void addingASongDoesNotReloadTheActiveMode() {
        MusicLibrary library = libraryOf("A");
        Player player = newPlayer(library);
        StubMode mode = new StubMode();
        player.setMode(mode);

        player.addSong(song("B"));

        assertEquals(1, mode.loadCount,
                "a mode owns a snapshot; reloading would reset shuffle order and refill a drained queue");
    }

    // ---------- audio engine wiring (A5-04) ----------

    @Test
    void navigationLoadsTheSongIntoTheEngine() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());

        player.next();

        assertTrue(audio.calls.contains("load:next-1"));
        assertEquals("next-1", audio.loaded.getTitle());
    }

    @Test
    void navigatingWhilePausedDoesNotStartTheEngine() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());

        player.next();

        assertFalse(audio.calls.contains("play"), "navigating while paused must stay paused");
    }

    @Test
    void navigatingWhilePlayingKeepsPlaying() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        player.play();
        audio.calls.clear();

        player.next();

        assertEquals(List.of("load:next-1", "play"), audio.calls);
    }

    @Test
    void pauseStopsTheEngine() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        player.play();
        audio.calls.clear();

        player.pause();

        assertEquals(List.of("pause"), audio.calls);
    }

    @Test
    void switchingModesStopsTheEngine() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        player.play();
        audio.calls.clear();

        player.setMode(new StubMode());

        assertTrue(audio.calls.contains("stop"));
        assertFalse(player.isPlaying());
    }

    @Test
    void engineProgressIsRepublishedWithTheSongDuration() {
        Player player = newPlayer(libraryOf("A"));
        player.setMode(new StubMode());
        player.next();
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        audio.progress.accept(42);

        assertEquals(List.of("progress:42/180"), listener.events);
    }

    @Test
    void progressBeforeAnySongIsIgnored() {
        Player player = newPlayer(libraryOf("A"));
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        audio.progress.accept(5);

        assertTrue(listener.events.isEmpty(), "no current song means no consistent total to send");
    }

    @Test
    void advancesToNextSongOnCompletion() {
        Player player = newPlayer(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);
        player.play();
        audio.calls.clear();

        audio.completion.run();

        assertEquals(List.of("load:next-1", "play", "play"), audio.calls,
                "the next song is loaded and started");
    }

    @Test
    void completionOnTheLastSongStopsPlaybackInsteadOfThrowing() {
        Player player = newPlayer(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);
        player.play();
        mode.exhausted = true;
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        audio.completion.run();

        assertFalse(player.isPlaying());
        assertEquals(List.of("playing:false"), listener.events);
    }

    @Test
    void disposeReleasesTheEngine() {
        Player player = newPlayer(libraryOf("A"));

        player.dispose();

        assertTrue(audio.calls.contains("dispose"));
    }

    // ---------- click to play ----------

    @Test
    void playSongRepositionsTheModeAndStartsPlaying() {
        MusicLibrary library = libraryOf("A", "B");
        Player player = newPlayer(library);
        player.setMode(new StubMode());
        Song target = library.getAllSongs().get(1);
        audio.calls.clear();

        Song played = player.playSong(target);

        assertSame(target, played);
        assertSame(target, player.current());
        assertTrue(player.isPlaying(), "the user picked that song; loading it silently reads as a dead click");
        assertEquals(List.of("load:B", "play"), audio.calls);
    }

    @Test
    void playSongAnnouncesTheSongChange() {
        MusicLibrary library = libraryOf("A", "B");
        Player player = newPlayer(library);
        player.setMode(new StubMode());
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        player.playSong(library.getAllSongs().get(0));

        assertTrue(listener.events.contains("song:A"));
    }

    @Test
    void canPlaySongFollowsTheMode() {
        Player player = newPlayer(libraryOf("A"));
        StubMode mode = new StubMode();

        assertFalse(player.canPlaySong(), "no mode selected yet");
        player.setMode(mode);
        assertTrue(player.canPlaySong());

        mode.jumpSupported = false;
        assertFalse(player.canPlaySong(), "the UI reads this to disable click-to-play");
    }

    @Test
    void playSongOnAModeThatCannotJumpSurfacesTheRefusal() {
        MusicLibrary library = libraryOf("A");
        Player player = newPlayer(library);
        StubMode mode = new StubMode();
        player.setMode(mode);
        mode.jumpSupported = false;

        assertThrows(UnsupportedOperationException.class,
                () -> player.playSong(library.getAllSongs().get(0)),
                "arrival order refuses; the UI must see that, not a swallowed no-op");
    }

    @Test
    void playSongWithoutAModeFailsLoudly() {
        MusicLibrary library = libraryOf("A");
        Player player = newPlayer(library);

        assertThrows(IllegalStateException.class,
                () -> player.playSong(library.getAllSongs().get(0)));
    }
}
