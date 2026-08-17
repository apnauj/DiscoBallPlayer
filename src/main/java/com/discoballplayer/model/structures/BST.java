package main.java.com.discoballplayer.model.structures;

public class BST <T extends Comparable<T>> {

    private static class Node<T extends Comparable<T>>{
        T value;
        Node<T> left, rigth;
        public Node(T value){
            this.value = value;
        }
    }

    Node<T> root;

    public void insert(T value){
        root = insertr(root, value);
    }

    private Node insertr(Node<T> current, T value){
        if(current == null){
            return new Node<T>(value);
        }

        else if (value.compareTo(current.value) < 0){
            current.left = insertr(current.rigth, value);
        }

        else if (value.compareTo(current.value) > 0){
            current.rigth = insertr(current.rigth, value);
        }

        return current;

    }
}
