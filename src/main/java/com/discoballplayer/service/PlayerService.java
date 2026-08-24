package com.discoballplayer.service;

import java.util.List;

import com.discoballplayer.model.Song;
import com.discoballplayer.playback.PlaybackMode;

/**
 * The only type the UI is allowed to call.
 *
 * <p>Existing as an interface is what lets the UI be built and run against
 * {@link DemoPlayerService} while the real implementation is still being written.</p>
 */
public interface PlayerService {

    void setMode(PlaybackMode mode);

    PlaybackMode getMode();

    Song next();

    Song previous();

    boolean hasNext();

    boolean hasPrevious();

    Song current();

    /**
     * Plays {@code song} immediately, repositioning the active mode onto it.
     *
     * @throws UnsupportedOperationException if the active mode cannot reposition
     * @throws com.discoballplayer.exception.SongNotFoundException if the song is not loaded
     */
    Song playSong(Song song);

    /**
     * @return whether {@link #playSong} is available right now; the UI enables click-to-play
     *         from this value rather than naming the mode
     */
    boolean canPlaySong();

    void play();

    void pause();

    boolean isPlaying();

    void addSong(Song song);

    /**
     * @throws com.discoballplayer.exception.SongNotFoundException if the song is not in the library
     */
    void removeSong(Song song);

    void updateSong(Song song);

    List<Song> listAll();

    /**
     * @return every song whose title, artist or album contains {@code query}, or the whole
     *         library when the query is blank
     */
    List<Song> search(String query);

    /**
     * @throws IllegalArgumentException unless {@code rating} is between 0 and 100 inclusive
     */
    void rate(Song song, int rating);

    void addListener(PlaybackListener listener);

    void removeListener(PlaybackListener listener);
}
