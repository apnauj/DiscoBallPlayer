package com.discoballplayer;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.discoballplayer.model.MusicLibrary;
import com.discoballplayer.playback.ShuffleMode;
import com.discoballplayer.playback.audio.JavaFxAudioEngine;
import com.discoballplayer.repository.JsonLibraryRepository;
import com.discoballplayer.repository.LibraryRepository;
import com.discoballplayer.service.Player;
import com.discoballplayer.ui.Logo;
import com.discoballplayer.ui.MainController;
import com.discoballplayer.util.SampleLibrary;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Application entry point, and the only place the object graph is assembled.
 *
 * <p>Composition lives here rather than in the controller so that exactly one
 * {@link MusicLibrary} exists. A controller that built its own would show a catalogue nobody
 * loaded and save edits nobody reads — silently, with nothing thrown and every test still
 * green.</p>
 *
 * <p>Resources are resolved relative to this class, so the package path
 * {@code com/discoballplayer/} is implicit and the lookup keeps working once the application is
 * packaged into a jar.</p>
 */
public class Main extends Application {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private static final String MAIN_VIEW = "fxml/main-view.fxml";
    private static final String STYLESHEET = "css/app.css";

    private static final String WINDOW_TITLE = "DiscoBallPlayer";
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 600;

    private LibraryRepository repository;
    private MusicLibrary library;
    private Player player;

    @Override
    public void start(Stage stage) throws IOException {
        repository = new JsonLibraryRepository();
        library = loadLibrary(repository);
        player = new Player(library, new JavaFxAudioEngine());
        player.setMode(new ShuffleMode());

        Scene scene = new Scene(loadView(), MIN_WIDTH, MIN_HEIGHT);
        scene.getStylesheets().add(resource(STYLESHEET).toExternalForm());

        stage.setTitle(WINDOW_TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        wearTheLogo(stage);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Puts the logo on the window and in the dock, when there is one.
     *
     * <p>Guarded for the same reason the library load is: an icon is decoration, and an
     * application that will not open because a decoration is missing has its priorities
     * backwards. A missing file leaves the platform's default icon in place.</p>
     *
     * <p>Track B author: this method is the only edit to this file, made for B11-04.</p>
     */
    private static void wearTheLogo(Stage stage) {
        Image logo = Logo.image();
        if (logo != null) {
            stage.getIcons().add(logo);
        }
    }

    /**
     * Reads the stored catalogue, seeding it when there is nothing to read.
     *
     * <p>A corrupt file must not stop the application from opening. The user is far better
     * served by a working window on seed data, with the failure logged, than by a stack trace
     * and no way in — their file is left untouched on disk either way.</p>
     */
    static MusicLibrary loadLibrary(LibraryRepository repository) {
        MusicLibrary stored;
        try {
            stored = repository.load();
        } catch (RuntimeException failure) {
            LOG.log(Level.WARNING, "Could not read the stored library; starting from the sample "
                    + "catalogue. The existing file has been left untouched.", failure);
            return SampleLibrary.create();
        }
        return stored.isEmpty() ? SampleLibrary.create() : stored;
    }

    /**
     * Loads the view with the controller built by hand, so the real service reaches it.
     *
     * <p>{@code FXMLLoader.load(url)} would call the no-argument constructor and quietly hand
     * the window the demo stub, which is why the loader is instantiated rather than used
     * statically.</p>
     */
    private Parent loadView() throws IOException {
        FXMLLoader loader = new FXMLLoader(resource(MAIN_VIEW));
        loader.setControllerFactory(type -> new MainController(player));
        return loader.load();
    }

    /**
     * Saves the catalogue and releases the audio engine's ticker thread.
     *
     * <p>A failed save must not stop the window from closing; it is logged instead. Nothing
     * useful happens if the application refuses to exit.</p>
     */
    @Override
    public void stop() {
        shutdown(repository, library, player);
    }

    /**
     * Saves the catalogue and releases the audio engine's ticker thread.
     *
     * <p>Package-private and static so it can be tested. {@code stop()} itself only runs under
     * a live JavaFX toolkit, and an unverified save path is the one place in this application
     * where a bug costs the user everything they have.</p>
     *
     * <p>A failed save is logged, not rethrown: nothing useful happens if the application
     * refuses to exit, and the ticker thread must be released either way.</p>
     */
    static void shutdown(LibraryRepository repository, MusicLibrary library, Player player) {
        try {
            if (repository != null && library != null) {
                repository.save(library);
            }
        } catch (RuntimeException failure) {
            LOG.log(Level.SEVERE, "Could not save the library on shutdown.", failure);
        } finally {
            if (player != null) {
                player.dispose();
            }
        }
    }

    /**
     * Resolves a resource bundled next to this class.
     *
     * @throws IllegalStateException if the resource is missing, which means the build did not
     *         copy it rather than something a user can recover from at runtime
     */
    private URL resource(String path) {
        return Objects.requireNonNull(
                Main.class.getResource(path),
                () -> "Missing resource on the classpath: " + path);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
