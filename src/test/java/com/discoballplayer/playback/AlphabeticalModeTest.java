package com.discoballplayer.playback;

import static com.discoballplayer.playback.PlaybackFixtures.libraryOf;
import static com.discoballplayer.playback.PlaybackFixtures.playNext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.model.MusicLibrary;

/** Flat by convention: {@code @Nested} would make a method selector run zero tests. */
class AlphabeticalModeTest {

    /** Deliberately unsorted, and mixed case, so ordering cannot come from insertion order. */
    private static final String[] SCRAMBLED =
            {"Tania", "aerodynamic", "Dare", "one more time", "Bailando"};

    private static final List<String> ALPHABETICAL =
            List.of("aerodynamic", "Bailando", "Dare", "one more time", "Tania");

    private static AlphabeticalMode loaded(MusicLibrary library) {
        AlphabeticalMode mode = new AlphabeticalMode();
        mode.load(library);
        return mode;
    }

    @Test
    void visitsSongsInTitleOrder() {
        AlphabeticalMode mode = loaded(libraryOf(SCRAMBLED));

        assertEquals(ALPHABETICAL, playNext(mode, SCRAMBLED.length));
    }

    @Test
    void orderIsCaseInsensitive() {
        AlphabeticalMode mode = loaded(libraryOf("banana", "Apple", "cherry"));

        assertEquals(List.of("Apple", "banana", "cherry"), playNext(mode, 3));
    }

    @Test
    void walksBackwardInReverseTitleOrder() {
        AlphabeticalMode mode = loaded(libraryOf(SCRAMBLED));
        playNext(mode, SCRAMBLED.length);

        List<String> backwards = new ArrayList<>();
        backwards.add(mode.current().getTitle());
        while (mode.hasPrevious()) {
            backwards.add(mode.previous().getTitle());
        }

        List<String> descending = new ArrayList<>(ALPHABETICAL);
        java.util.Collections.reverse(descending);
        assertEquals(descending, backwards);
    }

    @Test
    void hasPreviousIsFalseOnTheFirstTitle() {
        AlphabeticalMode mode = loaded(libraryOf(SCRAMBLED));

        assertFalse(mode.hasPrevious(), "before the first next() there is nowhere to go back to");
        mode.next();
        assertEquals("aerodynamic", mode.current().getTitle());
        assertFalse(mode.hasPrevious());
        assertThrows(NoSuchElementException.class, mode::previous);
    }

    @Test
    void hasNextIsFalseOnTheLastTitle() {
        AlphabeticalMode mode = loaded(libraryOf(SCRAMBLED));

        playNext(mode, SCRAMBLED.length);

        assertEquals("Tania", mode.current().getTitle());
        assertFalse(mode.hasNext(), "unlike shuffle, the walk has a real end");
        assertThrows(NoSuchElementException.class, mode::next);
    }

    @Test
    void nextThenPreviousReturnsToTheSameSong() {
        AlphabeticalMode mode = loaded(libraryOf(SCRAMBLED));
        mode.next();

        while (mode.hasNext()) {
            String before = mode.current().getTitle();
            mode.next();
            assertEquals(before, mode.previous().getTitle());
            mode.next();
        }
    }

    @Test
    void currentIsNullBeforeTheFirstNext() {
        assertNull(loaded(libraryOf(SCRAMBLED)).current());
    }

    @Test
    void firstNextReturnsTheSmallestTitleRatherThanSkippingIt() {
        AlphabeticalMode mode = loaded(libraryOf(SCRAMBLED));

        assertEquals("aerodynamic", mode.next().getTitle());
    }

    @Test
    void duplicateTitlesBothSurvive() {
        AlphabeticalMode mode = loaded(libraryOf("Tania", "Tania", "Alpha"));

        List<String> played = playNext(mode, 3);

        assertEquals(List.of("Alpha", "Tania", "Tania"), played,
                "Song.compareTo breaks ties on the id, so neither is swallowed by the tree");
    }

    @Test
    void navigatingDoesNotConsumeTheTree() {
        AlphabeticalMode mode = loaded(libraryOf(SCRAMBLED));
        playNext(mode, SCRAMBLED.length);

        while (mode.hasPrevious()) {
            mode.previous();
        }

        assertEquals(ALPHABETICAL.subList(1, ALPHABETICAL.size()),
                playNext(mode, SCRAMBLED.length - 1),
                "walking the tree must be repeatable; nothing is removed by traversal");
    }

    @Test
    void reloadingResetsToTheStart() {
        MusicLibrary library = libraryOf(SCRAMBLED);
        AlphabeticalMode mode = loaded(library);
        playNext(mode, 3);

        mode.load(library);

        assertNull(mode.current());
        assertFalse(mode.hasPrevious());
        assertEquals("aerodynamic", mode.next().getTitle());
    }

    @Test
    void singleSongLibraryHasNoNeighbours() {
        AlphabeticalMode mode = loaded(libraryOf("Only"));

        assertTrue(mode.hasNext());
        assertEquals("Only", mode.next().getTitle());
        assertFalse(mode.hasNext());
        assertFalse(mode.hasPrevious());
    }

    @Test
    void emptyLibraryReportsNoNavigationAndThrows() {
        AlphabeticalMode mode = loaded(new MusicLibrary());

        assertFalse(mode.hasNext());
        assertFalse(mode.hasPrevious());
        assertThrows(EmptyStructureException.class, mode::next);
        assertThrows(EmptyStructureException.class, mode::previous);
    }

    @Test
    void displayNameIsShownInTheModeSelector() {
        assertEquals("Alphabetical", new AlphabeticalMode().displayName());
    }
}
