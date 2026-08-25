package com.discoballplayer.ui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;

/**
 * The mirror ball, drawn rather than shipped as an image.
 *
 * <p>Drawing it keeps it sharp at any size and lets it take its colours from the theme, which a
 * PNG could not do. It is three layers: a sphere, a cage of mirror facets that turns, and a
 * highlight that does not.</p>
 *
 * <p>The highlight staying still is the whole illusion. A ball whose every layer rotates reads
 * as a flat disc being spun; keeping the lit spot fixed while the facets pass underneath is
 * what makes it read as a sphere turning under a lamp.</p>
 */
final class DiscoBall extends Group {

    private static final int LATITUDES = 5;
    private static final int LONGITUDES = 6;

    private final Group facets = new Group();

    /**
     * @param radius radius of the ball in pixels
     */
    DiscoBall(double radius) {
        getStyleClass().add("disco-ball");

        getChildren().add(mount(radius));
        getChildren().add(sphere(radius));
        buildFacets(radius);
        getChildren().add(facets);
        getChildren().add(highlight(radius));
    }

    /** The layer that turns. Handed to the animator so nothing else has to know the structure. */
    Group spinningPart() {
        return facets;
    }

    private Line mount(double radius) {
        Line rod = new Line(0, -radius * 1.8, 0, -radius);
        rod.getStyleClass().add("disco-ball-mount");
        return rod;
    }

    private Circle sphere(double radius) {
        Circle ball = new Circle(radius);
        ball.setFill(new RadialGradient(
                0, 0, 0.32, 0.28, 0.95, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#e9dcff")),
                new Stop(0.45, Color.web("#9d4edd")),
                new Stop(1, Color.web("#2d1b4e"))));
        return ball;
    }

    /**
     * Latitude lines and longitude ellipses, clipped to the sphere.
     *
     * <p>A longitude drawn as an ellipse narrowing towards the poles is what suggests curvature;
     * straight vertical lines would read as a grid painted on a coin.</p>
     */
    private void buildFacets(double radius) {
        for (int i = 1; i < LATITUDES; i++) {
            double y = -radius + (2 * radius * i / LATITUDES);
            double halfWidth = Math.sqrt(Math.max(0, radius * radius - y * y));
            Line latitude = new Line(-halfWidth, y, halfWidth, y);
            latitude.getStyleClass().add("disco-ball-facet");
            facets.getChildren().add(latitude);
        }
        for (int i = 0; i < LONGITUDES; i++) {
            double width = radius * Math.cos(Math.PI * i / LONGITUDES);
            Ellipse longitude = new Ellipse(0, 0, Math.abs(width), radius);
            longitude.setFill(null);
            longitude.getStyleClass().add("disco-ball-facet");
            facets.getChildren().add(longitude);
        }
        facets.setClip(new Circle(radius));
    }

    private Circle highlight(double radius) {
        Circle lit = new Circle(radius);
        lit.setMouseTransparent(true);
        lit.setFill(new RadialGradient(
                0, 0, 0.3, 0.26, 0.55, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffffff", 0.55)),
                new Stop(1, Color.TRANSPARENT)));
        return lit;
    }
}
