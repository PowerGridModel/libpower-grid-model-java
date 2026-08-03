// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package org.lfenergy.pgm.loader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.lfenergy.pgm.loader.Architecture.ARM64;
import static org.lfenergy.pgm.loader.OperatingSystem.WINDOWS;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.lfenergy.pgm.PowerGridModelC;

public class PGMLoader {

    private final PlatformDetector platformDetector;
    private final ResourceLibraryLoader resourceLibraryLoader;
    private final PGMInvoker pgmInvoker;

    // Thread-safe state
    private final AtomicReference<String> pgmBuildVersion = new AtomicReference<>();
    private final AtomicReference<String> pgmRuntimeVersion = new AtomicReference<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    public PGMLoader() {

        this(new PlatformDetector(), new ResourceLibraryLoader(), PowerGridModelC::PGM_version);
    }

    PGMLoader(PlatformDetector platformDetector,
        ResourceLibraryLoader resourceLibraryLoader,
        PGMInvoker pgmInvoker) {

        this.platformDetector = platformDetector;
        this.resourceLibraryLoader = resourceLibraryLoader;
        this.pgmInvoker = pgmInvoker;
    }

    /**
     * Resolves and loads the Power Grid Model native library exactly once,
     * regardless of how many times this method is called. Subsequent calls are
     * no-ops and return immediately.
     * <p>
     * This is a convenience alternative to the explicit two-step pattern:
     * <pre>{@code
     * String path = NativeLibraryResolver.resolveNativeLibraryPath(PowerGridModelC.class);
     * System.load(path);
     * }</pre>
     *
     * @throws IllegalStateException if the native library cannot be found
     * @throws RuntimeException      if the library resource cannot be extracted
     */
    public void autoload() {

        if (loaded.compareAndSet(false, true)) {
            // Detect platform
            final Platform platform = platformDetector.detectPlatform();

            // Determine resource path for library
            final String libraryResourcePath = determineLibraryResourcePath(platform);

            // Load library
            resourceLibraryLoader.loadResourceLibrary(libraryResourcePath);

            // Ensure it was loaded correctly
            check(true);
        }
    }

    /**
     * Checks if the native PGM library is loaded. Optionally also checks if the build version equals the runtime version.
     *
     * @param matchVersion when set to true, an exception is thrown when the PGM is loaded, but the build and runtime versions don't match
     *
     * @throws PGMLoaderException thrown when the native PGM library is not loaded
     * @throws VersionMismatchException thrown when {@code matchVersion} is true and the build and runtime versions don't match
     */
    public void check(boolean matchVersion) {

        final String buildVersion = getPGMBuildVersion();
        final String runtimeVersion = getPGMRuntimeVersion();

        if (matchVersion && !runtimeVersion.equals(buildVersion)) {
            throw new VersionMismatchException(runtimeVersion, buildVersion);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public String getPGMRuntimeVersion() {

        if (pgmRuntimeVersion.get() != null) {
            return pgmRuntimeVersion.get();
        }

        try {
            pgmRuntimeVersion.set(pgmInvoker.PGM_version().getString(0, UTF_8));
        } catch (Throwable t) {
            throw new PGMLoaderException("PGM library is not loaded", t);
        }

        return pgmRuntimeVersion.get();
    }

    public String getPGMBuildVersion() {

        if (pgmBuildVersion.get() != null) {
            return pgmBuildVersion.get();
        }

        final InputStream ris = getClass().getResourceAsStream("/version");
        if (ris == null) {
            throw new PGMLoaderException("Failed to fetch build version from resource file");
        }

        try (InputStream is = new BufferedInputStream(ris)) {
            pgmBuildVersion.set(new String(is.readAllBytes(), UTF_8));
        } catch (IOException e) {
            throw new PGMLoaderException("Failed to fetch build version from resource file", e);
        }

        return pgmBuildVersion.get();
    }

    private String determineLibraryResourcePath(Platform platform) {

        final String archPart = switch (platform.architecture()) {
            case ARM64 -> "arm64";
            case X86_64 -> "x86_64";
            default -> throw new PGMLoaderException("Unsupported CPU architecture");
        };

        final String osPart = switch (platform.operatingSystem()) {
            case MACOS -> "macosx.dylib";
            case LINUX -> "linux.so";
            case WINDOWS -> "windows.dll";
            default -> throw new PGMLoaderException("Unsupported CPU architecture");
        };

        if (platform.architecture() == ARM64 && platform.operatingSystem() == WINDOWS) {
            throw new PGMLoaderException("Windows ARM64 is not supported");
        }

        return String.format("power_grid_model_c_%s_%s", archPart, osPart);
    }

    /**
     * Interface that allows for mocking in unit tests.
     */
    @SuppressWarnings({"checkstyle:MethodName", "PMD.MethodNamingConventions"})
    @FunctionalInterface
    interface PGMInvoker {

        MemorySegment PGM_version();
    }
}
