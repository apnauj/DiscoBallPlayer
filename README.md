# DiscoBallPlayer

Desktop music player written in Java, built for the **Languages and Compilers** course at
Universidad EIA.

The goal is not a commercial music player. It is to implement three data structures by hand
and show that playback behaviour changes depending on which structure backs it:

| Playback mode | Backing structure | Behaviour |
|---|---|---|
| Shuffle | `DoublyCircularLinkedList<Song>` | Order randomized once on activation; navigation wraps infinitely in both directions |
| Arrival | `SimpleQueue<Song>` | Strict FIFO; a played song leaves the queue and Previous is disabled |
| Alphabetical | `BST<Song>` | In-order traversal by title, forward and backward via successor/predecessor |

No `java.util` collection is used as a playback structure.

## Prerequisites

- **JDK 21 or newer.** The build targets Java 21 via `maven.compiler.release`, so a newer JDK
  works too.
- **Maven 3.9+.** On macOS: `brew install maven`.

JavaFX is resolved as a normal Maven dependency, so there is no separate SDK to install.

## Build and run

```bash
mvn clean compile     # compile
mvn test              # run the unit tests
mvn javafx:run        # launch the application
mvn clean package     # build the jar
```

`Tests run: 0` is a failure even when Maven prints `BUILD SUCCESS`. Surefire fails on an
unmatched test class but not on an unmatched test method, so a typo in `-Dtest=Class#method`
reports success while running nothing. Read the count, not the colour.

## Project layout

```
src/main/java/com/discoballplayer
├── Main.java            Entry point; composes the object graph and injects it
├── model/               Song, Genre, Artist, Album, Playlist, MusicLibrary
├── structures/          BST, DoublyCircularLinkedList, SimpleQueue — hand-written, generic
├── playback/            PlaybackMode, AbstractPlaybackMode and the three modes
│   └── audio/           AudioEngine and SimulatedAudioEngine
├── service/             PlayerService (the seam), Player, DemoPlayerService, PlaybackListener
├── repository/          LibraryRepository, JSON implementation, DTOs and mapper
├── ui/                  MainController, SongDialogController
├── util/                TimeFormatter, SampleLibrary
└── exception/           EmptyStructureException, SongNotFoundException, PersistenceException

src/main/resources/com/discoballplayer
├── fxml/                main-view.fxml, song-dialog.fxml
├── css/                 app.css (tokens and every rule), dark.css (token overrides only)
└── images/              default-cover.png

src/test/java/com/discoballplayer
├── structures/          One suite per structure
├── playback/            One suite per mode, plus the audio engine
├── service/             Player facade
├── repository/          Persistence round trip and the DTO mapper
├── util/                Seed catalogue
└── ui/                  Controllers, driven through the real FXML
```

Resources mirror the package path so `getClass().getResource("fxml/main-view.fxml")` keeps
resolving once the application is packaged.

Audio files are **not** resources. The user picks MP3/WAV files from disk and `Song` stores
the absolute path.

## Architecture

```
JavaFX UI  →  PlayerService  →  PlaybackMode  →  three implementations
              (interface)      (interface)          ↓
                    ↓                     one hand-written structure each
              MusicLibrary
                    ↓
            LibraryRepository  →  JSON on disk
```

`PlayerService` is the seam. The UI compiles against the interface and never names an
implementation, which is what allowed the interface to be built against `DemoPlayerService`
while `Player` was still being written, and what makes swapping the two a constructor
argument rather than an edit.

`MusicLibrary` is the single source of truth for the catalogue. Each playback mode builds its
own structure from the library when the mode is activated, so `ArrivalMode` can consume its
queue without destroying the catalogue, and adding a song never means reconciling three
structures by hand.

`Main` composes the whole graph and injects it through `FXMLLoader.setControllerFactory`.
There is exactly one `MusicLibrary`: the one loaded from disk is the one the window shows and
the one that gets saved.

The UI never touches a data structure directly. A `ui` class importing from `structures` is a
bug, not a shortcut.

## Branching model

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

Nothing is committed directly to `main` or `develop`. Every change arrives through a pull
request into `develop` with at least one approving review, and `mvn test` must pass before the
pull request is opened.

Commit messages follow Conventional Commits in English, for example
`feat(structures): add generic doubly circular linked list`.

[docs/plan.md](docs/plan.md) is the single source of truth for what is done and what is next.
Every ticket lists the files it touches and the command that verifies it, and a box is ticked
only in the same pull request as the work.

See [CLAUDE.md](CLAUDE.md) for the full contribution guide: ground rules, the `PlaybackMode`
contract, testing baseline and the work split.

## Team

Each member owns one structure end to end — the structure, its playback mode, its tests and
its UI behaviour.
