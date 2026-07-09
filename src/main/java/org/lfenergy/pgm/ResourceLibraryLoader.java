package org.lfenergy.pgm;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.lfenergy.pgm.libraryloader.TemporaryFileHandler;

public final class ResourceLibraryLoader {

    private final Consumer<String> systemLoader;
    private final TemporaryFileHandler temporaryFileHandler;

    public ResourceLibraryLoader(
        final Consumer<String> systemLoader,
        final TemporaryFileHandler temporaryFileHandler
    ) {
        this.systemLoader = systemLoader;
        this.temporaryFileHandler = temporaryFileHandler;
    }

    public void loadResourceLibrary(final String resourceLibraryPath) {

        Path libraryPath = resolveResourceLibraryPath(resourceLibraryPath);
        systemLoader.accept(libraryPath.toString());
    }

    private Path resolveResourceLibraryPath(final String resourceLibraryPath) {

        ClassLoader classLoader = PowerGridModelC.class.getClassLoader();

        URL resourceUrl = classLoader.getResource(resourceLibraryPath);
        if (resourceUrl == null) {
            // todo throw
            return null;
        }

        try {
            if ("file".equals(resourceUrl.getProtocol())) {
                return Path.of(resourceUrl.toURI());
            }

            try (InputStream in = classLoader.getResourceAsStream(resourceLibraryPath)) {
                if (in == null) {
                    // todo throw
                    return null;
                }

                return temporaryFileHandler.copyIntoTemporaryFile(
                    in,
                    "pgm-native-",
                    resourceLibraryPath.replace('.', '-')
                );
            }
        } catch (Exception e) {
            // todo: custom exception
            throw new RuntimeException("Unable to resolve native library resource: " + resourceLibraryPath, e);
        }
    }
}
