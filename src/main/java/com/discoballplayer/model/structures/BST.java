package main.java.com.discoballplayer.model.structures;

import java.util.ArrayList;
import java.util.List;

public class BST <T extends Comparable<T>> {

    private static class Node<T extends Comparable<T>>{
        T value;
        Node<T> left, rigth, parent;
        public Node(T value, Node<T> parent){
            this.value = value;
            this.parent = parent;
        }
    }

    Node<T> root;

    public void insert(T value){

        root = insertr(root, value, null);
    }

    private Node insertr(Node<T> current, T value, Node<T> parent){
        if(current == null){
            return new Node(value,parent);
        }

        else if (value.compareTo(current.value) < 0){
            current.left = insertr(current.rigth, value, current);
        }

        else if (value.compareTo(current.value) > 0){
            current.rigth = insertr(current.rigth, value, current);
        }

        return current;

    }

    public boolean isEmpty() {
        return root == null;
    }
}
