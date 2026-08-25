package com.discoballplayer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Album {

    private final String id;
    private String title;
    private final List<Artist> artists = new ArrayList<>();
    private int year;
    private String description;
    private String coverPath;

    public Album(String title) {
        this.id = UUID.randomUUID().toString();
        setTitle(title);
    }

    public Album(String title, List<Artist> artists, int year, String description, String coverPath) {
        this(title);
        if (artists != null) {
            this.artists.addAll(artists);
        }
        setYear(year);
        this.description = description;
        this.coverPath = coverPath;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("The album title can't be empty.");
        }
        this.title = title.trim();
    }

    public List<Artist> getArtists() {
        return Collections.unmodifiableList(artists);
    }

    public void addArtist(Artist artist) {
        Objects.requireNonNull(artist, "The artist can't be null.");
        if (!artists.contains(artist)) {
            artists.add(artist);
        }
    }

    public void removeArtist(Artist artist) {
        artists.remove(artist);
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        if (year != 0 && (year < 1900 || year > 2100)) {
            throw new IllegalArgumentException("Invalid Album's year: " + year);
        }
        this.year = year;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album)) return false;
        return id.equals(((Album) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title;
    }
}