package com.discoballplayer.playback;

import java.util.Random;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.exception.SongNotFoundException;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.structures.DoublyCircularLinkedList;

/**
 * Random playback over a {@link DoublyCircularLinkedList}.
 *
 * <p><strong>The order is randomized once, when the mode is loaded — never on each
 * {@link #next()}.</strong> Shuffling per call would make {@link #previous()} meaningless:
 * stepping back would land on a song that was never played. Randomizing the insertion order
 * instead gives a fixed ring that can be walked in both directions.</p>
 *
 * <p>The ring is circular, so navigation never ends. {@link #hasNext()} and
 * {@link #hasPrevious()} are true for any non-empty library, and past the last song the cursor
 * wraps to the first.</p>
 */
public class ShuffleMode extends AbstractPlaybackMode {

    private final Random random;
    private DoublyCircularLinkedList<Song> ring = new DoublyCircularLinkedList<>();
    private DoublyCircularLinkedList<Song>.Cursor cursor;

    public ShuffleMode() {
        this(new Random());
    }

    /**
     * Seeded constructor, so a test can assert on a specific shuffled order instead of
     * asserting that "something changed" and hoping.
     */
    public ShuffleMode(Random random) {
        this.random = random;
    }

    @Override
    protected void loadStructure(MusicLibrary library) {
        ring = new DoublyCircularLinkedList<>();
        cursor = null;

        Song[] songs = library.getAllSongs().toArray(new Song[0]);
        shuffle(songs);
        for (Song song : songs) {
            ring.insertAtEnd(song);
        }
    }

    /**
     * Fisher-Yates over a plain array. Every permutation is equally likely and it runs in
     * O(n); {@code Collections.shuffle} is off limits inside the playback layer.
     */
    private void shuffle(Song[] songs) {
        for (int i = songs.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Song swap = songs[i];
            songs[i] = songs[j];
            songs[j] = swap;
        }
    }

    /**
     * Adds to the ring without re-randomizing the songs already in it.
     *
     * @implNote Time complexity: O(1) to insert, O(n) to re-seat the cursor.
     */
    @Override
    protected void insertIntoStructure(Song song) {
        ring.insertAtEnd(song);
    }

    /**
     * Any insertion invalidates a live cursor, so this walks back to the song being played.
     */
    @Override
    protected void reseat() {
        Song playing = current();
        cursor = null;
        if (playing != null) {
            jumpTo(playing);
        }
    }

    @Override
    public Song next() {
        if (ring.isEmpty()) {
            throw new EmptyStructureException("No songs loaded in shuffle mode.");
        }
        if (cursor == null) {
            cursor = ring.cursor();
            return moveTo(cursor.current());
        }
        return moveTo(cursor.next());
    }

    @Override
    public Song previous() {
        if (ring.isEmpty()) {
            throw new EmptyStructureException("No songs loaded in shuffle mode.");
        }
        if (cursor == null) {
            cursor = ring.cursor();
            return moveTo(cursor.current());
        }
        return moveTo(cursor.previous());
    }

    /**
     * @return true for any non-empty library; a circular ring has no end
     */
    @Override
    public boolean hasNext() {
        return !ring.isEmpty();
    }

    /**
     * @return true for any non-empty library; a circular ring has no beginning either
     */
    @Override
    public boolean hasPrevious() {
        return !ring.isEmpty();
    }

    /**
     * Walks the ring until it lands on {@code song}, leaving the cursor there.
     *
     * <p>The shuffled order is untouched: jumping repositions the cursor, it does not
     * re-randomize. Stepping forward from here continues the same permutation, which is what
     * makes Previous still mean something afterwards.</p>
     *
     * @implNote Time complexity: O(n). A ring has no index.
     */
    @Override
    public Song jumpTo(Song song) {
        if (ring.isEmpty()) {
            throw new EmptyStructureException("No songs loaded in shuffle mode.");
        }
        DoublyCircularLinkedList<Song>.Cursor candidate = ring.cursor();
        for (int step = 0; step < ring.size(); step++) {
            if (candidate.current().equals(song)) {
                cursor = candidate;
                return moveTo(song);
            }
            candidate.next();
        }
        throw new SongNotFoundException("Not in the shuffled ring: " + song.getTitle());
    }

    @Override
    public boolean canJumpTo() {
        return true;
    }

    @Override
    public String displayName() {
        return "Shuffle";
    }
}
