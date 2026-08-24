package com.discoballplayer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.discoballplayer.exception.PersistenceException;
import com.discoballplayer.model.Album;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/** Flat by convention: {@code @Nested} would make a method selector run zero tests. */
class JsonLibraryRepositoryTest {

    private static MusicLibrary sampleLibrary() {
        MusicLibrary library = new MusicLibrary();
        Artist artist = new Artist("Gorillaz");
        Album album = new Album("Demon Days");
        Song song = new Song("Feel Good Inc.", List.of(artist), album, 222, Genre.INDIE, 2005);
        song.setRating(95);
        library.addSong(song);
        library.addSong(new Song("Dare", List.of(artist), album, 244, Genre.INDIE, 2005));
        return library;
    }

    @Test
    void missingFileYieldsEmptyLibrary(@TempDir Path directory) {
        JsonLibraryRepository repository =
                new JsonLibraryRepository(directory.resolve("library.json"));

        assertTrue(repository.load().isEmpty(), "a first run is a normal state, not an error");
    }

    @Test
    void saveThenLoadRoundTripsTheLibrary(@TempDir Path directory) {
        Path file = directory.resolve("library.json");
        JsonLibraryRepository repository = new JsonLibraryRepository(file);

        repository.save(sampleLibrary());
        MusicLibrary restored = repository.load();

        assertEquals(2, restored.size());
        Song first = restored.getAllSongs().get(0);
        assertEquals("Feel Good Inc.", first.getTitle());
        assertEquals("Gorillaz", first.getArtistsNames());
        assertEquals("Demon Days", first.getAlbum().getTitle());
        assertEquals(95, first.getRating());
        assertEquals(Genre.INDIE, first.getGenre());
    }

    @Test
    void saveCreatesMissingDirectories(@TempDir Path directory) {
        Path file = directory.resolve("nested/deeper/library.json");

        new JsonLibraryRepository(file).save(sampleLibrary());

        assertTrue(Files.exists(file));
    }

    @Test
    void saveReplacesPreviousContents(@TempDir Path directory) {
        Path file = directory.resolve("library.json");
        JsonLibraryRepository repository = new JsonLibraryRepository(file);
        repository.save(sampleLibrary());

        repository.save(new MusicLibrary());

        assertTrue(repository.load().isEmpty());
    }

    @Test
    void saveLeavesNoTemporaryFilesBehind(@TempDir Path directory) throws IOException {
        JsonLibraryRepository repository =
                new JsonLibraryRepository(directory.resolve("library.json"));

        repository.save(sampleLibrary());

        try (var entries = Files.list(directory)) {
            assertEquals(List.of("library.json"), entries.map(p -> p.getFileName().toString())
                    .sorted().toList(), "the atomic-write temp file must be moved, not left");
        }
    }

    @Test
    void malformedJsonSurfacesAClearException(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("library.json");
        Files.writeString(file, "{ this is not json");

        PersistenceException failure =
                assertThrows(PersistenceException.class, () -> new JsonLibraryRepository(file).load());

        assertTrue(failure.getMessage().contains("library.json"),
                "the message must name the file the user has to deal with");
        assertTrue(failure.getMessage().contains("move it aside"),
                "and tell them what to do about it");
    }

    @Test
    void emptyFileSurfacesAClearException(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("library.json");
        Files.writeString(file, "");

        assertThrows(PersistenceException.class, () -> new JsonLibraryRepository(file).load());
    }

    @Test
    void unknownFieldsInTheFileDoNotBreakTheLoad(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("library.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 7,
                  "songs": [
                    {"title": "Tania", "artists": ["Joe Arroyo"], "album": null,
                     "durationSeconds": 288, "genre": "SALSA", "year": 1984, "rating": 87,
                     "coverPath": null, "audioPath": null, "somethingNew": true}
                  ]
                }
                """);

        MusicLibrary restored = new JsonLibraryRepository(file).load();

        assertEquals(1, restored.size(), "a file from a newer build must still open");
        assertEquals("Tania", restored.getAllSongs().get(0).getTitle());
        assertEquals(87, restored.getAllSongs().get(0).getRating());
    }

    @Test
    void savedFileIsHumanReadable(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("library.json");

        new JsonLibraryRepository(file).save(sampleLibrary());

        String written = Files.readString(file);
        assertTrue(written.contains("\n"), "indented output so the file can be inspected by hand");
        assertTrue(written.contains("Feel Good Inc."));
    }

    @Test
    void emptyLibrarySavesAndLoadsBack(@TempDir Path directory) {
        Path file = directory.resolve("library.json");
        JsonLibraryRepository repository = new JsonLibraryRepository(file);

        repository.save(new MusicLibrary());

        assertTrue(repository.load().isEmpty());
        assertFalse(Files.notExists(file), "an empty library still writes a file");
    }
}
