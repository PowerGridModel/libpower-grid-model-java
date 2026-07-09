package org.lfenergy.pgm.libraryloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@FunctionalInterface
public interface TemporaryFileHandler {
    Path copyIntoTemporaryFile(InputStream from, final String prefix, final String suffix) throws IOException;

    static TemporaryFileHandler defaultHandler() {
        return (from, tempFilePrefix, tempFileSuffix) -> {
            Path tempFile = Files.createTempFile(tempFilePrefix, tempFileSuffix);
            tempFile.toFile().deleteOnExit();

            Files.copy(from, tempFile, StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        };
    }
}
