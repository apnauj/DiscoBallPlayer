package com.discoballplayer.structures;

import com.discoballplayer.exception.EmptyStructureException;

/**
 * Hand-written FIFO queue: elements leave in exactly the order they arrived.
 *
 * <p>Backs {@code ArrivalMode}. Holding both a {@code head} and a {@code tail} reference is
 * what keeps every operation O(1): without the tail, {@code enqueue} would have to walk the
 * whole chain to find the end.</p>
 *
 * <p>A dequeued element is gone. That is the point of the mode, not a limitation — the mode
 * rebuilds its queue from the library on {@code load()}, so consuming it never damages the
 * catalogue.</p>
 *
 * @param <T> the element type; unlike {@link BST} it needs no ordering, since arrival order
 *            is the only order a queue knows
 */
public class SimpleQueue<T> {

    private static class Node<T> {
        final T data;
        Node<T> next;

        Node(T data) {
            this.data = data;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /**
     * Adds an element at the back of the queue.
     *
     * @throws NullPointerException if {@code data} is null, matching the other structures:
     *         a null element would be indistinguishable from an empty queue at the front
     * @implNote Time complexity: O(1).
     */
    public void enqueue(T data) {
        if (data == null) {
            throw new NullPointerException("null elements not allowed");
        }
        Node<T> newNode = new Node<>(data);
        if (isEmpty()) {
            head = newNode;
        } else {
            tail.next = newNode;
        }
        tail = newNode;
        size++;
    }

    /**
     * Removes and returns the element at the front.
     *
     * @throws EmptyStructureException if the queue is empty
     * @implNote Time complexity: O(1).
     */
    public T dequeue() {
        if (isEmpty()) {
            throw new EmptyStructureException("Cannot dequeue from an empty queue.");
        }
        T data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return data;
    }

    /**
     * Returns the element at the front without removing it.
     *
     * @throws EmptyStructureException if the queue is empty
     * @implNote Time complexity: O(1).
     */
    public T peek() {
        if (isEmpty()) {
            throw new EmptyStructureException("Cannot peek into an empty queue.");
        }
        return head.data;
    }

    /**
     * @implNote Time complexity: O(1).
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * @implNote Time complexity: O(1).
     */
    public int size() {
        return size;
    }
}
