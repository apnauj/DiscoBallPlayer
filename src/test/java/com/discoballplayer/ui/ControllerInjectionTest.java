package com.discoballplayer.ui;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import com.discoballplayer.model.Artist;
import com.discoballplayer.model.Genre;
import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.model.Song;
import com.discoballplayer.playback.AlphabeticalMode;
import com.discoballplayer.playback.ShuffleMode;
import com.discoballplayer.playback.audio.SimulatedAudioEngine;
import com.discoballplayer.service.DemoPlayerService;
import com.discoballplayer.service.Player;
import com.discoballplayer.service.PlayerService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the seam {@code Main} composes through.
 *
 * <p>The point of these is narrow and worth stating: the controller must show the very library
 * it was handed, not one of its own. If it built its own, the view and the file on disk would
 * be two different catalogues — nothing would throw, and every edit would vanish on restart.
 * {@link #showsTheInjectedLibraryAndNotOneOfItsOwn()} is the test that would catch that.</p>
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class ControllerInjectionTest extends JavaFxTestBase {

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
    }

    @Test
    void theNoArgumentConstructorFallsBackToTheDemoStub() {
        MainController controller = new MainController();

        assertInstanceOf(DemoPlayerService.class, controller.player(),
                "FXMLLoader uses this one, so the view has to stay loadable standalone");
    }

    @Test
    void anInjectedServiceIsTheOneTheControllerUses() {
        PlayerService injected = new Player(new MusicLibrary(), new SimulatedAudioEngine());

        MainController controller = new MainController(injected);

        assertSame(injected, controller.player());
    }

    @Test
    void aNullServiceIsRefusedAtConstruction() {
        assertThrows(NullPointerException.class, () -> new MainController(null));
    }

    @Test
    void showsTheInjectedLibraryAndNotOneOfItsOwn() {
        MusicLibrary library = new MusicLibrary();
        library.addSong(new Song("Only Song In The World", List.of(new Artist("Nobody")), null,
                123, Genre.JAZZ, 1999));
        Player player = new Player(library, new SimulatedAudioEngine());

        Parent root = loadWith(player);

        @SuppressWarnings("unchecked")
        TableView<Song> table = (TableView<Song>) root.lookup("#libraryTable");
        assertEquals(1, table.getItems().size(),
                "the view is showing a catalogue nobody injected");
        assertEquals("Only Song In The World", table.getItems().get(0).getTitle());

        player.dispose();
    }

    @Test
    void anEditThroughTheViewReachesTheInjectedLibrary() {
        MusicLibrary library = new MusicLibrary();
        Player player = new Player(library, new SimulatedAudioEngine());
        loadWith(player);

        Song added = new Song("Added Through The Seam", List.of(new Artist("Nobody")), null,
                200, Genre.ROCK, 2020);
        player.addSong(added);

        assertTrue(library.getAllSongs().contains(added),
                "an edit that does not reach the injected library is an edit that will not be saved");

        player.dispose();
    }

    @Test
    void aSongAddedThroughTheRealPlayerJoinsTheModeItIsPlaying() {
        // The UI suites run on the demo stub, so without this the production path -- Player,
        // a real mode, a real structure -- had nothing asserting that an added song is
        // reachable. Removing mode.add from Player.addSong passed every other test.
        MusicLibrary library = new MusicLibrary();
        library.addSong(new Song("First", List.of(new Artist("Nobody")), null, 100, Genre.ROCK, 2020));
        Player player = new Player(library, new SimulatedAudioEngine());
        player.setMode(new ShuffleMode());
        player.next();

        Song latecomer = new Song("Added While Playing", List.of(new Artist("Nobody")), null,
                100, Genre.JAZZ, 2024);
        player.addSong(latecomer);

        assertSame(latecomer, player.playSong(latecomer),
                "a song added while a mode is playing has to be reachable in it");
        player.dispose();
    }

    @Test
    void addingASongThroughTheRealPlayerDoesNotRestartTheMode() {
        MusicLibrary library = new MusicLibrary();
        for (String title : List.of("Aaa", "Bbb", "Ccc")) {
            library.addSong(new Song(title, List.of(new Artist("Nobody")), null,
                    100, Genre.ROCK, 2020));
        }
        Player player = new Player(library, new SimulatedAudioEngine());
        player.setMode(new AlphabeticalMode());
        player.next();
        player.next();
        Song playing = player.current();

        player.addSong(new Song("Aaa0 Sorts First", List.of(new Artist("Nobody")), null,
                100, Genre.ROCK, 2020));

        assertSame(playing, player.current(),
                "an insertion must not move the cursor off the song being played");
        player.dispose();
    }

    /** Loads the main view the way {@code Main} will: through a controller factory. */
    private Parent loadWith(PlayerService service) {
        Parent[] root = new Parent[1];
        onFxThread(() -> {
            URL view = Objects.requireNonNull(
                    MainController.class.getResource("/com/discoballplayer/fxml/main-view.fxml"),
                    "main-view.fxml is not on the test classpath");
            FXMLLoader loader = new FXMLLoader(view);
            loader.setControllerFactory(type -> new MainController(service));
            try {
                root[0] = loader.load();
            } catch (IOException e) {
                throw new IllegalStateException("main-view.fxml failed to load", e);
            }
        });
        flushFxThread();
        return root[0];
    }
}
