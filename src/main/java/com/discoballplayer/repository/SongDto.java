package com.discoballplayer.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat, Jackson-friendly view of a song.
 *
 * <p>{@code Song} has a final generated id and no no-argument constructor, so Jackson cannot
 * bind it directly. Rather than weaken the model or annotate it, persistence gets its own
 * shape: mutable, with a no-argument constructor, and holding artist and album by name instead
 * of by reference.</p>
 */
public class SongDto {

    private String title;
    private List<String> artists = new ArrayList<>();
    private String album;
    private int durationSeconds;
    private String genre;
    private int year;
    private int rating;
    private String coverPath;
    private String audioPath;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getArtists() {
        return artists;
    }

    public void setArtists(List<String> artists) {
        this.artists = artists == null ? new ArrayList<>() : artists;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getCoverPath() {
        return coverPath;
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
}
