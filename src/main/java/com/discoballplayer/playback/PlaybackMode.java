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
     * Repositions the mode onto {@code song}, so a user can play what they clicked.
     *
     * @throws UnsupportedOperationException in modes that cannot reposition. Arrival order is
     *         one: honouring a jump would mean discarding everything queued ahead of the
     *         target, which is not FIFO any more.
     * @throws com.discoballplayer.exception.SongNotFoundException if the song is not in this
     *         mode's structure
     */
    Song jumpTo(Song song);

    /**
     * @return whether {@link #jumpTo} is supported; the UI enables click-to-play from this
     *         value rather than naming the mode
     */
    boolean canJumpTo();

    /**
     * @return the human-readable name shown in the mode selector
     */
    String displayName();
}
