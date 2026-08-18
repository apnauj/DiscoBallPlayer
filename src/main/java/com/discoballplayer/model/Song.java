package com.discoballplayer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class Song implements Comparable<Song> {

    public static final int MIN_RATING = 0;
    public static final int MAX_RATING = 100;

    private final String id;
    private String title;
    private final List<Artist> artists = new ArrayList<>();
    private Album album;
    private int durationSeconds;
    private Genre genre;
    private int year;
    private int rating;
    private String coverPath;
    private String audioPath;

    public Song(String title, List<Artist> artists, Album album,
                int durationSeconds, Genre genre, int year) {
        this.id = UUID.randomUUID().toString();
        setTitle(title);
        if (artists != null) {
            this.artists.addAll(artists);
        }
        this.album = album;
        setDurationSeconds(durationSeconds);
        setGenre(genre);
        setYear(year);
        this.rating = MIN_RATING;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("The name of the song can't be empty.");
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

    public String getArtistsNames() {
        if (artists.isEmpty()) {
            return "Unknown artist";
        }
        return artists.stream().map(Artist::getName).collect(Collectors.joining(", "));
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("The duration must be longer than 0.");
        }
        this.durationSeconds = durationSeconds;
    }

    public String getFormattedDuration() {
        return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = (genre == null) ? Genre.OTHER : genre;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Invalid release year: " + year);
        }
        this.year = year;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                    "The rating must be between " + MIN_RATING + " and " + MAX_RATING + ".");
        }
        this.rating = rating;
    }

    public String getCoverPath() {
        if (coverPath != null && !coverPath.isBlank()) {
            return coverPath;
        }
        return (album != null) ? album.getCoverPath() : null;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    @Override
    public int compareTo(Song other) {
        int byTitle = this.title.compareToIgnoreCase(other.title);
        return (byTitle != 0) ? byTitle : this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song)) return false;
        return id.equals(((Song) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " - " + getArtistsNames();
    }
}