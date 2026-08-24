package com.discoballplayer.exception;

/**
 * Thrown when an operation needs an element and the structure has none.
 *
 * <p>Structures signal emptiness by throwing this, never by returning {@code null}, so a
 * caller cannot silently propagate a missing value into the playback layer.</p>
 */
public class EmptyStructureException extends RuntimeException {

    public EmptyStructureException(String message) {
        super(message);
    }
}
