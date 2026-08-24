# Rubric self-audit

One row per grading criterion, each pointing at the files that satisfy it and the command that
proves it. Every figure below was produced by running the command shown, not from memory.

Audited against `develop` after `C-02`. Suite at the time of writing:
**`Tests run: 259, Failures: 0, Errors: 0, Skipped: 0`.**

---

## Summary

| Criterion | Weight | Where it lives | Status |
|---|---|---|---|
| Data structures work correctly | 35% | `structures/` + tests | Behaviour complete and covered; **`A1-10` open** — see [Open items](#open-items) |
| Graphical interface implemented | 20% | `ui/`, `fxml/` | Complete |
| Code quality (OOP, organization) | 15% | Everywhere | Complete |
| Three playback modes fulfilled | 15% | `playback/` | Complete |
| Interface design and UX | 10% | `css/`, layout | Complete |
| Creativity and extras | 5% | Bonus tickets | Complete |

---

## 1. Data structures work correctly — 35%

Three structures, hand-written and generic, with no `java.util` collection inside.

| Structure | File | Suite | Tests |
|---|---|---|---|
| `BST<T>` | `structures/BST.java` | `BSTTest` | 23 |
| `DoublyCircularLinkedList<T>` | `structures/DoublyCircularLinkedList.java` | `DoublyCircularLinkedListTest` | 19 |
| `SimpleQueue<T>` | `structures/SimpleQueue.java` | `SimpleQueueTest` | 13 |

**Proof that no `java.util` collection backs a structure.** Both commands return nothing:

```bash
grep -rn "java.util.\(List\|Queue\|Deque\|TreeMap\|TreeSet\|ArrayList\|LinkedList\|Collections\)" \
  src/main/java/com/discoballplayer/structures/

grep -rn "java.util.\(List\|Queue\|Deque\|TreeMap\|TreeSet\|LinkedList\|Collections\)" \
  src/main/java/com/discoballplayer/playback/
```

**Proof that `AlphabeticalMode` never flattens the tree.** This is the first thing the
professor looks for. Navigation goes through in-order successor and predecessor over parent
pointers, so the file contains no list, no array and no sort:

```bash
grep -n "List\|\[\]\|sort\|toArray" src/main/java/com/discoballplayer/playback/AlphabeticalMode.java
```

Returns nothing.

**Encapsulation.** `Node` is private in all three structures, so no caller can reach the
internal representation:

```
BST.java:24:                       private static class Node<T extends Comparable<T>>
DoublyCircularLinkedList.java:9:   private class Node
SimpleQueue.java:21:               private static class Node<T>
```

---

## 2. Graphical interface implemented — 20%

| Piece | Files | Suite | Tests |
|---|---|---|---|
| Application shell, library table, search | `ui/MainController.java`, `fxml/main-view.fxml` | `MainControllerTest` | 19 |
| Transport and mode selector | same | `TransportControlsTest` | 12 |
| Add / edit / delete and rating | `ui/SongDialogController.java`, `fxml/song-dialog.fxml` | `SongDialogControllerTest`, `LibraryEditingTest` | 23 + 12 |
| Service injection seam | `ui/MainController.java` | `ControllerInjectionTest` | 5 |
| Theme and shortcuts | `css/`, `ui/MainController.java` | `ThemeAndShortcutsTest` | 11 |

Every UI suite loads the real FXML through `FXMLLoader` and reads the scene graph by `fx:id`,
so a renamed id or an unbound column fails the suite rather than the demo.

**Not covered by tests, verified by hand:** Add and Edit open a modal window through
`showAndWait`, which cannot run headless. The tests cover the dialog in isolation and
everything around the call; the click-through itself was verified with `mvn javafx:run`.

---

## 3. Code quality — 15%

**The UI never touches a data structure.** Returns nothing:

```bash
grep -rn "^import com.discoballplayer.structures" \
  src/main/java/com/discoballplayer/ui/ src/test/java/com/discoballplayer/ui/
```

The UI depends on `PlayerService` only. That is what let the interface be built and run
against `DemoPlayerService` while `Player` was still being written.

**One `MusicLibrary`.** `Main` composes the graph and injects it through
`FXMLLoader.setControllerFactory`. `MainController` never constructs a library, so the
catalogue loaded from disk is the one shown and the one saved. `ControllerInjectionTest`
asserts this directly; building a service internally instead fails it with
`the view is showing a catalogue nobody injected: expected <1> but was <12>`.

**No `System.out.println` in committed code.** Returns nothing:

```bash
grep -rn "System.out.println" src/main/java/
```

**Custom exceptions over `null` returns.** `EmptyStructureException`, `SongNotFoundException`
and `PersistenceException` in `exception/`. A structure throws rather than returning `null`
for "empty".

**Tests are flat, with no `@Nested`.** Under `@Nested` the outer class matches
`-Dtest=Class#method` and runs zero tests, so a verification command reports success while
testing nothing. Related: **`Tests run: 0` is a failure even when Maven prints
`BUILD SUCCESS`** — Surefire fails on an unmatched test class but not on an unmatched test
method.

---

## 4. Three playback modes fulfilled — 15%

| Mode | Structure | Suite | Tests |
|---|---|---|---|
| `ShuffleMode` | `DoublyCircularLinkedList<Song>` | `ShuffleModeTest` | 14 |
| `ArrivalMode` | `SimpleQueue<Song>` | `ArrivalModeTest` | 13 |
| `AlphabeticalMode` | `BST<Song>` | `AlphabeticalModeTest` | 14 |

**The demo moment.** Previous greys out in arrival order. The button is disabled from
`PlayerService.hasPrevious()`, never from a check on which mode is selected — the mode says it
cannot go back and the UI obeys, so the rule lives in the playback layer where it belongs.
Asserted from three directions in `TransportControlsTest`; hard-coding the button enabled
fails three tests.

**Each mode owns the structure it built.** A mode builds its structure from the library at
`load()` and keeps it. That is why arrival order can drain its queue with the catalogue
untouched — visible in the app as "Queue finished" while all songs remain in the table.

---

## 5. Interface design and UX — 10%

Every colour is declared once as a design token on `.root` in `app.css` and referenced by name
afterwards. No rule past the token block holds a hex literal, which is what lets `dark.css`
re-theme the window by overriding tokens alone, with no rule restated and no size, padding or
radius touched — so a theme swap cannot relayout anything.

Two tests read the stylesheets as text, because "swapping the theme cannot relayout the
window" is a claim about what `dark.css` is permitted to contain; a rule added later would
break it silently. A third lays the scene out, toggles, lays it out again and compares bounds.

Row hover, selection and a disabled state that drops fill, text and opacity together — opacity
alone is too subtle across a room, and the greyed-out Previous button is graded.

---

## 6. Creativity and extras — 5%

| Extra | Ticket | Where |
|---|---|---|
| Dark theme with a runtime toggle | `B6-03` | `css/dark.css`, `ui/MainController.java` |
| Keyboard shortcuts (Space, ←/→, `Ctrl+F`, `Ctrl+N`) | `B6-04` | `ui/MainController.java` |
| JSON persistence with an atomic write | `A4-03` | `repository/JsonLibraryRepository.java` |
| Seed catalogue so the defense never opens on a blank window | `C-03` | `util/SampleLibrary.java` |
| Generated default cover artwork | `B3-03` | `images/default-cover.png` |

Shortcuts are registered on the scene, not on individual buttons. Space and the arrow keys are
event **filters** rather than accelerators: both already mean something inside a text field,
so the filter stands down whenever a text input has focus. Asserted in both directions.

---

## On the test figures

A green suite is not the same as a suite that asserts anything. Every Track B suite was
mutation-checked: a behaviour was deliberately broken, the suite was run, and the code was
restored. Three mutations found real defects rather than confirming good tests.

| Mutation | Outcome |
|---|---|
| Remove the progress-bar clamp | Caught |
| Remove the cover `isError` check | **Survived** — the branch was unreachable from a bad path; a test was added |
| Hard-code Previous enabled | Caught, 3 tests |
| Swallow the exhausted queue silently | Caught |
| Edit builds a new `Song` instead of mutating | Caught |
| Skip form validation | Caught, 7 tests |
| Drop the rating sync guard | **Survived** — the assertion re-read a value the mutation rewrote identically; replaced with an event count |
| Slip a layout rule into `dark.css` | Caught |
| Raw hex outside the token block | Caught |
| Drop the stylesheet-order deferral | Caught |
| Controller ignores its injected service | Caught, 2 tests |

The two survivors are the point of the exercise. Both were green tests that proved nothing,
and neither would have been found by reading them.

---

## Open items

- **`A1-10` — complexity Javadoc on `DoublyCircularLinkedList`.** `BST` and `SimpleQueue`
  carry `Time complexity` on every public method; `DoublyCircularLinkedList` carries it on
  none of its eleven. `CLAUDE.md` calls these comments the oral-defense script, and structures
  are 35% of the grade, so this is the one open item that touches the heaviest criterion. The
  file belongs to Track A; filed under *Cross-track requests* in `docs/plan.md`.

  Re-check with:

  ```bash
  grep -c "Time complexity" src/main/java/com/discoballplayer/structures/DoublyCircularLinkedList.java
  ```

  Currently returns `4`, all of them on the `Cursor` class rather than on the list's own
  methods.
