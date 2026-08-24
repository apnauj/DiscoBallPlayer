package com.discoballplayer.exception;

/**
 * Thrown when a song is looked up in the library and is not there.
 */
public class SongNotFoundException extends RuntimeException {

    public SongNotFoundException(String message) {
        super(message);
    }
}
