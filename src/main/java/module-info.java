/**
 * Module descriptor for DiscoBallPlayer.
 *
 * <p>The two {@code opens} directives are not optional. JavaFX injects {@code @FXML}
 * fields reflectively, and Jackson reads and writes model fields reflectively; without
 * them both fail at runtime with confusing {@code IllegalAccessException} traces.</p>
 */
module com.discoballplayer {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires com.fasterxml.jackson.databind;
    requires java.logging;

    opens com.discoballplayer.ui to javafx.fxml;
    opens com.discoballplayer.model to com.fasterxml.jackson.databind;

    // Finalized in F0-09, once every package had its first class. Only non-empty packages
    // may be exported, so this list is complete and is not edited again.
    exports com.discoballplayer;
    exports com.discoballplayer.model;
    exports com.discoballplayer.structures;
    exports com.discoballplayer.playback;
    exports com.discoballplayer.service;
    exports com.discoballplayer.repository;
    exports com.discoballplayer.util;
    exports com.discoballplayer.exception;
}
