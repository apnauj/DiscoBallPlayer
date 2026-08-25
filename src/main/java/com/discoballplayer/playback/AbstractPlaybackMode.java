package com.discoballplayer.playback;

import java.util.Objects;

import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;

/**
 * Shared state for the three playback modes.
 *
 * <p>This is the one place in the project where inheritance is genuinely warranted: all three
 * modes hold a current song and a library reference, and two of the three navigate backward
 * identically. {@code ArrivalMode} overrides only what actually differs. Do not force this
 * pattern anywhere else.</p>
 *
 * <p>Subclasses build their own structure in {@link #loadStructure(MusicLibrary)}. The
 * catalogue itself is never consumed: a mode reads the library and fills its own container,
 * which is why arrival order can drain its queue without emptying the library.</p>
 */
public abstract class AbstractPlaybackMode implements PlaybackMode {

    private MusicLibrary library;
    private Song current;

    @Override
    public final void load(MusicLibrary library) {
        this.library = library;
        this.current = null;
        loadStructure(library);
    }

    /**
     * Fills this mode's structure from the library, discarding any previous state.
     *
     * <p>Called by {@link #load}, which has already reset {@link #current()}. Implementations
     * must tolerate an empty library rather than throwing here — emptiness surfaces on
     * {@link #next()}, when the caller actually asks for a song.</p>
     */
    protected abstract void loadStructure(MusicLibrary library);

    /**
     * Inserts the song into this mode's structure and repairs any cursor the insertion
     * invalidated, keeping {@link #current()} where it was.
     */
    @Override
    public final void add(Song song) {
        Objects.requireNonNull(song, "The song can't be null.");
        insertIntoStructure(song);
        reseat();
    }

    /**
     * Places {@code song} where this mode's structure says it belongs.
     *
     * <p>Called by {@link #add}. Implementations insert only; repositioning is {@link #reseat}.</p>
     */
    protected abstract void insertIntoStructure(Song song);

    /**
     * Re-seats a cursor that the insertion invalidated.
     *
     * <p>Does nothing by default, which is right for a mode that navigates without a cursor.
     * A mode holding one overrides this, because a structural change leaves a live cursor
     * pointing at a detached node.</p>
     */
    protected void reseat() {
    }

    /**
     * @return the library this mode was last loaded from, or {@code null} before the first
     *         {@link #load}
     */
    protected final MusicLibrary library() {
        return library;
    }

    @Override
    public final Song current() {
        return current;
    }

    /**
     * Records the song a subclass has navigated to. The only way {@code current} changes.
     */
    protected final Song moveTo(Song song) {
        this.current = song;
        return song;
    }

    /**
     * Refused by default. A mode that can reposition says so by overriding both this and
     * {@link #canJumpTo()}, which keeps the two answers from drifting apart.
     */
    @Override
    public Song jumpTo(Song song) {
        throw new UnsupportedOperationException(displayName() + " cannot jump to a song.");
    }

    @Override
    public boolean canJumpTo() {
        return false;
    }

    /**
     * @return {@code true} once {@link #load} has run on a library holding at least one song
     */
    protected final boolean isLoaded() {
        return library != null && !library.isEmpty();
    }
}
