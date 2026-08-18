package main.java.com.discoballplayer.model;

public enum Genre {
    ROCK("Rock"),
    POP("Pop"),
    HIP_HOP("Hip Hop"),
    REGGAETON("Reggaetón"),
    SALSA("Salsa"),
    ELECTRONIC("Electrónica"),
    JAZZ("Jazz"),
    CLASSICAL("Clásica"),
    METAL("Metal"),
    INDIE("Indie"),
    OTHER("Otro");

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