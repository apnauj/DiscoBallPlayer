package com.discoballplayer.playback;

import java.util.ArrayList;
import java.util.List;

import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/** Shared library builders for the playback mode suites. */
final class PlaybackFixtures {

    private PlaybackFixtures() {
    }

    static Song song(String title) {
        return new Song(title, List.of(new Artist("Tester")), null, 180, Genre.OTHER, 2020);
    }

    /** A library holding exactly these titles, in this insertion order. */
    static MusicLibrary libraryOf(String... titles) {
        MusicLibrary library = new MusicLibrary();
        for (String title : titles) {
            library.addSong(song(title));
        }
        return library;
    }

    static List<String> titlesOf(List<Song> songs) {
        List<String> titles = new ArrayList<>();
        for (Song song : songs) {
            titles.add(song.getTitle());
        }
        return titles;
    }

    /** Calls {@code next()} {@code count} times and collects the titles it returns. */
    static List<String> playNext(PlaybackMode mode, int count) {
        List<String> played = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            played.add(mode.next().getTitle());
        }
        return played;
    }
}
