package com.discoballplayer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.discoballplayer.model.Album;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/** Flat by convention: {@code @Nested} would make a method selector run zero tests. */
class LibraryMapperTest {

    private static Song fullSong() {
        Song song = new Song("Ojitos Lindos", List.of(new Artist("Bad Bunny")),
                new Album("Un Verano Sin Ti"), 258, Genre.REGGAETON, 2022);
        song.setRating(81);
        song.setCoverPath("/covers/verano.png");
        song.setAudioPath("/music/ojitos.mp3");
        return song;
    }

    private static MusicLibrary libraryWith(Song... songs) {
        MusicLibrary library = new MusicLibrary();
        for (Song song : songs) {
            library.addSong(song);
        }
        return library;
    }

    @Test
    void roundTripPreservesEveryField() {
        MusicLibrary restored = LibraryMapper.toModel(
                LibraryMapper.toDto(libraryWith(fullSong())));

        Song song = restored.getAllSongs().get(0);
        assertEquals("Ojitos Lindos", song.getTitle());
        assertEquals("Bad Bunny", song.getArtistsNames());
        assertEquals("Un Verano Sin Ti", song.getAlbum().getTitle());
        assertEquals(258, song.getDurationSeconds());
        assertEquals(Genre.REGGAETON, song.getGenre());
        assertEquals(2022, song.getYear());
        assertEquals(81, song.getRating());
        assertEquals("/covers/verano.png", song.getCoverPath());
        assertEquals("/music/ojitos.mp3", song.getAudioPath());
    }

    @Test
    void artistsSharedBySeveralSongsBecomeOneInstance() {
        Artist artist = new Artist("Daft Punk");
        Album album = new Album("Discovery");
        MusicLibrary restored = LibraryMapper.toModel(LibraryMapper.toDto(libraryWith(
                new Song("One More Time", List.of(artist), album, 320, Genre.ELECTRONIC, 2001),
                new Song("Aerodynamic", List.of(artist), album, 212, Genre.ELECTRONIC, 2001))));

        List<Song> songs = restored.getAllSongs();

        assertSame(songs.get(0).getArtists().get(0), songs.get(1).getArtists().get(0),
                "filtering compares entities by id, so one artist must not become two");
        assertSame(songs.get(0).getAlbum(), songs.get(1).getAlbum());
    }

    @Test
    void artistFilteringStillWorksAfterAReload() {
        Artist artist = new Artist("Joe Arroyo");
        MusicLibrary restored = LibraryMapper.toModel(LibraryMapper.toDto(libraryWith(
                new Song("La Rebelion", List.of(artist), null, 340, Genre.SALSA, 1986),
                new Song("Tania", List.of(artist), null, 288, Genre.SALSA, 1984))));

        Artist reloaded = restored.getAllSongs().get(0).getArtists().get(0);

        assertEquals(2, restored.findByArtist(reloaded).size());
    }

    @Test
    void songWithoutAnAlbumSurvives() {
        MusicLibrary restored = LibraryMapper.toModel(LibraryMapper.toDto(libraryWith(
                new Song("Loose Track", List.of(new Artist("Nobody")), null,
                        100, Genre.OTHER, 2000))));

        assertNull(restored.getAllSongs().get(0).getAlbum());
    }

    @Test
    void songWithSeveralArtistsKeepsThemAllInOrder() {
        MusicLibrary restored = LibraryMapper.toModel(LibraryMapper.toDto(libraryWith(
                new Song("Collab", List.of(new Artist("First"), new Artist("Second")),
                        null, 200, Genre.POP, 2015))));

        assertEquals("First, Second", restored.getAllSongs().get(0).getArtistsNames());
    }

    @Test
    void unknownGenreFallsBackToOtherInsteadOfFailingTheLoad() {
        LibraryDto dto = LibraryMapper.toDto(libraryWith(fullSong()));
        dto.getSongs().get(0).setGenre("SKA_PUNK_FROM_2049");

        MusicLibrary restored = LibraryMapper.toModel(dto);

        assertEquals(Genre.OTHER, restored.getAllSongs().get(0).getGenre(),
                "one bad field must not cost the user their whole library");
    }

    @Test
    void missingGenreFallsBackToOther() {
        LibraryDto dto = LibraryMapper.toDto(libraryWith(fullSong()));
        dto.getSongs().get(0).setGenre(null);

        assertEquals(Genre.OTHER, LibraryMapper.toModel(dto).getAllSongs().get(0).getGenre());
    }

    @Test
    void emptyLibraryRoundTripsToAnEmptyLibrary() {
        MusicLibrary restored = LibraryMapper.toModel(LibraryMapper.toDto(new MusicLibrary()));

        assertTrue(restored.isEmpty());
    }

    @Test
    void songCountSurvivesTheRoundTrip() {
        MusicLibrary original = libraryWith(fullSong(), fullSong(), fullSong());

        assertEquals(3, LibraryMapper.toModel(LibraryMapper.toDto(original)).size(),
                "songs with identical fields are still distinct entries");
    }

    @Test
    void idsAreRegeneratedOnLoad() {
        MusicLibrary original = libraryWith(fullSong());
        String originalId = original.getAllSongs().get(0).getId();

        MusicLibrary restored = LibraryMapper.toModel(LibraryMapper.toDto(original));

        assertNotNull(restored.getAllSongs().get(0).getId());
        assertEquals(originalId.length(), restored.getAllSongs().get(0).getId().length());
    }
}
