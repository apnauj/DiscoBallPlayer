# DiscoBallPlayer — Implementation Plan

> **Single source of truth.** Both Claude Code sessions (Track A and Track B) read this file
> at the start of every work session and tick boxes here as tasks land. Nothing is "done"
> until its box is `- [x]` **and** its verification command passes.

---

## 0. Repository state at plan time (2026-08-19, `develop` @ `99b6888`)

Verified by running the build, not assumed:

| Fact | Status |
|---|---|
| `mvn clean compile` | **FAILS.** `BST.java` declares package `main.java.com.discoballplayer.structures` and calls a two-argument `findMinimun` that does not exist. |
| `CLAUDE.md` | **Deleted** (staged deletion in the working tree). Must be restored — see `F0-01`. |
| `model/` | `Song`, `Artist`, `Album`, `Genre`, `Playlist`, `MusicLibrary` implemented and sound. |
| `structures/` | `DoublyCircularLinkedList` present but has no bidirectional cursor. `BST` broken. No queue at all. |
| `playback/`, `service/`, `repository/`, `util/`, `exception/` | Empty (`.gitkeep` only). |
| `src/test/` | Empty. **Zero tests exist.** Structures carry 35% of the grade. |
| UI | `main-view.fxml` + `MainController` are Phase-1 placeholders (one label). |

**Consequence:** Phase 0 is hard-blocking. Neither track can start until `mvn clean compile`
is green again, because Track B cannot run the app and Track A cannot run a test.

---

## 1. Naming decisions (locked — do not re-litigate)

The prompt, the README and the committed code disagree on three names. Locked resolutions:

| Concept | Locked name | Why |
|---|---|---|
| FIFO queue | `SimpleQueue<T>` | Already the name in `README.md` and `CLAUDE.md`. "CustomQueue" appears in no file. |
| Binary search tree | `BST<T>` | The file exists with merged PR history (`#10`). Renaming to `BinarySearchTree` is churn; the README gets aligned instead (`C-05`). |
| UI seam | `PlayerService` (interface) | The UI must compile against an interface so Track B can run on a stub. `Player` is the real implementation of it. |

---

## 2. Parallelization protocol

### Isolation is a precondition, not a nicety

**Each session runs in its own git worktree.** Disjoint file ownership is necessary but not
sufficient: two agents in one working directory share a single `HEAD`, a single index and a
single checkout, so one session's `git checkout` yanks the branch out from under the other and
its commits land on the wrong branch. This is not hypothetical — it happened on 2026-08-23 and
put a Track B commit at the base of a Track A branch.

```bash
# Track A stays in the original directory. Once, to give Track B its own:
git worktree add ../DiscoBallPlayer-ui develop
```

One `.git` is shared, so both sessions see the same branches and the same `origin`. `HEAD`, the
index and the working tree are independent. A branch checked out in one worktree cannot be
checked out in the other, which is the guard rail that makes this safe.

**Neither session parks on `develop`.** That same guard rail cuts both ways: whoever holds
`develop` blocks the other from checking it out. Branch straight off the remote instead, which
never needs a local `develop` at all:

```bash
git fetch --prune
git checkout -b feature/<ticket>-<slug> origin/develop
```

Two sessions run concurrently on **disjoint file sets**. Ownership is absolute — if you need a
change in a file you do not own, write it under "Cross-track requests" at the bottom of this
file and keep working on something else.

| Owner | Owns exclusively |
|---|---|
| **Track A** | `structures/`, `playback/`, `repository/`, `util/`, `exception/`, `model/`, `service/Player.java`, `service/*.java` interfaces, `module-info.java`, `src/test/` **except** `src/test/java/com/discoballplayer/ui/` |
| **Track B** | `ui/`, `resources/**/fxml/`, `resources/**/css/`, `resources/**/images/`, `service/DemoPlayerService.java`, `src/test/java/com/discoballplayer/ui/` |
| **Shared, Phase 0 only** | Everything created in `F0-*`. After Phase 0 ends, Phase 0 files fall under the ownership table above. |

Rules:

1. `module-info.java` is touched **once**, in `F0-09`, and never again. All `exports` for
   packages that will exist are added there up front.
2. Branch per ticket: `feature/<ticket-code-lowercase>`, e.g. `feature/a1-03-simple-queue`.
   PR into `develop`. Never commit to `develop` directly.
   **Never stack a PR on another feature branch.** A stacked PR merges into its base, not
   into `develop`, and GitHub does not always retarget it when the base merges — the PR reads
   `MERGED` while its work never reaches `develop`. `A2-04` through `A2-07` were lost this way
   and needed a separate recovery PR. If a unit needs code from an unmerged branch, wait for
   the merge or accept one larger PR.
3. `mvn test` must be green before opening a PR. A red `develop` stops both tracks.
   **`Tests run: 0` is a failure even when Maven prints `BUILD SUCCESS`.** Surefire fails on an
   unmatched test *class* but not on an unmatched test *method*, so a typo in a `-Dtest=Class#method`
   verification command reports success while running nothing. Read the count, not the colour.
   For the same reason test classes are flat: under `@Nested` the outer class matches and runs zero tests.
4. Tick the box in this file **in the same PR** as the work.

---

## 3. Phase 0 — Alignment, contracts and configuration (BLOCKING)

Executed by **one session only** (Track A takes it). Track B waits. Target: 1 sitting.

- [x] **[F0-01] Restore `CLAUDE.md` at the repository root**
  - **Files:** `CLAUDE.md`
  - **Objective:** Recreate the deleted contributor guide, updated for the two-track workflow
    and for the obligation to maintain `docs/plan.md`.
  - **Content:** paste the block in §7 of this document verbatim.
  - **Verification:** `git status --short CLAUDE.md` shows `A`/`M`, not `D`; file contains the
    string `docs/plan.md`.

- [x] **[F0-02] Fix `BST.java` package and compilation errors**
  - **Files:** `src/main/java/com/discoballplayer/structures/BST.java`
  - **Objective:** Make the repository compile again. Package becomes
    `com.discoballplayer.structures`; `insertr`'s left branch recurses on `current.left` (today
    it recurses on `current.rigth`); `deleter` calls `deleter(current.rigth, successor)` instead
    of the non-existent two-arg `findMinimun`.
  - **Scope limit:** compilation and those two logic bugs only. The parent-pointer rework and
    the cursor are `A1-06`/`A1-07`. Do **not** start them here.
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[F0-03] Rename the `rigth` typo across `BST`**
  - **Files:** `src/main/java/com/discoballplayer/structures/BST.java`
  - **Objective:** `rigth` → `right` everywhere. Pure rename, no behaviour change, own commit
    so it never pollutes a logic diff.
  - **Verification:** `grep -c rigth src/main/java/com/discoballplayer/structures/BST.java`
    returns `0`; `mvn clean compile` exits 0.

- [x] **[F0-04] Add the two runtime exceptions**
  - **Files:** `exception/EmptyStructureException.java`, `exception/SongNotFoundException.java`
  - **Objective:** Both extend `RuntimeException` with a message constructor. Structures signal
    "empty" by throwing, never by returning `null`.
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[F0-05] Define the `PlaybackMode` contract**
  - **Files:** `playback/PlaybackMode.java`
  - **Objective:** Interface only, no implementation, no abstract base yet.
  - **Signature (locked):**
    ```java
    public interface PlaybackMode {
        void load(MusicLibrary library);
        Song next();            // throws EmptyStructureException when exhausted
        Song previous();        // throws UnsupportedOperationException in ArrivalMode
        boolean hasNext();
        boolean hasPrevious();  // always false in ArrivalMode
        Song current();
        String displayName();
    }
    ```
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[F0-06] Define the `PlaybackListener` observer contract**
  - **Files:** `service/PlaybackListener.java`
  - **Objective:** Plain-Java callback the UI implements. **No JavaFX types here** — the
    backend must not import `javafx.*`. Callbacks may fire off the FX thread; Track B wraps
    every handler body in `Platform.runLater`.
  - **Signature (locked):**
    ```java
    public interface PlaybackListener {
        void onSongChanged(Song song);
        void onPlaybackStateChanged(boolean playing);
        void onProgress(int elapsedSeconds, int totalSeconds);
        void onLibraryChanged();
    }
    ```
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[F0-07] Define the `PlayerService` façade contract**
  - **Files:** `service/PlayerService.java`
  - **Objective:** The only type the UI is allowed to call. Locked signature:
    ```java
    public interface PlayerService {
        void setMode(PlaybackMode mode);   PlaybackMode getMode();
        Song next();      Song previous();
        boolean hasNext(); boolean hasPrevious();
        Song current();
        void play();      void pause();    boolean isPlaying();
        void addSong(Song song);  void removeSong(Song song);  void updateSong(Song song);
        List<Song> listAll();     List<Song> search(String query);
        void rate(Song song, int rating);
        void addListener(PlaybackListener l);  void removeListener(PlaybackListener l);
    }
    ```
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[F0-08] Define the `LibraryRepository` contract**
  - **Files:** `repository/LibraryRepository.java`
  - **Objective:** `MusicLibrary load()` and `void save(MusicLibrary library)`. Interface only.
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[F0-09] Add `util/TimeFormatter` and open `module-info` for all packages**
  - **Files:** `util/TimeFormatter.java`, `src/main/java/module-info.java`
  - **Objective:** `TimeFormatter.mmss(int seconds)`. Then add `exports` for `playback`,
    `service`, `repository`, `util`, `exception` — legal now that each package has a class.
    **This is the last edit to `module-info.java` in the whole project.**
  - **Verification:** `mvn clean compile && mvn javafx:run` opens the placeholder window.

- [x] **[F0-10] Ship `DemoPlayerService` so Track B can start immediately**
  - **Files:** `service/DemoPlayerService.java`
  - **Objective:** In-memory `PlayerService` stub over ~12 hard-coded songs. `next()`/`previous()`
    walk an `ArrayList` index, `play()`/`pause()` flip a boolean and drive a
    `ScheduledExecutorService` that fires `onProgress` once a second. No real structures.
  - **Ownership note:** after this ticket the file belongs to **Track B**.
  - **Verification:** `mvn clean compile` exits 0; `new DemoPlayerService().listAll().size() == 12`.

**Phase 0 exit gate — PASSED 2026-08-23** on branch `feature/f0-contracts`:
`mvn clean compile` exits 0, `mvn javafx:run` opens the placeholder window with no exception,
`DemoPlayerService` seeds 12 songs, `CLAUDE.md` restored. **Both tracks may start.**

Note for Track A: `F0-02` was a full rewrite of `BST.java` rather than a patch — the two logic
bugs could not be fixed without also correcting the `parent` maintenance the delete path
destroyed. See the amended `A1-05` and `A1-06`.

---

## 4. TRACK A — Backend, data structures, persistence

Runs in parallel with Track B. Never opens an FXML file.

**Every implementation ticket ships the test that verifies it.** A ticket's Files list names
both the production file and the test file holding its verification method, so nothing merges
untested. The dedicated test-suite tickets that follow *extend* that same file to the full
coverage baseline in `CLAUDE.md`; they never create it from nothing.

### A1 — Hand-written structures (`structures/`) — 35% of the grade

- [x] **[A1-01] Bidirectional cursor on `DoublyCircularLinkedList`**
  - **Files:** `structures/DoublyCircularLinkedList.java`, `src/test/java/com/discoballplayer/structures/DoublyCircularLinkedListTest.java`
  - **Objective:** Add a public nested `Cursor` (`current()`, `next()`, `previous()`) that walks
    the ring infinitely in both directions, plus `Cursor cursor()` returning one positioned at
    the head. `ShuffleMode` navigates through this and never sees `Node`.
  - **Constraint:** `Node` stays private. `cursor()` on an empty list throws `EmptyStructureException`.
  - **Verification:** `mvn test -Dtest=DoublyCircularLinkedListTest#cursorWrapsForwardAndBackward`

- [x] **[A1-02] Test suite for `DoublyCircularLinkedList`**
  - **Files:** `src/test/java/com/discoballplayer/structures/DoublyCircularLinkedListTest.java`
  - **Objective:** Cover insert into empty, forward wrap tail→head, backward wrap head→tail,
    delete of head / tail / middle / only element, `size` after each, `has` on absent element.
  - **Verification:** `mvn test -Dtest=DoublyCircularLinkedListTest` — all green.

- [x] **[A1-03] Implement `SimpleQueue<T>`**
  - **Files:** `structures/SimpleQueue.java`, `src/test/java/com/discoballplayer/structures/SimpleQueueTest.java`
  - **Objective:** Hand-written FIFO with head/tail node references. API: `enqueue`, `dequeue`,
    `peek`, `isEmpty`, `size`. `dequeue`/`peek` on empty throw `EmptyStructureException`.
  - **Constraint:** no `java.util` collection inside. Javadoc states O(1) for every operation.
  - **Verification:** `mvn test -Dtest=SimpleQueueTest#preservesFifoOrder`

- [x] **[A1-04] Test suite for `SimpleQueue`**
  - **Files:** `src/test/java/com/discoballplayer/structures/SimpleQueueTest.java`
  - **Objective:** FIFO order over ≥10 enqueues, `dequeue` on empty throws, `peek` does not
    remove, `size` tracks both operations, drain-then-refill works.
  - **Verification:** `mvn test -Dtest=SimpleQueueTest` — all green.

- [x] **[A1-05] Clean up the `BST` public API**
  - **Files:** `structures/BST.java`, `src/test/java/com/discoballplayer/structures/BSTTest.java`
  - **Objective:** `F0-02` already deleted the `Node`-returning `search` and made `Node` private.
    What remains: add `boolean contains(T value)`, `T find(T value)` and `size()`.
  - **Verification:** `mvn test -Dtest=BSTTest#containsFindsInsertedValue`

- [x] **[A1-06] Maintain `parent` pointers through insert and delete in `BST`**
  - **Files:** `structures/BST.java`, `src/test/java/com/discoballplayer/structures/BSTTest.java`
  - **Objective:** `F0-02` rewired `parent` through `insert` and `delete` while fixing the
    rewrite. This ticket is now the **proof**: write the test that would have caught the
    dangling pointers, and fix whatever it finds. Do not assume `F0-02` got every case right.
  - **Verification:** `mvn test -Dtest=BSTTest#parentPointersStayConsistentAfterDeletes`

- [x] **[A1-07] In-order successor / predecessor and a bidirectional cursor on `BST`**
  - **Files:** `structures/BST.java`, `src/test/java/com/discoballplayer/structures/BSTTest.java`
  - **Objective:** `successor(node)` / `predecessor(node)` via parent pointers, plus a public
    `Cursor` (`current`, `next`, `previous`, `hasNext`, `hasPrevious`) starting at the minimum.
  - **Hard constraint:** **never flatten the tree into a list.** No `List`, no array, no
    `Collections.sort` anywhere in this file. This is the single most-inspected rule at defense.
  - **Verification:** `mvn test -Dtest=BSTTest#cursorWalksInOrderBothDirections`

- [x] **[A1-08] Test suite for `BST` — traversal and boundaries**
  - **Files:** `src/test/java/com/discoballplayer/structures/BSTTest.java`
  - **Objective:** In-order walk returns sorted order; first element has no predecessor; last
    has no successor; duplicate titles by different artists both survive (`Song.compareTo`
    breaks ties on `id`).
  - **Verification:** `mvn test -Dtest=BSTTest` — all green.

- [x] **[A1-09] Test suite for `BST` — deletion cases**
  - **Files:** `src/test/java/com/discoballplayer/structures/BSTTest.java` (append)
  - **Objective:** Delete a leaf, a node with one child, a node with two children, and the root;
    in-order order stays sorted after each; `size` decrements correctly.
  - **Verification:** `mvn test -Dtest=BSTTest#deleteNodeWithTwoChildren`

- [x] **[A1-10] Javadoc with complexity on every public structure method**
  - **Files:** `structures/DoublyCircularLinkedList.java`, `structures/SimpleQueue.java`, `structures/BST.java`
  - **Objective:** Every public method gets `@implNote Time complexity: O(...)`. These comments
    are the oral-defense script.
  - **Verification:** `mvn clean compile` exits 0; every public method in the three files has a
    complexity line (visual check).

- [x] **[A1-11] Fail fast when a cursor outlives a structural change**
  - **Files:** `structures/DoublyCircularLinkedList.java`, `src/test/java/com/discoballplayer/structures/DoublyCircularLinkedListTest.java`
  - **Objective:** A cursor opened before an insert or delete keeps walking detached nodes and
    returns stale data instead of failing. Add a modification counter and throw
    `ConcurrentModificationException` from `next`/`previous`/`current` when it moves.
  - **Origin:** found during `A1-01`; documented in the `Cursor` Javadoc as a known limitation.
  - **Verification:** `mvn test -Dtest=DoublyCircularLinkedListTest#cursorRejectsUseAfterDelete`

### A2 — Playback modes (`playback/`)

- [x] **[A2-01] `AbstractPlaybackMode` shared base**
  - **Files:** `playback/AbstractPlaybackMode.java`
  - **Objective:** Holds `current` and the `MusicLibrary` reference; default `previous()` /
    `hasPrevious()`. Subclasses override only what differs. This is the project's one justified
    use of inheritance — do not force it elsewhere.
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[A2-02] `ShuffleMode` over `DoublyCircularLinkedList`**
  - **Files:** `playback/ShuffleMode.java`, `src/test/java/com/discoballplayer/playback/ShuffleModeTest.java`
  - **Objective:** `load()` shuffles the **insertion order once**, then navigates via the
    `A1-01` cursor. Never re-shuffles inside `next()` — that would make `previous()` meaningless.
    `hasNext()`/`hasPrevious()` are always `true` for a non-empty library.
  - **Verification:** `mvn test -Dtest=ShuffleModeTest#nextThenPreviousReturnsToSameSong`

- [x] **[A2-03] Test suite for `ShuffleMode`**
  - **Files:** `src/test/java/com/discoballplayer/playback/ShuffleModeTest.java`
  - **Objective:** Wraps infinitely forward and backward; `next()` then `previous()` returns the
    same song; two `load()` calls on the same library produce different orders (seeded, tolerant).
  - **Verification:** `mvn test -Dtest=ShuffleModeTest` — all green.

- [x] **[A2-04] `ArrivalMode` over `SimpleQueue`**
  - **Files:** `playback/ArrivalMode.java`, `src/test/java/com/discoballplayer/playback/ArrivalModeTest.java`
  - **Objective:** Strict FIFO. `next()` dequeues permanently. `previous()` throws
    `UnsupportedOperationException`, `hasPrevious()` returns `false` — that disabled Previous
    button is the visible proof of FIFO at defense. `next()` on empty throws `EmptyStructureException`.
  - **Verification:** `mvn test -Dtest=ArrivalModeTest#previousAlwaysUnsupported`

- [x] **[A2-05] Test suite for `ArrivalMode`**
  - **Files:** `src/test/java/com/discoballplayer/playback/ArrivalModeTest.java`
  - **Objective:** FIFO order matches library insertion order; queue drains to empty; exhausted
    `next()` throws; `hasPrevious()` is false at every step; the source `MusicLibrary` is untouched.
  - **Verification:** `mvn test -Dtest=ArrivalModeTest` — all green.

- [x] **[A2-06] `AlphabeticalMode` over `BST`**
  - **Files:** `playback/AlphabeticalMode.java`, `src/test/java/com/discoballplayer/playback/AlphabeticalModeTest.java`
  - **Objective:** Builds a `BST<Song>` on `load()` and steps the `A1-07` cursor. Ordering comes
    from `Song.compareTo` (title, case-insensitive, tie-broken on `id`).
  - **Verification:** `mvn test -Dtest=AlphabeticalModeTest#visitsSongsInTitleOrder`

- [x] **[A2-07] Test suite for `AlphabeticalMode`**
  - **Files:** `src/test/java/com/discoballplayer/playback/AlphabeticalModeTest.java`
  - **Objective:** Title order forward; reverse order backward; `hasNext()` false at the last
    song; `hasPrevious()` false at the first; unsorted library input still yields sorted output.
  - **Verification:** `mvn test -Dtest=AlphabeticalModeTest` — all green.

### A3 — Player façade (`service/`)

- [x] **[A3-01] `Player` — mode delegation half**
  - **Files:** `service/Player.java`, `src/test/java/com/discoballplayer/service/PlayerTest.java`
  - **Objective:** Implements `PlayerService`; holds a `MusicLibrary` and the active
    `PlaybackMode`. `setMode()` calls `mode.load(library)`. `next`/`previous`/`current`/
    `hasNext`/`hasPrevious` delegate straight through.
  - **Verification:** `mvn test -Dtest=PlayerTest#setModeReloadsFromLibrary`

- [x] **[A3-02] `Player` — library CRUD half**
  - **Files:** `service/Player.java`, `src/test/java/com/discoballplayer/service/PlayerTest.java`
  - **Objective:** `addSong`, `removeSong`, `updateSong`, `listAll`, `search`, `rate` delegate to
    `MusicLibrary`. `rate` validates 0–100 and rethrows as `IllegalArgumentException`.
    `removeSong` on an unknown song throws `SongNotFoundException`.
  - **Verification:** `mvn test -Dtest=PlayerTest#rateRejectsOutOfRangeValues`

- [x] **[A3-03] `Player` — listener registry and event dispatch**
  - **Files:** `service/Player.java`, `src/test/java/com/discoballplayer/service/PlayerTest.java`
  - **Objective:** `addListener`/`removeListener` over a copy-on-write list; fire `onSongChanged`
    from `next`/`previous`, `onPlaybackStateChanged` from `play`/`pause`, `onLibraryChanged` from
    every CRUD method. A throwing listener must not break the loop.
  - **Verification:** `mvn test -Dtest=PlayerTest#notifiesListenersOnSongChange`

- [x] **[A3-04] Test suite for `Player`**
  - **Files:** `src/test/java/com/discoballplayer/service/PlayerTest.java`
  - **Objective:** Cover the three tickets above with a recording fake `PlaybackListener`.
  - **Verification:** `mvn test -Dtest=PlayerTest` — all green.

### A4 — JSON persistence (`repository/`)

- [x] **[A4-01] Jackson DTOs for the model**
  - **Files:** `repository/dto/SongDto.java`, `repository/dto/LibraryDto.java`
  - **Objective:** `Song` has no no-arg constructor and a final generated `id`, so Jackson cannot
    bind it directly. Flat DTOs carry `id`, title, artist names, album title, duration, genre,
    year, rating, coverPath, audioPath. **Do not add Jackson annotations to `model/`.**
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[A4-02] `LibraryMapper` between DTO and model**
  - **Files:** `repository/LibraryMapper.java`, `src/test/java/com/discoballplayer/repository/LibraryMapperTest.java`
  - **Objective:** `LibraryDto toDto(MusicLibrary)` and `MusicLibrary toModel(LibraryDto)`,
    de-duplicating `Artist` and `Album` instances by name/title on the way back.
  - **Verification:** `mvn test -Dtest=LibraryMapperTest#roundTripPreservesEveryField`

- [x] **[A4-03] `JsonLibraryRepository`**
  - **Files:** `repository/JsonLibraryRepository.java`, `src/test/java/com/discoballplayer/repository/JsonLibraryRepositoryTest.java`
  - **Objective:** Reads and writes `~/.discoballplayer/library.json` via Jackson. A missing file
    means an empty library, not an exception. Write to a temp file and move, so a crash mid-save
    cannot corrupt the catalogue.
  - **Verification:** `mvn test -Dtest=JsonLibraryRepositoryTest#missingFileYieldsEmptyLibrary`

- [x] **[A4-04] Test suite for the repository**
  - **Files:** `src/test/java/com/discoballplayer/repository/JsonLibraryRepositoryTest.java`
  - **Objective:** Save-then-load round trip over `@TempDir`; missing file; malformed JSON
    surfaces a clear exception rather than a Jackson stack trace.
  - **Verification:** `mvn test -Dtest=JsonLibraryRepositoryTest` — all green.

### A5 — Audio and progress (`playback/audio/`)

- [x] **[A5-01] `AudioEngine` interface**
  - **Files:** `playback/audio/AudioEngine.java`
  - **Objective:** `load(Song)`, `play()`, `pause()`, `stop()`, `elapsedSeconds()`,
    `setProgressCallback(IntConsumer)`. Keeps `Player` independent of whether audio is real.
  - **Verification:** `mvn clean compile` exits 0.

- [x] **[A5-02] `SimulatedAudioEngine` (timer-driven)**
  - **Files:** `playback/audio/SimulatedAudioEngine.java`, `src/test/java/com/discoballplayer/playback/SimulatedAudioEngineTest.java`
  - **Objective:** `ScheduledExecutorService` ticking once a second up to `song.getDurationSeconds()`,
    then signalling completion. This is the guaranteed-working progress bar; real audio is a bonus.
  - **Verification:** `mvn test -Dtest=SimulatedAudioEngineTest#emitsOneTickPerSecond`

- [x] **[A5-03] Test suite for `SimulatedAudioEngine`**
  - **Files:** `src/test/java/com/discoballplayer/playback/SimulatedAudioEngineTest.java`
  - **Objective:** Ticks advance while playing, freeze on `pause`, reset on `stop`, and stop at
    the song duration. Use an injectable clock so the test does not sleep for minutes.
  - **Verification:** `mvn test -Dtest=SimulatedAudioEngineTest` — all green.

- [x] **[A5-04] Wire `AudioEngine` into `Player`**
  - **Files:** `service/Player.java`, `src/test/java/com/discoballplayer/service/PlayerTest.java`
  - **Objective:** `Player` takes an `AudioEngine` by constructor, forwards `play`/`pause`, and
    republishes engine ticks as `onProgress`. On track completion it calls `next()` if `hasNext()`.
  - **Verification:** `mvn test -Dtest=PlayerTest#advancesToNextSongOnCompletion`

- [ ] **[A5-05] [BONUS] `JavaFxAudioEngine` over `javafx.media`**
  - **Files:** `playback/audio/JavaFxAudioEngine.java`
  - **Objective:** Real MP3/WAV playback from `song.getAudioPath()`. Falls back to
    `SimulatedAudioEngine` when the path is null or the file will not open.
  - **Verification:** `mvn javafx:run`, load a real MP3, audio is audible and the bar tracks it.

---

## 5. TRACK B — UI, UX and controllers

Runs in parallel with Track A, compiling against `PlayerService` and running against
`DemoPlayerService` (`F0-10`). **Track B never imports `com.discoballplayer.structures`** —
if it does, that is a bug, not a shortcut.

Every `PlaybackListener` callback body is wrapped in `Platform.runLater`: events arrive from a
background timer thread and touching a node off the FX thread throws at runtime.

### B1 — Application shell

- [x] **[B1-01] Three-region layout in `main-view.fxml`**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`
  - **Objective:** Replace the placeholder with a `BorderPane`: left sidebar (mode selector,
    filters), centre (library table), bottom (now-playing bar). Empty containers with `fx:id`s;
    no controls yet.
  - **Verification:** `mvn javafx:run` opens a window with three visibly distinct regions.

- [x] **[B1-02] `MainController` holds a `PlayerService`**
  - **Files:** `ui/MainController.java`
  - **Objective:** Field `private PlayerService player = new DemoPlayerService();` (one line to
    swap in `C-01`), plus `initialize()` registering the controller as a `PlaybackListener`.
  - **Verification:** `mvn javafx:run` starts with no FXML injection exception in the console.

### B2 — Library view

- [x] **[B2-01] Library `TableView` columns**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`
  - **Objective:** `TableView<Song>` with columns Title, Artist, Album, Duration, Genre, Year,
    Rating. Column widths proportional; the table grows with the window.
  - **Verification:** `mvn javafx:run` shows seven headed columns.

- [x] **[B2-02] Populate the table from `player.listAll()`**
  - **Files:** `ui/MainController.java`
  - **Objective:** Bind cell value factories, load rows in `initialize()`, refresh on
    `onLibraryChanged`. Duration renders through `TimeFormatter.mmss`.
  - **Verification:** `mvn javafx:run` lists the 12 demo songs with formatted durations.

- [x] **[B2-03] Search box filtering the table**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`, `ui/MainController.java`
  - **Objective:** A `TextField` whose text listener calls `player.search(query)` and replaces the
    table's items. Empty query restores the full list.
  - **Verification:** `mvn javafx:run`, type an artist name, only matching rows remain.

### B3 — Now-playing panel

- [x] **[B3-01] Now-playing bar markup**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`
  - **Objective:** Bottom bar: `ImageView` cover (64×64), title and artist labels, elapsed /
    total labels, `ProgressBar`. `fx:id`s only, wiring comes next.
  - **Verification:** `mvn javafx:run` shows the bar with placeholder text.

- [x] **[B3-02] Render metadata on `onSongChanged`**
  - **Files:** `ui/MainController.java`
  - **Objective:** Update title, artist and cover from the incoming `Song`. Missing or unreadable
    `coverPath` falls back to `images/default-cover.png` — never an exception, never a blank box.
  - **Verification:** `mvn javafx:run`, press Next, metadata changes.

- [x] **[B3-03] Default cover asset**
  - **Files:** `resources/com/discoballplayer/images/default-cover.png`
  - **Objective:** Ship a 512×512 placeholder so `B3-02`'s fallback resolves.
  - **Verification:** the file exists and `mvn javafx:run` renders it for a song with no cover.

- [x] **[B3-04] Progress bar driven by `onProgress`**
  - **Files:** `ui/MainController.java`
  - **Objective:** Set `progressBar.setProgress(elapsed / (double) total)` and both time labels,
    inside `Platform.runLater`. Reset to 0 on song change.
  - **Verification:** `mvn javafx:run`, press Play, the bar advances once a second.

### B4 — Transport and mode selection

- [x] **[B4-01] Transport controls markup**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`
  - **Objective:** Previous / Play-Pause / Next buttons with `fx:id` and `onAction` handlers.
  - **Verification:** `mvn javafx:run` shows three clickable buttons.

- [x] **[B4-02] Transport handlers and Play/Pause toggle**
  - **Files:** `ui/MainController.java`
  - **Objective:** Handlers call `player.next()`, `player.previous()`, `player.play()`/`pause()`.
    The button label follows `onPlaybackStateChanged`, not local state.
  - **Verification:** `mvn javafx:run`, Play flips the label to Pause and back.

- [x] **[B4-03] Mode selector and Previous-button disabling**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`, `ui/MainController.java`
  - **Objective:** Three radio buttons (Shuffle / Arrival / Alphabetical) calling
    `player.setMode(...)`. After every navigation, `previousButton.setDisable(!player.hasPrevious())`.
  - **Grading note:** the Previous button greying out in Arrival mode is the demo moment for the
    "three playback modes" rubric item. Get it right.
  - **Verification:** `mvn javafx:run`, pick Arrival, Previous is greyed out; pick Shuffle, it enables.

- [x] **[B4-04] Guard the UI against an exhausted queue**
  - **Files:** `ui/MainController.java`
  - **Objective:** Catch `EmptyStructureException` around `next()` and show a status message
    ("Queue finished") instead of letting the exception reach the FX event loop.
  - **Verification:** `mvn javafx:run`, Arrival mode, press Next past the last song — a message,
    no stack trace in the console.

- [x] **[B4-05] Drive `DemoPlayerService` through the active `PlaybackMode`**
  - **Files:** `service/DemoPlayerService.java`, `src/test/java/com/discoballplayer/ui/TransportControlsTest.java`
  - **Objective:** Found while doing `B4-03`. The stub answered `hasPrevious()` with
    `!songs.isEmpty()` and walked an index, so Previous could never grey out and the queue
    could never run dry — `B4-03` and `B4-04` were both unverifiable against it. `setMode`
    now loads the mode from a snapshot of the demo catalogue and `next`/`previous`/`hasNext`/
    `hasPrevious`/`current` delegate to it, falling back to the index walk when no mode is set.
  - **Scope limit:** the stub delegates to a `PlaybackMode`; it never names or imports a
    structure. Replacing it with the real `Player` is still `C-01`.
  - **Verification:** `mvn test -Dtest=TransportControlsTest#previousIsDisabledInArrivalOrder`

### B5 — Add / edit dialog

- [x] **[B5-01] `song-dialog.fxml` form**
  - **Files:** `resources/com/discoballplayer/fxml/song-dialog.fxml`
  - **Objective:** `GridPane` with fields for title, artist, album, duration, genre
    (`ComboBox<Genre>`), year, rating, cover path, audio path, plus Save / Cancel.
  - **Verification:** the file loads in Scene Builder / `FXMLLoader` without error.

- [x] **[B5-02] `SongDialogController` — read and write the form**
  - **Files:** `ui/SongDialogController.java`
  - **Objective:** `setSong(Song)` fills the form for edit mode (null means create); `getResult()`
    returns a built `Song`. Validation errors mark the field, they do not throw.
  - **Verification:** `mvn javafx:run`, open the dialog, empty title is rejected with a visible message.

- [x] **[B5-03] File pickers for cover and audio**
  - **Files:** `ui/SongDialogController.java`
  - **Objective:** Two `FileChooser` buttons storing **absolute paths**. Audio files are never
    copied into `resources/`.
  - **Verification:** `mvn javafx:run`, pick a file, the absolute path appears in the field.

- [x] **[B5-04] Wire Add / Edit / Delete from the main view**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`, `ui/MainController.java`
  - **Objective:** Three toolbar buttons. Add opens an empty dialog then `player.addSong`; Edit
    opens the selected row then `player.updateSong`; Delete calls `player.removeSong`. Edit and
    Delete are disabled when nothing is selected.
  - **Verification:** `mvn javafx:run`, add a song — it appears in the table immediately.

- [x] **[B5-05] Rating control (0–100)**
  - **Files:** `ui/MainController.java`
  - **Objective:** A `Slider` (0–100, snap to 5) in the now-playing bar calling
    `player.rate(current, value)` and refreshing the row.
  - **Verification:** `mvn javafx:run`, drag the slider, the table's Rating cell follows.

### B6 — Visual design (10% of the grade)

- [x] **[B6-01] Design tokens and base theme in `app.css`**
  - **Files:** `resources/com/discoballplayer/css/app.css`
  - **Objective:** Define the palette as CSS `-fx-` looked-up colours in `.root`, then style
    backgrounds, typography and spacing. Every later rule references a token, never a raw hex.
  - **Verification:** `mvn javafx:run` — no unstyled default-grey panels.

- [x] **[B6-02] Table, button and slider styling**
  - **Files:** `resources/com/discoballplayer/css/app.css`
  - **Objective:** Row hover and selection states, a distinct primary style for Play, a visibly
    dimmed `:disabled` state (Previous in Arrival mode must *read* as disabled).
  - **Verification:** `mvn javafx:run` — hover, selection and disabled states are all distinguishable.

- [x] **[B6-03] [BONUS] Dark theme and toggle**
  - **Files:** `resources/com/discoballplayer/css/dark.css`, `ui/MainController.java`
  - **Objective:** `dark.css` overrides only the token block. A toolbar toggle swaps the
    stylesheet on the scene at runtime.
  - **Verification:** `mvn javafx:run`, click the toggle, the whole window re-themes with no relayout.

- [x] **[B6-04] [BONUS] Keyboard shortcuts**
  - **Files:** `ui/MainController.java`
  - **Objective:** Space = play/pause, ←/→ = previous/next, `Ctrl+F` focuses search,
    `Ctrl+N` opens the add dialog. Registered on the scene, not on individual buttons.
  - **Verification:** `mvn javafx:run`, press Space, playback toggles.

---

### B7 — Fixes from manual testing

Found by using the application rather than by reading it. Track A fixed the backend of all
three in `#35`; these are the two that stayed invisible until the UI caught up.

- [x] **[B7-01] Double-clicking a row plays that song**
  - **Files:** `ui/MainController.java`, `src/test/java/com/discoballplayer/ui/ClickToPlayTest.java`
  - **Objective:** A row's double click calls `player.playSong(song)`. Both refusals become a
    status message: `UnsupportedOperationException` when the mode cannot reposition, and
    `SongNotFoundException` when the song is not loaded in it. Either reaching the FX event
    loop breaks the window.
  - **Constraint:** availability comes from `canPlaySong()`, re-read on every mode change. The
    UI never names the mode, exactly as the Previous button reads `hasPrevious()`.
  - **Verification:** `mvn test -Dtest=ClickToPlayTest#arrivalOrderRefusesTheJumpAndSaysWhy`

- [x] **[B7-02] Duration read from the chosen audio file**
  - **Files:** `ui/SongDialogController.java`, `src/test/java/com/discoballplayer/ui/SongDialogControllerTest.java`
  - **Objective:** Picking an audio file fills `durationField` from
    `AudioMetadata.durationSeconds`, formatted with `TimeFormatter.mmss` so `parseDuration`
    reads it back. An unreadable file leaves the field untouched and always typeable — a song
    with no audio can only get its duration by being typed.
  - **Constraint:** read off the FX thread. The reader blocks for up to three seconds on a file
    it cannot measure, and this runs from a file-chooser handler.
  - **Verification:** `mvn test -Dtest=SongDialogControllerTest#theDurationIsNeverReadOnTheFxThread`

- [x] **[B7-03] Make `DemoPlayerService.playSong` go through the active mode**
  - **Files:** `service/DemoPlayerService.java`, `src/test/java/com/discoballplayer/ui/ClickToPlayTest.java`
  - **Objective:** The version added under the ownership-crossing walked the `ArrayList` index
    directly, so the stub accepted a jump in arrival order and never raised
    `UnsupportedOperationException` — the one behaviour click-to-play has to get right, and the
    same class of gap `B4-05` closed for `hasPrevious`. It now delegates to the mode, as the
    rest of the class does and as `Player` does.
  - **Verification:** `mvn test -Dtest=ClickToPlayTest#arrivalOrderIsAskedRatherThanNamed`

---

### B8 — Duration comes from the file, not from the user

- [x] **[B8-01] Fix `AudioMetadata` so it can actually measure a file**
  - **Files:** `playback/audio/AudioMetadata.java`
  - **Objective:** The reader waited on the `Media` metadata map, but that map holds tags —
    artist, title, album art — while `getDuration()` stays `UNKNOWN` until a `MediaPlayer`
    prepares the stream. Any untagged file, which is most of them, timed out and answered
    empty. It now reads from a `MediaPlayer` reaching `READY`, releases the latch on both
    error paths, and always disposes the player.
  - **Ownership crossing:** this file is Track A's. Recorded below and in Cross-track requests.
  - **Verification:** `mvn test -Dtest=SongDialogControllerTest#aRealAudioFileIsMeasuredByTheRealReader`

- [x] **[B8-02] Duration stops being an input**
  - **Files:** `resources/com/discoballplayer/fxml/song-dialog.fxml`, `ui/SongDialogController.java`, `src/test/java/com/discoballplayer/ui/SongDialogControllerTest.java`
  - **Objective:** The duration row is a derived display, not a text field. It shows what the
    chosen audio file reports, or "Read from the audio file" before there is one. Creating a
    song with no measurable duration is refused and the message points at the audio field.
    Editing keeps a stored duration, so a catalogue entry with no audio file still saves.
  - **Consequence:** `parseDuration` is gone with the field that fed it.
  - **Verification:** `mvn test -Dtest=SongDialogControllerTest#theDurationIsNotSomethingTheUserCanType`

- [x] **[B8-03] The dialog inherits the whole theme**
  - **Files:** `ui/MainController.java`
  - **Objective:** The dialog copied only the first stylesheet from the main scene, so it opened
    in the light theme while the window behind it stayed dark. It copies all of them now.
  - **Verification:** `mvn javafx:run`, dark theme, open Add — the form is dark too.

---

### B9 — Playback experience, from using the app

- [x] **[B9-01] A newly added song joins the mode already playing**
  - **Files:** `playback/PlaybackMode.java`, `playback/AbstractPlaybackMode.java`, the three modes, `service/Player.java`, `service/DemoPlayerService.java`
  - **Objective:** `PlaybackMode.add(Song)`. Each mode inserts where its own structure says the
    song belongs — the back of the queue in arrival order, the sorted position in the tree, the
    ring in shuffle — instead of rebuilding, which would re-randomize shuffle and refill a
    drained queue. The two cursor-driven modes re-seat afterwards, because insertion
    invalidates a live cursor by design.
  - **Ownership crossing:** `playback/` and two Track A test doubles.
  - **Verification:** `mvn test -Dtest=ControllerInjectionTest#aSongAddedThroughTheRealPlayerJoinsTheModeItIsPlaying`

- [x] **[B9-02] Choosing a mode starts it playing**
  - **Files:** `ui/MainController.java`
  - **Objective:** Choosing a mode is a request to hear it, not to arm it.
  - **Verification:** `mvn test -Dtest=TransportControlsTest#choosingAModeStartsItPlaying`

- [x] **[B9-03] Volume control**
  - **Files:** `playback/audio/AudioEngine.java` and both engines, `service/PlayerService.java`, `service/Player.java`, `service/DemoPlayerService.java`, `ui/`, `fxml`
  - **Objective:** `setVolume`/`getVolume` from 0.0 to 1.0, clamped rather than rejected. The
    simulated engine has no output to attenuate but still holds the level, so a song with no
    audio file does not reset what the user chose.
  - **Ownership crossing:** `playback/audio/`, `service/`, and two Track A test doubles.
  - **Verification:** `mvn test -Dtest=FiltersAndVolumeTest#movingTheVolumeSliderReachesTheService`

- [x] **[B9-04] Sidebar filters**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`, `ui/MainController.java`
  - **Objective:** Genre, artist and minimum rating, combining with each other and with the
    search box through one predicate. The artist list is derived from the library, so an artist
    is an option exactly as long as a song credits them. A filter hides rows; it never deletes.
  - **Verification:** `mvn test -Dtest=FiltersAndVolumeTest#filtersCombineRatherThanReplaceEachOther`

- [x] **[B9-05] Icon transport controls**
  - **Files:** `resources/com/discoballplayer/fxml/main-view.fxml`, `resources/com/discoballplayer/css/app.css`, `ui/MainController.java`
  - **Objective:** Inline `SVGPath` glyphs — no image assets, and they take their colour from
    the stylesheet. The play glyph becomes a pause glyph from `onPlaybackStateChanged`, not
    from the click. The text stays as the accessible name and the tooltip.
  - **Verification:** `mvn test -Dtest=FiltersAndVolumeTest#theIconFollowsTheServiceRatherThanTheClick`

---

### B10 — Fixes from the suite itself

- [x] **[B10-01] The UI tests stop racing the FX thread**
  - **Files:** `src/test/java/com/discoballplayer/ui/JavaFxTestBase.java` and the five test
    classes that load `main-view.fxml`
  - **Objective:** `B9-05` was the first test to assert on a widget written by a
    `PlaybackListener` callback, and it exposed a fault the suite had carried since `B1`: a
    controller's `initialize` runs inside `load()`, every callback body is required to defer
    itself with `Platform.runLater`, and the `@BeforeEach` read those widgets before that pulse
    ran. The result passed or failed depending on which thread won — roughly one run in five.
    Loading now goes through one `loadView` helper that drains the pulse, so the five classes
    cannot drift back into the race one copy at a time.
  - **Note:** a race cannot be pinned by a single run in either direction. The evidence is the
    rate: before, 5 failures in 8 runs of the class; after, 6 clean class runs and 8 clean runs
    of the full 322-test suite.
  - **Verification:** `mvn test -Dtest=FiltersAndVolumeTest#theIconFollowsTheServiceRatherThanTheClick`

---

## 6. Phase C — Integration and defense material (both tracks, sequential)

Starts only when Track A reaches `A5-04` and Track B reaches `B5-05`. Run these in order.

- [x] **[C-01] Accept an injected `PlayerService` in `MainController`** *(Track B)*
  - **Files:** `ui/MainController.java`
  - **Objective:** Replace the inline `new DemoPlayerService()` with a constructor parameter,
    keeping a no-argument constructor that supplies the demo stub so the FXML still loads
    standalone in Scene Builder and in the UI tests.
  - **Why it changed:** the ticket used to say "construct the real `Player` here". That would
    have given the controller its own `MusicLibrary` while `C-02` loaded a different one from
    disk — the saved library would never reach the UI and edits would never be saved. Nothing
    would throw; the app would just silently lose everything on restart. Composition belongs
    in `Main`, which is `C-02`.
  - **Verification:** `mvn javafx:run` still opens on demo data; `new MainController(service)`
    compiles.

- [x] **[C-02] Compose the object graph in `Main`** *(Track A, needs `C-01`)*
  - **Files:** `Main.java`
  - **Objective:** Build `JsonLibraryRepository` → `MusicLibrary` (seeded from `SampleLibrary`
    when empty) → `SimulatedAudioEngine` → `Player`, inject it through
    `FXMLLoader.setControllerFactory`, and on `stop()` save the library and call
    `Player.dispose()` to release the ticker thread.
  - **One library instance only.** The controller must never construct its own; that is the
    whole point of `C-01`.
  - **Verification:** `mvn javafx:run`, add a song, close, reopen — the song is still there.

- [x] **[C-03] Seed data for the demo**
  - **Files:** `util/SampleLibrary.java`
  - **Objective:** ~20 songs across ≥5 genres and ≥3 albums, loaded only when the repository comes
    back empty, so the defense never opens on a blank window.
  - **Verification:** `mvn javafx:run` with no `library.json` shows 20 songs.

- [ ] **[C-04] Full-suite green run**
  - **Files:** none (fix-forward only)
  - **Objective:** `mvn clean test` with every suite passing and no skipped tests.
  - **Verification:** `mvn clean test` — `Failures: 0, Errors: 0, Skipped: 0`.

- [x] **[C-05] Align `README.md` with the locked names**
  - **Files:** `README.md`
  - **Objective:** `BinarySearchTree` → `BST` in the modes table and the layout tree; add
    `service/PlayerService` to the architecture diagram; point at `docs/plan.md`.
  - **Verification:** `grep -c BinarySearchTree README.md` returns `0`.

- [x] **[C-06] Class diagram**
  - **Files:** `docs/diagrams/class-diagram.md`
  - **Objective:** A Mermaid `classDiagram` covering `model`, `structures`, `playback`, `service`,
    `repository` and the arrows between layers.
  - **Verification:** the fenced block renders on GitHub.

- [x] **[C-07] Complexity table for the defense**
  - **Files:** `docs/complexity.md`
  - **Objective:** Insertion, deletion, search and traversal, average and worst case, for all
    three structures, each row justified in one sentence.
  - **Verification:** nine populated rows, every cell justified.

- [x] **[C-08] Rubric self-audit**
  - **Files:** `docs/rubric-check.md`
  - **Objective:** One row per rubric criterion with the files and tests that satisfy it, plus an
    explicit `grep` proving no `java.util` collection is used as a playback structure.
  - **Verification:** `grep -rn "java.util.\(List\|Queue\|Deque\|TreeMap\|TreeSet\)" src/main/java/com/discoballplayer/structures/`
    returns nothing.

---

## 7. `CLAUDE.md` content for [F0-01]

Write the block below to `CLAUDE.md` at the repository root, verbatim.

````markdown
# DiscoBallPlayer

Desktop music player for the **Languages and Compilers** course at Universidad EIA.
The point is **not** a commercial music player. It is to implement three data structures by
hand and prove that playback behaviour changes with the structure backing it. Every member
must be able to defend, orally, why a structure was chosen, how it works internally, and its
complexity for insertion, deletion, search and traversal.

## Working with `docs/plan.md`

`docs/plan.md` is the single source of truth for what is done and what is next.

1. Read it before starting any work. Pick the lowest-numbered unticked ticket in **your track**.
2. Do exactly one ticket per branch and per pull request. Never batch tickets.
3. Tick the box (`- [ ]` → `- [x]`) **in the same PR as the work**, only after the ticket's
   verification command passes.
4. Never edit a ticket owned by the other track, and never edit a file the ownership table
   assigns to the other track. Append to "Cross-track requests" at the bottom of the plan instead.
5. Scope discovered mid-ticket goes in as a **new ticket**; it does not get folded into the
   current one.

## Commands

```bash
mvn clean compile                       # compile
mvn test                                # full test suite
mvn test -Dtest=BSTTest                 # one test class
mvn test -Dtest=BSTTest#deleteLeafNode  # one test method
mvn javafx:run                          # launch the application
mvn clean package                       # build the jar
```

## Ground rules

1. **Everything in the repo is in English.** Code, identifiers, comments, Javadoc, commits,
   branches, PR titles, docs. No Spanish.
2. **The three structures are hand-written and generic.** No `java.util.LinkedList`, `Queue`,
   `Deque`, `TreeMap`, `TreeSet` or `Collections.sort` inside `structures` or `playback`.
   `ArrayList` is allowed only in `ui` and in `model` as entity storage — never as a playback
   structure.
3. **The UI never touches a data structure.** It talks to `PlayerService` only. A `ui` class
   importing from `structures` is a bug.
4. **`AlphabeticalMode` never flattens the tree into a list.** Navigation goes through
   in-order successor/predecessor with parent pointers. This is the first thing the professor
   looks for.
5. **No direct commits to `main` or `develop`.** Every change arrives by PR from a
   `feature/` or `fix/` branch.
6. Ask before adding a dependency. The list in `pom.xml` is the whole list.

## Tech stack

Java 21 · JavaFX 21 (FXML + CSS) · Maven (`javafx-maven-plugin`) · JUnit 5 · Jackson Databind ·
`javafx.media` for bonus audio. Nothing else without discussion.

## Architecture

```
JavaFX UI  →  PlayerService  →  PlaybackMode  →  three hand-written structures
                    ↓
              MusicLibrary  →  LibraryRepository (JSON)
```

`MusicLibrary` is the single source of truth for the catalogue. Each playback mode **builds its
own structure** from the library on `load()`, which is why `ArrivalMode` can consume its queue
without destroying the catalogue, and why adding a song does not mean keeping three structures
in sync by hand.

`PlayerService` is the seam between the tracks: the UI compiles against the interface, so it
can run on `DemoPlayerService` while the real `Player` is still being written.

| Mode | Structure | Behaviour |
|---|---|---|
| Shuffle | `DoublyCircularLinkedList<Song>` | Order randomized once on activation; wraps infinitely both ways |
| Arrival | `SimpleQueue<Song>` | Strict FIFO; a played song leaves the queue; Previous is disabled |
| Alphabetical | `BST<Song>` | In-order navigation by title via successor/predecessor |

## Track ownership

| Track | Owns |
|---|---|
| A (backend) | `structures/`, `playback/`, `repository/`, `util/`, `exception/`, `model/`, `service/` (except the demo), `module-info.java`, all of `src/test/` |
| B (UI) | `ui/`, `fxml/`, `css/`, `images/`, `service/DemoPlayerService.java` |

`module-info.java` was finalized in `F0-09` and is not edited again.

## Java practices

- Fields `private`; expose behaviour, not state. `final` on anything set once.
- Validate at the boundary: rating is 0–100 inclusive, `Genre` is an enum.
- Custom exceptions over `null` returns. A structure never returns `null` for "empty" — it
  throws `EmptyStructureException`.
- **Javadoc on every public method of `structures`, stating complexity:**
  `@implNote Time complexity: O(log n) average, O(n) worst case.` These are the defense script.
- Descriptive English names: `inOrderSuccessor`, not `sig` or `nodo2`.
- `equals` and `hashCode` together, always.
- No `System.out.println` in committed code.
- No business logic in FXML controllers. A controller reads input, calls `PlayerService`,
  updates widgets. If it decides anything about playback order, it is in the wrong layer.
- Every `PlaybackListener` callback body in the UI runs inside `Platform.runLater`.

## Testing

Tests live in `src/test/java` mirroring the main packages. Structures carry 35% of the grade,
so they carry the tests. Minimum before a structure PR merges:

- **`DoublyCircularLinkedList`** — insert into empty; wrap forward and backward; delete head,
  tail, middle and only element; size correct throughout.
- **`SimpleQueue`** — FIFO across many enqueues; dequeue on empty throws; peek does not remove.
- **`BST`** — in-order returns sorted; delete leaf / one child / two children; successor and
  predecessor at both boundaries; duplicate titles both survive.
- **Modes** — `ArrivalMode.hasPrevious()` always false; `ShuffleMode` next-then-previous returns
  the same song; `AlphabeticalMode` visits in title order.

## Git workflow

GitFlow. `main` and `develop` are long-lived; everything else is deleted after merge.
Branch names are lowercase English with the ticket code: `feature/a1-03-simple-queue`.

```bash
git checkout develop && git pull origin develop
git checkout -b feature/a1-03-simple-queue
# work in small commits
mvn test
git push -u origin feature/a1-03-simple-queue   # then open a PR into develop
```

Never force-push `main` or `develop`. Rebase `develop` into your branch to resolve conflicts on
your side. `mvn test` must pass before the PR is opened — a red `develop` blocks both tracks.

Conventional Commits, English, imperative, subject under 72 characters:

```
feat(structures): add generic simple queue
fix(playback): keep previous() consistent after shuffle reload
test(structures): cover BST deletion with two children
```

Scopes: `model`, `structures`, `playback`, `service`, `repository`, `ui`, `util`, `build`, `docs`.

### PR description template

```markdown
## What
One or two sentences.

## Why
Which plan ticket and rubric item this covers.

## How to verify
The ticket's verification command.

## Checklist
- [ ] `mvn test` passes
- [ ] No java.util collection used as a playback structure
- [ ] Javadoc with complexity on new public structure methods
- [ ] The ticket box is ticked in docs/plan.md
- [ ] No Spanish in code or comments
```

## Grading rubric

| Criterion | Weight | Where it lives |
|---|---|---|
| Data structures work correctly | 35% | `structures/` + tests |
| Graphical interface implemented | 20% | `ui/`, `fxml/` |
| Code quality (OOP, organization) | 15% | Everywhere |
| Three playback modes fulfilled | 15% | `playback/` |
| Interface design and UX | 10% | `css/`, layout |
| Creativity and extras | 5% | Bonus tickets |
````

---

## 8. Cross-track requests

**Docs split for `C-04`–`C-08`:** `C-06` (class diagram) and `C-07` (complexity table) are
Track A's — they document Track A's structures and are its oral-defense script. `C-05` (README)
and `C-08` (rubric audit) are Track B's. No shared files.

Append here when you need something from a file the other track owns. Format:
`- [ ] (from Track X to Track Y) <what and why>`.

- [x] (from Track A to Track B) `C-01` was rewritten. Do **not** construct a `Player` inside
  `MainController`. Add a constructor taking a `PlayerService`, keep the no-argument one
  delegating to `DemoPlayerService`, and let `Main` compose the real graph in `C-02`. Two
  `MusicLibrary` instances would silently discard everything the user saved.
  **Done.** `MainController(PlayerService)` is the injected constructor and the no-argument one
  delegates to `DemoPlayerService`, so `FXMLLoader` and Scene Builder still load the view
  standalone. `ControllerInjectionTest` asserts the view shows the injected library and that an
  edit reaches it; reverting to a self-built service fails it with "the view is showing a
  catalogue nobody injected". `C-02` is unblocked — compose through
  `FXMLLoader.setControllerFactory`.

- [ ] (from Track B to Track A) `A1-10` is still open on `DoublyCircularLinkedList`.
  `BST` and `SimpleQueue` carry `Time complexity` on every public method; the list carries it
  on none of its eleven — the four matches in that file are all inside `Cursor`. `CLAUDE.md`
  calls these comments the oral-defense script and structures are 35% of the grade, so of
  everything still unticked this is the one that touches the heaviest criterion. Recorded in
  `docs/rubric-check.md` under Open items. The file is Track A's; Track B did not touch it.
  Re-check with
  `grep -c "Time complexity" src/main/java/com/discoballplayer/structures/DoublyCircularLinkedList.java`
  — currently `4`.

- [ ] (from Track B to Track A) `B9-01` and `B9-03` widen two contracts you own.
  `PlaybackMode` gains `add(Song)` and `AudioEngine` gains `setVolume`/`getVolume`. Both were
  user-reported gaps: a song added while a mode was playing was unreachable until the mode was
  reselected, and there was no way to change the volume at all.

  `AbstractPlaybackMode` owns the add-then-reseat sequence the same way it owns
  `load`/`loadStructure`, so each mode implements `insertIntoStructure` and nothing else.
  Rebuilding on add was rejected deliberately: it re-randomizes shuffle, which makes Previous
  meaningless, and it refills a queue arrival order has already drained.

  Widening a shared interface obliges its implementors, so three of your test doubles gained
  the new methods — `PlayerTest.StubMode`, `PlayerTest.FakeAudio` and
  `MainLibraryLoadingTest.RecordingEngine`. Each records rather than acts, and each is marked
  with a comment saying Track B added it. Please review those alongside the contracts.

- [x] (from Track B to Track A) `AudioMetadata.durationSeconds` returned empty for any file
  without metadata tags, after burning the full timeout. It waited on the `Media` metadata
  map, but that map carries tags — artist, title, album art — while the duration only becomes
  known once a `MediaPlayer` reaches `READY`. A tagless file never fired the listener.

  Measured on a generated three-second WAV, before and after:

  ```
  before:  AUDIOMETADATA=OptionalInt.empty  tookMs=3056
  after:   AUDIOMETADATA=OptionalInt[3]     tookMs=111
  ```

  **Fixed by Track B in `B8-01`, crossing into Track A's file.** The user hit this by hand and
  asked for it directly, and the ticket that depends on it could not work without it. Same
  reasoning Track A used when it added `playSong` to `DemoPlayerService`: leaving the feature
  broken was worse than crossing. Please review — it is a small change, and
  `SongDialogControllerTest#aRealAudioFileIsMeasuredByTheRealReader` covers it with a real,
  untagged WAV. Reverting to the metadata-map wait fails that test with `expected: <3> but
  was: <0>`.

  The reader still blocks until the decoder is ready, so it must not be called on the FX
  thread; `SongDialogController` reads it on a worker.

- [x] (from Track B to Track A) Open `src/test/java/com/discoballplayer/ui/` to Track B.
  The coherent-unit rule asks every PR to carry the tests that verify it, but the ownership
  table assigned all of `src/test/` to Track A and no Track B ticket named a test file, so a
  UI pull request could not satisfy the rule as written. Track B was verifying by loading the
  real FXML through `FXMLLoader`, rendering the scene off-screen and reading the result, which
  catches injection and binding failures but leaves nothing in the suite.
  **Granted.** A track that cannot test what it writes cannot verify its own tickets. The
  ownership table above now assigns that directory to Track B; Track A keeps the rest of
  `src/test/`. Those off-screen FXML checks should now be committed as real tests.
