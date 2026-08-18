package main.java.com.discoballplayer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MusicLibrary {

    private final List<Song> songs = new ArrayList<>();

    public boolean addSong(Song song) {
        Objects.requireNonNull(song, "The song can't be null.");
        if (songs.contains(song)) {
            return false;
        }
        return songs.add(song);
    }

    public boolean removeSong(Song song) {
        return songs.remove(song);
    }

    public List<Song> getAllSongs() {
        return Collections.unmodifiableList(songs);
    }

    public int size() {
        return songs.size();
    }

    public boolean isEmpty() {
        return songs.isEmpty();
    }

    public Song findById(String id) {
        for (Song song : songs) {
            if (song.getId().equals(id)) {
                return song;
            }
        }
        return null;
    }

    public List<Song> search(String text) {
        List<Song> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            result.addAll(songs);
            return result;
        }
        String query = text.trim().toLowerCase();
        for (Song song : songs) {
            boolean matchesTitle = song.getTitle().toLowerCase().contains(query);
            boolean matchesArtist = song.getArtistsNames().toLowerCase().contains(query);
            boolean matchesAlbum = song.getAlbum() != null
                    && song.getAlbum().getTitle().toLowerCase().contains(query);
            if (matchesTitle || matchesArtist || matchesAlbum) {
                result.add(song);
            }
        }
        return result;
    }

    public List<Song> findByArtist(Artist artist) {
        List<Song> result = new ArrayList<>();
        for (Song song : songs) {
            if (song.getArtists().contains(artist)) {
                result.add(song);
            }
        }
        return result;
    }

    public List<Song> findByAlbum(Album album) {
        List<Song> result = new ArrayList<>();
        for (Song song : songs) {
            if (album.equals(song.getAlbum())) {
                result.add(song);
            }
        }
        return result;
    }

    public List<Song> findByGenre(Genre genre) {
        List<Song> result = new ArrayList<>();
        for (Song song : songs) {
            if (song.getGenre() == genre) {
                result.add(song);
            }
        }
        return result;
    }

    public void clear() {
        songs.clear();
    }
}