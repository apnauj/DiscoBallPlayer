package com.discoballplayer.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.discoballplayer.exception.EmptyStructureException;

/**
 * Kept deliberately flat: {@code @Nested} makes the outer class match {@code -Dtest=Class#method}
 * with zero tests, so a mistyped verification command in {@code docs/plan.md} would report
 * BUILD SUCCESS while running nothing.
 */
class DoublyCircularLinkedListTest {

    private static DoublyCircularLinkedList<String> listOf(String... values) {
        DoublyCircularLinkedList<String> list = new DoublyCircularLinkedList<>();
        for (String value : values) {
            list.insertAtEnd(value);
        }
        return list;
    }

    private static List<String> drain(DoublyCircularLinkedList<String> list) {
        List<String> seen = new ArrayList<>();
        list.forEach(seen::add);
        return seen;
    }

    // ---------- insertion ----------

    @Test
    void insertIntoEmptyListCreatesSingleElementRing() {
        DoublyCircularLinkedList<String> list = listOf("A");

        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
        assertEquals(List.of("A"), drain(list));
    }

    @Test
    void insertAtEndAppendsInOrder() {
        assertEquals(List.of("A", "B", "C"), drain(listOf("A", "B", "C")));
    }

    @Test
    void insertAtBeginningPrependsWithoutMovingTheTail() {
        DoublyCircularLinkedList<String> list = listOf("B", "C");
        list.insertAtBeginning("A");

        assertEquals(List.of("A", "B", "C"), drain(list));
        assertEquals(3, list.size());
        assertEquals("C", list.get(2));
    }

    @Test
    void insertRejectsNullElements() {
        DoublyCircularLinkedList<String> list = new DoublyCircularLinkedList<>();

        assertThrows(NullPointerException.class, () -> list.insertAtEnd(null));
        assertThrows(NullPointerException.class, () -> list.insertAtBeginning(null));
    }

    // ---------- deletion ----------

    @Test
    void deleteHeadRelinksTailToTheNewHead() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C");

        assertTrue(list.delete("A"));
        assertEquals(List.of("B", "C"), drain(list));
        assertEquals(2, list.size());
    }

    @Test
    void deleteTailMovesTheTailBack() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C");

        assertTrue(list.delete("C"));
        assertEquals(List.of("A", "B"), drain(list));
        assertEquals("B", list.get(1));
    }

    @Test
    void deleteMiddleClosesTheGapInBothDirections() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C");

        assertTrue(list.delete("B"));
        assertEquals(List.of("A", "C"), drain(list));

        DoublyCircularLinkedList<String>.Cursor cursor = list.cursor();
        assertEquals("C", cursor.next());
        assertEquals("A", cursor.next());
        assertEquals("C", cursor.previous());
    }

    @Test
    void deleteOnlyElementEmptiesTheList() {
        DoublyCircularLinkedList<String> list = listOf("A");

        assertTrue(list.delete("A"));
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void deleteAbsentElementReturnsFalseAndKeepsSize() {
        DoublyCircularLinkedList<String> list = listOf("A", "B");

        assertFalse(list.delete("Z"));
        assertEquals(2, list.size());
    }

    @Test
    void deleteOnEmptyListReturnsFalse() {
        assertFalse(new DoublyCircularLinkedList<String>().delete("A"));
    }

    @Test
    void sizeStaysCorrectAcrossAFullDrain() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C", "D");

        for (String value : List.of("C", "A", "D", "B")) {
            int before = list.size();
            assertTrue(list.delete(value));
            assertEquals(before - 1, list.size());
        }
        assertTrue(list.isEmpty());
    }

    // ---------- membership and indexing ----------

    @Test
    void hasFindsPresentAndRejectsAbsent() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C");

        assertTrue(list.has("B"));
        assertFalse(list.has("Z"));
        assertFalse(new DoublyCircularLinkedList<String>().has("A"));
    }

    @Test
    void getWalksFromWhicheverEndIsCloser() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C", "D", "E");

        assertEquals("A", list.get(0));
        assertEquals("C", list.get(2));
        assertEquals("E", list.get(4));
    }

    @Test
    void getRejectsOutOfBoundsIndexes() {
        DoublyCircularLinkedList<String> list = listOf("A");

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    // ---------- cursor ----------

    @Test
    void cursorWrapsForwardAndBackward() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C");
        DoublyCircularLinkedList<String>.Cursor cursor = list.cursor();

        assertEquals("A", cursor.current());
        assertEquals("B", cursor.next());
        assertEquals("C", cursor.next());
        assertEquals("A", cursor.next(), "past the tail the cursor wraps to the head");
        assertEquals("C", cursor.previous(), "before the head the cursor wraps to the tail");
        assertEquals("B", cursor.previous());
    }

    @Test
    void nextThenPreviousReturnsToTheSameElement() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C", "D");
        DoublyCircularLinkedList<String>.Cursor cursor = list.cursor();

        for (int step = 0; step < 6; step++) {
            String before = cursor.current();
            cursor.next();
            assertEquals(before, cursor.previous());
        }
    }

    @Test
    void navigationIsInfiniteInBothDirections() {
        DoublyCircularLinkedList<String> list = listOf("A", "B", "C");
        DoublyCircularLinkedList<String>.Cursor forward = list.cursor();
        DoublyCircularLinkedList<String>.Cursor backward = list.cursor();

        for (int lap = 0; lap < 10; lap++) {
            assertEquals("B", forward.next());
            assertEquals("C", forward.next());
            assertEquals("A", forward.next());

            assertEquals("C", backward.previous());
            assertEquals("B", backward.previous());
            assertEquals("A", backward.previous());
        }
    }

    @Test
    void singleElementCursorStaysOnThatElement() {
        DoublyCircularLinkedList<String>.Cursor cursor = listOf("A").cursor();

        assertEquals("A", cursor.next());
        assertEquals("A", cursor.previous());
        assertEquals("A", cursor.current());
    }

    @Test
    void cursorOnEmptyListThrows() {
        DoublyCircularLinkedList<String> empty = new DoublyCircularLinkedList<>();

        assertThrows(EmptyStructureException.class, empty::cursor);
    }
}
