package main.java.com.discoballplayer.model;

import java.util.List;

public class Song {
    private String name;
    private List<Artist> artist;
    private Album album;
    private int duration; // In seconds
    private Genre genre;
    private int year;
    private int calification;
    private String cover;


    public Song(String name, List<Artist> artist, Album album, int duration, Genre genre, int year, int calification, String cover) {
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.genre = genre;
        this.year = year;
        this.calification = calification;
        this.cover = cover;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Artist> getArtist() {
        return artist;
    }

    public void setArtist(List<Artist> artist) {
        this.artist = artist;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getCalification() {
        return calification;
    }

    public void setCalification(int calification) {
        this.calification = calification;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }
}
