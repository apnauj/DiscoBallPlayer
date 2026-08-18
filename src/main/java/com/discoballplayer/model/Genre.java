package com.discoballplayer.model;

public enum Genre {
    ROCK("Rock"),
    POP("Pop"),
    HIP_HOP("Hip Hop"),
    REGGAETON("Reggaeton"),
    SALSA("Salsa"),
    ELECTRONIC("Electronic"),
    JAZZ("Jazz"),
    CLASSICAL("Classical"),
    METAL("Metal"),
    INDIE("Indie"),
    OTHER("Other");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}