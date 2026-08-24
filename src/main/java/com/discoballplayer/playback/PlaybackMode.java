package com.discoballplayer.playback;

import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/**
 * A strategy for walking the library, each implementation backed by a different
 * hand-written structure.
 *
 * <p>An implementation builds its own structure from the library on {@link #load}, so a mode
 * that consumes as it goes (arrival order) never damages the catalogue.</p>
 */
public interface PlaybackMode {

    /**
     * Builds this mode's structure from the library, discarding any previous state.
     */
    void load(MusicLibrary library);

    /**
     * @throws com.discoballplayer.exception.EmptyStructureException when nothing is left to play
     */
    Song next();

    /**
     * @throws UnsupportedOperationException in modes that cannot go back, such as arrival order
     */
    Song previous();

    boolean hasNext();

    /**
     * @return {@code false} in modes that cannot go back; the UI disables its Previous button
     *         from this value rather than hard-coding the mode
     */
    boolean hasPrevious();

    /**
     * @return the song currently pointed at, or {@code null} before the first {@link #next()}
     */
    Song current();

    /**
     * @return the human-readable name shown in the mode selector
     */
    String displayName();
}
