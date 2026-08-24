package com.discoballplayer.playback;

import static com.discoballplayer.playback.PlaybackFixtures.libraryOf;
import static com.discoballplayer.playback.PlaybackFixtures.playNext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.model.MusicLibrary;

/** Flat by convention: {@code @Nested} would make a method selector run zero tests. */
class ShuffleModeTest {

    private static final String[] TITLES = {"A", "B", "C", "D", "E"};

    /** Seeded, so the order under test is fixed rather than merely "different". */
    private static ShuffleMode seededMode(MusicLibrary library) {
        ShuffleMode mode = new ShuffleMode(new Random(42));
        mode.load(library);
        return mode;
    }

    @Test
    void currentIsNullBeforeTheFirstNext() {
        assertNull(seededMode(libraryOf(TITLES)).current());
    }

    @Test
    void firstNextReturnsTheFirstSongRatherThanSkippingIt() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));

        String first = mode.next().getTitle();

        assertEquals(first, mode.current().getTitle());
    }

    @Test
    void playsEverySongOnceBeforeRepeating() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));

        Set<String> firstLap = new HashSet<>(playNext(mode, TITLES.length));

        assertEquals(Set.of("A", "B", "C", "D", "E"), firstLap);
    }

    @Test
    void nextThenPreviousReturnsToSameSong() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));
        mode.next();

        for (int step = 0; step < 8; step++) {
            String before = mode.current().getTitle();
            mode.next();
            assertEquals(before, mode.previous().getTitle());
        }
    }

    @Test
    void navigationWrapsInfinitelyForward() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));

        List<String> threeLaps = playNext(mode, TITLES.length * 3);

        assertEquals(threeLaps.subList(0, 5), threeLaps.subList(5, 10));
        assertEquals(threeLaps.subList(0, 5), threeLaps.subList(10, 15));
    }

    @Test
    void navigationWrapsInfinitelyBackward() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));
        mode.next();
        String start = mode.current().getTitle();

        for (int step = 0; step < TITLES.length; step++) {
            mode.next();
        }
        assertEquals(start, mode.current().getTitle(), "a full lap forward returns to start");

        for (int step = 0; step < TITLES.length; step++) {
            mode.previous();
        }
        assertEquals(start, mode.current().getTitle(), "a full lap backward returns to start");
    }

    @Test
    void steppingBackFromTheFirstSongWrapsToTheLast() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));
        mode.next();

        List<String> lap = playNext(mode, TITLES.length - 1);
        String last = lap.get(lap.size() - 1);

        mode.next();
        assertEquals(last, mode.previous().getTitle(),
                "from the head, previous() must land on the tail");
    }

    @Test
    void orderIsRandomizedOnceAndNotOnEveryNext() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));

        List<String> firstLap = playNext(mode, TITLES.length);
        List<String> secondLap = playNext(mode, TITLES.length);

        assertEquals(firstLap, secondLap,
                "re-shuffling inside next() would make previous() meaningless");
    }

    @Test
    void reloadingReshufflesTheOrder() {
        MusicLibrary library = libraryOf("A", "B", "C", "D", "E", "F", "G", "H");

        ShuffleMode first = new ShuffleMode(new Random(1));
        first.load(library);
        ShuffleMode second = new ShuffleMode(new Random(2));
        second.load(library);

        assertNotEquals(playNext(first, 8), playNext(second, 8));
    }

    @Test
    void hasNextAndHasPreviousAreAlwaysTrueForANonEmptyLibrary() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));

        for (int step = 0; step < 12; step++) {
            assertTrue(mode.hasNext());
            assertTrue(mode.hasPrevious());
            mode.next();
        }
    }

    @Test
    void emptyLibraryReportsNoNavigationAndThrowsOnNext() {
        ShuffleMode mode = seededMode(new MusicLibrary());

        assertFalse(mode.hasNext());
        assertFalse(mode.hasPrevious());
        assertThrows(EmptyStructureException.class, mode::next);
        assertThrows(EmptyStructureException.class, mode::previous);
    }

    @Test
    void singleSongLibraryKeepsReturningThatSong() {
        ShuffleMode mode = seededMode(libraryOf("Only"));

        assertEquals(List.of("Only", "Only", "Only"), playNext(mode, 3));
    }

    @Test
    void loadResetsCurrentAndTheOrder() {
        ShuffleMode mode = seededMode(libraryOf(TITLES));
        playNext(mode, 3);

        mode.load(libraryOf("X", "Y"));

        assertNull(mode.current(), "load must discard the previous position");
        assertEquals(Set.of("X", "Y"), new HashSet<>(playNext(mode, 2)));
    }

    @Test
    void displayNameIsShownInTheModeSelector() {
        assertEquals("Shuffle", new ShuffleMode().displayName());
    }
}
