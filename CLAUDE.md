# DiscoBallPlayer

Desktop music player built for the **Languages and Compilers** course at Universidad EIA.
The point of this project is **not** to build a commercial music player. It is to implement
three data structures by hand and prove that the playback behaviour changes depending on
which structure backs it.

Every team member must be able to defend, in the oral defense, why a structure was chosen,
how it works internally, and its time complexity for insertion, deletion, search and traversal.

---

## Ground rules

1. **Everything is in English.** Code, identifiers, comments, Javadoc, commit messages,
   branch names, PR titles and descriptions, README, and this file. No Spanish in the repo.
2. **The three core data structures are implemented from scratch and are generic.**
   No `java.util.LinkedList`, `Queue`, `Deque`, `TreeMap`, `TreeSet`, or `Collections.sort`
   inside `structures` or `playback`. `ArrayList` is allowed only in the UI layer and in
   `MusicLibrary` as backing storage for the catalogue, never as a playback structure.
3. **The UI never touches a data structure directly.** It talks to `Player`, which talks to
   the `PlaybackMode` interface. If a UI class imports anything from `structures`, that is a bug.
4. **No work happens on `main` or `develop` directly.** Every change arrives through a pull
   request from a `feature/` or `fix/` branch into `develop`.
5. Ask before adding a dependency. The dependency list below is the whole list.

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | Java 21 (LTS) |
| UI | JavaFX 21 (FXML + CSS) |
| Build | Maven (`javafx-maven-plugin`) |
| Tests | JUnit 5 (`junit-jupiter`) |
| JSON persistence | Jackson Databind |
| Audio (bonus) | `javafx.media` |

Nothing else without discussion.

---

## Repository layout

```
DiscoBallPlayer
├── pom.xml
├── README.md
├── .gitignore
├── docs/
│   └── diagrams/
└── src
    ├── main
    │   ├── java
    │   │   ├── module-info.java
    │   │   └── com/discoballplayer
    │   │       ├── Main.java
    │   │       ├── model
    │   │       │   ├── Song.java
    │   │       │   ├── Genre.java              (enum)
    │   │       │   └── MusicLibrary.java
    │   │       ├── structures
    │   │       │   ├── DoublyCircularLinkedList.java
    │   │       │   ├── SimpleQueue.java
    │   │       │   └── BinarySearchTree.java
    │   │       ├── playback
    │   │       │   ├── PlaybackMode.java       (interface)
    │   │       │   ├── AbstractPlaybackMode.java
    │   │       │   ├── ShuffleMode.java
    │   │       │   ├── ArrivalMode.java
    │   │       │   └── AlphabeticalMode.java
    │   │       ├── service
    │   │       │   └── Player.java             (facade)
    │   │       ├── repository
    │   │       │   ├── LibraryRepository.java  (interface)
    │   │       │   └── JsonLibraryRepository.java
    │   │       ├── ui
    │   │       │   ├── MainController.java
    │   │       │   ├── SongDialogController.java
    │   │       │   └── components/
    │   │       ├── util
    │   │       │   └── TimeFormatter.java
    │   │       └── exception
    │   │           ├── SongNotFoundException.java
    │   │           └── EmptyStructureException.java
    │   └── resources/com/discoballplayer
    │       ├── fxml/    main-view.fxml, song-dialog.fxml
    │       ├── css/     app.css, dark.css
    │       └── images/  default-cover.png
    └── test/java/com/discoballplayer
        ├── structures/
        └── playback/
```

Notes on the layout:

- Resources mirror the package path so `getClass().getResource("fxml/main-view.fxml")`
  resolves with a relative path and keeps working once packaged.
- `module-info.java` must contain `opens com.discoballplayer.ui to javafx.fxml;` and
  `opens com.discoballplayer.model to com.fasterxml.jackson.databind;`, or FXML injection
  and JSON serialization fail at runtime with confusing reflection errors.
- Audio files are **not** resources. The user picks MP3/WAV files from disk; `Song` stores
  the absolute path. Never copy audio into `src/main/resources`.

---

## Architecture

```
JavaFX UI  →  Player (facade)  →  PlaybackMode (interface)  →  three implementations
                    ↓                                              ↓
             MusicLibrary                              one hand-written structure each
                    ↓
            LibraryRepository
```

**`MusicLibrary` is the single source of truth.** It owns the catalogue. Each playback mode
*builds* its own structure from the library when the mode is activated. This is why
`ArrivalMode` can consume its queue without destroying the catalogue, and why adding a song
does not require keeping three structures in sync by hand.

### Contracts

Agree on these signatures **before** splitting the work. They are the seam between the
three people; changing them later means rework for everyone.

```java
public interface PlaybackMode {
    void load(MusicLibrary library);
    Song next();              // throws EmptyStructureException when exhausted
    Song previous();          // throws UnsupportedOperationException in ArrivalMode
    boolean hasNext();
    boolean hasPrevious();    // always false in ArrivalMode
    Song current();
    String displayName();
}
```

`AbstractPlaybackMode` holds the shared state (`current`, the library reference) and provides
default `previous()` / `hasPrevious()` behaviour so `ArrivalMode` only overrides what differs.
That is inheritance where it is genuinely warranted — do not force it anywhere else.

`Player` exposes `setMode(PlaybackMode)`, `next()`, `previous()`, `play()`, `pause()`,
`addSong(Song)`, `removeSong(Song)`, `search(String)`, and `rate(Song, int)`. The UI knows
`Player` and `Song`. Nothing else.

### Mode-specific requirements

**`ShuffleMode` — `DoublyCircularLinkedList<Song>`**
Randomize the **insertion order** once, when the mode is activated. Do not randomize on each
`next()` call — if you do, `previous()` becomes meaningless. Navigation is infinite in both
directions; past the last node it wraps to the first. There is never an end of playback.

**`ArrivalMode` — `SimpleQueue<Song>`**
Strict FIFO. `next()` dequeues and the song leaves the queue permanently. `previous()` throws
`UnsupportedOperationException` and `hasPrevious()` returns `false`, so the UI disables the
Previous button. That disabled button is the visible proof of FIFO behaviour during the defense.

**`AlphabeticalMode` — `BinarySearchTree<Song>`**
Ordered by song title. Navigation follows the in-order traversal, forward and backward.
Store a `parent` reference in each node and implement `inOrderSuccessor` / `inOrderPredecessor`
in O(log n). **Do not flatten the traversal into a list and index into it** — that defeats the
purpose of the exercise and is the first thing the professor will look for.
Comparison key is `(title, artist)` so songs with duplicate titles are not swallowed by the tree.

---

## Git workflow

GitFlow. `main` and `develop` are long-lived; everything else is short-lived and deleted
after merge.

| Branch | Purpose | Branches from | Merges into |
|---|---|---|---|
| `main` | Tagged, deliverable releases only | — | — |
| `develop` | Integration branch, always green | `main` | — |
| `feature/<scope>` | New functionality | `develop` | `develop` |
| `fix/<scope>` | Bug fix on unreleased work | `develop` | `develop` |
| `hotfix/<scope>` | Urgent fix on a delivered version | `main` | `main` + `develop` |
| `release/<version>` | Freeze before delivery | `develop` | `main` + `develop` |

Branch names are lowercase, English, hyphen-separated:
`feature/doubly-circular-linked-list`, `fix/bst-duplicate-titles`, `release/1.0.0`.

### Cycle for every unit of work

```bash
git checkout develop
git pull origin develop
git checkout -b feature/simple-queue
# ... work, committing in small steps ...
mvn test
git push -u origin feature/simple-queue
# open a PR into develop, get a review, merge, delete the branch
git checkout develop && git pull origin develop
```

Rules:

- **Never commit directly to `main` or `develop`.** Never force-push either one.
- Every branch merges into `develop` through a **pull request** with at least one approving
  review from another team member. The author does not approve their own PR.
- Always `git pull origin develop` before opening a PR, and rebase or merge `develop` into
  your branch to resolve conflicts on your side — not in the PR.
- Delete the branch after merge.
- `mvn test` must pass before a PR is opened. A red `develop` blocks the whole team.

### Commit messages

Conventional Commits, in English, imperative mood, subject under 72 characters:

```
feat(structures): add generic doubly circular linked list
fix(playback): keep previous() consistent after shuffle reload
test(structures): cover BST deletion with two children
docs(readme): document build and run instructions
refactor(ui): extract song row into reusable component
chore(build): configure javafx-maven-plugin
```

Scopes: `model`, `structures`, `playback`, `service`, `repository`, `ui`, `util`, `build`, `docs`.

### PR description template

```markdown
## What
One or two sentences.

## Why
Which requirement or rubric item this covers.

## How to verify
Steps to run, or the tests that cover it.

## Checklist
- [ ] `mvn test` passes
- [ ] No java.util collections used as playback structures
- [ ] Javadoc on public methods with time complexity noted
- [ ] No Spanish left in code or comments
```

---

## Java practices for this repo

- **Encapsulation.** Fields are `private`; expose behaviour, not state. `Song` is immutable
  apart from `rating`; use a builder or a full constructor rather than a no-arg constructor
  plus setters.
- **Validation at the boundary.** `rating` is 0–100 inclusive; reject anything else with
  `IllegalArgumentException`. `Genre` is an enum, not a free-text string.
- **Custom exceptions over null returns.** `SongNotFoundException` and
  `EmptyStructureException` extend `RuntimeException`. A structure method never returns
  `null` to signal "empty".
- **Javadoc on every public method of `structures`**, and it must state the time complexity:
  `@implNote Time complexity: O(log n) average, O(n) worst case.` These comments are the
  script for the oral defense — write them as you go, not the night before.
- **Naming.** Classes `PascalCase`, methods and fields `camelCase`, constants
  `UPPER_SNAKE_CASE`, packages lowercase. Names are descriptive and in English:
  `inOrderSuccessor`, not `sig` or `nodo2`.
- **Small methods, one responsibility.** If a method needs a comment to explain its middle
  section, extract that section into a named method.
- **`equals` and `hashCode` together**, always, on `Song`.
- **`final` on fields that never change** after construction.
- **No `System.out.println` in committed code.** Use `java.util.logging` or surface the
  message in the UI.
- **No business logic in FXML controllers.** A controller reads input, calls `Player`,
  updates widgets. If it decides something about playback order, it is in the wrong layer.

---

## Testing

Unit tests live in `src/test/java` and mirror the main package structure. The structures carry
35% of the grade, so they carry the tests.

Minimum coverage before a structure PR is merged:

- **`DoublyCircularLinkedList`** — insert into empty list; wrap forward from tail to head;
  wrap backward from head to tail; delete head, tail, and a middle node; delete the only
  element; size stays correct throughout.
- **`SimpleQueue`** — FIFO order preserved across many enqueues; dequeue on empty throws
  `EmptyStructureException`; `isEmpty` and `size` behave; peek does not remove.
- **`BinarySearchTree`** — in-order traversal returns sorted order; delete a leaf, a node
  with one child, and a node with two children; successor and predecessor at the boundaries
  (last element has no successor, first has no predecessor); duplicate titles by different
  artists both survive.
- **Playback modes** — `ArrivalMode.hasPrevious()` is always `false`; `ShuffleMode` wraps
  infinitely and `next()` then `previous()` returns to the same song; `AlphabeticalMode`
  visits songs in title order.

---

## Implementation order

Work through the phases in order. Phase 1 has to be solid before anything else starts —
a broken structure poisons every layer above it.

**Phase 1 — Foundation**
`pom.xml`, `module-info.java`, `.gitignore`, `README.md`, `main`/`develop` branches.
`Song`, `Genre`, `MusicLibrary`. The three generic structures with their full test suites.
No UI at all in this phase.

**Phase 2 — Playback layer**
`PlaybackMode`, `AbstractPlaybackMode`, the three modes, `Player`. Tests for each mode.
Still no UI — verify from tests, not by clicking.

**Phase 3 — Functional UI**
`main-view.fxml` and `MainController`: song library table, current-song panel, cover art,
progress bar, mode selector, and the Add / Delete / Edit / Search / Play / Pause / Next /
Previous buttons. Ugly is fine here; wired correctly is not optional. The Previous button
must disable itself from `hasPrevious()`.

**Phase 4 — Polish and bonus points**
CSS styling, dark mode, JSON persistence via `JsonLibraryRepository`, album covers,
filters by artist/genre/album, favourites, playback history, statistics, keyboard shortcuts,
real MP3/WAV playback with a live progress bar.

**Phase 5 — Defense material**
`docs/`: class diagram, and a complexity table covering insertion, deletion, search and
traversal for all three structures.

---

## Work split (3 people)

Each person owns one structure end to end: the structure, its playback mode, its tests, and
the UI behaviour specific to it. Since each member must defend "their" structure orally, this
is the only split where nobody shows up unable to explain what they built.

- **Person A** — `DoublyCircularLinkedList` + `ShuffleMode`
- **Person B** — `SimpleQueue` + `ArrivalMode`
- **Person C** — `BinarySearchTree` + `AlphabeticalMode`

`Song`, `Genre`, `MusicLibrary`, `PlaybackMode`, and `Player` are defined by all three
together in one sitting, on a single `feature/core-contracts` branch, before the split.

---

## Grading rubric

| Criterion | Weight | Where it lives |
|---|---|---|
| Data structures work correctly | 35% | `structures/` + its tests |
| Graphical interface implemented | 20% | `ui/`, `fxml/` |
| Code quality (OOP, organization, practices) | 15% | Everywhere |
| Three playback modes fulfilled | 15% | `playback/` |
| Interface design and user experience | 10% | `css/`, layout |
| Creativity and extra features | 5% | Phase 4 |

Bonus points: real MP3/WAV playback, live progress bar, album covers, dark mode, persistence,
filters, favourites, playback history, statistics, keyboard shortcuts, and generics
(already covered by making the three structures generic).
