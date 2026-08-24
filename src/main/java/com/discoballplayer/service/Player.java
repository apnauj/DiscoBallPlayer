package com.discoballplayer.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.discoballplayer.exception.SongNotFoundException;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.PlaybackMode;
import com.discoballplayer.playback.audio.AudioEngine;

/**
 * The facade the UI talks to, and the only place playback and the catalogue meet.
 *
 * <p>Navigation is delegated wholesale to the active {@link PlaybackMode}. This class decides
 * nothing about ordering; swapping the mode swaps the behaviour, which is the whole point of
 * the exercise.</p>
 *
 * <p><strong>A mode owns a snapshot, not a live view.</strong> {@link #setMode} loads the mode
 * from the library, and the library is not re-read afterwards. Adding or removing a song fires
 * {@link PlaybackListener#onLibraryChanged()} so the UI can refresh its table, but it
 * deliberately does not reload the mode: reloading would reset the shuffle order mid-song and
 * would refill an arrival queue the user had already played through. The change takes effect
 * the next time a mode is selected.</p>
 */
public class Player implements PlayerService {

    private static final Logger LOG = Logger.getLogger(Player.class.getName());

    private final MusicLibrary library;
    private final AudioEngine audio;
    private final List<PlaybackListener> listeners = new CopyOnWriteArrayList<>();

    private PlaybackMode mode;
    private boolean playing;

    public Player(MusicLibrary library, AudioEngine audio) {
        this.library = Objects.requireNonNull(library, "The library can't be null.");
        this.audio = Objects.requireNonNull(audio, "The audio engine can't be null.");
        audio.setProgressCallback(this::onEngineProgress);
        audio.setCompletionCallback(this::onSongCompleted);
    }

    /**
     * Republishes an engine tick as a progress event.
     *
     * <p>The total comes from the current song rather than from the engine, so the UI always
     * receives a consistent pair even if a song change races a tick.</p>
     */
    private void onEngineProgress(int elapsedSeconds) {
        Song song = current();
        if (song != null) {
            dispatch(listener -> listener.onProgress(elapsedSeconds, song.getDurationSeconds()));
        }
    }

    /**
     * Advances to the next song when one finishes.
     *
     * <p>At the end of the last song in a finite mode there is nothing to advance to, so
     * playback simply stops. Shuffle never reaches this state: its ring has no end.</p>
     */
    private void onSongCompleted() {
        if (hasNext()) {
            next();
            audio.play();
        } else {
            setPlaying(false);
        }
    }

    // ---------- mode delegation ----------

    @Override
    public void setMode(PlaybackMode mode) {
        this.mode = Objects.requireNonNull(mode, "The playback mode can't be null.");
        mode.load(library);
        audio.stop();
        setPlaying(false);
    }

    @Override
    public PlaybackMode getMode() {
        return mode;
    }

    private PlaybackMode requireMode() {
        if (mode == null) {
            throw new IllegalStateException("No playback mode selected.");
        }
        return mode;
    }

    @Override
    public Song next() {
        return startPlaying(requireMode().next());
    }

    @Override
    public Song previous() {
        return startPlaying(requireMode().previous());
    }

    /**
     * Hands a newly selected song to the engine and announces it.
     *
     * <p>The engine is loaded but not started here: navigating while paused must stay paused,
     * and a caller that wants sound calls {@link #play()}.</p>
     */
    private Song startPlaying(Song song) {
        audio.load(song);
        fireSongChanged(song);
        if (playing) {
            audio.play();
        }
        return song;
    }

    @Override
    public boolean hasNext() {
        return mode != null && mode.hasNext();
    }

    @Override
    public boolean hasPrevious() {
        return mode != null && mode.hasPrevious();
    }

    @Override
    public Song current() {
        return mode == null ? null : mode.current();
    }

    // ---------- transport ----------

    @Override
    public void play() {
        if (current() == null && hasNext()) {
            next();
        }
        if (current() == null) {
            return;
        }
        setPlaying(true);
        audio.play();
    }

    @Override
    public void pause() {
        audio.pause();
        setPlaying(false);
    }

    /**
     * Releases the audio engine's threads. Called once, when the application shuts down.
     */
    public void dispose() {
        audio.dispose();
    }

    private void setPlaying(boolean value) {
        if (playing == value) {
            return;
        }
        playing = value;
        dispatch(listener -> listener.onPlaybackStateChanged(value));
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    // ---------- library ----------

    @Override
    public void addSong(Song song) {
        Objects.requireNonNull(song, "The song can't be null.");
        if (library.addSong(song)) {
            fireLibraryChanged();
        }
    }

    @Override
    public void removeSong(Song song) {
        Objects.requireNonNull(song, "The song can't be null.");
        if (!library.removeSong(song)) {
            throw new SongNotFoundException("Not in the library: " + song.getTitle());
        }
        fireLibraryChanged();
    }

    /**
     * Announces that a song's fields were edited in place.
     *
     * <p>The model is mutable, so the edit has already happened by the time this is called.
     * What the UI needs is the notification, and what the caller needs is the guarantee that
     * the song is actually in the catalogue.</p>
     *
     * @throws SongNotFoundException if the song is not in the library
     */
    @Override
    public void updateSong(Song song) {
        Objects.requireNonNull(song, "The song can't be null.");
        if (library.findById(song.getId()) == null) {
            throw new SongNotFoundException("Not in the library: " + song.getTitle());
        }
        fireLibraryChanged();
    }

    @Override
    public List<Song> listAll() {
        return library.getAllSongs();
    }

    @Override
    public List<Song> search(String query) {
        return library.search(query);
    }

    @Override
    public void rate(Song song, int rating) {
        Objects.requireNonNull(song, "The song can't be null.");
        if (library.findById(song.getId()) == null) {
            throw new SongNotFoundException("Not in the library: " + song.getTitle());
        }
        song.setRating(rating);
        fireLibraryChanged();
    }

    // ---------- listeners ----------

    @Override
    public void addListener(PlaybackListener listener) {
        listeners.add(Objects.requireNonNull(listener, "The listener can't be null."));
    }

    @Override
    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    private void fireSongChanged(Song song) {
        dispatch(listener -> listener.onSongChanged(song));
    }

    private void fireLibraryChanged() {
        dispatch(PlaybackListener::onLibraryChanged);
    }

    /**
     * Delivers an event to every listener.
     *
     * <p>A listener that throws is logged and skipped rather than allowed to abort the loop.
     * One misbehaving UI component must not stop the others from being told the song changed;
     * the alternative is a half-updated window with no error anywhere.</p>
     */
    private void dispatch(java.util.function.Consumer<PlaybackListener> event) {
        for (PlaybackListener listener : listeners) {
            try {
                event.accept(listener);
            } catch (RuntimeException failure) {
                LOG.log(Level.WARNING, "A playback listener threw; skipping it.", failure);
            }
        }
    }
}
