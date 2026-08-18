package main.java.com.discoballplayer.structures;

public class CircularList <T> {
    private class Node {
        T data;
        Node next;

        public Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    public CircularList() {
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
        Node newNode = new Node(data);
        if(isEmpty()){
            tail = newNode;
            tail.next = tail;
        } else {
            newNode.next = tail.next;
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    public void insertAtBeginning(T data){
        Node newNode = new Node(data);
        if(isEmpty()){
            tail = newNode;
            tail.next = tail;
        } else {
            newNode.next = tail.next;
            tail.next = newNode;
        }
        size++;
    }

    public boolean delete(T data) {
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
            last.next = curr.next;
            if (curr == tail) {
                tail = last;
            }
        }

        size--;
        return true;
    }

    public boolean has(T data) {
        if (isEmpty()) return false;

        Node curr = tail.next; // head
        do {
            if (curr.data.equals(data)) return true;
            curr = curr.next;
        } while (curr != tail.next);

        return false;
    }


}
