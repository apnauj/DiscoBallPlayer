package com.discoballplayer.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the logo: that it loads, that it is lifted off the white it ships on, and that the
 * lifting does not eat the white inside the artwork.
 *
 * <p>Flat by design; see {@link MainControllerTest}.</p>
 */
class LogoTest extends JavaFxTestBase {

    private static final double PALE = 0.88;

    private Parent root;

    @BeforeAll
    static void startJavaFx() {
        startToolkit();
    }

    @BeforeEach
    void loadTheView() {
        FXMLLoader loader = loadView("/com/discoballplayer/fxml/main-view.fxml");
        root = loader.getRoot();
    }

    @Test
    void theHeaderWearsTheLogo() {
        ImageView mark = onFxThreadGet(() -> (ImageView) root.lookup("#brandLogo"));
        assertNotNull(mark, "the header has no place for a logo");
        assertNotNull(onFxThreadGet(mark::getImage), "the header has no logo in it");
    }

    /**
     * The artwork ships as a JPEG, and JPEG has no alpha. Dropped straight onto the dance floor
     * it arrives as a white square with a logo inside it.
     */
    @Test
    void theBackgroundIsLiftedOff() {
        Image logo = onFxThreadGet(Logo::image);
        assertNotNull(logo, "there is no logo to check");

        PixelReader pixels = logo.getPixelReader();
        int width = (int) logo.getWidth();
        int height = (int) logo.getHeight();

        assertEquals(0, pixels.getColor(0, 0).getOpacity(), "the top-left corner is still opaque");
        assertEquals(0, pixels.getColor(width - 1, 0).getOpacity(), "the top-right corner is still opaque");
        assertEquals(0, pixels.getColor(0, height - 1).getOpacity(), "the bottom-left corner is still opaque");
        assertEquals(0, pixels.getColor(width - 1, height - 1).getOpacity(),
                "the bottom-right corner is still opaque");
    }

    /**
     * The reason the removal is a flood fill from the border rather than "clear every white
     * pixel". The logo has white in it -- a lit mirror tile, the highlight on the note head --
     * and keying on colour alone punches holes through both. Only background connected to the
     * edge is background, and this counts what a colour key would have destroyed.
     */
    @Test
    void theWhiteInsideTheArtworkSurvives() {
        Image logo = onFxThreadGet(Logo::image);
        PixelReader pixels = logo.getPixelReader();

        long paleAndKept = 0;
        for (int y = 0; y < (int) logo.getHeight(); y += 2) {
            for (int x = 0; x < (int) logo.getWidth(); x += 2) {
                Color colour = pixels.getColor(x, y);
                if (colour.getOpacity() > 0.9 && colour.getRed() >= PALE
                        && colour.getGreen() >= PALE && colour.getBlue() >= PALE) {
                    paleAndKept++;
                }
            }
        }
        assertTrue(paleAndKept > 0,
                "every pale pixel was cleared, so this is a colour key and the highlights "
                        + "inside the artwork have been punched out");
    }

    /** The artwork itself has to survive: a fill that ran away would clear the whole image. */
    @Test
    void theArtworkItselfIsStillThere() {
        Image logo = onFxThreadGet(Logo::image);
        PixelReader pixels = logo.getPixelReader();

        long opaque = 0;
        for (int y = 0; y < (int) logo.getHeight(); y += 4) {
            for (int x = 0; x < (int) logo.getWidth(); x += 4) {
                if (pixels.getColor(x, y).getOpacity() > 0.9) {
                    opaque++;
                }
            }
        }
        assertTrue(opaque > 1000, "the fill cleared the logo as well as its background");
    }

    private static <T> T onFxThreadGet(java.util.function.Supplier<T> supplier) {
        java.util.List<T> box = new java.util.ArrayList<>(1);
        onFxThread(() -> box.add(supplier.get()));
        return box.get(0);
    }
}
