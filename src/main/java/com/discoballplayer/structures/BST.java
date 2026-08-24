package com.discoballplayer.structures;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import com.discoballplayer.exception.EmptyStructureException;

/**
 * Hand-written binary search tree ordered by the natural ordering of {@code T}.
 *
 * <p>Nodes keep a {@code parent} reference. That is not decoration: in-order successor and
 * predecessor are found by walking up through it, which is what lets {@code AlphabeticalMode}
 * step forward and backward <strong>without ever flattening the tree into a list</strong>.
 * Flattening would make the traversal O(n) in space and would defeat the point of the
 * exercise.</p>
 *
 * <p>Duplicates are rejected. {@code Song.compareTo} breaks ties on the generated id, so two
 * songs sharing a title are still two distinct nodes and neither is swallowed.</p>
 *
 * <p>The tree is unbalanced by design. Inserting already-sorted input degrades it to a chain,
 * which is why every complexity note below carries an O(n) worst case.</p>
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
    private int size;

    /** Bumped on every structural change, so a live {@link Cursor} can detect staleness. */
    private int modificationCount;

    // ---------- modification ----------

    /**
     * Inserts a value, ignoring it if an equal value is already present.
     *
     * @throws NullPointerException if {@code value} is null
     * @implNote Time complexity: O(log n) average, O(n) worst case.
     */
    public void insert(T value) {
        if (value == null) {
            throw new NullPointerException("null elements not allowed");
        }
        root = insertRecursive(root, value, null);
    }

    private Node<T> insertRecursive(Node<T> current, T value, Node<T> parent) {
        if (current == null) {
            size++;
            modificationCount++;
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
     * Removes a value if present, leaving the tree untouched otherwise.
     *
     * <p>A node with two children is not unlinked. Its value is overwritten with its in-order
     * successor and the successor's original node is removed instead, which is the only case
     * where a live {@code Cursor} can observe a value change under it.</p>
     *
     * @implNote Time complexity: O(log n) average, O(n) worst case.
     */
    public void delete(T value) {
        if (value == null) {
            return;
        }
        int before = size;
        root = deleteRecursive(root, value);
        if (root != null) {
            root.parent = null;
        }
        if (size != before) {
            modificationCount++;
        }
    }

    private Node<T> deleteRecursive(Node<T> current, T value) {
        if (current == null) {
            return null;
        }

        int comparison = value.compareTo(current.value);
        if (comparison < 0) {
            current.left = deleteRecursive(current.left, value);
            reparent(current.left, current);
        } else if (comparison > 0) {
            current.right = deleteRecursive(current.right, value);
            reparent(current.right, current);
        } else {
            if (current.left == null) {
                size--;
                return current.right;
            }
            if (current.right == null) {
                size--;
                return current.left;
            }
            T successor = findMinimum(current.right);
            current.value = successor;
            current.right = deleteRecursive(current.right, successor);
            reparent(current.right, current);
        }
        return current;
    }

    /**
     * Re-hangs a subtree under its new parent. Skipping this is the classic way to leave
     * dangling parent pointers behind a delete, and dangling parents break traversal silently
     * rather than loudly.
     */
    private void reparent(Node<T> child, Node<T> parent) {
        if (child != null) {
            child.parent = parent;
        }
    }

    // ---------- lookup ----------

    /**
     * @implNote Time complexity: O(log n) average, O(n) worst case.
     */
    public boolean contains(T value) {
        return findNode(value) != null;
    }

    /**
     * Returns the stored element equal to {@code value}.
     *
     * <p>Not redundant with the argument: ordering may consider only part of the element, so
     * the stored instance can carry more than the probe used to look it up.</p>
     *
     * @throws NoSuchElementException if no equal element is present; the structures never
     *         return null to report absence
     * @implNote Time complexity: O(log n) average, O(n) worst case.
     */
    public T find(T value) {
        Node<T> node = findNode(value);
        if (node == null) {
            throw new NoSuchElementException("No element equal to: " + value);
        }
        return node.value;
    }

    private Node<T> findNode(T value) {
        Node<T> current = root;
        while (current != null) {
            int comparison = value.compareTo(current.value);
            if (comparison < 0) {
                current = current.left;
            } else if (comparison > 0) {
                current = current.right;
            } else {
                return current;
            }
        }
        return null;
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

    // ---------- in-order traversal ----------

    private Node<T> findMinimumNode(Node<T> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private T findMinimum(Node<T> node) {
        return findMinimumNode(node).value;
    }

    private Node<T> findMaximumNode(Node<T> node) {
        while (node.right != null) {
            node = node.right;
        }
        return node;
    }

    /**
     * The node visited immediately after {@code node} in an in-order walk, or null at the end.
     *
     * <p>With a right subtree the answer is its minimum. Without one, the walk has finished
     * that subtree, so it climbs until it steps up from a left child — that ancestor is the
     * next unvisited node.</p>
     */
    private Node<T> successor(Node<T> node) {
        if (node.right != null) {
            return findMinimumNode(node.right);
        }
        Node<T> ancestor = node.parent;
        while (ancestor != null && node == ancestor.right) {
            node = ancestor;
            ancestor = ancestor.parent;
        }
        return ancestor;
    }

    /**
     * The mirror of {@link #successor}: the node visited immediately before {@code node}.
     */
    private Node<T> predecessor(Node<T> node) {
        if (node.left != null) {
            return findMaximumNode(node.left);
        }
        Node<T> ancestor = node.parent;
        while (ancestor != null && node == ancestor.left) {
            node = ancestor;
            ancestor = ancestor.parent;
        }
        return ancestor;
    }

    /**
     * A movable position in the in-order sequence, stepping through successor and predecessor
     * links rather than through a materialized list.
     *
     * <p>Unlike the circular list's cursor this one has ends: {@link #hasNext()} is false on
     * the largest element and {@link #hasPrevious()} is false on the smallest. That is what
     * lets {@code AlphabeticalMode} disable its buttons at the boundaries.</p>
     *
     * <p>A cursor is invalidated by any structural change to the tree: every method throws
     * {@link ConcurrentModificationException} rather than walking nodes the tree no longer
     * contains. Deletion is the dangerous case — a stale parent chain does not give a wrong
     * answer, it gives a cycle.</p>
     */
    public final class Cursor {

        private Node<T> node;
        private final int expectedModificationCount;

        private Cursor(Node<T> start) {
            this.node = start;
            this.expectedModificationCount = modificationCount;
        }

        /**
         * @throws ConcurrentModificationException if the tree changed since this cursor opened
         */
        private void checkNotStale() {
            if (expectedModificationCount != modificationCount) {
                throw new ConcurrentModificationException(
                        "The tree changed after this cursor was opened; reopen it.");
            }
        }

        /**
         * @implNote Time complexity: O(1).
         */
        public T current() {
            checkNotStale();
            return node.value;
        }

        /**
         * @implNote Time complexity: O(log n) average, O(n) worst case.
         */
        public boolean hasNext() {
            checkNotStale();
            return successor(node) != null;
        }

        /**
         * @throws NoSuchElementException past the largest element
         * @implNote Time complexity: O(log n) average, O(n) worst case.
         */
        public T next() {
            checkNotStale();
            Node<T> following = successor(node);
            if (following == null) {
                throw new NoSuchElementException("Already at the last element.");
            }
            node = following;
            return node.value;
        }

        /**
         * @implNote Time complexity: O(log n) average, O(n) worst case.
         */
        public boolean hasPrevious() {
            checkNotStale();
            return predecessor(node) != null;
        }

        /**
         * @throws NoSuchElementException before the smallest element
         * @implNote Time complexity: O(log n) average, O(n) worst case.
         */
        public T previous() {
            checkNotStale();
            Node<T> preceding = predecessor(node);
            if (preceding == null) {
                throw new NoSuchElementException("Already at the first element.");
            }
            node = preceding;
            return node.value;
        }
    }

    /**
     * Opens a cursor on the smallest element, the start of the in-order walk.
     *
     * @throws EmptyStructureException if the tree is empty
     * @implNote Time complexity: O(log n) average, O(n) worst case.
     */
    public Cursor cursor() {
        if (isEmpty()) {
            throw new EmptyStructureException("Cannot open a cursor on an empty tree.");
        }
        return new Cursor(findMinimumNode(root));
    }

    // ---------- test support ----------

    /**
     * Walks every node and reports whether each child's {@code parent} points back at it.
     *
     * <p>Package-private on purpose: dangling parent pointers break traversal silently, and a
     * test needs to assert on the invariant directly rather than infer it from symptoms.</p>
     */
    boolean parentPointersAreConsistent() {
        return root != null ? (root.parent == null && subtreeParentsConsistent(root)) : size == 0;
    }

    private boolean subtreeParentsConsistent(Node<T> node) {
        if (node.left != null && (node.left.parent != node || !subtreeParentsConsistent(node.left))) {
            return false;
        }
        return node.right == null
                || (node.right.parent == node && subtreeParentsConsistent(node.right));
    }
}
