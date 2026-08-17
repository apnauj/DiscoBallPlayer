package main.java.com.discoballplayer.model;

import java.util.List;

public class Album {
    private List<Artist> artists;
    private String description;
    private List<Song> songs;
    private String cover;


    public Album(List<Artist> artists, String description, List<Song> songs, String cover) {
        this.artists = artists;
        this.description = description;
        this.songs = songs;
        this.cover = cover;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }
}
