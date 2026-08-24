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

        @Override
        public void load(MusicLibrary library) {
            loadCount++;
            loadedFrom = library;
            currentSong = null;
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
        Player player = new Player(library);
        StubMode mode = new StubMode();

        player.setMode(mode);

        assertEquals(1, mode.loadCount);
        assertSame(library, mode.loadedFrom);
        assertSame(mode, player.getMode());
    }

    @Test
    void navigationDelegatesToTheMode() {
        Player player = new Player(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);

        assertEquals("next-1", player.next().getTitle());
        assertEquals("previous-1", player.previous().getTitle());
        assertEquals("previous-1", player.current().getTitle());
    }

    @Test
    void hasNextAndHasPreviousDelegateToTheMode() {
        Player player = new Player(libraryOf("A"));
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
        Player player = new Player(libraryOf("A"));

        assertThrows(IllegalStateException.class, player::next);
        assertThrows(IllegalStateException.class, player::previous);
    }

    @Test
    void queriesWithoutAModeAreSafe() {
        Player player = new Player(libraryOf("A"));

        assertNull(player.current());
        assertFalse(player.hasNext());
        assertFalse(player.hasPrevious());
        assertNull(player.getMode());
    }

    @Test
    void modeExceptionsReachTheCallerUnchanged() {
        Player player = new Player(libraryOf("A"));
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
        assertThrows(NullPointerException.class, () -> new Player(libraryOf("A")).setMode(null));
    }

    // ---------- library CRUD (A3-02) ----------

    @Test
    void addSongPutsItInTheLibrary() {
        MusicLibrary library = libraryOf("A");
        Player player = new Player(library);

        player.addSong(song("B"));

        assertEquals(2, player.listAll().size());
    }

    @Test
    void removeSongRejectsAnUnknownSong() {
        Player player = new Player(libraryOf("A"));

        assertThrows(SongNotFoundException.class, () -> player.removeSong(song("Ghost")));
    }

    @Test
    void removeSongTakesItOutOfTheLibrary() {
        MusicLibrary library = libraryOf("A", "B");
        Player player = new Player(library);
        Song target = library.getAllSongs().get(0);

        player.removeSong(target);

        assertEquals(1, player.listAll().size());
    }

    @Test
    void updateSongRejectsAnUnknownSong() {
        Player player = new Player(libraryOf("A"));

        assertThrows(SongNotFoundException.class, () -> player.updateSong(song("Ghost")));
    }

    @Test
    void searchDelegatesToTheLibrary() {
        Player player = new Player(libraryOf("Bailando", "Tania"));

        assertEquals(1, player.search("bail").size());
        assertEquals(2, player.search("").size(), "a blank query returns everything");
    }

    @Test
    void rateRejectsOutOfRangeValues() {
        MusicLibrary library = libraryOf("A");
        Player player = new Player(library);
        Song target = library.getAllSongs().get(0);

        assertThrows(IllegalArgumentException.class, () -> player.rate(target, -1));
        assertThrows(IllegalArgumentException.class, () -> player.rate(target, 101));
    }

    @Test
    void rateAcceptsTheInclusiveBounds() {
        MusicLibrary library = libraryOf("A");
        Player player = new Player(library);
        Song target = library.getAllSongs().get(0);

        player.rate(target, 0);
        assertEquals(0, target.getRating());
        player.rate(target, 100);
        assertEquals(100, target.getRating());
    }

    @Test
    void rateRejectsASongOutsideTheLibrary() {
        Player player = new Player(libraryOf("A"));

        assertThrows(SongNotFoundException.class, () -> player.rate(song("Ghost"), 50));
    }

    // ---------- listeners (A3-03) ----------

    @Test
    void notifiesListenersOnSongChange() {
        Player player = new Player(libraryOf("A"));
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);
        player.setMode(new StubMode());

        player.next();

        assertTrue(listener.events.contains("song:next-1"));
    }

    @Test
    void notifiesListenersOnLibraryChange() {
        MusicLibrary library = libraryOf("A");
        Player player = new Player(library);
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        player.addSong(song("B"));
        player.rate(library.getAllSongs().get(0), 50);
        player.removeSong(library.getAllSongs().get(0));

        assertEquals(List.of("library", "library", "library"), listener.events);
    }

    @Test
    void notifiesListenersOnPlaybackStateChange() {
        Player player = new Player(libraryOf("A"));
        player.setMode(new StubMode());
        RecordingListener listener = new RecordingListener();
        player.addListener(listener);

        player.play();
        player.pause();

        assertEquals(List.of("song:next-1", "playing:true", "playing:false"), listener.events);
    }

    @Test
    void repeatedPlayDoesNotRefireTheStateEvent() {
        Player player = new Player(libraryOf("A"));
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
        Player player = new Player(libraryOf("A"));
        player.setMode(new StubMode());

        player.play();

        assertEquals("next-1", player.current().getTitle());
        assertTrue(player.isPlaying());
    }

    @Test
    void playOnAnExhaustedModeDoesNotThrow() {
        Player player = new Player(libraryOf("A"));
        StubMode mode = new StubMode();
        player.setMode(mode);
        mode.exhausted = true;

        player.play();

        assertTrue(player.isPlaying());
        assertNull(player.current());
    }

    @Test
    void aThrowingListenerDoesNotStopTheOthers() {
        Player player = new Player(libraryOf("A"));
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
        Player player = new Player(libraryOf("A"));
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
        Player player = new Player(library);
        StubMode mode = new StubMode();
        player.setMode(mode);

        player.addSong(song("B"));

        assertEquals(1, mode.loadCount,
                "a mode owns a snapshot; reloading would reset shuffle order and refill a drained queue");
    }
}
