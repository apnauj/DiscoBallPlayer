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

    // Only non-empty packages may be exported; add playback, service and repository
    // here as those packages gain their first class.
    exports com.discoballplayer;
    exports com.discoballplayer.model;
    exports com.discoballplayer.structures;
}
