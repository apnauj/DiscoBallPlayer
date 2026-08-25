package com.discoballplayer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.discoballplayer.exception.SongNotFoundException;
import com.discoballplayer.model.Album;
import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.PlaybackMode;

/**
 * In-memory {@link PlayerService} stub that exists so the UI can be built and run before the
 * real {@code Player} lands.
 *
 * <p>With no mode selected, navigation is a plain index into an {@code ArrayList}. Once the
 * UI selects one, navigation is delegated to that {@link PlaybackMode}, because the behaviour
 * the UI has to render — a disabled Previous in arrival order, a queue that runs out — comes
 * from the mode and cannot be faked by an index walk.</p>
 *
 * <p>The delegation is the mode's, not the structure's: this class never names a structure and
 * never imports one. It is scaffolding, replaced in ticket {@code C-01}.</p>
 */
public class DemoPlayerService implements PlayerService {

    private static final int TICK_SECONDS = 1;

    private final List<Song> songs = new ArrayList<>();
    private final List<PlaybackListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService ticker =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "demo-player-ticker");
                thread.setDaemon(true);
                return thread;
            });

    private PlaybackMode mode;
    private int index = -1;
    private boolean playing;
    private int elapsedSeconds;

    public DemoPlayerService() {
        seed();
        ticker.scheduleAtFixedRate(this::tick, TICK_SECONDS, TICK_SECONDS, TimeUnit.SECONDS);
    }

    private void seed() {
        Artist daft = new Artist("Daft Punk");
        Artist gorillaz = new Artist("Gorillaz");
        Artist bad = new Artist("Bad Bunny");
        Artist joe = new Artist("Joe Arroyo");

        Album discovery = new Album("Discovery");
        Album demonDays = new Album("Demon Days");
        Album verano = new Album("Un Verano Sin Ti");

        add("One More Time", daft, discovery, 320, Genre.ELECTRONIC, 2001, 92);
        add("Aerodynamic", daft, discovery, 212, Genre.ELECTRONIC, 2001, 85);
        add("Digital Love", daft, discovery, 301, Genre.ELECTRONIC, 2001, 88);
        add("Feel Good Inc.", gorillaz, demonDays, 222, Genre.INDIE, 2005, 95);
        add("Dare", gorillaz, demonDays, 244, Genre.INDIE, 2005, 80);
        add("El Manana", gorillaz, demonDays, 231, Genre.INDIE, 2005, 76);
        add("Tito Me Pregunto", bad, verano, 244, Genre.REGGAETON, 2022, 70);
        add("Moscow Mule", bad, verano, 245, Genre.REGGAETON, 2022, 74);
        add("Ojitos Lindos", bad, verano, 258, Genre.REGGAETON, 2022, 81);
        add("La Rebelion", joe, null, 340, Genre.SALSA, 1986, 99);
        add("En Barranquilla Me Quedo", joe, null, 295, Genre.SALSA, 1981, 90);
        add("Tania", joe, null, 288, Genre.SALSA, 1984, 87);
    }

    private void add(String title, Artist artist, Album album,
                     int duration, Genre genre, int year, int rating) {
        Song song = new Song(title, List.of(artist), album, duration, genre, year);
        song.setRating(rating);
        songs.add(song);
    }

    private void tick() {
        Song song = current();
        if (!playing || song == null) {
            return;
        }
        elapsedSeconds = Math.min(elapsedSeconds + TICK_SECONDS, song.getDurationSeconds());
        int elapsed = elapsedSeconds;
        int total = song.getDurationSeconds();
        listeners.forEach(listener -> listener.onProgress(elapsed, total));
        if (elapsed >= total && hasNext()) {
            next();
        }
    }

    /**
     * Loads the mode from a snapshot of the demo catalogue and hands navigation over to it.
     *
     * <p>The snapshot is taken here and not kept in sync afterwards, which is the documented
     * architecture: a mode owns the structure it built at {@code load} time, so adding a song
     * does not mean reconciling three structures by hand.</p>
     */
    @Override
    public void setMode(PlaybackMode mode) {
        this.mode = mode;
        this.index = -1;
        this.elapsedSeconds = 0;
        if (mode != null) {
            mode.load(snapshot());
        }
    }

    private MusicLibrary snapshot() {
        MusicLibrary library = new MusicLibrary();
        songs.forEach(library::addSong);
        return library;
    }

    @Override
    public PlaybackMode getMode() {
        return mode;
    }

    /**
     * @throws com.discoballplayer.exception.EmptyStructureException when the active mode has
     *         run out of songs, which is what the UI renders as a finished queue
     */
    @Override
    public Song next() {
        if (mode != null) {
            return announce(mode.next());
        }
        if (songs.isEmpty()) {
            return null;
        }
        index = (index + 1) % songs.size();
        return announce(songs.get(index));
    }

    /**
     * @throws UnsupportedOperationException in a mode that cannot go back, such as arrival order
     */
    @Override
    public Song previous() {
        if (mode != null) {
            return announce(mode.previous());
        }
        if (songs.isEmpty()) {
            return null;
        }
        index = (index <= 0) ? songs.size() - 1 : index - 1;
        return announce(songs.get(index));
    }

    private Song announce(Song song) {
        elapsedSeconds = 0;
        listeners.forEach(listener -> listener.onSongChanged(song));
        return song;
    }

    /**
     * Jumps to a song, through the active mode.
     *
     * <p>Arrived from Track A walking the index directly, which meant the stub accepted a jump
     * in arrival order and never raised {@code UnsupportedOperationException} — the one
     * behaviour click-to-play has to get right. Delegating matches the rest of this class and
     * matches {@code Player}.</p>
     *
     * @throws UnsupportedOperationException if the active mode cannot reposition
     * @throws SongNotFoundException if the song is not loaded in the active mode
     */
    @Override
    public Song playSong(Song song) {
        Objects.requireNonNull(song, "The song can't be null.");
        Song selected = (mode != null) ? mode.jumpTo(song) : jumpByIndex(song);
        elapsedSeconds = 0;
        listeners.forEach(listener -> listener.onSongChanged(selected));
        setPlaying(true);
        return selected;
    }

    private Song jumpByIndex(Song song) {
        int target = songs.indexOf(song);
        if (target < 0) {
            throw new SongNotFoundException("Not in the demo library: " + song.getTitle());
        }
        index = target;
        return songs.get(target);
    }

    @Override
    public boolean canPlaySong() {
        return (mode != null) ? mode.canJumpTo() : !songs.isEmpty();
    }

    @Override
    public boolean hasNext() {
        return (mode != null) ? mode.hasNext() : !songs.isEmpty();
    }

    @Override
    public boolean hasPrevious() {
        return (mode != null) ? mode.hasPrevious() : !songs.isEmpty();
    }

    @Override
    public Song current() {
        if (mode != null) {
            return mode.current();
        }
        return (index < 0 || songs.isEmpty()) ? null : songs.get(index);
    }

    @Override
    public void play() {
        if (current() == null && hasNext()) {
            next();
        }
        setPlaying(true);
    }

    @Override
    public void pause() {
        setPlaying(false);
    }

    private void setPlaying(boolean value) {
        playing = value;
        listeners.forEach(listener -> listener.onPlaybackStateChanged(value));
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public void addSong(Song song) {
        songs.add(song);
        if (mode != null) {
            mode.add(song);
        }
        fireLibraryChanged();
    }

    @Override
    public void removeSong(Song song) {
        if (songs.remove(song)) {
            index = Math.min(index, songs.size() - 1);
            fireLibraryChanged();
        }
    }

    /**
     * Releases the timer thread. The real {@code Player} owns an {@code AudioEngine} that will
     * need the same, so the UI calls this on shutdown either way.
     */
    public void shutdown() {
        ticker.shutdownNow();
    }

    @Override
    public void updateSong(Song song) {
        fireLibraryChanged();
    }

    @Override
    public List<Song> listAll() {
        return List.copyOf(songs);
    }

    @Override
    public List<Song> search(String query) {
        if (query == null || query.isBlank()) {
            return listAll();
        }
        String needle = query.trim().toLowerCase();
        List<Song> matches = new ArrayList<>();
        for (Song song : songs) {
            boolean hit = song.getTitle().toLowerCase().contains(needle)
                    || song.getArtistsNames().toLowerCase().contains(needle)
                    || (song.getAlbum() != null
                        && song.getAlbum().getTitle().toLowerCase().contains(needle));
            if (hit) {
                matches.add(song);
            }
        }
        return matches;
    }

    @Override
    public void rate(Song song, int rating) {
        song.setRating(rating);
        fireLibraryChanged();
    }

    private void fireLibraryChanged() {
        listeners.forEach(PlaybackListener::onLibraryChanged);
    }

    @Override
    public void addListener(PlaybackListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }
}
