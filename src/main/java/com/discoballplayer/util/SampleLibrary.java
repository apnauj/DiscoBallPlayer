package com.discoballplayer.util;

import java.util.List;

import com.discoballplayer.model.Album;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/**
 * Seed catalogue used when persistence comes back empty.
 *
 * <p>A first run must not open on a blank window: an empty library makes every mode look
 * broken and leaves nothing to demonstrate. These twenty songs span seven genres and five
 * albums so genre filtering, album grouping and alphabetical ordering all have something to
 * show.</p>
 *
 * <p>No audio paths. The simulated engine drives the progress bar from the duration alone, so
 * the demo works without shipping any audio files.</p>
 */
public final class SampleLibrary {

    private SampleLibrary() {
    }

    public static MusicLibrary create() {
        MusicLibrary library = new MusicLibrary();

        Artist daftPunk = new Artist("Daft Punk");
        Artist gorillaz = new Artist("Gorillaz");
        Artist badBunny = new Artist("Bad Bunny");
        Artist joeArroyo = new Artist("Joe Arroyo");
        Artist radiohead = new Artist("Radiohead");
        Artist milesDavis = new Artist("Miles Davis");
        Artist metallica = new Artist("Metallica");

        Album discovery = new Album("Discovery");
        Album demonDays = new Album("Demon Days");
        Album verano = new Album("Un Verano Sin Ti");
        Album okComputer = new Album("OK Computer");
        Album kindOfBlue = new Album("Kind of Blue");

        add(library, "One More Time", daftPunk, discovery, 320, Genre.ELECTRONIC, 2001, 92);
        add(library, "Aerodynamic", daftPunk, discovery, 212, Genre.ELECTRONIC, 2001, 85);
        add(library, "Digital Love", daftPunk, discovery, 301, Genre.ELECTRONIC, 2001, 88);
        add(library, "Harder, Better, Faster, Stronger", daftPunk, discovery, 224,
                Genre.ELECTRONIC, 2001, 90);

        add(library, "Feel Good Inc.", gorillaz, demonDays, 222, Genre.INDIE, 2005, 95);
        add(library, "Dare", gorillaz, demonDays, 244, Genre.INDIE, 2005, 80);
        add(library, "El Manana", gorillaz, demonDays, 231, Genre.INDIE, 2005, 76);
        add(library, "Kids with Guns", gorillaz, demonDays, 224, Genre.INDIE, 2005, 72);

        add(library, "Tito Me Pregunto", badBunny, verano, 244, Genre.REGGAETON, 2022, 70);
        add(library, "Moscow Mule", badBunny, verano, 245, Genre.REGGAETON, 2022, 74);
        add(library, "Ojitos Lindos", badBunny, verano, 258, Genre.REGGAETON, 2022, 81);

        add(library, "La Rebelion", joeArroyo, null, 340, Genre.SALSA, 1986, 99);
        add(library, "En Barranquilla Me Quedo", joeArroyo, null, 295, Genre.SALSA, 1981, 90);
        add(library, "Tania", joeArroyo, null, 288, Genre.SALSA, 1984, 87);

        add(library, "Paranoid Android", radiohead, okComputer, 383, Genre.ROCK, 1997, 96);
        add(library, "Karma Police", radiohead, okComputer, 264, Genre.ROCK, 1997, 93);
        add(library, "No Surprises", radiohead, okComputer, 229, Genre.ROCK, 1997, 89);

        add(library, "So What", milesDavis, kindOfBlue, 545, Genre.JAZZ, 1959, 98);
        add(library, "Blue in Green", milesDavis, kindOfBlue, 337, Genre.JAZZ, 1959, 91);

        add(library, "Master of Puppets", metallica, null, 515, Genre.METAL, 1986, 94);

        return library;
    }

    private static void add(MusicLibrary library, String title, Artist artist, Album album,
                            int durationSeconds, Genre genre, int year, int rating) {
        Song song = new Song(title, List.of(artist), album, durationSeconds, genre, year);
        song.setRating(rating);
        if (album != null) {
            album.addArtist(artist);
            album.setYear(year);
        }
        library.addSong(song);
    }
}
