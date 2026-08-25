package com.discoballplayer.structures;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.discoballplayer.exception.EmptyStructureException;

public class DoublyCircularLinkedList<T> implements Iterable<T> {
    private class Node {
        T data;
        Node next;
        Node prev;

        public Node(T data) {
            this.data = data;
            this.next = null;
            this.prev = null;
        }
    }

    public DoublyCircularLinkedList() {
        this.tail = null;
        this.size = 0;
    }

    private Node tail;
    private int size;

    /**
     * Bumped on every structural change, so a live {@link Cursor} can tell it is walking a
     * list that no longer exists. Without it a stale cursor keeps traversing detached nodes
     * and returns plausible-looking wrong answers instead of failing.
     */
    private int modificationCount;

    /**
     * @implNote Time complexity: O(1). The size is a maintained counter, not a walk.
     */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * @implNote Time complexity: O(1).
     */
    public int size(){
        return size;
    }

    /**
     * Inserts an element at the tail of the ring.
     *
     * @throws NullPointerException if {@code data} is null
     * @implNote Time complexity: O(1). The list holds a tail reference, so appending never
     *           walks the chain. Without it this would be O(n).
     */
    public void insertAtEnd(T data){
        if (data == null) throw new NullPointerException("null elements not allowed");
        Node newNode = new Node(data);
        if(isEmpty()){
            tail = newNode;
            tail.next = tail;
            tail.prev = tail;
        } else {
            newNode.next = tail.next;
            tail.next.prev = newNode;
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
        modificationCount++;
    }

    /**
     * Inserts an element at the head of the ring, leaving the tail where it was.
     *
     * @throws NullPointerException if {@code data} is null; a null element could not be told
     *         apart from an absent one by {@link #has}
     * @implNote Time complexity: O(1). {@code tail.next} is the head, so no walk is needed.
     */
    public void insertAtBeginning(T data){
        if (data == null) throw new NullPointerException("null elements not allowed");
        Node newNode = new Node(data);
        if(isEmpty()){
            tail = newNode;
            tail.next = tail;
            tail.prev = tail;
        } else {
            newNode.next = tail.next;
            tail.next.prev = newNode;
            tail.next = newNode;
            newNode.prev = tail;
        }
        size++;
        modificationCount++;
    }

    /**
     * Removes the first element equal to {@code data}.
     *
     * @return whether anything was removed
     * @throws NullPointerException if {@code data} is null
     * @implNote Time complexity: O(n). Finding the node is a linear scan — a linked list has
     *           no index to exploit — while the unlinking itself is O(1), because every node
     *           knows its predecessor. This runs when a user deletes a song by hand, not
     *           during playback.
     */
    public boolean delete(T data) {
        if (data == null) throw new NullPointerException("null elements not allowed");
        if (isEmpty()) return false;

        Node curr = tail.next; // head
        Node last = tail;

        // Continue while we haven't gone through all of it
        while (!curr.data.equals(data) && curr != tail) {
            last = curr;
            curr = curr.next;
        }

        if (!curr.data.equals(data)) {
            return false; // it wasn't in the list
        }

        if (curr == last) {
            tail = null; // only node in the list
        } else {
            curr.next.prev = last;
            last.next = curr.next;
            if (curr == tail) {
                tail = last;
            }
        }

        size--;
        modificationCount++;
        return true;
    }

    /**
     * @return whether an element equal to {@code data} is in the list
     * @throws NullPointerException if {@code data} is null
     * @implNote Time complexity: O(n). There is no ordering to exploit.
     */
    public boolean has(T data) {
        if (data == null) throw new NullPointerException("null elements not allowed");
        if (isEmpty()) return false;

        Node curr = tail.next; // head
        do {
            if (curr.data.equals(data)) return true;
            curr = curr.next;
        } while (curr != tail.next);

        return false;
    }

    /**
     * Iterates the ring once, from the head, stopping after {@link #size()} elements.
     *
     * <p>Bounded on purpose: the list is circular, so an iterator that stopped only on a null
     * link would never stop at all. Playback navigates through {@link #cursor()} instead,
     * which is unbounded by design.</p>
     *
     * @implNote Time complexity: O(1) per step, O(n) for a full pass.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node curr = isEmpty() ? null : tail.next;
            private int visited = 0;

            public boolean hasNext() { return visited < size; }

            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T data = curr.data;
                curr = curr.next;
                visited++;
                return data;
            }
        };
    }

    /**
     * @throws IndexOutOfBoundsException if {@code index} is outside the list
     * @implNote Time complexity: O(n), about n/2 steps. Walks from whichever end is closer,
     *           which halves the constant but not the complexity class.
     */
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        Node curr;
        if (index < size / 2) {
            curr = tail.next; // head, 0 index
            for (int i = 0; i < index; i++) {
                curr = curr.next;
            }
        } else {
            curr = tail; // index size - 1
            for (int i = size - 1; i > index; i--) {
                curr = curr.prev;
            }
        }
        return curr.data;
    }

    /**
     * A movable pointer into the ring, used to walk the list forward and backward without
     * exposing {@code Node}.
     *
     * <p>{@code ShuffleMode} navigates through this. Because the list is circular there is no
     * end: past the last element the cursor lands on the first, and before the first it lands
     * on the last. {@link #next()} followed by {@link #previous()} always returns to the
     * element it started on, which is what makes a Previous button meaningful.</p>
     *
     * <p><strong>A cursor is invalidated by any structural change to the list.</strong>
     * Inserting or deleting while a cursor is live leaves it pointing at a detached node, so
     * every method throws {@link ConcurrentModificationException} rather than returning a
     * plausible-looking wrong answer from a part of the list nothing can reach any more.</p>
     */
    public final class Cursor {

        private Node node;
        private final int expectedModificationCount;

        private Cursor(Node start) {
            this.node = start;
            this.expectedModificationCount = modificationCount;
        }

        /**
         * @throws ConcurrentModificationException if the list changed since this cursor opened
         */
        private void checkNotStale() {
            if (expectedModificationCount != modificationCount) {
                throw new ConcurrentModificationException(
                        "The list changed after this cursor was opened; reopen it.");
            }
        }

        /**
         * @return the element the cursor currently points at, without moving
         * @implNote Time complexity: O(1).
         */
        public T current() {
            checkNotStale();
            return node.data;
        }

        /**
         * Advances one position, wrapping from the last element to the first.
         *
         * @return the element landed on
         * @implNote Time complexity: O(1).
         */
        public T next() {
            checkNotStale();
            node = node.next;
            return node.data;
        }

        /**
         * Steps back one position, wrapping from the first element to the last.
         *
         * @return the element landed on
         * @implNote Time complexity: O(1).
         */
        public T previous() {
            checkNotStale();
            node = node.prev;
            return node.data;
        }
    }

    /**
     * Opens a cursor positioned on the first element.
     *
     * @throws EmptyStructureException if the list is empty; an empty ring has no position to
     *         point at, and returning {@code null} would push that check onto every caller
     * @implNote Time complexity: O(1).
     */
    public Cursor cursor() {
        if (isEmpty()) {
            throw new EmptyStructureException("Cannot open a cursor on an empty list.");
        }
        return new Cursor(tail.next);
    }
}
