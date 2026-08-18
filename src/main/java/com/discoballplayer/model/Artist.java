package com.discoballplayer.model;

import java.util.Objects;
import java.util.UUID;


public class Artist {

    private final String id;
    private String name;
    private String country;
    private int birthYear;
    private String description;
    private String picturePath;

    public Artist(String name) {
        this.id = UUID.randomUUID().toString();
        setName(name);
    }

    public Artist(String name, String country, int birthYear, String description, String picturePath) {
        this(name);
        this.country = country;
        setBirthYear(birthYear);
        this.description = description;
        this.picturePath = picturePath;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The artist name can't be empty.");
        }
        this.name = name.trim();
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        if (birthYear != 0 && (birthYear < 1800 || birthYear > 2100)) {
            throw new IllegalArgumentException("Invalid birth year: " + birthYear);
        }
        this.birthYear = birthYear;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist)) return false;
        return id.equals(((Artist) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}