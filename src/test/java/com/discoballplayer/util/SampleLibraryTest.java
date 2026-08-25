package com.discoballplayer.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.discoballplayer.model.Album;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/** Flat by convention: {@code @Nested} would make a method selector run zero tests. */
class SampleLibraryTest {

    @Test
    void seedsTwentySongs() {
        assertEquals(20, SampleLibrary.create().size());
    }

    @Test
    void spansAtLeastFiveGenresSoFilteringHasSomethingToShow() {
        Set<Genre> genres = new HashSet<>();
        for (Song song : SampleLibrary.create().getAllSongs()) {
            genres.add(song.getGenre());
        }

        assertTrue(genres.size() >= 5, "found only " + genres.size() + " genres");
    }

    @Test
    void spansAtLeastThreeAlbums() {
        Set<Album> albums = new HashSet<>();
        for (Song song : SampleLibrary.create().getAllSongs()) {
            if (song.getAlbum() != null) {
                albums.add(song.getAlbum());
            }
        }

        assertTrue(albums.size() >= 3, "found only " + albums.size() + " albums");
    }

    @Test
    void everySongIsPlayableByTheSimulatedEngine() {
        for (Song song : SampleLibrary.create().getAllSongs()) {
            assertTrue(song.getDurationSeconds() > 0, song.getTitle() + " has no duration");
            assertNotNull(song.getGenre());
            assertTrue(song.getRating() >= 0 && song.getRating() <= 100);
        }
    }

    @Test
    void songsSharingAnArtistShareTheArtistInstance() {
        MusicLibrary library = SampleLibrary.create();

        Song first = library.search("One More Time").get(0);

        assertEquals(4, library.findByArtist(first.getArtists().get(0)).size(),
                "filtering compares by id, so one artist must not become four instances");
    }

    @Test
    void createReturnsAFreshLibraryEachTime() {
        MusicLibrary first = SampleLibrary.create();
        first.clear();

        assertEquals(20, SampleLibrary.create().size(), "no shared mutable state between calls");
    }
}
