package com.discoballplayer.playback;

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
