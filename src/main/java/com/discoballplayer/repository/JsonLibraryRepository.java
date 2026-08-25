package com.discoballplayer.repository;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.discoballplayer.exception.PersistenceException;
import com.discoballplayer.model.MusicLibrary;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Stores the catalogue as JSON under the user's home directory.
 *
 * <p>Audio and cover files are referenced by absolute path and never copied here; this file
 * holds metadata only.</p>
 */
public class JsonLibraryRepository implements LibraryRepository {

    private static final String DEFAULT_DIRECTORY = ".discoballplayer";
    private static final String DEFAULT_FILE = "library.json";

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            // A file written by a newer build must still open in an older one. Refusing to
            // load because of a field this version does not know would lose the user their
            // whole library over an addition that does not concern it.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public JsonLibraryRepository() {
        this(Path.of(System.getProperty("user.home"), DEFAULT_DIRECTORY, DEFAULT_FILE));
    }

    public JsonLibraryRepository(Path file) {
        this.file = file;
    }

    /**
     * @return the stored library, or an empty one if nothing has been saved yet
     * @throws PersistenceException if the file exists but cannot be read or parsed
     */
    @Override
    public MusicLibrary load() {
        if (!Files.exists(file)) {
            return new MusicLibrary();
        }
        try {
            LibraryDto dto = mapper.readValue(Files.readString(file), LibraryDto.class);
            return LibraryMapper.toModel(dto);
        } catch (IOException | RuntimeException failure) {
            throw new PersistenceException(
                    "Could not read the library from " + file + ". "
                            + "The file may be corrupt; move it aside to start fresh.", failure);
        }
    }

    /**
     * Writes the library, replacing any previous contents.
     *
     * <p>Serialized to a temporary file first and then moved into place. Writing directly would
     * leave a half-written catalogue behind if the application died mid-save, and the user's
     * whole library is in this one file.</p>
     *
     * @throws PersistenceException if the file cannot be written
     */
    @Override
    public void save(MusicLibrary library) {
        try {
            Path directory = file.toAbsolutePath().getParent();
            Files.createDirectories(directory);

            Path temporary = Files.createTempFile(directory, "library", ".json.tmp");
            Files.writeString(temporary, mapper.writeValueAsString(LibraryMapper.toDto(library)));
            moveIntoPlace(temporary);
        } catch (IOException failure) {
            throw new PersistenceException("Could not save the library to " + file, failure);
        }
    }

    /** Atomic where the filesystem supports it, plain replace where it does not. */
    private void moveIntoPlace(Path temporary) throws IOException {
        try {
            Files.move(temporary, file,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
