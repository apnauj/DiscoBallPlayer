package com.discoballplayer.util;

/**
 * Formats durations for display.
 */
public final class TimeFormatter {

    private TimeFormatter() {
    }

    /**
     * Formats a duration as {@code m:ss}, clamping negatives to zero.
     */
    public static String mmss(int seconds) {
        int safe = Math.max(seconds, 0);
        return String.format("%d:%02d", safe / 60, safe % 60);
    }
}
