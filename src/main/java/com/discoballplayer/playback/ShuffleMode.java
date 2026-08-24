package com.discoballplayer.playback;

import java.util.Random;

import com.discoballplayer.exception.EmptyStructureException;
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

    @Override
    public String displayName() {
        return "Shuffle";
    }
}
