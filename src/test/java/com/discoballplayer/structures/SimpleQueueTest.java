package com.discoballplayer.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.discoballplayer.exception.EmptyStructureException;

/**
 * Flat by convention: under {@code @Nested} the outer class matches {@code -Dtest=Class#method}
 * and runs zero tests, so a verification command would report success while testing nothing.
 */
class SimpleQueueTest {

    private static SimpleQueue<String> queueOf(String... values) {
        SimpleQueue<String> queue = new SimpleQueue<>();
        for (String value : values) {
            queue.enqueue(value);
        }
        return queue;
    }

    private static List<String> drain(SimpleQueue<String> queue) {
        List<String> seen = new ArrayList<>();
        while (!queue.isEmpty()) {
            seen.add(queue.dequeue());
        }
        return seen;
    }

    // ---------- FIFO ordering ----------

    @Test
    void preservesFifoOrder() {
        assertEquals(List.of("A", "B", "C"), drain(queueOf("A", "B", "C")));
    }

    @Test
    void preservesFifoOrderAcrossManyEnqueues() {
        SimpleQueue<String> queue = new SimpleQueue<>();
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String value = "song-" + i;
            queue.enqueue(value);
            expected.add(value);
        }

        assertEquals(50, queue.size());
        assertEquals(expected, drain(queue));
    }

    @Test
    void interleavedEnqueueAndDequeueKeepArrivalOrder() {
        SimpleQueue<String> queue = queueOf("A", "B");

        assertEquals("A", queue.dequeue());
        queue.enqueue("C");
        assertEquals("B", queue.dequeue());
        queue.enqueue("D");

        assertEquals(List.of("C", "D"), drain(queue));
    }

    // ---------- emptiness ----------

    @Test
    void dequeueOnEmptyQueueThrows() {
        assertThrows(EmptyStructureException.class, () -> new SimpleQueue<String>().dequeue());
    }

    @Test
    void peekOnEmptyQueueThrows() {
        assertThrows(EmptyStructureException.class, () -> new SimpleQueue<String>().peek());
    }

    @Test
    void exhaustedQueueThrowsRatherThanReturningNull() {
        SimpleQueue<String> queue = queueOf("A");

        assertEquals("A", queue.dequeue());
        assertThrows(EmptyStructureException.class, queue::dequeue);
    }

    @Test
    void newQueueIsEmpty() {
        SimpleQueue<String> queue = new SimpleQueue<>();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    // ---------- peek ----------

    @Test
    void peekDoesNotRemove() {
        SimpleQueue<String> queue = queueOf("A", "B");

        assertEquals("A", queue.peek());
        assertEquals("A", queue.peek());
        assertEquals(2, queue.size());
        assertEquals("A", queue.dequeue());
    }

    @Test
    void peekFollowsTheFrontAsItMoves() {
        SimpleQueue<String> queue = queueOf("A", "B", "C");

        assertEquals("A", queue.peek());
        queue.dequeue();
        assertEquals("B", queue.peek());
        queue.dequeue();
        assertEquals("C", queue.peek());
    }

    // ---------- size ----------

    @Test
    void sizeTracksEnqueueAndDequeue() {
        SimpleQueue<String> queue = new SimpleQueue<>();

        for (int i = 1; i <= 5; i++) {
            queue.enqueue("value-" + i);
            assertEquals(i, queue.size());
        }
        for (int expected = 4; expected >= 0; expected--) {
            queue.dequeue();
            assertEquals(expected, queue.size());
        }
        assertTrue(queue.isEmpty());
    }

    @Test
    void drainThenRefillReusesTheQueue() {
        SimpleQueue<String> queue = queueOf("A", "B");

        assertEquals(List.of("A", "B"), drain(queue));
        assertTrue(queue.isEmpty());

        queue.enqueue("C");
        queue.enqueue("D");

        assertEquals(2, queue.size());
        assertEquals(List.of("C", "D"), drain(queue),
                "after draining, the tail must not still point at a detached node");
    }

    // ---------- validation ----------

    @Test
    void enqueueRejectsNullElements() {
        SimpleQueue<String> queue = new SimpleQueue<>();

        assertThrows(NullPointerException.class, () -> queue.enqueue(null));
        assertTrue(queue.isEmpty());
    }
}
