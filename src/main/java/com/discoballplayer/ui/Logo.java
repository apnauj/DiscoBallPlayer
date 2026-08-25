package com.discoballplayer.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Loads the application logo and lifts it off its background.
 *
 * <p>The artwork ships as a JPEG, and JPEG has no alpha channel: dropped straight onto the
 * dance floor it arrives as a white square with a logo inside it. The background is removed
 * here rather than asked for as a PNG, so the application looks right with the file that
 * actually exists.</p>
 *
 * <p>The removal is a flood fill inward from the border, not a "make every white pixel
 * transparent" pass. The logo has white in it -- a lit mirror tile, the highlight on the note
 * head -- and keying on colour alone punches holes through both. Only background that is
 * connected to the edge is background.</p>
 *
 * <p>Every failure here is survivable and none of them stop the window from opening. A logo is
 * decoration; an application that refuses to start because a decoration is missing has its
 * priorities backwards.</p>
 */
public final class Logo {

    private static final Logger LOG = Logger.getLogger(Logo.class.getName());

    private static final String LOGO = "/com/discoballplayer/images/logo.jpeg";

    /**
     * How pale a pixel has to be before it counts as background. Generous on purpose: the
     * artwork is anti-aliased against white, so a strict threshold leaves a pale fringe. At the
     * sizes the logo is drawn, anything this misses disappears in the downscale.
     */
    private static final double BACKGROUND_PALENESS = 0.88;

    private static Image cached;
    private static boolean attempted;

    private Logo() {
    }

    /**
     * The logo with its background removed, or {@code null} if it could not be loaded.
     *
     * <p>Cached: the fill walks every pixel of a 1024-square image, and the window asks for the
     * logo more than once.</p>
     *
     * @return the logo, or {@code null} when the file is missing or unreadable
     */
    public static synchronized Image image() {
        if (attempted) {
            return cached;
        }
        attempted = true;
        try {
            if (Logo.class.getResource(LOGO) == null) {
                LOG.log(Level.INFO, "No logo at {0}; carrying on without one.", LOGO);
                return null;
            }
            Image raw = new Image(Logo.class.getResourceAsStream(LOGO));
            if (raw.isError() || raw.getWidth() == 0) {
                LOG.log(Level.WARNING, "The logo could not be decoded; carrying on without one.");
                return null;
            }
            cached = withoutBackground(raw);
        } catch (RuntimeException broken) {
            LOG.log(Level.WARNING, "The logo could not be read; carrying on without one.", broken);
        }
        return cached;
    }

    /** Visible for tests: forgets the cached logo so a test can exercise the load again. */
    static synchronized void forget() {
        cached = null;
        attempted = false;
    }

    /**
     * Copies the image, clearing every pale pixel reachable from the border.
     *
     * @implNote Time complexity: O(n) in the number of pixels; each is queued at most once.
     */
    private static Image withoutBackground(Image raw) {
        int width = (int) raw.getWidth();
        int height = (int) raw.getHeight();

        PixelReader source = raw.getPixelReader();
        WritableImage lifted = new WritableImage(width, height);
        PixelWriter target = lifted.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                target.setArgb(x, y, source.getArgb(x, y));
            }
        }

        boolean[] cleared = new boolean[width * height];
        Deque<int[]> pending = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            consider(x, 0, source, cleared, pending, width, height);
            consider(x, height - 1, source, cleared, pending, width, height);
        }
        for (int y = 0; y < height; y++) {
            consider(0, y, source, cleared, pending, width, height);
            consider(width - 1, y, source, cleared, pending, width, height);
        }

        while (!pending.isEmpty()) {
            int[] pixel = pending.removeFirst();
            target.setColor(pixel[0], pixel[1], Color.TRANSPARENT);
            consider(pixel[0] - 1, pixel[1], source, cleared, pending, width, height);
            consider(pixel[0] + 1, pixel[1], source, cleared, pending, width, height);
            consider(pixel[0], pixel[1] - 1, source, cleared, pending, width, height);
            consider(pixel[0], pixel[1] + 1, source, cleared, pending, width, height);
        }
        return lifted;
    }

    private static void consider(int x, int y, PixelReader source, boolean[] cleared,
                                 Deque<int[]> pending, int width, int height) {
        if (x < 0 || y < 0 || x >= width || y >= height || cleared[y * width + x]) {
            return;
        }
        if (!isPale(source.getColor(x, y))) {
            return;
        }
        cleared[y * width + x] = true;
        pending.addLast(new int[] {x, y});
    }

    private static boolean isPale(Color colour) {
        return colour.getRed() >= BACKGROUND_PALENESS
                && colour.getGreen() >= BACKGROUND_PALENESS
                && colour.getBlue() >= BACKGROUND_PALENESS;
    }
}
