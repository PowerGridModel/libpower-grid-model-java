package org.lfenergy.pgm.loader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.lfenergy.pgm.PowerGridModelC;

final class ResourceLibraryLoader {

    private final Dependencies dependencies;

    ResourceLibraryLoader() {
        this(
            new Dependencies() {

                @Override
                public void loadNativeLibrary(final String nativeLibraryPath) {
                    System.load(nativeLibraryPath);
                }

                @Override
                public URL openResource(String path) {
                    return PowerGridModelC.class.getClassLoader().getResource(path);
                }

                @Override
                public boolean resourceIsFile(URL resourceUrl) {
                    return "file".equals(resourceUrl.getProtocol());
                }

                @Override
                public Path copyIntoTemporaryFile(final InputStream from, final String prefix, final String suffix) throws IOException {

                    Path tempFile = Files.createTempFile(prefix, suffix);
                    tempFile.toFile().deleteOnExit();

                    Files.copy(from, tempFile, StandardCopyOption.REPLACE_EXISTING);

                    return tempFile;
                }
            }
        );
    }

    ResourceLibraryLoader(
        final Dependencies dependencies
    ) {
        this.dependencies = dependencies;
    }

    /**
     * Looks up the native library at the provided path, and loads it using {@link System#load(String)}.
     * <p>
     * If the resource URL shows the native library is available as file, it loads the native library directly from that file. In other cases, it
     * first copies the native library to a temporary file, creates a shutdown hook to delete it on shutdown, then loads the file from the temporary
     * file path.
     * <p>
     * @throws PGMLoaderException if the resource file containing the native library cannot be found or opened, or if something unexpected happens
     *                            when copying the native library to a temporary file.
     */
    void loadResourceLibrary(final String resourceLibraryPath) {

        Path libraryPath = provideLoadableLibraryFromResources(resourceLibraryPath);
        dependencies.loadNativeLibrary(libraryPath.toString());
    }

    /**
     * Looks up the native library at the provided path, and ensures it is available for loading. Returns a path to a file containing the native
     * library that can be loaded by {@link System#load(String)}.
     * <p>
     * If the resource URL shows the native library is available as file, simply returns the path of the file. In other cases, it copies the native
     * library to a temporary file, and creates a shutdown hook to delete it on shutdown.
     * <p>
     * @return a path to a file containing the native library that can be loaded by {@link System#load(String)}.
     * @throws PGMLoaderException if the resource file containing the native library cannot be found or opened, or if something unexpected happens
     *                            when copying the native library to a temporary file.
     */
    private Path provideLoadableLibraryFromResources(final String resourceLibraryPath) {

        URL resourceUrl = dependencies.openResource(resourceLibraryPath);
        if (resourceUrl == null) {
            throw new PGMLoaderException("Failed to find packaged native library");
        }

        try {
            // Shortcut for situation common in local development (no jar-file)
            if (dependencies.resourceIsFile(resourceUrl)) {
                return Path.of(resourceUrl.toURI());
            }

            try (InputStream in = new BufferedInputStream(resourceUrl.openStream())) {
                return dependencies.copyIntoTemporaryFile(
                    in,
                    "pgm-native-",
                    resourceLibraryPath.replace('.', '-')
                );
            }
        } catch (Exception e) {
            throw new PGMLoaderException("Failed to open packaged native library", e);
        }
    }

    /**
     * Interface that allows for mocking in unit tests.
     */
    interface Dependencies {
        void loadNativeLibrary(String path);
        URL openResource(String path);
        boolean resourceIsFile(URL resourceUrl);
        Path copyIntoTemporaryFile(InputStream from, final String prefix, final String suffix) throws IOException;
    }
}
