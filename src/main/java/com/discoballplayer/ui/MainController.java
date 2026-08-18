package com.discoballplayer.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for {@code main-view.fxml}.
 *
 * <p>Placeholder for Phase 3. It reads input, calls {@code Player} and updates widgets;
 * it never decides anything about playback order and never touches
 * {@code com.discoballplayer.structures} directly.</p>
 */
public class MainController {

    @FXML
    private Label statusLabel;

    /**
     * Called by {@link javafx.fxml.FXMLLoader} once the widget tree is built.
     */
    @FXML
    private void initialize() {
        statusLabel.setText("No song loaded");
    }
}
