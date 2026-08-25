package com.discoballplayer.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * The root object written to {@code library.json}.
 *
 * <p>A wrapper rather than a bare array so the format can gain fields — a schema version, user
 * playlists — without breaking files already on disk.</p>
 */
public class LibraryDto {

    private List<SongDto> songs = new ArrayList<>();

    public List<SongDto> getSongs() {
        return songs;
    }

    public void setSongs(List<SongDto> songs) {
        this.songs = songs == null ? new ArrayList<>() : songs;
    }
}
