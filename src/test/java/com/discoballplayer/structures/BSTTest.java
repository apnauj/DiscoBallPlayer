package com.discoballplayer.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.discoballplayer.exception.EmptyStructureException;

/**
 * Flat by convention: under {@code @Nested} the outer class matches {@code -Dtest=Class#method}
 * and runs zero tests, so a verification command would report success while testing nothing.
 */
class BSTTest {

    /** Inserted in this order the root is 50, giving every deletion case a real shape. */
    private static final int[] SHAPED = {50, 30, 70, 20, 40, 60, 80, 35, 45};

    private static BST<Integer> treeOf(int... values) {
        BST<Integer> tree = new BST<>();
        for (int value : values) {
            tree.insert(value);
        }
        return tree;
    }

    /**
     * Walks the cursor, which steps through successor links — never a flattened traversal.
     *
     * <p>Bounded by {@code size()} on purpose. A dangling parent pointer does not produce a
     * wrong answer, it produces a cycle: the walk climbs forever and the JVM dies with
     * {@code Java heap space} instead of reporting a failed assertion. The cap turns that into
     * a readable diagnosis.</p>
     */
    private static List<Integer> inOrder(BST<Integer> tree) {
        List<Integer> seen = new ArrayList<>();
        if (tree.isEmpty()) {
            return seen;
        }
        BST<Integer>.Cursor cursor = tree.cursor();
        seen.add(cursor.current());
        while (cursor.hasNext()) {
            seen.add(cursor.next());
            if (seen.size() > tree.size()) {
                throw new AssertionError(
                        "in-order walk visited more nodes than the tree holds ("
                                + tree.size() + "); the parent chain is cyclic");
            }
        }
        return seen;
    }

    private static List<Integer> sortedAscending(int... values) {
        List<Integer> sorted = new ArrayList<>();
        for (int value : values) {
            sorted.add(value);
        }
        sorted.sort(Integer::compareTo);
        return sorted;
    }

    // ---------- insertion and lookup ----------

    @Test
    void newTreeIsEmpty() {
        BST<Integer> tree = new BST<>();

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
    }

    @Test
    void containsFindsInsertedValue() {
        BST<Integer> tree = treeOf(SHAPED);

        assertTrue(tree.contains(50));
        assertTrue(tree.contains(35));
        assertTrue(tree.contains(80));
        assertFalse(tree.contains(99));
    }

    @Test
    void findReturnsTheStoredElement() {
        BST<Integer> tree = treeOf(SHAPED);

        assertEquals(45, tree.find(45));
    }

    @Test
    void findThrowsWhenAbsentRatherThanReturningNull() {
        BST<Integer> tree = treeOf(SHAPED);

        assertThrows(NoSuchElementException.class, () -> tree.find(99));
    }

    @Test
    void duplicatesAreIgnoredAndDoNotGrowTheTree() {
        BST<Integer> tree = treeOf(50, 30, 50, 30, 50);

        assertEquals(2, tree.size());
        assertEquals(List.of(30, 50), inOrder(tree));
    }

    @Test
    void insertRejectsNullElements() {
        BST<Integer> tree = new BST<>();

        assertThrows(NullPointerException.class, () -> tree.insert(null));
    }

    @Test
    void sizeTracksInsertionAndDeletion() {
        BST<Integer> tree = treeOf(SHAPED);
        assertEquals(SHAPED.length, tree.size());

        tree.delete(20);
        assertEquals(SHAPED.length - 1, tree.size());

        tree.delete(99);
        assertEquals(SHAPED.length - 1, tree.size(), "deleting an absent value changes nothing");
    }

    // ---------- in-order traversal ----------

    @Test
    void cursorWalksInOrderBothDirections() {
        BST<Integer> tree = treeOf(SHAPED);
        List<Integer> ascending = sortedAscending(SHAPED);

        assertEquals(ascending, inOrder(tree));

        BST<Integer>.Cursor cursor = tree.cursor();
        while (cursor.hasNext()) {
            cursor.next();
        }
        List<Integer> backwards = new ArrayList<>();
        backwards.add(cursor.current());
        while (cursor.hasPrevious()) {
            backwards.add(cursor.previous());
        }
        List<Integer> descending = new ArrayList<>(ascending);
        java.util.Collections.reverse(descending);
        assertEquals(descending, backwards);
    }

    @Test
    void unsortedInputStillYieldsSortedTraversal() {
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), inOrder(treeOf(4, 2, 6, 1, 3, 5, 7)));
    }

    @Test
    void firstElementHasNoPredecessorAndLastHasNoSuccessor() {
        BST<Integer> tree = treeOf(SHAPED);
        BST<Integer>.Cursor cursor = tree.cursor();

        assertEquals(20, cursor.current());
        assertFalse(cursor.hasPrevious());
        assertThrows(NoSuchElementException.class, cursor::previous);

        while (cursor.hasNext()) {
            cursor.next();
        }
        assertEquals(80, cursor.current());
        assertFalse(cursor.hasNext());
        assertThrows(NoSuchElementException.class, cursor::next);
    }

    @Test
    void nextThenPreviousReturnsToTheSameElement() {
        BST<Integer> tree = treeOf(SHAPED);
        BST<Integer>.Cursor cursor = tree.cursor();

        while (cursor.hasNext()) {
            int before = cursor.current();
            cursor.next();
            assertEquals(before, cursor.previous());
            cursor.next();
        }
    }

    @Test
    void cursorOnEmptyTreeThrows() {
        assertThrows(EmptyStructureException.class, () -> new BST<Integer>().cursor());
    }

    @Test
    void degenerateChainStillTraversesInOrder() {
        assertEquals(List.of(1, 2, 3, 4, 5), inOrder(treeOf(1, 2, 3, 4, 5)));
        assertEquals(List.of(1, 2, 3, 4, 5), inOrder(treeOf(5, 4, 3, 2, 1)));
    }

    // ---------- deletion ----------

    @Test
    void deleteLeafNode() {
        BST<Integer> tree = treeOf(SHAPED);

        tree.delete(20);

        assertFalse(tree.contains(20));
        assertEquals(List.of(30, 35, 40, 45, 50, 60, 70, 80), inOrder(tree));
    }

    @Test
    void deleteNodeWithOneChild() {
        BST<Integer> tree = treeOf(SHAPED);

        tree.delete(20);
        tree.delete(30);

        assertFalse(tree.contains(30));
        assertEquals(List.of(35, 40, 45, 50, 60, 70, 80), inOrder(tree));
    }

    @Test
    void deleteNodeWithTwoChildren() {
        BST<Integer> tree = treeOf(SHAPED);

        tree.delete(30);

        assertFalse(tree.contains(30));
        assertEquals(List.of(20, 35, 40, 45, 50, 60, 70, 80), inOrder(tree));
        assertEquals(8, tree.size());
    }

    @Test
    void deleteRootWithTwoChildren() {
        BST<Integer> tree = treeOf(SHAPED);

        tree.delete(50);

        assertFalse(tree.contains(50));
        assertEquals(List.of(20, 30, 35, 40, 45, 60, 70, 80), inOrder(tree));
    }

    @Test
    void deleteOnlyElementEmptiesTheTree() {
        BST<Integer> tree = treeOf(42);

        tree.delete(42);

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertThrows(EmptyStructureException.class, tree::cursor);
    }

    @Test
    void traversalStaysSortedAfterEveryDeletion() {
        BST<Integer> tree = treeOf(SHAPED);

        for (int value : new int[] {50, 30, 80, 35, 20, 70, 45, 60, 40}) {
            tree.delete(value);
            List<Integer> seen = inOrder(tree);
            List<Integer> expected = new ArrayList<>(seen);
            expected.sort(Integer::compareTo);
            assertEquals(expected, seen, "traversal lost its order after deleting " + value);
        }
        assertTrue(tree.isEmpty());
    }

    // ---------- parent pointer invariant (A1-06) ----------

    @Test
    void parentPointersStayConsistentAfterDeletes() {
        BST<Integer> tree = treeOf(SHAPED);
        assertTrue(tree.parentPointersAreConsistent(), "broken right after insertion");

        for (int value : new int[] {20, 30, 50, 80, 35, 45, 40, 60, 70}) {
            tree.delete(value);
            assertTrue(tree.parentPointersAreConsistent(),
                    "parent pointers dangled after deleting " + value);
        }
        assertTrue(tree.parentPointersAreConsistent());
    }

    @Test
    void parentPointersSurviveDeletingTheRootRepeatedly() {
        BST<Integer> tree = treeOf(SHAPED);

        while (!tree.isEmpty()) {
            int root = tree.cursor().current();
            tree.delete(root);
            assertTrue(tree.parentPointersAreConsistent(),
                    "parent pointers dangled after deleting " + root);
        }
    }

    @Test
    void deletionKeepsTraversalReachableInBothDirections() {
        BST<Integer> tree = treeOf(SHAPED);
        tree.delete(30);
        tree.delete(70);

        List<Integer> forward = inOrder(tree);

        BST<Integer>.Cursor cursor = tree.cursor();
        while (cursor.hasNext()) {
            cursor.next();
        }
        List<Integer> backward = new ArrayList<>();
        backward.add(cursor.current());
        while (cursor.hasPrevious()) {
            backward.add(cursor.previous());
        }
        java.util.Collections.reverse(backward);

        assertEquals(forward, backward,
                "walking back must visit exactly what walking forward visited");
    }

    // ---------- ordering that mirrors Song.compareTo ----------

    @Test
    void elementsEqualOnTheMainKeySurviveWhenTheTieBreakDiffers() {
        record Track(String title, String id) implements Comparable<Track> {
            @Override
            public int compareTo(Track other) {
                int byTitle = title.compareToIgnoreCase(other.title);
                return byTitle != 0 ? byTitle : id.compareTo(other.id);
            }
        }

        BST<Track> tree = new BST<>();
        tree.insert(new Track("Tania", "id-2"));
        tree.insert(new Track("Tania", "id-1"));

        assertEquals(2, tree.size(), "duplicate titles by different artists must both survive");

        BST<Track>.Cursor cursor = tree.cursor();
        assertEquals("id-1", cursor.current().id());
        assertEquals("id-2", cursor.next().id());
    }
}
