package com.discoballplayer.exception;

/**
 * Thrown when the library cannot be read from or written to disk.
 *
 * <p>Wraps the underlying I/O or parse failure so callers see one exception type and a message
 * naming the file, rather than a Jackson stack trace they cannot act on.</p>
 */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
