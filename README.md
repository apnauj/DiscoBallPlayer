# DiscoBallPlayer

Desktop music player written in Java, built for the **Languages and Compilers** course at
Universidad EIA.

The goal is not a commercial music player. It is to implement three data structures by hand
and show that playback behaviour changes depending on which structure backs it:

| Playback mode | Backing structure | Behaviour |
|---|---|---|
| Shuffle | `DoublyCircularLinkedList<Song>` | Order randomized once on activation; navigation wraps infinitely in both directions |
| Arrival | `SimpleQueue<Song>` | Strict FIFO; a played song leaves the queue and Previous is disabled |
| Alphabetical | `BinarySearchTree<Song>` | In-order traversal by title, forward and backward via successor/predecessor |

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

## Project layout

```
src/main/java/com/discoballplayer
├── Main.java            Application entry point
├── model/               Song, Genre, Artist, Album, Playlist, MusicLibrary
├── structures/          The three hand-written generic structures
├── playback/            PlaybackMode interface and its three implementations
├── service/             Player facade — the only thing the UI talks to
├── repository/          JSON persistence
├── ui/                  JavaFX controllers and components
├── util/                Helpers
└── exception/           SongNotFoundException, EmptyStructureException

src/main/resources/com/discoballplayer
├── fxml/                main-view.fxml, song-dialog.fxml
├── css/                 app.css, dark.css
└── images/              default-cover.png

src/test/java/com/discoballplayer
├── structures/          Structure test suites
└── playback/            Playback mode test suites
```

Resources mirror the package path so `getClass().getResource("fxml/main-view.fxml")` keeps
resolving once the application is packaged.

Audio files are **not** resources. The user picks MP3/WAV files from disk and `Song` stores
the absolute path.

## Architecture

```
JavaFX UI  →  Player (facade)  →  PlaybackMode (interface)  →  three implementations
                    ↓                                              ↓
             MusicLibrary                              one hand-written structure each
                    ↓
            LibraryRepository
```

`MusicLibrary` is the single source of truth for the catalogue. Each playback mode builds its
own structure from the library when the mode is activated, so `ArrivalMode` can consume its
queue without destroying the catalogue.

The UI never touches a data structure directly.

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

See [CLAUDE.md](CLAUDE.md) for the full contribution guide: ground rules, the `PlaybackMode`
contract, testing baseline and the work split.

## Team

Each member owns one structure end to end — the structure, its playback mode, its tests and
its UI behaviour.
