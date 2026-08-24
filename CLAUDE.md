# DiscoBallPlayer

Desktop music player for the **Languages and Compilers** course at Universidad EIA.
The point is **not** a commercial music player. It is to implement three data structures by
hand and prove that playback behaviour changes with the structure backing it. Every member
must be able to defend, orally, why a structure was chosen, how it works internally, and its
complexity for insertion, deletion, search and traversal.

## Working with `docs/plan.md`

`docs/plan.md` is the single source of truth for what is done and what is next.

1. Read it before starting any work. Pick the lowest-numbered unticked ticket in **your track**.
2. One branch and one pull request per **coherent unit**: a single structure, a single
   playback mode, a single UI panel — implementation plus the tests that verify it. Group the
   tickets that make up that unit; never mix two units in one PR. A PR that ships production
   code without its test is not a coherent unit.
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

Tests live in `src/test/java` mirroring the main packages. Test classes are **flat**: under
`@Nested` the outer class matches `-Dtest=Class#method` and runs zero tests, so a verification
command reports success while testing nothing.

**`Tests run: 0` is a failure even when Maven prints `BUILD SUCCESS`.** Surefire fails on an
unmatched test class but not on an unmatched test method. Read the count, not the colour.

Structures carry 35% of the grade, so they carry the tests. Minimum before a structure PR merges:

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

**Two sessions never share a working directory.** One `HEAD` and one index between two agents
means one session's checkout rewrites the other's working tree and its commits land on the
wrong branch. Track B works from a separate worktree:

```bash
git worktree add ../DiscoBallPlayer-ui develop   # once, from the main checkout
```

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
