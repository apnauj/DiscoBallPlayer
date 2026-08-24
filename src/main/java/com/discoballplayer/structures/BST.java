package com.discoballplayer.structures;

/**
 * Hand-written binary search tree ordered by the natural ordering of {@code T}.
 *
 * <p>Nodes keep a {@code parent} reference. That is not decoration: in-order successor and
 * predecessor are computed by walking up through it, which is what lets
 * {@code AlphabeticalMode} step forward and backward without ever flattening the tree
 * into a list.</p>
 *
 * <p>Duplicates are rejected. {@code Song.compareTo} breaks ties on the generated id, so two
 * songs sharing a title are still two distinct nodes.</p>
 */
public class BST<T extends Comparable<T>> {

    private static class Node<T extends Comparable<T>> {
        T value;
        Node<T> left, right, parent;

        Node(T value, Node<T> parent) {
            this.value = value;
            this.parent = parent;
        }
    }

    private Node<T> root;

    /**
     * Inserts a value, ignoring it if an equal value is already present.
     *
     * @implNote Time complexity: O(log n) average, O(n) worst case.
     */
    public void insert(T value) {
        root = insertRecursive(root, value, null);
    }

    private Node<T> insertRecursive(Node<T> current, T value, Node<T> parent) {
        if (current == null) {
            return new Node<>(value, parent);
        }

        int comparison = value.compareTo(current.value);
        if (comparison < 0) {
            current.left = insertRecursive(current.left, value, current);
        } else if (comparison > 0) {
            current.right = insertRecursive(current.right, value, current);
        }

        return current;
    }

    /**
     * @implNote Time complexity: O(1).
     */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * @implNote Time complexity: O(log n) average, O(n) worst case.
     */
    public void delete(T value) {
        root = deleteRecursive(root, value);
        if (root != null) {
            root.parent = null;
        }
    }

    private Node<T> deleteRecursive(Node<T> current, T value) {
        if (current == null) {
            return null;
        }

        int comparison = value.compareTo(current.value);
        if (comparison < 0) {
            current.left = deleteRecursive(current.left, value);
            if (current.left != null) {
                current.left.parent = current;
            }
        } else if (comparison > 0) {
            current.right = deleteRecursive(current.right, value);
            if (current.right != null) {
                current.right.parent = current;
            }
        } else {
            if (current.left == null) {
                return current.right;
            }
            if (current.right == null) {
                return current.left;
            }

            T successor = findMinimum(current.right);
            current.value = successor;
            current.right = deleteRecursive(current.right, successor);
            if (current.right != null) {
                current.right.parent = current;
            }
        }

        return current;
    }

    private T findMinimum(Node<T> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node.value;
    }
}
