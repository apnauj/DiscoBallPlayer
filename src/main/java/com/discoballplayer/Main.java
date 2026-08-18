package com.discoballplayer;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application entry point. Loads the main view and shows the primary window.
 *
 * <p>Resources are resolved relative to this class, so the package path
 * {@code com/discoballplayer/} is implicit and the lookup keeps working once the
 * application is packaged into a jar.</p>
 */
public class Main extends Application {

    private static final String MAIN_VIEW = "fxml/main-view.fxml";
    private static final String STYLESHEET = "css/app.css";

    private static final String WINDOW_TITLE = "DiscoBallPlayer";
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 600;

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(resource(MAIN_VIEW));

        Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
        scene.getStylesheets().add(resource(STYLESHEET).toExternalForm());

        stage.setTitle(WINDOW_TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Resolves a resource bundled next to this class.
     *
     * @throws IllegalStateException if the resource is missing, which means the build
     *         did not copy it rather than something a user can recover from at runtime
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
