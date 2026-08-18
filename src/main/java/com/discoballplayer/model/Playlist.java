package com.discoballplayer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A manual grouping of songs made by the user.
 *
 * <p>Unlike Album/Artist/Genre, here the list genuinely belongs to the entity: a
 * playlist is nothing more than its set of songs. Even so it is exposed as immutable
 * and modified through methods, not through a public {@code setSongs()}.</p>
 */
public class Playlist {

    private final String id;
    private String name;
    private String description;
    private String coverPath;
    private final List<Song> songs = new ArrayList<>();

    public Playlist(String name) {
        this.id = UUID.randomUUID().toString();
        setName(name);
    }

    public Playlist(String name, String description, String coverPath) {
        this(name);
        this.description = description;
        this.coverPath = coverPath;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The name of the playlist can't be empty.");
        }
        this.name = name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public List<Song> getSongs() {
        return Collections.unmodifiableList(songs);
    }

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

    public boolean contains(Song song) {
        return songs.contains(song);
    }

    public int size() {
        return songs.size();
    }

    public int getTotalDurationSeconds() {
        int total = 0;
        for (Song song : songs) {
            total += song.getDurationSeconds();
        }
        return total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Playlist)) return false;
        return id.equals(((Playlist) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + songs.size() + ")";
    }
}