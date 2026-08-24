package com.discoballplayer.playback;

import static com.discoballplayer.playback.PlaybackFixtures.libraryOf;
import static com.discoballplayer.playback.PlaybackFixtures.playNext;
import static com.discoballplayer.playback.PlaybackFixtures.titlesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.model.MusicLibrary;

/** Flat by convention: {@code @Nested} would make a method selector run zero tests. */
class ArrivalModeTest {

    private static final String[] TITLES = {"First", "Second", "Third", "Fourth"};

    private static ArrivalMode loaded(MusicLibrary library) {
        ArrivalMode mode = new ArrivalMode();
        mode.load(library);
        return mode;
    }

    @Test
    void playsInLibraryInsertionOrder() {
        ArrivalMode mode = loaded(libraryOf(TITLES));

        assertEquals(List.of("First", "Second", "Third", "Fourth"), playNext(mode, 4));
    }

    @Test
    void previousAlwaysUnsupported() {
        ArrivalMode mode = loaded(libraryOf(TITLES));

        assertThrows(UnsupportedOperationException.class, mode::previous);
        mode.next();
        assertThrows(UnsupportedOperationException.class, mode::previous);
        playNext(mode, 3);
        assertThrows(UnsupportedOperationException.class, mode::previous);
    }

    @Test
    void hasPreviousIsFalseAtEveryStep() {
        ArrivalMode mode = loaded(libraryOf(TITLES));

        assertFalse(mode.hasPrevious());
        for (int step = 0; step < TITLES.length; step++) {
            mode.next();
            assertFalse(mode.hasPrevious(), "the UI disables Previous from this value");
        }
    }

    @Test
    void aPlayedSongLeavesTheQueuePermanently() {
        ArrivalMode mode = loaded(libraryOf(TITLES));

        assertEquals(4, mode.remaining());
        mode.next();
        assertEquals(3, mode.remaining());
        playNext(mode, 3);
        assertEquals(0, mode.remaining());
    }

    @Test
    void queueDrainsToEmptyAndThenReportsNoNext() {
        ArrivalMode mode = loaded(libraryOf(TITLES));

        playNext(mode, 4);

        assertFalse(mode.hasNext());
    }

    @Test
    void exhaustedQueueThrowsOnNext() {
        ArrivalMode mode = loaded(libraryOf("Only"));

        mode.next();

        assertThrows(EmptyStructureException.class, mode::next);
    }

    @Test
    void drainingTheQueueLeavesTheLibraryIntact() {
        MusicLibrary library = libraryOf(TITLES);
        ArrivalMode mode = loaded(library);

        playNext(mode, 4);

        assertEquals(4, library.size(), "consuming the queue must not consume the catalogue");
        assertEquals(List.of("First", "Second", "Third", "Fourth"),
                titlesOf(library.getAllSongs()));
    }

    @Test
    void reloadingRefillsTheQueueFromTheLibrary() {
        MusicLibrary library = libraryOf(TITLES);
        ArrivalMode mode = loaded(library);
        playNext(mode, 4);

        mode.load(library);

        assertTrue(mode.hasNext());
        assertNull(mode.current(), "load must discard the previous position");
        assertEquals(List.of("First", "Second", "Third", "Fourth"), playNext(mode, 4));
    }

    @Test
    void currentIsNullBeforeTheFirstNext() {
        assertNull(loaded(libraryOf(TITLES)).current());
    }

    @Test
    void currentFollowsTheDequeuedSong() {
        ArrivalMode mode = loaded(libraryOf(TITLES));

        mode.next();
        assertEquals("First", mode.current().getTitle());
        mode.next();
        assertEquals("Second", mode.current().getTitle());
    }

    @Test
    void emptyLibraryReportsNoNextAndThrows() {
        ArrivalMode mode = loaded(new MusicLibrary());

        assertFalse(mode.hasNext());
        assertFalse(mode.hasPrevious());
        assertThrows(EmptyStructureException.class, mode::next);
    }

    @Test
    void songsAddedAfterLoadDoNotAppearUntilReload() {
        MusicLibrary library = libraryOf("First");
        ArrivalMode mode = loaded(library);
        library.addSong(PlaybackFixtures.song("Late"));

        assertEquals(1, mode.remaining(),
                "the mode owns a snapshot; the library is not a live view");
    }

    @Test
    void displayNameIsShownInTheModeSelector() {
        assertEquals("Arrival order", new ArrivalMode().displayName());
    }
}
