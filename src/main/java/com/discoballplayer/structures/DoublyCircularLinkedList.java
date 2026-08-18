package com.discoballplayer.structures;

import java.util.Iterator;
import java.util.NoSuchElementException;

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


}

