# Class diagram

The layering the project is built on, and the seams that make it defensible.

Read it in one sentence: **the UI talks only to `PlayerService`, `PlayerService` delegates
ordering to `PlaybackMode`, and each mode owns one hand-written structure.** Nothing above the
`playback` package ever names a structure.

## Layers and their seams

```mermaid
flowchart TB
    subgraph ui["ui — JavaFX"]
        MC["MainController"]
        SDC["SongDialogController"]
    end

    subgraph service["service"]
        PS{{"PlayerService<br/><i>interface</i>"}}
        PL["Player"]
        DEMO["DemoPlayerService<br/><i>UI stub</i>"]
        LIS{{"PlaybackListener<br/><i>interface</i>"}}
    end

    subgraph playback["playback"]
        PM{{"PlaybackMode<br/><i>interface</i>"}}
        APM["AbstractPlaybackMode<br/><i>abstract</i>"]
        SM["ShuffleMode"]
        AM["ArrivalMode"]
        ALM["AlphabeticalMode"]
        AE{{"AudioEngine<br/><i>interface</i>"}}
        SAE["SimulatedAudioEngine"]
    end

    subgraph structures["structures — hand written"]
        DCLL["DoublyCircularLinkedList~T~"]
        SQ["SimpleQueue~T~"]
        BST["BST~T~"]
    end

    subgraph model["model"]
        ML["MusicLibrary"]
        SONG["Song"]
    end

    subgraph repository["repository"]
        LR{{"LibraryRepository<br/><i>interface</i>"}}
        JLR["JsonLibraryRepository"]
    end

    MAIN["Main — composition root"]

    MC -->|"only ever this"| PS
    SDC --> SONG
    MC -.->|implements| LIS
    PS -.-|implemented by| PL
    PS -.-|implemented by| DEMO
    PL --> PM
    PL --> AE
    PL --> ML
    PL -->|notifies| LIS
    PM -.-|extended by| APM
    APM --> SM
    APM --> AM
    APM --> ALM
    SM -->|"randomized once"| DCLL
    AM -->|"strict FIFO"| SQ
    ALM -->|"in-order cursor"| BST
    AE -.-|implemented by| SAE
    ML --> SONG
    LR -.-|implemented by| JLR
    MAIN --> JLR
    MAIN --> ML
    MAIN --> PL
    MAIN --> MC
```

**The three dashed interface edges are the whole design.** `PlayerService` lets the UI be built
against a stub while the backend is written. `PlaybackMode` lets the same buttons produce three
different behaviours. `AudioEngine` lets the progress bar work without any audio file existing.

## The structures

Only these three classes are hand written. No `java.util` collection backs playback.

```mermaid
classDiagram
    class DoublyCircularLinkedList~T~ {
        -Node tail
        -int size
        +insertAtEnd(T) void
        +insertAtBeginning(T) void
        +delete(T) boolean
        +has(T) boolean
        +get(int) T
        +size() int
        +isEmpty() boolean
        +cursor() Cursor
    }
    class DoublyCircularLinkedList_Cursor {
        -Node node
        +current() T
        +next() T
        +previous() T
    }
    class SimpleQueue~T~ {
        -Node head
        -Node tail
        -int size
        +enqueue(T) void
        +dequeue() T
        +peek() T
        +size() int
        +isEmpty() boolean
        ~tailData() T
    }
    class BST~T~ {
        -Node root
        -int size
        +insert(T) void
        +delete(T) void
        +contains(T) boolean
        +find(T) T
        +size() int
        +isEmpty() boolean
        +cursor() Cursor
        -successor(Node) Node
        -predecessor(Node) Node
        ~parentPointersAreConsistent() boolean
    }
    class BST_Cursor {
        -Node node
        +current() T
        +hasNext() boolean
        +next() T
        +hasPrevious() boolean
        +previous() T
    }
    DoublyCircularLinkedList~T~ *-- DoublyCircularLinkedList_Cursor : nested
    BST~T~ *-- BST_Cursor : nested
```

Three details a reviewer will look for:

- **`Node` is private in all three.** Navigation leaves through a `Cursor`, so no caller can
  reach into the structure. The two package-private methods (`tailData`,
  `parentPointersAreConsistent`) are test seams for invariants no behavioural assertion can
  observe — retention, and dangling parent pointers.
- **`BST` keeps a `parent` reference per node.** That is what makes `successor` and
  `predecessor` possible without flattening the tree into a list.
- **The two cursors differ deliberately.** `DoublyCircularLinkedList.Cursor` has no `hasNext`:
  a ring has no end. `BST.Cursor` has both, because an in-order walk does.

## Why each mode uses the structure it uses

```mermaid
flowchart LR
    S["Shuffle"] --> D["DoublyCircularLinkedList"]
    A["Arrival"] --> Q["SimpleQueue"]
    L["Alphabetical"] --> B["BST"]
    D --> D1["wraps in both directions<br/>so Previous is meaningful"]
    Q --> Q1["dequeue is permanent<br/>so Previous is impossible"]
    B --> B1["in-order successor<br/>gives sorted order for free"]
```

The behaviour differences are not enforced by `if` statements anywhere. They fall out of the
structures: `hasPrevious()` returns `false` in arrival order because a queue genuinely cannot
go back, and the UI greys the button out from that value alone.

## Ownership of the catalogue

`MusicLibrary` is the single source of truth. Each mode **builds its own structure** from it on
`load()` and never re-reads it. Two consequences worth being able to state out loud:

1. `ArrivalMode` can drain its queue to empty without touching the catalogue.
2. A song added mid-session appears in playback only when a mode is next selected. That is a
   deliberate trade: reloading on every edit would reset the shuffle order mid-song and refill
   a queue the user had already played through.

`Main` is the composition root and the only place these pieces are assembled, so exactly one
`MusicLibrary` exists. A controller building its own would show a catalogue nobody loaded and
save edits nobody reads — silently.
