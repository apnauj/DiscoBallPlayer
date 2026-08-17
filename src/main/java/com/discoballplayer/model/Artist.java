package main.java.com.discoballplayer.model;

import java.util.List;

public class Artist {
    private String name;
    private int birth;
    private List<Song> songs;
    private String country;
    private String description;
    private String picture;


    public Artist(String name, int birth, List<Song> songs, String country, String description, String picture) {
        this.name = name;
        this.birth = birth;
        this.songs = songs;
        this.country = country;
        this.description = description;
        this.picture = picture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBirth() {
        return birth;
    }

    public void setBirth(int birth) {
        this.birth = birth;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
