package com.discoballplayer.playback;

import java.util.NoSuchElementException;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.exception.SongNotFoundException;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.structures.BST;

/**
 * Alphabetical playback over a {@link BST}, ordered by {@code Song.compareTo}: title first,
 * case-insensitive, ties broken on the generated id so two songs sharing a title are still two
 * distinct nodes.
 *
 * <p>Navigation follows the in-order walk through the tree's cursor, which steps via in-order
 * successor and predecessor using parent pointers. <strong>The tree is never flattened into a
 * list.</strong> That keeps traversal O(1) in space and is the property the exercise is
 * actually about.</p>
 *
 * <p>Unlike the circular ring, this sequence has ends: {@link #hasNext()} is false on the last
 * title and {@link #hasPrevious()} is false on the first.</p>
 */
public class AlphabeticalMode extends AbstractPlaybackMode {

    private BST<Song> tree = new BST<>();
    private BST<Song>.Cursor cursor;

    @Override
    protected void loadStructure(MusicLibrary library) {
        tree = new BST<>();
        cursor = null;
        for (Song song : library.getAllSongs()) {
            tree.insert(song);
        }
    }

    /**
     * @throws EmptyStructureException if nothing was loaded
     * @throws NoSuchElementException past the last title
     */
    @Override
    public Song next() {
        if (cursor == null) {
            return moveTo(openCursor().current());
        }
        return moveTo(cursor.next());
    }

    /**
     * @throws EmptyStructureException if nothing was loaded
     * @throws NoSuchElementException before the first title
     */
    @Override
    public Song previous() {
        if (cursor == null) {
            return moveTo(openCursor().current());
        }
        return moveTo(cursor.previous());
    }

    private BST<Song>.Cursor openCursor() {
        if (tree.isEmpty()) {
            throw new EmptyStructureException("No songs loaded in alphabetical mode.");
        }
        cursor = tree.cursor();
        return cursor;
    }

    /**
     * @return false once the cursor sits on the last title; the walk has a real end
     */
    @Override
    public boolean hasNext() {
        if (tree.isEmpty()) {
            return false;
        }
        return cursor == null || cursor.hasNext();
    }

    /**
     * @return false before the first {@link #next()} and on the first title
     */
    @Override
    public boolean hasPrevious() {
        return cursor != null && cursor.hasPrevious();
    }

    /**
     * Repositions onto {@code song} by walking the in-order sequence to it.
     *
     * <p>The tree can find the node in O(log n), but the cursor is the only way in and it
     * exposes no seek — deliberately, since handing out nodes would let callers reach into the
     * structure. Stepping is O(n) overall and runs once per click, which is the right trade
     * against widening the structure's API.</p>
     *
     * @implNote Time complexity: O(n) steps, each O(log n) in the worst case.
     */
    @Override
    public Song jumpTo(Song song) {
        if (tree.isEmpty()) {
            throw new EmptyStructureException("No songs loaded in alphabetical mode.");
        }
        if (!tree.contains(song)) {
            throw new SongNotFoundException("Not in the tree: " + song.getTitle());
        }
        BST<Song>.Cursor candidate = tree.cursor();
        while (!candidate.current().equals(song) && candidate.hasNext()) {
            candidate.next();
        }
        cursor = candidate;
        return moveTo(candidate.current());
    }

    @Override
    public boolean canJumpTo() {
        return true;
    }

    @Override
    public String displayName() {
        return "Alphabetical";
    }
}
