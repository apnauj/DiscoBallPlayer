package com.discoballplayer.repository;

import com.discoballplayer.model.MusicLibrary;

/**
 * Persists the catalogue between runs.
 */
public interface LibraryRepository {

    /**
     * @return the stored library, or an empty one when nothing has been saved yet — a first
     *         run is a normal state, not an error
     */
    MusicLibrary load();

    void save(MusicLibrary library);
}
