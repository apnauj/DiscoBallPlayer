# Complexity of the three structures

Every cell is justified. In the oral defense the number matters less than being able to say
*why* — and, for the worst cases, *when it actually happens in this application*.

`n` is the number of songs in the structure.

## Summary

| Operation | `DoublyCircularLinkedList` | `SimpleQueue` | `BST` (average) | `BST` (worst) |
|---|---|---|---|---|
| Insertion | **O(1)** | **O(1)** | **O(log n)** | **O(n)** |
| Deletion | **O(n)** | **O(1)** | **O(log n)** | **O(n)** |
| Search | **O(n)** | — | **O(log n)** | **O(n)** |
| Traversal, one step | **O(1)** | **O(1)** | **O(log n)** | **O(n)** |
| Traversal, full pass | **O(n)** | **O(n)** | **O(n)** | **O(n)** |
| Space, traversal | **O(1)** | **O(1)** | **O(1)** | **O(1)** |

The last row is the one to point at. **No traversal allocates.** The alphabetical walk steps
through parent pointers rather than materializing a sorted list, which is the property this
part of the exercise is actually about.

## `DoublyCircularLinkedList` — shuffle

| Operation | Cost | Why |
|---|---|---|
| `insertAtEnd` | O(1) | The list holds a `tail` reference, and `tail.next` is the head. Both ends are one hop away, so appending never walks. |
| `insertAtBeginning` | O(1) | Same reference, opposite side. Only `tail` is not moved. |
| `delete(T)` | O(n) | Finding the node is a linear scan; there is no index. The unlinking itself is O(1) because each node knows its predecessor. |
| `has(T)` | O(n) | A linked list has no ordering to exploit. |
| `get(int)` | O(n), ~n/2 | Walks from whichever end is closer, which halves the constant but not the class. |
| `Cursor.next` / `previous` | O(1) | One pointer hop. This is the operation shuffle mode actually performs. |

**Why circular is the right choice here.** Shuffle must wrap infinitely in both directions with
no end state. In a ring that is free: past the tail is the head, before the head is the tail.
A plain doubly linked list would need a boundary check on every step and an explicit decision
about what "before the first song" means.

**The O(n) delete never hurts the user.** Deletion happens when someone removes a song from the
library, once, by hand. The operation that runs sixty times a minute is `Cursor.next`, and that
is O(1).

## `SimpleQueue` — arrival order

| Operation | Cost | Why |
|---|---|---|
| `enqueue` | O(1) | A `tail` reference means appending never walks the chain. Without it this would be O(n) — the single design decision in this class. |
| `dequeue` | O(1) | Unlinks the head and moves the reference forward. |
| `peek` | O(1) | Reads `head.data`. |
| `size` / `isEmpty` | O(1) | A counter maintained on both operations, not recomputed. |

**Everything is O(1), and that is the point.** A queue that gave up any of these would not be a
queue. It is the cheapest of the three structures because it supports the fewest operations —
there is no search and no backward navigation, because FIFO does not permit them.

**Space note.** `dequeue` clears `tail` when the queue empties. Functionally invisible, but
without it a drained queue holds a strong reference to the last song it played.

## `BST` — alphabetical order

Ordering comes from `Song.compareTo`: title first, case-insensitive, ties broken on the
generated id so two songs sharing a title remain two nodes.

| Operation | Average | Worst | Why |
|---|---|---|---|
| `insert` | O(log n) | O(n) | Each comparison discards one subtree. On a balanced tree that halves the search space; on a degenerate one there is nothing to discard. |
| `delete` | O(log n) | O(n) | Descent to the node, plus finding the in-order successor when it has two children. Both bounded by the height. |
| `contains` / `find` | O(log n) | O(n) | The same descent. |
| `Cursor.next` / `previous` | O(log n) | O(n) | Successor is either the minimum of the right subtree or the first ancestor reached from a left child. Either way bounded by the height, not by `n` directly. |
| Full in-order pass | O(n) | O(n) | Amortized: each edge is traversed at most twice across the whole walk, so `n` steps cost O(n) total even though a single step is O(log n). |

**The worst case is real, not theoretical.** This tree is unbalanced by construction. Insert an
already-sorted catalogue — exactly what happens when the library is loaded from a file that was
saved in alphabetical order — and every node becomes a right child. The tree degenerates into a
linked list and every operation becomes O(n).

Three honest answers to "so why not balance it?":

1. Self-balancing (AVL, red-black) is the correct fix and would restore O(log n) worst case.
2. At the scale this application runs — hundreds of songs, one operation per user click — O(n)
   on a degenerate tree is still imperceptible.
3. `BSTTest.degenerateChainStillTraversesInOrder` covers it, so the failure mode is a
   performance characteristic, not a bug.

**Why `parent` pointers instead of a stack.** The usual iterative in-order walk carries an
explicit stack, which is O(h) space and only moves forward. Storing a parent reference per node
makes both `successor` and `predecessor` possible in O(1) space, which is what a Previous button
requires. The cost is that every structural change must rewire those pointers —
`parentPointersAreConsistent()` exists because getting that wrong produces a **cycle**, not a
wrong answer: the walk climbs forever and the JVM dies with `Java heap space` rather than
failing an assertion.

## Comparison at a glance

Same operation, three structures, and why the answers differ:

| Question | Ring | Queue | Tree |
|---|---|---|---|
| Step forward | O(1) | O(1) | O(log n) |
| Step backward | O(1) | impossible | O(log n) |
| Is there a next? | always yes | O(1) | O(log n) |
| Find a specific song | O(n) | not supported | O(log n) |
| Get songs in order | not supported | arrival only | O(n), no extra space |

**The tree pays for ordering.** It is the only structure that answers "what comes after this,
alphabetically" without sorting anything, and it charges O(log n) per step for it. The ring is
faster per step but knows no order at all. The queue is fastest and knows the least.

That trade is the answer to "why three structures instead of one".
