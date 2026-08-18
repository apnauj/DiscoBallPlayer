package main.java.com.discoballplayer.model;

import java.util.List;

public class Genre {
    private String name;
    private String description;
    private List<Song> songs;


    public Genre(String name, String description, List<Song> songs) {
        this.name = name;
        this.description = description;
        this.songs = songs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
