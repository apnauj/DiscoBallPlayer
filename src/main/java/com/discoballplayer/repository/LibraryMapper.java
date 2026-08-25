package com.discoballplayer.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.discoballplayer.model.Album;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/**
 * Converts between the model and its persisted shape.
 *
 * <p>Reading back de-duplicates artists and albums by name: the JSON holds a name per song, so
 * a naive mapping would create one {@code Artist} instance per song and break filtering, which
 * compares entities by their generated id.</p>
 *
 * <p><strong>Two things do not survive a round trip, by design.</strong> Generated ids are new
 * on every load, so ordering among songs with identical titles is stable within a run but not
 * across runs — already documented for the tree's tie-break. And artist and album carry only
 * their name here; country, birth year and descriptions are not persisted because nothing in
 * the UI collects them yet. Widening the format later is why {@link LibraryDto} is an object
 * rather than a bare array.</p>
 */
public final class LibraryMapper {

    private LibraryMapper() {
    }

    public static LibraryDto toDto(MusicLibrary library) {
        LibraryDto dto = new LibraryDto();
        List<SongDto> songs = new ArrayList<>();
        for (Song song : library.getAllSongs()) {
            songs.add(toDto(song));
        }
        dto.setSongs(songs);
        return dto;
    }

    private static SongDto toDto(Song song) {
        SongDto dto = new SongDto();
        dto.setTitle(song.getTitle());
        List<String> artistNames = new ArrayList<>();
        for (Artist artist : song.getArtists()) {
            artistNames.add(artist.getName());
        }
        dto.setArtists(artistNames);
        dto.setAlbum(song.getAlbum() == null ? null : song.getAlbum().getTitle());
        dto.setDurationSeconds(song.getDurationSeconds());
        dto.setGenre(song.getGenre().name());
        dto.setYear(song.getYear());
        dto.setRating(song.getRating());
        dto.setCoverPath(song.getCoverPath());
        dto.setAudioPath(song.getAudioPath());
        return dto;
    }

    public static MusicLibrary toModel(LibraryDto dto) {
        MusicLibrary library = new MusicLibrary();
        Map<String, Artist> artists = new HashMap<>();
        Map<String, Album> albums = new HashMap<>();

        for (SongDto songDto : dto.getSongs()) {
            library.addSong(toModel(songDto, artists, albums));
        }
        return library;
    }

    private static Song toModel(SongDto dto, Map<String, Artist> artists, Map<String, Album> albums) {
        List<Artist> songArtists = new ArrayList<>();
        for (String name : dto.getArtists()) {
            songArtists.add(artists.computeIfAbsent(name, Artist::new));
        }

        Album album = null;
        if (dto.getAlbum() != null && !dto.getAlbum().isBlank()) {
            album = albums.computeIfAbsent(dto.getAlbum(), Album::new);
        }

        Song song = new Song(dto.getTitle(), songArtists, album,
                dto.getDurationSeconds(), parseGenre(dto.getGenre()), dto.getYear());
        song.setRating(dto.getRating());
        song.setCoverPath(dto.getCoverPath());
        song.setAudioPath(dto.getAudioPath());
        return song;
    }

    /**
     * An unrecognised genre becomes {@link Genre#OTHER} rather than aborting the load. One bad
     * value should cost the user that field, not their whole library.
     */
    private static Genre parseGenre(String name) {
        if (name == null) {
            return Genre.OTHER;
        }
        try {
            return Genre.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return Genre.OTHER;
        }
    }
}
