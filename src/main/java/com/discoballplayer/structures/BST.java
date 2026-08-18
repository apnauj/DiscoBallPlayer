package main.java.com.discoballplayer.structures;

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

    private Node<T> insertr(Node<T> current, T value, Node<T> parent){
        if(current == null){
            return new Node<>(value,parent);
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


    public Node<T> search(T value){
        return searchr(this.root, value);
    }


    private Node<T> searchr(Node<T> current, T value){
        if(current == null){
            return null;

        }

        if(value.compareTo(current.value)<0){
            return searchr(current.left, value);
        }

        else if (value.compareTo(current.value) > 0){
            return searchr(current.rigth, value);
        }

        return current;
    }

    public void delete(T value){
        root = deleter(root, value);
    }

    private Node<T> deleter(Node<T> current, T value){

        if(current == null){
            return null;
        }

        if(value.compareTo(current.value) < 0){
            current.left = deleter(current.left, value);
        }

        else if (value.compareTo(current.value) > 0){
            current.rigth = deleter(current.rigth, value);
        }

        else{
            if(current.left == null){
                return current.rigth;
            }

            if (current.rigth == null){
                return current.left;
            }

            T succesor = findMinimun(current.rigth);

            current.value = succesor;

            current.rigth = findMinimun(current.rigth, succesor);


        }

        return current;
    }


    private T findMinimun(Node<T> node){
        while(node.left != null ){
            node = node.left;
        }
        return node.value;
    }


}
