package com.discoballplayer.structures;

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

    public boolean isEmpty(){
        return size == 0;
    }

    public int size(){
        return size;
    }

    /*
     * Insert an element at the end of the list
     * We use the tail reference that we have
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
    }

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
    }

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
        return true;
    }

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
     * Inserting or deleting while a cursor is live leaves it pointing at a detached node.
     * Playback modes rebuild their structure on {@code load()}, so they never hit this;
     * fail-fast detection is ticket {@code A1-11}.</p>
     */
    public final class Cursor {

        private Node node;

        private Cursor(Node start) {
            this.node = start;
        }

        /**
         * @return the element the cursor currently points at, without moving
         * @implNote Time complexity: O(1).
         */
        public T current() {
            return node.data;
        }

        /**
         * Advances one position, wrapping from the last element to the first.
         *
         * @return the element landed on
         * @implNote Time complexity: O(1).
         */
        public T next() {
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
