package com.discoballplayer.playback;

import com.discoballplayer.exception.EmptyStructureException;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.structures.SimpleQueue;

/**
 * Strict first-in, first-out playback over a {@link SimpleQueue}.
 *
 * <p>A song leaves the queue when it plays and does not come back. There is no way to go
 * back, so {@link #previous()} throws and {@link #hasPrevious()} is always false — the UI
 * disables its Previous button from that value rather than special-casing the mode. That
 * greyed-out button is the visible proof of FIFO behaviour during the defense.</p>
 *
 * <p>The queue is filled from the library on load, so draining it never touches the
 * catalogue: switching away and back refills it in full.</p>
 */
public class ArrivalMode extends AbstractPlaybackMode {

    private SimpleQueue<Song> queue = new SimpleQueue<>();

    @Override
    protected void loadStructure(MusicLibrary library) {
        queue = new SimpleQueue<>();
        for (Song song : library.getAllSongs()) {
            queue.enqueue(song);
        }
    }

    /**
     * A song that arrives later goes to the back, which is what arrival order means.
     *
     * @implNote Time complexity: O(1).
     */
    @Override
    protected void insertIntoStructure(Song song) {
        queue.enqueue(song);
    }

    /**
     * Dequeues the next song permanently.
     *
     * @throws EmptyStructureException once the queue is drained; unlike shuffle, arrival order
     *         has a real end
     */
    @Override
    public Song next() {
        if (queue.isEmpty()) {
            throw new EmptyStructureException("The arrival queue is empty.");
        }
        return moveTo(queue.dequeue());
    }

    /**
     * Always fails. A queue has no memory of what it handed out.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public Song previous() {
        throw new UnsupportedOperationException(
                "Arrival order is FIFO: a played song has left the queue.");
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    /**
     * @return always {@code false}
     */
    @Override
    public boolean hasPrevious() {
        return false;
    }

    /**
     * @return how many songs are still queued, for the UI to show what is left
     */
    public int remaining() {
        return queue.size();
    }

    @Override
    public String displayName() {
        return "Arrival order";
    }
}
